package com.obe.engine;

import com.obe.common.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Level2CalculatorTest {

    // Single objective → single indicator, weight=1.0, C̄_j=0.8 → E_k=0.8
    @Test
    @DisplayName("单目标单指标点→E_k=C̄_j")
    void singleObjectiveSingleIndicator() {
        Map<Long, BigDecimal> objAchievements = Map.of(1L, new BigDecimal("0.8000"));
        List<Level2Calculator.WeightRecord> weights = List.of(
                new Level2Calculator.WeightRecord(1L, 10L, BigDecimal.ONE)
        );

        Map<Long, BigDecimal> result = Level2Calculator.calcCourseAchievement(objAchievements, weights);
        assertEquals(1, result.size());
        assertEquals(0, new BigDecimal("0.8000").compareTo(result.get(10L)));
    }

    // Two objectives → one indicator: weighted sum
    @Test
    @DisplayName("两目标加权→加权求和")
    void twoObjectivesWeighted() {
        Map<Long, BigDecimal> objAchievements = Map.of(
                1L, new BigDecimal("0.8000"),
                2L, new BigDecimal("0.6000")
        );
        List<Level2Calculator.WeightRecord> weights = List.of(
                new Level2Calculator.WeightRecord(1L, 10L, new BigDecimal("0.4")),
                new Level2Calculator.WeightRecord(2L, 10L, new BigDecimal("0.6"))
        );

        Map<Long, BigDecimal> result = Level2Calculator.calcCourseAchievement(objAchievements, weights);
        // E_k = 0.8*0.4 + 0.6*0.6 = 0.32 + 0.36 = 0.68
        assertEquals(0, new BigDecimal("0.6800").compareTo(result.get(10L)));
    }

    // One objective → two indicators: same C̄_j, different weights
    @Test
    @DisplayName("单目标两指标点→独立加权计算")
    void oneObjectiveTwoIndicators() {
        Map<Long, BigDecimal> objAchievements = Map.of(1L, new BigDecimal("0.7500"));
        List<Level2Calculator.WeightRecord> weights = List.of(
                new Level2Calculator.WeightRecord(1L, 10L, new BigDecimal("0.3")),
                new Level2Calculator.WeightRecord(1L, 20L, new BigDecimal("0.7"))
        );

        Map<Long, BigDecimal> result = Level2Calculator.calcCourseAchievement(objAchievements, weights);
        // E_10 = 0.75*0.3 = 0.225
        assertEquals(0, new BigDecimal("0.2250").compareTo(result.get(10L)));
        // E_20 = 0.75*0.7 = 0.525
        assertEquals(0, new BigDecimal("0.5250").compareTo(result.get(20L)));
    }

    // Weight sum not 1.0 → throws BizException
    @Test
    @DisplayName("权重和不等于1→抛出异常")
    void invalidWeightSum() {
        Map<Long, BigDecimal> objAchievements = Map.of(1L, new BigDecimal("0.8000"));
        List<Level2Calculator.WeightRecord> weights = List.of(
                new Level2Calculator.WeightRecord(1L, 10L, new BigDecimal("0.5"))
        );

        assertThrows(BizException.class,
                () -> Level2Calculator.calcCourseAchievement(objAchievements, weights));
    }

    // Weight sum within tolerance (0.99-1.01) → should pass
    @Test
    @DisplayName("权重和在容差范围内→通过校验")
    void weightWithinTolerance() {
        Map<Long, BigDecimal> objAchievements = Map.of(1L, new BigDecimal("0.8000"));
        List<Level2Calculator.WeightRecord> weights = List.of(
                new Level2Calculator.WeightRecord(1L, 10L, new BigDecimal("0.995"))
        );

        Map<Long, BigDecimal> result = Level2Calculator.calcCourseAchievement(objAchievements, weights);
        assertNotNull(result.get(10L));
    }

    // Missing objective achievement → treated as 0
    @Test
    @DisplayName("目标达成度缺失→按0处理")
    void missingObjectiveAchievement() {
        Map<Long, BigDecimal> objAchievements = Map.of(
                1L, new BigDecimal("0.8000")
                // objective 2 missing
        );
        List<Level2Calculator.WeightRecord> weights = List.of(
                new Level2Calculator.WeightRecord(1L, 10L, new BigDecimal("0.4")),
                new Level2Calculator.WeightRecord(2L, 10L, new BigDecimal("0.6"))
        );

        Map<Long, BigDecimal> result = Level2Calculator.calcCourseAchievement(objAchievements, weights);
        // 0.8*0.4 + 0*0.6 = 0.32
        assertEquals(0, new BigDecimal("0.3200").compareTo(result.get(10L)));
    }

    // Empty weights → empty result, no exception
    @Test
    @DisplayName("空权重列表→空结果")
    void emptyWeights() {
        Map<Long, BigDecimal> objAchievements = Map.of(1L, new BigDecimal("0.8000"));
        List<Level2Calculator.WeightRecord> weights = List.of();

        Map<Long, BigDecimal> result = Level2Calculator.calcCourseAchievement(objAchievements, weights);
        assertTrue(result.isEmpty());
    }
}
