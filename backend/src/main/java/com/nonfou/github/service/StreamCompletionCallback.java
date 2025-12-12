package com.nonfou.github.service;

/**
 * 流式请求完成回调接口
 * 用于在 SSE 流式请求完成后进行计费等后续处理
 */
@FunctionalInterface
public interface StreamCompletionCallback {

    /**
     * 流式请求完成时回调
     *
     * @param inputTokens  输入 token 数（如果无法获取则为估算值）
     * @param outputTokens 输出 token 数（如果无法获取则为估算值）
     * @param success      请求是否成功
     * @param errorMessage 错误信息（成功时为 null）
     */
    void onComplete(int inputTokens, int outputTokens, boolean success, String errorMessage);
}
