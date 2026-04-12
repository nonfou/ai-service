package com.nonfou.github.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 启动时执行 SQLite 版本化迁移脚本。
 */
@Component
public class DatabaseMigrationRunner implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    private static final String CREATE_MIGRATION_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS schema_migrations (
                version TEXT PRIMARY KEY,
                description TEXT NOT NULL,
                script_name TEXT NOT NULL UNIQUE,
                executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private static final String QUERY_APPLIED_MIGRATIONS_SQL = """
            SELECT version
            FROM schema_migrations
            """;

    private static final String INSERT_MIGRATION_SQL = """
            INSERT INTO schema_migrations (version, description, script_name)
            VALUES (?, ?, ?)
            """;

    private static final Pattern SCRIPT_NAME_PATTERN = Pattern.compile("^V(?<version>.+?)__(?<description>.+)\\.sql$");

    private final DataSource dataSource;
    private final DatabaseMigrationProperties properties;
    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    public DatabaseMigrationRunner(DataSource dataSource, DatabaseMigrationProperties properties) {
        this.dataSource = dataSource;
        this.properties = properties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!properties.isEnabled()) {
            log.info("数据库自动迁移已禁用");
            return;
        }

        List<MigrationScript> migrationScripts = loadMigrationScripts();
        if (migrationScripts.isEmpty()) {
            log.info("未发现数据库迁移脚本，跳过自动迁移");
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            ensureMigrationTable(connection);
            Set<String> appliedVersions = loadAppliedVersions(connection);
            int executedCount = 0;

            for (MigrationScript script : migrationScripts) {
                if (appliedVersions.contains(script.version())) {
                    continue;
                }

                executeMigration(connection, script);
                appliedVersions.add(script.version());
                executedCount++;
            }

            if (executedCount == 0) {
                log.info("数据库迁移检查完成，当前无待执行脚本");
            } else {
                log.info("数据库迁移完成，共执行 {} 个脚本", executedCount);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("数据库迁移失败，无法建立数据库连接", ex);
        }
    }

    private List<MigrationScript> loadMigrationScripts() {
        try {
            Resource[] resources = resourcePatternResolver.getResources(properties.getLocationPattern());
            Set<String> versions = new HashSet<>();

            return Arrays.stream(resources)
                    .filter(Resource::exists)
                    .sorted(Comparator.comparing(resource -> resource.getFilename() == null ? "" : resource.getFilename()))
                    .map(this::toMigrationScript)
                    .peek(script -> {
                        if (!versions.add(script.version())) {
                            throw new IllegalStateException("存在重复的数据库迁移版本: " + script.version());
                        }
                    })
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("读取数据库迁移脚本失败", ex);
        }
    }

    private MigrationScript toMigrationScript(Resource resource) {
        String fileName = resource.getFilename();
        if (fileName == null) {
            throw new IllegalStateException("发现未命名的数据库迁移脚本");
        }

        Matcher matcher = SCRIPT_NAME_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            throw new IllegalStateException("数据库迁移脚本命名不合法: " + fileName);
        }

        String version = matcher.group("version");
        String description = matcher.group("description").replace('_', ' ');
        return new MigrationScript(version, description, fileName, resource);
    }

    private void ensureMigrationTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_MIGRATION_TABLE_SQL);
        }
    }

    private Set<String> loadAppliedVersions(Connection connection) throws SQLException {
        Set<String> versions = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(QUERY_APPLIED_MIGRATIONS_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                versions.add(resultSet.getString("version"));
            }
        }
        return versions;
    }

    private void executeMigration(Connection connection, MigrationScript script) {
        boolean originalAutoCommit = true;
        try {
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
        } catch (SQLException ex) {
            throw new IllegalStateException("初始化数据库迁移事务失败: " + script.fileName(), ex);
        }

        try {
            log.info("开始执行数据库迁移: {}", script.fileName());
            ScriptUtils.executeSqlScript(connection, new EncodedResource(script.resource(), StandardCharsets.UTF_8));

            try (PreparedStatement statement = connection.prepareStatement(INSERT_MIGRATION_SQL)) {
                statement.setString(1, script.version());
                statement.setString(2, script.description());
                statement.setString(3, script.fileName());
                statement.executeUpdate();
            }

            connection.commit();
            log.info("数据库迁移执行成功: {}", script.fileName());
        } catch (Exception ex) {
            rollbackQuietly(connection, script.fileName());
            throw new IllegalStateException("执行数据库迁移脚本失败: " + script.fileName(), ex);
        } finally {
            restoreAutoCommit(connection, originalAutoCommit, script.fileName());
        }
    }

    private void rollbackQuietly(Connection connection, String fileName) {
        try {
            connection.rollback();
        } catch (SQLException rollbackEx) {
            log.error("数据库迁移回滚失败: {}", fileName, rollbackEx);
        }
    }

    private void restoreAutoCommit(Connection connection, boolean originalAutoCommit, String fileName) {
        try {
            connection.setAutoCommit(originalAutoCommit);
        } catch (SQLException ex) {
            throw new IllegalStateException("恢复数据库连接状态失败: " + fileName, ex);
        }
    }

    private record MigrationScript(String version, String description, String fileName, Resource resource) {
    }
}
