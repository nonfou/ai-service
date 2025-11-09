package com.nonfou.github.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nonfou.github.entity.ApiCall;
import com.nonfou.github.entity.BalanceLog;
import com.nonfou.github.entity.Model;
import com.nonfou.github.entity.User;
import com.nonfou.github.mapper.ApiCallMapper;
import com.nonfou.github.mapper.BalanceLogMapper;
import com.nonfou.github.mapper.ModelMapper;
import com.nonfou.github.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 余额管理服务
 */
@Slf4j
@Service
public class BalanceService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BalanceLogMapper balanceLogMapper;

    @Autowired
    private ApiCallMapper apiCallMapper;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 计算费用（支持模型倍率）
     * @param modelName 模型名称
     * @param inputTokens 输入 tokens
     * @param outputTokens 输出 tokens
     * @return 费用（元）
     */
    public BigDecimal calculateCost(String modelName, int inputTokens, int outputTokens) {
        // 获取基础价格配置
        BigDecimal inputPrice = systemConfigService.getBigDecimal("input_token_price"); // 4.1 元/百万
        BigDecimal outputPrice = systemConfigService.getBigDecimal("output_token_price"); // 16.4 元/百万

        // 获取模型倍率
        BigDecimal multiplier = getModelMultiplier(modelName);

        // 计算费用
        BigDecimal inputCost = new BigDecimal(inputTokens)
                .multiply(inputPrice)
                .divide(new BigDecimal("1000000"), 6, RoundingMode.HALF_UP);

        BigDecimal outputCost = new BigDecimal(outputTokens)
                .multiply(outputPrice)
                .divide(new BigDecimal("1000000"), 6, RoundingMode.HALF_UP);

        BigDecimal totalCost = inputCost.add(outputCost);

        // 应用倍率
        return totalCost.multiply(multiplier).setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * 获取模型倍率
     */
    private BigDecimal getModelMultiplier(String modelName) {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Model::getModelName, modelName);
        wrapper.eq(Model::getStatus, 1);
        Model model = modelMapper.selectOne(wrapper);

        if (model != null && model.getPriceMultiplier() != null) {
            log.info("模型倍率: model={}, multiplier={}", modelName, model.getPriceMultiplier());
            return model.getPriceMultiplier();
        }

        // 默认倍率为 1.0
        log.warn("未找到模型配置，使用默认倍率1.0: model={}", modelName);
        return BigDecimal.ONE;
    }

    /**
     * 扣除余额（带事务）
     * @param userId 用户ID
     * @param cost 费用
     * @param apiCallId API调用ID
     */
    @Transactional
    public void deductBalance(Long userId, BigDecimal cost, Long apiCallId) {
        // 查询用户（加行锁）
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查余额
        if (user.getBalance().compareTo(cost) < 0) {
            throw new RuntimeException("余额不足，请先充值");
        }

        // 扣除余额
        BigDecimal newBalance = user.getBalance().subtract(cost);
        user.setBalance(newBalance);
        userMapper.updateById(user);

        // 记录余额变动日志
        BalanceLog log = new BalanceLog();
        log.setUserId(userId);
        log.setAmount(cost.negate()); // 负数表示扣除
        log.setBalanceAfter(newBalance);
        log.setType("consume");
        log.setRelatedId(apiCallId);
        log.setRemark("API调用扣费");
        log.setCreatedAt(LocalDateTime.now());
        balanceLogMapper.insert(log);

        this.log.info("余额扣除成功: userId={}, cost={}, balance={}", userId, cost, newBalance);
    }

    /**
     * 充值余额（带事务）
     * @param userId 用户ID
     * @param amount 充值金额
     * @param orderId 订单ID
     */
    @Transactional
    public void rechargeBalance(Long userId, BigDecimal amount, Long orderId) {
        // 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 增加余额
        BigDecimal newBalance = user.getBalance().add(amount);
        user.setBalance(newBalance);
        userMapper.updateById(user);

        // 记录余额变动日志
        BalanceLog log = new BalanceLog();
        log.setUserId(userId);
        log.setAmount(amount);
        log.setBalanceAfter(newBalance);
        log.setType("recharge");
        log.setRelatedId(orderId);
        log.setRemark("在线充值");
        log.setCreatedAt(LocalDateTime.now());
        balanceLogMapper.insert(log);

        this.log.info("余额充值成功: userId={}, amount={}, balance={}", userId, amount, newBalance);
    }

    /**
     * 记录 API 调用
     */
    @Transactional
    public Long logApiCall(Long userId, String apiKey, String model,
                           int inputTokens, int outputTokens, BigDecimal cost,
                           LocalDateTime requestTime, LocalDateTime responseTime,
                           Integer duration, Integer status, String errorMsg) {
        ApiCall apiCall = new ApiCall();
        apiCall.setUserId(userId);
        apiCall.setApiKey(apiKey);
        apiCall.setModel(model);
        apiCall.setInputTokens(inputTokens);
        apiCall.setOutputTokens(outputTokens);
        apiCall.setCost(cost);
        apiCall.setRequestTime(requestTime);
        apiCall.setResponseTime(responseTime);
        apiCall.setDuration(duration);
        apiCall.setStatus(status);
        apiCall.setErrorMsg(errorMsg);
        apiCall.setCreatedAt(LocalDateTime.now());

        apiCallMapper.insert(apiCall);
        return apiCall.getId();
    }

    /**
     * 根据 API Key 查询用户
     */
    public User getUserByApiKey(String apiKey) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getApiKey, apiKey);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 获取用户余额
     */
    public BigDecimal getUserBalance(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getBalance() : BigDecimal.ZERO;
    }

    /**
     * 增加余额（通用方法）
     */
    @Transactional
    public void addBalance(Long userId, BigDecimal amount, String type, Long relatedId, String remark) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 增加余额
        BigDecimal newBalance = user.getBalance().add(amount);
        user.setBalance(newBalance);
        userMapper.updateById(user);

        // 记录余额变动日志
        BalanceLog log = new BalanceLog();
        log.setUserId(userId);
        log.setAmount(amount);
        log.setBalanceAfter(newBalance);
        log.setType(type);
        log.setRelatedId(relatedId);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        balanceLogMapper.insert(log);

        this.log.info("余额增加成功: userId={}, amount={}, balance={}, type={}", userId, amount, newBalance, type);
    }

    /**
     * 扣除余额（通用方法）
     */
    @Transactional
    public void deductBalance(Long userId, BigDecimal amount, String type, Long relatedId, String remark) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查余额
        if (user.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("余额不足");
        }

        // 扣除余额
        BigDecimal newBalance = user.getBalance().subtract(amount);
        user.setBalance(newBalance);
        userMapper.updateById(user);

        // 记录余额变动日志
        BalanceLog log = new BalanceLog();
        log.setUserId(userId);
        log.setAmount(amount.negate()); // 负数表示扣除
        log.setBalanceAfter(newBalance);
        log.setType(type);
        log.setRelatedId(relatedId);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        balanceLogMapper.insert(log);

        this.log.info("余额扣除成功: userId={}, amount={}, balance={}, type={}", userId, amount, newBalance, type);
    }

    /**
     * 根据用户ID查询用户
     */
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }
}
