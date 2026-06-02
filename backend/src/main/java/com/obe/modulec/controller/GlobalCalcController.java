package com.obe.modulec.controller;

import com.obe.common.Result;
import com.obe.modulec.service.GlobalCalcService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/global")
@RequiredArgsConstructor
public class GlobalCalcController {

    private final GlobalCalcService globalCalcService;

    /** 获取所有可用的入学年份（年级） */
    @GetMapping("/enrollment-years")
    @PreAuthorize("hasAnyRole('DIRECTOR','ACADEMIC')")
    public Result<List<Integer>> getEnrollmentYears() {
        return Result.ok(globalCalcService.getEnrollmentYears());
    }

    /** 宏观看板 */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('DIRECTOR','ACADEMIC')")
    public Result<GlobalCalcService.DashboardData> dashboard(
            @RequestParam(required = false) Integer enrollmentYear,
            @RequestParam(required = false) Long majorId) {
        return Result.ok(globalCalcService.getDashboard(enrollmentYear, majorId));
    }

    /** 执行专业级计算 */
    @PostMapping("/compute")
    @PreAuthorize("hasAnyRole('DIRECTOR','ACADEMIC')")
    public Result<GlobalCalcService.MajorCalcResult> compute(
            @RequestParam(required = false) Integer enrollmentYear,
            @RequestParam(required = false) Long majorId) {
        Long operator = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.ok(globalCalcService.compute(enrollmentYear, majorId, operator));
    }

    /** 获取已有计算结果 */
    @GetMapping("/results")
    @PreAuthorize("hasAnyRole('DIRECTOR','ACADEMIC')")
    public Result<Map<Long, BigDecimal>> getResults(@RequestParam Long majorId,
                                                       @RequestParam Integer enrollmentYear) {
        return Result.ok(globalCalcService.getResults(majorId, enrollmentYear));
    }
}
