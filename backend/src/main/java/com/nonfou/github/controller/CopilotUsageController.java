package com.nonfou.github.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.nonfou.github.common.Result;
import com.nonfou.github.exception.BusinessException;
import com.nonfou.github.exception.ChatAuthorizationException;
import com.nonfou.github.exception.ChatProcessingException;
import com.nonfou.github.exception.ChatUpstreamException;
import com.nonfou.github.service.ChatWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Copilot usage 代理接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CopilotUsageController {

    private final ChatWorkflowService chatWorkflowService;

    @GetMapping("/usage")
    public Object getUsage(@RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            JsonNode response = chatWorkflowService.handleUsage(authorization);
            return response;
        } catch (ChatAuthorizationException e) {
            return Result.error(e.getStatusCode(), e.getMessage());
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (ChatUpstreamException e) {
            return Result.error(e.getStatusCode(), e.getMessage());
        } catch (ChatProcessingException e) {
            return Result.error(500, e.getMessage());
        } catch (Exception e) {
            log.error("获取 usage 失败", e);
            return Result.error("获取 usage 失败: " + e.getMessage());
        }
    }
}
