package com.obe.moduleb.controller;

import com.obe.common.Result;
import com.obe.modulea.entity.Indicator;
import com.obe.moduleb.entity.ObjectiveIndicatorWeight;
import com.obe.moduleb.service.WeightService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes/{classId}/weights")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class WeightController {

    private final WeightService weightService;

    @GetMapping
    public Result<List<ObjectiveIndicatorWeight>> list(@PathVariable Long classId) {
        return Result.ok(weightService.getWeights(classId));
    }

    @PutMapping
    public Result<Void> update(@PathVariable Long classId,
                               @RequestBody List<ObjectiveIndicatorWeight> weights) {
        weightService.updateWeights(classId, weights);
        return Result.ok();
    }

    @GetMapping("/supported-indicators")
    public Result<List<Indicator>> supportedIndicators(@PathVariable Long classId) {
        return Result.ok(weightService.getSupportedIndicators(classId));
    }
}
