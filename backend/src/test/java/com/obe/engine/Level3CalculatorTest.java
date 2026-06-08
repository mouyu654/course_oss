package com.obe.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Level3CalculatorTest {

    // Single course, single indicator → G_k = E_k * W_c
    @Test
    @DisplayName("单课程单指标点→直接加权")
    void singleCourseSingleIndicator() {
        Map<Long, Map<Long, BigDecimal>> courseAchievements = Map.of(
                1L, Map.of(10L, new BigDecimal("0.8000"))
        );
        List<Level3Calculator.MacroWeightRecord> macroWeights = List.of(
                new Level3Calculator.MacroWeightRecord(1L, 10L, new BigDecimal("0.4"))
        );

        Map<Long, BigDecimal> result = Level3Calculator.calcMajorAchievement(courseAchievements, macroWeights);
        // G_10 = 0.8 * 0.4 = 0.32
        assertEquals(0, new BigDecimal("0.3200").compareTo(result.get(10L)));
    }

    // Two courses → one indicator: weighted sum
    @Test
    @DisplayName("两课程同指标点→加权求和")
    void twoCoursesOneIndicator() {
        Map<Long, Map<Long, BigDecimal>> courseAchievements = Map.of(
                1L, Map.of(10L, new BigDecimal("0.8000")),
                2L, Map.of(10L, new BigDecimal("0.6000"))
        );
        List<Level3Calculator.MacroWeightRecord> macroWeights = List.of(
                new Level3Calculator.MacroWeightRecord(1L, 10L, new BigDecimal("0.4")),
                new Level3Calculator.MacroWeightRecord(2L, 10L, new BigDecimal("0.6"))
        );

        Map<Long, BigDecimal> result = Level3Calculator.calcMajorAchievement(courseAchievements, macroWeights);
        // G_10 = 0.8*0.4 + 0.6*0.6 = 0.32 + 0.36 = 0.68
        assertEquals(0, new BigDecimal("0.6800").compareTo(result.get(10L)));
    }

    // One course → two indicators
    @Test
    @DisplayName("单课程多指标点→分别计算")
    void oneCourseTwoIndicators() {
        Map<Long, Map<Long, BigDecimal>> courseAchievements = Map.of(
                1L, Map.of(
                        10L, new BigDecimal("0.8000"),
                        20L, new BigDecimal("0.6000")
                )
        );
        List<Level3Calculator.MacroWeightRecord> macroWeights = List.of(
                new Level3Calculator.MacroWeightRecord(1L, 10L, new BigDecimal("0.5")),
                new Level3Calculator.MacroWeightRecord(1L, 20L, new BigDecimal("0.5"))
        );

        Map<Long, BigDecimal> result = Level3Calculator.calcMajorAchievement(courseAchievements, macroWeights);
        assertEquals(0, new BigDecimal("0.4000").compareTo(result.get(10L)));
        assertEquals(0, new BigDecimal("0.3000").compareTo(result.get(20L)));
    }

    // Missing course achievements → skip
    @Test
    @DisplayName("课程达成度缺失→跳过")
    void missingCourseAchievement() {
        Map<Long, Map<Long, BigDecimal>> courseAchievements = Map.of(
                1L, Map.of(10L, new BigDecimal("0.8000"))
                // course 2 missing
        );
        List<Level3Calculator.MacroWeightRecord> macroWeights = List.of(
                new Level3Calculator.MacroWeightRecord(1L, 10L, new BigDecimal("0.4")),
                new Level3Calculator.MacroWeightRecord(2L, 10L, new BigDecimal("0.6"))
        );

        Map<Long, BigDecimal> result = Level3Calculator.calcMajorAchievement(courseAchievements, macroWeights);
        // Only course 1 contributes: 0.8*0.4 = 0.32
        assertEquals(0, new BigDecimal("0.3200").compareTo(result.get(10L)));
    }

    // Missing indicator achievement within course → skip
    @Test
    @DisplayName("指标点达成度缺失→跳过")
    void missingIndicatorAchievement() {
        Map<Long, Map<Long, BigDecimal>> courseAchievements = Map.of(
                1L, Map.of(10L, new BigDecimal("0.8000"))
                // indicator 20 not in course 1's results
        );
        List<Level3Calculator.MacroWeightRecord> macroWeights = List.of(
                new Level3Calculator.MacroWeightRecord(1L, 10L, new BigDecimal("0.5")),
                new Level3Calculator.MacroWeightRecord(1L, 20L, new BigDecimal("0.5"))
        );

        Map<Long, BigDecimal> result = Level3Calculator.calcMajorAchievement(courseAchievements, macroWeights);
        // Only indicator 10 has result
        assertEquals(0, new BigDecimal("0.4000").compareTo(result.get(10L)));
        assertNull(result.get(20L));
    }

    // Empty inputs → empty result
    @Test
    @DisplayName("空输入→空结果")
    void emptyInputs() {
        Map<Long, Map<Long, BigDecimal>> courseAchievements = Collections.emptyMap();
        List<Level3Calculator.MacroWeightRecord> macroWeights = Collections.emptyList();

        Map<Long, BigDecimal> result = Level3Calculator.calcMajorAchievement(courseAchievements, macroWeights);
        assertTrue(result.isEmpty());
    }
}
