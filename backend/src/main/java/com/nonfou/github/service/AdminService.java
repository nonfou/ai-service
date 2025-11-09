package com.nonfou.github.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nonfou.github.entity.Admin;
import com.nonfou.github.entity.User;
import com.nonfou.github.mapper.AdminMapper;
import com.nonfou.github.mapper.UserMapper;
import com.nonfou.github.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 管理员服务
 */
@Slf4j
@Service
public class AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.nonfou.github.config.AdminSecurityConfig adminSecurityConfig;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 管理员登录
     */
    public Map<String, Object> login(String username, String password) {
        // 检查是否被锁定
        if (adminSecurityConfig.isEnableLoginLock()) {
            String lockKey = "admin:login:lock:" + username;
            String locked = redisTemplate.opsForValue().get(lockKey);
            if ("1".equals(locked)) {
                throw new RuntimeException("账号已被锁定,请" + adminSecurityConfig.getLockDuration() + "分钟后重试");
            }
        }

        // 查询管理员
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, username);
        Admin admin = adminMapper.selectOne(wrapper);

        if (admin == null) {
            recordLoginFailure(username);
            throw new RuntimeException("用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            recordLoginFailure(username);
            throw new RuntimeException("用户名或密码错误");
        }

        // 检查状态
        if (admin.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }

        // 登录成功,清除失败记录
        clearLoginFailure(username);

        // 生成token (管理员统一使用ADMIN角色,确保与Spring Security的hasRole("ADMIN")匹配)
        String token = jwtUtil.generateToken(admin.getId(), admin.getUsername(), "ADMIN");

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("adminId", admin.getId());
        result.put("username", admin.getUsername());
        result.put("role", admin.getRole());

        log.info("管理员登录成功: username={}", username);

        return result;
    }

    /**
     * 记录登录失败
     */
    private void recordLoginFailure(String username) {
        if (!adminSecurityConfig.isEnableLoginLock()) {
            return;
        }

        String failKey = "admin:login:fail:" + username;
        String lockKey = "admin:login:lock:" + username;

        // 递增失败次数
        Long failCount = redisTemplate.opsForValue().increment(failKey);
        if (failCount == 1) {
            redisTemplate.expire(failKey, adminSecurityConfig.getLockDuration(), TimeUnit.MINUTES);
        }

        // 超过最大次数,锁定账号
        if (failCount >= adminSecurityConfig.getMaxLoginAttempts()) {
            redisTemplate.opsForValue().set(lockKey, "1", adminSecurityConfig.getLockDuration(), TimeUnit.MINUTES);
            log.warn("管理员账号已锁定: username={}, 失败次数={}", username, failCount);
        }
    }

    /**
     * 清除登录失败记录
     */
    private void clearLoginFailure(String username) {
        if (!adminSecurityConfig.isEnableLoginLock()) {
            return;
        }

        String failKey = "admin:login:fail:" + username;
        String lockKey = "admin:login:lock:" + username;

        redisTemplate.delete(failKey);
        redisTemplate.delete(lockKey);
    }

    /**
     * 获取用户列表
     */
    public Page<User> getUserList(int pageNum, int pageSize, String email, Integer status) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (email != null && !email.isEmpty()) {
            wrapper.like(User::getEmail, email);
        }

        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }

        wrapper.orderByDesc(User::getCreatedAt);

        return userMapper.selectPage(page, wrapper);
    }

    /**
     * 获取用户详情
     */
    public User getUserDetail(Long userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 更新用户状态
     */
    @Transactional
    public void updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户状态更新: userId={}, status={}", userId, status);
    }

    /**
     * 调整用户余额
     */
    @Transactional
    public void adjustUserBalance(Long userId, BigDecimal amount, String remark) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            // 增加余额
            balanceService.addBalance(userId, amount, "admin_adjust", userId, remark);
        } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
            // 扣减余额
            balanceService.deductBalance(userId, amount.abs(), "admin_adjust", userId, remark);
        }

        log.info("管理员调整用户余额: userId={}, amount={}, remark={}", userId, amount, remark);
    }

    /**
     * 获取平台统计数据
     */
    public Map<String, Object> getPlatformStatistics() {
        // 用户总数
        Long totalUsers = userMapper.selectCount(null);

        // 活跃用户数(状态为1)
        LambdaQueryWrapper<User> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(User::getStatus, 1);
        Long activeUsers = userMapper.selectCount(activeWrapper);

        // 今日注册用户数
        LambdaQueryWrapper<User> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(User::getCreatedAt, LocalDateTime.now().toLocalDate().atStartOfDay());
        Long todayRegistrations = userMapper.selectCount(todayWrapper);

        // 用户总余额
        BigDecimal totalBalance = userMapper.selectList(null).stream()
                .map(User::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("todayNewUsers", todayRegistrations);
        stats.put("totalBalance", totalBalance);

        return stats;
    }

    /**
     * 重置管理员密码 (临时接口,仅用于开发测试)
     */
    public Map<String, Object> resetAdminPassword(String username, String newPassword) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, username);
        Admin admin = adminMapper.selectOne(wrapper);

        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }

        String hash = passwordEncoder.encode(newPassword);
        admin.setPassword(hash);
        admin.setUpdatedAt(LocalDateTime.now());
        adminMapper.updateById(admin);

        Map<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("passwordHash", hash);
        result.put("hashLength", hash.length());

        log.info("管理员密码已重置: username={}, hashLength={}", username, hash.length());

        return result;
    }
}
