package com.nonfou.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SQLite 数据库迁移配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "database.migration")
public class DatabaseMigrationProperties {

    /**
     * 是否启用启动时自动迁移。
     */
    private boolean enabled = true;

    /**
     * 迁移脚本匹配路径。
     */
    private String locationPattern = "classpath*:db/migration/sqlite/V*.sql";
}
