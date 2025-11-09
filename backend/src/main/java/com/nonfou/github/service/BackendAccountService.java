package com.nonfou.github.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.entity.BackendAccount;
import com.nonfou.github.mapper.BackendAccountMapper;
import com.nonfou.github.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后端账户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackendAccountService {

    private final BackendAccountMapper backendAccountMapper;
    private final EncryptionUtil encryptionUtil;

    /**
     * 创建后端账户
     */
    @Transactional
    public BackendAccount createAccount(BackendAccount account) {
        // 加密 access_token
        if (account.getAccessToken() != null && !account.getAccessToken().isEmpty()) {
            String encryptedToken = encryptionUtil.encrypt(account.getAccessToken());
            account.setAccessToken(encryptedToken);
        }

        // 设置默认值
        if (account.getPriority() == null) {
            account.setPriority(1);
        }
        if (account.getStatus() == null) {
            account.setStatus("active");
        }
        if (account.getErrorCount() == null) {
            account.setErrorCount(0);
        }

        backendAccountMapper.insert(account);
        log.info("创建后端账户成功: id={}, name={}, provider={}",
                account.getId(), account.getAccountName(), account.getProvider());
        return account;
    }

    /**
     * 更新后端账户
     */
    @Transactional
    public BackendAccount updateAccount(BackendAccount account) {
        BackendAccount existing = backendAccountMapper.selectById(account.getId());
        if (existing == null) {
            throw new RuntimeException("账户不存在: " + account.getId());
        }

        // 如果 access_token 被修改，需要重新加密
        if (account.getAccessToken() != null && !account.getAccessToken().equals(existing.getAccessToken())) {
            String encryptedToken = encryptionUtil.encrypt(account.getAccessToken());
            account.setAccessToken(encryptedToken);
        }

        backendAccountMapper.updateById(account);
        log.info("更新后端账户成功: id={}, name={}", account.getId(), account.getAccountName());
        return account;
    }

    /**
     * 删除后端账户
     */
    @Transactional
    public void deleteAccount(Long id) {
        backendAccountMapper.deleteById(id);
        log.info("删除后端账户成功: id={}", id);
    }

    /**
     * 根据ID查询账户
     */
    public BackendAccount getAccountById(Long id) {
        return backendAccountMapper.selectById(id);
    }

    /**
     * 获取解密后的 Access Token
     */
    public String getDecryptedToken(Long accountId) {
        BackendAccount account = getAccountById(accountId);
        if (account == null || account.getAccessToken() == null) {
            return null;
        }
        return encryptionUtil.decrypt(account.getAccessToken());
    }

    /**
     * 根据提供商查询所有激活的账户
     */
    public List<BackendAccount> getActiveAccountsByProvider(String provider) {
        return backendAccountMapper.selectActiveByProvider(provider);
    }

    /**
     * 根据提供商和状态查询账户
     */
    public List<BackendAccount> getAccountsByProviderAndStatus(String provider, String status) {
        return backendAccountMapper.selectByProviderAndStatus(provider, status);
    }

    /**
     * 分页查询账户列表
     */
    public Page<BackendAccount> getAccountPage(int current, int size, String provider, String status) {
        Page<BackendAccount> page = new Page<>(current, size);
        LambdaQueryWrapper<BackendAccount> wrapper = new LambdaQueryWrapper<>();

        if (provider != null && !provider.isEmpty()) {
            wrapper.eq(BackendAccount::getProvider, provider);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(BackendAccount::getStatus, status);
        }

        wrapper.orderByAsc(BackendAccount::getPriority)
               .orderByDesc(BackendAccount::getLastUsedAt);

        return backendAccountMapper.selectPage(page, wrapper);
    }

    /**
     * 更新账户最后使用时间
     */
    @Transactional
    public void updateLastUsedAt(Long id) {
        backendAccountMapper.updateLastUsedAt(id, LocalDateTime.now());
    }

    /**
     * 增加错误计数
     */
    @Transactional
    public void incrementErrorCount(Long id, String errorMessage) {
        backendAccountMapper.incrementErrorCount(id, LocalDateTime.now(), errorMessage);

        // 检查错误次数，如果超过阈值则禁用账户
        BackendAccount account = getAccountById(id);
        if (account != null && account.getErrorCount() >= 3) {
            account.setStatus("error");
            backendAccountMapper.updateById(account);
            log.warn("账户错误次数过多，已自动禁用: id={}, errorCount={}", id, account.getErrorCount());
        }
    }

    /**
     * 重置错误计数
     */
    @Transactional
    public void resetErrorCount(Long id) {
        backendAccountMapper.resetErrorCount(id);
        log.info("重置账户错误计数: id={}", id);
    }

    /**
     * 启用账户
     */
    @Transactional
    public void enableAccount(Long id) {
        BackendAccount account = getAccountById(id);
        if (account != null) {
            account.setStatus("active");
            account.setErrorCount(0);
            backendAccountMapper.updateById(account);
            log.info("启用账户: id={}", id);
        }
    }

    /**
     * 禁用账户
     */
    @Transactional
    public void disableAccount(Long id) {
        BackendAccount account = getAccountById(id);
        if (account != null) {
            account.setStatus("disabled");
            backendAccountMapper.updateById(account);
            log.info("禁用账户: id={}", id);
        }
    }

    /**
     * 健康检查
     * TODO: 实现具体的健康检查逻辑（如发送测试请求）
     */
    public boolean healthCheck(Long id) {
        BackendAccount account = getAccountById(id);
        if (account == null) {
            return false;
        }

        // 简单检查：账户是否处于激活状态且错误次数不超过3次
        boolean isHealthy = "active".equals(account.getStatus()) && account.getErrorCount() < 3;

        if (isHealthy) {
            resetErrorCount(id);
        }

        return isHealthy;
    }

    /**
     * 记录成功调用
     */
    @Transactional
    public void recordSuccess(Long id) {
        updateLastUsedAt(id);
        resetErrorCount(id);
    }

    /**
     * 记录错误调用
     */
    @Transactional
    public void recordError(Long id, String errorMessage) {
        incrementErrorCount(id, errorMessage);
    }

    /**
     * 解密 Token
     */
    public String decryptToken(String encryptedToken) {
        if (encryptedToken == null || encryptedToken.isEmpty()) {
            return null;
        }
        return encryptionUtil.decrypt(encryptedToken);
    }

    /**
     * getById 方法别名（兼容性）
     */
    public BackendAccount getById(Long id) {
        return getAccountById(id);
    }

    /**
     * 更新健康状态
     */
    @Transactional
    public void updateHealth(Long id, String status, String message) {
        BackendAccount account = getAccountById(id);
        if (account != null) {
            account.setStatus(status);
            account.setLastErrorMessage(message);
            if ("active".equals(status)) {
                account.setErrorCount(0);
            }
            backendAccountMapper.updateById(account);
            log.info("更新账户健康状态: id={}, status={}, message={}", id, status, message);
        }
    }
}
