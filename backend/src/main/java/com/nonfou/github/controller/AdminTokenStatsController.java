package com.nonfou.github.controller;

import com.nonfou.github.common.Result;
import com.nonfou.github.dto.request.AdminTokenUsageQueryRequest;
import com.nonfou.github.dto.response.AdminTokenDashboardResponse;
import com.nonfou.github.dto.response.AdminTokenUsageRecordPageResponse;
import com.nonfou.github.service.TokenUsageStatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 管理端 Token 统计接口。
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/token-stats")
public class AdminTokenStatsController {

    private static final DateTimeFormatter EXPORT_FILE_NAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final TokenUsageStatisticsService tokenUsageStatisticsService;

    @GetMapping("/dashboard")
    public Result<AdminTokenDashboardResponse> dashboard(@RequestParam(defaultValue = "7") Integer days) {
        return Result.success(tokenUsageStatisticsService.getDashboard(days));
    }

    @GetMapping("/records")
    public Result<AdminTokenUsageRecordPageResponse> records(
            @Valid @ModelAttribute AdminTokenUsageQueryRequest request
    ) {
        return Result.success(tokenUsageStatisticsService.getUsageRecords(request));
    }

    @GetMapping("/records/export")
    public ResponseEntity<byte[]> exportRecords(
            @Valid @ModelAttribute AdminTokenUsageQueryRequest request
    ) {
        String fileName = "token-usage-records-" + LocalDateTime.now().format(EXPORT_FILE_NAME_FORMATTER) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(tokenUsageStatisticsService.exportUsageRecords(request));
    }
}
