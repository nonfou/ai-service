package com.nonfou.github.service;

import com.nonfou.github.service.CostCalculatorService.TokenUsage;

/**
 * 流式请求完成回调接口
 * 用于在 SSE 流式请求完成后进行计费等后续处理
 */
@FunctionalInterface
public interface StreamCompletionCallback {

    /**
     * 流式请求完成时回调
     *
     * @param tokenUsage   Token 使用情况（包含输入、输出、缓存读取、缓存写入）
     * @param success      请求是否成功
     * @param errorMessage 错误信息（成功时为 null）
     */
    void onComplete(TokenUsage tokenUsage, boolean success, String errorMessage);
}
