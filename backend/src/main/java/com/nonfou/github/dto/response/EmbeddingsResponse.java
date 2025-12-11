package com.nonfou.github.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * OpenAI 兼容的 Embeddings 响应
 * 对应 POST /v1/embeddings 接口
 */
@Data
public class EmbeddingsResponse {

    /**
     * 对象类型，固定为 "list"
     */
    private String object = "list";

    /**
     * 嵌入向量数据列表
     */
    private List<Embedding> data;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * Token 使用统计
     */
    private Usage usage;

    @Data
    public static class Embedding {
        /**
         * 对象类型，固定为 "embedding"
         */
        private String object = "embedding";

        /**
         * 在输入列表中的索引
         */
        private Integer index;

        /**
         * 嵌入向量（float 格式时为浮点数列表）
         */
        private List<Float> embedding;
    }

    @Data
    public static class Usage {
        /**
         * 提示词 token 数
         */
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        /**
         * 总 token 数
         */
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}
