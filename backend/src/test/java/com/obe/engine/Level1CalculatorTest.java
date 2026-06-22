package com.obe.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Level1CalculatorTest {

    // Single student, single assessment, 100% score → achievement = 1.0
    @Test
    @DisplayName("单学生满分→达成度为1.0")
    void perfectScore() {
        List<Level1Calculator.StudentScoreRecord> scores = List.of(
                new Level1Calculator.StudentScoreRecord(1L, 10L, null, new BigDecimal("100"))
        );
        Map<Long, BigDecimal> maxMap = Map.of(10L, new BigDecimal("100"));
        Map<Long, List<Long>> objMap = Map.of(10L, List.of(1L));

        BigDecimal result = Level1Calculator.calcObjectiveAchievement(scores, 1L, maxMap, objMap);
        assertEquals(0, new BigDecimal("1.0000").compareTo(result));
    }

    // Single student, single assessment, 50% score → achievement = 0.5
    @Test
    @DisplayName("单学生50%得分→达成度为0.5")
    void halfScore() {
        List<Level1Calculator.StudentScoreRecord> scores = List.of(
                new Level1Calculator.StudentScoreRecord(1L, 10L, null, new BigDecimal("50"))
        );
        Map<Long, BigDecimal> maxMap = Map.of(10L, new BigDecimal("100"));
        Map<Long, List<Long>> objMap = Map.of(10L, List.of(1L));

        BigDecimal result = Level1Calculator.calcObjectiveAchievement(scores, 1L, maxMap, objMap);
        assertEquals(0, new BigDecimal("0.5000").compareTo(result));
    }

    // Two students: 100% and 50% → average = 0.75
    @Test
    @DisplayName("两学生平均→达成度为0.75")
    void twoStudentsAverage() {
        List<Level1Calculator.StudentScoreRecord> scores = List.of(
                new Level1Calculator.StudentScoreRecord(1L, 10L, null, new BigDecimal("100")),
                new Level1Calculator.StudentScoreRecord(2L, 10L, null, new BigDecimal("50"))
        );
        Map<Long, BigDecimal> maxMap = Map.of(10L, new BigDecimal("100"));
        Map<Long, List<Long>> objMap = Map.of(10L, List.of(1L));

        BigDecimal result = Level1Calculator.calcObjectiveAchievement(scores, 1L, maxMap, objMap);
        assertEquals(0, new BigDecimal("0.7500").compareTo(result));
    }

    // Multi-assessment binding: one assessment bound to 2 objectives
    @Test
    @DisplayName("多考核点绑定→得分汇总后计算")
    void multiAssessmentBinding() {
        // Student scored 20/40 on assessment 10 (bound to obj 1) and 30/40 on assessment 20 (also bound to obj 1)
        List<Level1Calculator.StudentScoreRecord> scores = List.of(
                new Level1Calculator.StudentScoreRecord(1L, 10L, null, new BigDecimal("20")),
                new Level1Calculator.StudentScoreRecord(1L, 20L, null, new BigDecimal("30"))
        );
        Map<Long, BigDecimal> maxMap = Map.of(
                10L, new BigDecimal("40"),
                20L, new BigDecimal("40")
        );
        Map<Long, List<Long>> objMap = Map.of(
                10L, List.of(1L, 2L),  // assessment 10 supports objectives 1 and 2
                20L, List.of(1L)        // assessment 20 only supports objective 1
        );

        // For objective 1: actual=20+30=50, max=40+40=80 → 50/80 = 0.625 → rounded 0.6250
        BigDecimal result = Level1Calculator.calcObjectiveAchievement(scores, 1L, maxMap, objMap);
        assertEquals(0, new BigDecimal("0.6250").compareTo(result));
    }

    // Zero denominator → returns 0
    @Test
    @DisplayName("考核点满分为0→达成度为0")
    void zeroMaxScore() {
        List<Level1Calculator.StudentScoreRecord> scores = List.of(
                new Level1Calculator.StudentScoreRecord(1L, 10L, null, BigDecimal.ZERO)
        );
        Map<Long, BigDecimal> maxMap = Map.of(10L, BigDecimal.ZERO);
        Map<Long, List<Long>> objMap = Map.of(10L, List.of(1L));

        BigDecimal result = Level1Calculator.calcObjectiveAchievement(scores, 1L, maxMap, objMap);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    // No assessment bound to objective → returns 0
    @Test
    @DisplayName("无考核点绑定目标→达成度为0")
    void noAssessmentForObjective() {
        List<Level1Calculator.StudentScoreRecord> scores = List.of(
                new Level1Calculator.StudentScoreRecord(1L, 10L, null, new BigDecimal("100"))
        );
        Map<Long, BigDecimal> maxMap = Map.of(10L, new BigDecimal("100"));
        Map<Long, List<Long>> objMap = Collections.emptyMap();

        BigDecimal result = Level1Calculator.calcObjectiveAchievement(scores, 1L, maxMap, objMap);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    // Empty scores → returns 0
    @Test
    @DisplayName("空成绩列表→达成度为0")
    void emptyScores() {
        Map<Long, BigDecimal> maxMap = Map.of(10L, new BigDecimal("100"));
        Map<Long, List<Long>> objMap = Map.of(10L, List.of(1L));

        BigDecimal result = Level1Calculator.calcObjectiveAchievement(
                Collections.emptyList(), 1L, maxMap, objMap);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    // Question-level granularity test
    @Test
    @DisplayName("题目级粒度→使用题目分数计算")
    void questionLevelGranularity() {
        // Assessment 10 has two questions: q1 (max 30) and q2 (max 70), both bound to objective 1
        // Student scored 15 on q1 and 70 on q2
        List<Level1Calculator.StudentScoreRecord> scores = List.of(
                new Level1Calculator.StudentScoreRecord(1L, 10L, 100L, new BigDecimal("15")),
                new Level1Calculator.StudentScoreRecord(1L, 10L, 101L, new BigDecimal("70"))
        );
        Map<Long, BigDecimal> assessmentMaxMap = Map.of(10L, new BigDecimal("100"));
        Map<Long, List<Long>> assessmentObjMap = Map.of(10L, List.of(1L));
        Map<Long, BigDecimal> questionMaxMap = Map.of(
                100L, new BigDecimal("30"),
                101L, new BigDecimal("70")
        );
        Map<Long, List<Long>> questionObjMap = Map.of(
                100L, List.of(1L),
                101L, List.of(1L)
        );

        BigDecimal result = Level1Calculator.calcObjectiveAchievement(
                scores, 1L, assessmentMaxMap, assessmentObjMap, questionMaxMap, questionObjMap);
        // actual=15+70=85, max=30+70=100 → 0.85
        assertEquals(0, new BigDecimal("0.8500").compareTo(result));
    }

    // Mixed: some assessments have questions, some don't
    @Test
    @DisplayName("混合粒度→有题目的用题目，无题目的用考核点")
    void mixedGranularity() {
        // Assessment 10 has question q1 supporting obj1; Assessment 20 has NO questions, supports obj1
        List<Level1Calculator.StudentScoreRecord> scores = List.of(
                new Level1Calculator.StudentScoreRecord(1L, 10L, 100L, new BigDecimal("25")),  // question level
                new Level1Calculator.StudentScoreRecord(1L, 20L, null, new BigDecimal("50"))   // assessment level
        );
        Map<Long, BigDecimal> assessmentMaxMap = Map.of(
                10L, new BigDecimal("50"),
                20L, new BigDecimal("50")
        );
        Map<Long, List<Long>> assessmentObjMap = Map.of(
                10L, List.of(1L),
                20L, List.of(1L)
        );
        Map<Long, BigDecimal> questionMaxMap = Map.of(100L, new BigDecimal("30"));
        Map<Long, List<Long>> questionObjMap = Map.of(100L, List.of(1L));

        BigDecimal result = Level1Calculator.calcObjectiveAchievement(
                scores, 1L, assessmentMaxMap, assessmentObjMap, questionMaxMap, questionObjMap);
        // For assessment 10: questions exist → use q1 only (25/30)
        // For assessment 20: no questions → use assessment-level (50/50)
        // Total: actual=25+50=75, max=30+50=80 → 75/80 = 0.9375
        assertEquals(0, new BigDecimal("0.9375").compareTo(result));
    }
}
