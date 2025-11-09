package com.nonfou.github.controller;

import com.nonfou.github.common.Result;
import com.nonfou.github.entity.Model;
import com.nonfou.github.service.ModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型管理 Controller (公开接口)
 */
@Slf4j
@RestController
@RequestMapping("/api/models")
public class ModelController {

    @Autowired
    private ModelService modelService;

    /**
     * 获取启用的模型列表 (公开接口,供用户端使用)
     */
    @GetMapping
    public Result<List<Model>> getActiveModels() {
        List<Model> models = modelService.getActiveModels();
        return Result.success(models);
    }

    /**
     * 获取模型详情 (公开接口)
     */
    @GetMapping("/{id}")
    public Result<Model> getModelById(@PathVariable Long id) {
        Model model = modelService.getModelById(id);
        if (model == null) {
            return Result.error("模型不存在");
        }
        return Result.success(model);
    }
}
