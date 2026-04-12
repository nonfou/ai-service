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
     * @param result 流式请求的完整结果
     */
    void onComplete(StreamCompletionResult result);
}
