package com.nonfou.github.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * OpenAI 兼容的模型列表响应
 * 对应 GET /v1/models 接口
 */
@Data
public class ModelsResponse {

    private String object = "list";

    private List<Model> data;

    @Data
    public static class Model {
        /**
         * 模型标识符
         */
        private String id;

        /**
         * 对象类型，固定为 "model"
         */
        private String object = "model";

        /**
         * 创建时间戳（Unix 秒）
         */
        private Long created;

        /**
         * 模型所有者
         */
        @JsonProperty("owned_by")
        private String ownedBy;
    }
}
