package com.nonfou.github.annotation;

import java.lang.annotation.*;

/**
 * 需要管理员权限的注解
 * 用于标记需要ADMIN角色才能访问的接口
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAdmin {
    /**
     * 是否必须是超级管理员
     */
    boolean superAdmin() default false;
}
