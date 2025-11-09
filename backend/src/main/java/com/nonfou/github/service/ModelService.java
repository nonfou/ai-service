package com.nonfou.github.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nonfou.github.entity.Model;
import com.nonfou.github.mapper.ModelMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模型管理服务
 */
@Slf4j
@Service
public class ModelService {

    @Autowired
    private ModelMapper modelMapper;

    /**
     * 获取所有模型列表
     */
    public List<Model> getAllModels() {
        return modelMapper.selectList(null);
    }

    /**
     * 获取启用的模型列表
     */
    public List<Model> getActiveModels() {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Model::getStatus, 1)
                .orderByAsc(Model::getSortOrder)
                .orderByAsc(Model::getId);

        return modelMapper.selectList(wrapper);
    }

    /**
     * 根据ID获取模型
     */
    public Model getModelById(Long id) {
        return modelMapper.selectById(id);
    }

    /**
     * 根据名称获取模型
     */
    public Model getModelByName(String modelName) {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Model::getModelName, modelName);
        return modelMapper.selectOne(wrapper);
    }

    /**
     * 更新模型
     */
    @Transactional
    public void updateModel(Long id, Model model) {
        Model existingModel = modelMapper.selectById(id);
        if (existingModel == null) {
            throw new RuntimeException("模型不存在");
        }

        existingModel.setModelName(model.getModelName());
        existingModel.setDisplayName(model.getDisplayName());
        existingModel.setProvider(model.getProvider());
        existingModel.setPriceMultiplier(model.getPriceMultiplier());
        existingModel.setStatus(model.getStatus());
        existingModel.setDescription(model.getDescription());
        existingModel.setTags(model.getTags());
        existingModel.setSortOrder(model.getSortOrder());
        existingModel.setUpdatedAt(LocalDateTime.now());

        modelMapper.updateById(existingModel);

        log.info("模型更新成功: id={}, modelName={}, tags={}, sortOrder={}",
                id, existingModel.getModelName(), existingModel.getTags(), existingModel.getSortOrder());
    }

    /**
     * 更新模型状态
     */
    @Transactional
    public void updateModelStatus(Long id, Integer status) {
        Model model = modelMapper.selectById(id);
        if (model == null) {
            throw new RuntimeException("模型不存在");
        }

        model.setStatus(status);
        model.setUpdatedAt(LocalDateTime.now());
        modelMapper.updateById(model);

        log.info("模型状态更新: id={}, status={}", id, status);
    }

    /**
     * 删除模型
     */
    @Transactional
    public void deleteModel(Long id) {
        Model model = modelMapper.selectById(id);
        if (model == null) {
            throw new RuntimeException("模型不存在");
        }

        modelMapper.deleteById(id);
        log.info("模型删除成功: id={}, modelName={}", id, model.getModelName());
    }
}
