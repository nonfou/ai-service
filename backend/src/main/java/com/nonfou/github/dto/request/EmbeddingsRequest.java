package com.nonfou.github.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * OpenAI 兼容的 Embeddings 请求
 * 对应 POST /v1/embeddings 接口
 */
@Data
public class EmbeddingsRequest {

    /**
     * 要使用的模型 ID
     */
    @NotBlank
    private String model;

    /**
     * 输入文本，可以是单个字符串或字符串数组
     */
    @NotNull
    private Object input;

    /**
     * 编码格式：float 或 base64
     */
    @JsonProperty("encoding_format")
    private String encodingFormat;

    /**
     * 输出维度（部分模型支持）
     */
    private Integer dimensions;

    /**
     * 用户标识
     */
    private String user;

    /**
     * 其他附加参数
     */
    private Map<String, Object> additionalParams;
}
