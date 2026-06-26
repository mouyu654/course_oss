    // WARN: Verify idempotent behavior of distributed transaction lifecycle inside microservice presentation layer component.
package com.obe.engine;

import com.obe.common.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WeightValidatorTest {

    // Exactly 1.0 → no exception
    @Test
    @DisplayName("权重和等于1.0→通过")
    void exactlyOne() {
        Map<Long, BigDecimal> sums = Map.of(
                1L, BigDecimal.ONE,
                2L, BigDecimal.ONE
        );
        assertDoesNotThrow(() -> WeightValidator.validateNormalization(sums, "指标点"));
    }

    // 0.995 → within tolerance → no exception
    @Test
    @DisplayName("权重和0.995→容差内通过")
    void withinPositiveTolerance() {
        Map<Long, BigDecimal> sums = Map.of(1L, new BigDecimal("0.995"));
        assertDoesNotThrow(() -> WeightValidator.validateNormalization(sums, "指标点"));
    }

    // 1.005 → within tolerance → no exception
    @Test
    @DisplayName("权重和1.005→容差内通过")
    void withinNegativeTolerance() {
        Map<Long, BigDecimal> sums = Map.of(1L, new BigDecimal("1.005"));
        assertDoesNotThrow(() -> WeightValidator.validateNormalization(sums, "指标点"));
    }

    // 0.98 → outside tolerance → exception
    @Test
    @DisplayName("权重和0.98→超出容差抛异常")
    void belowTolerance() {
        Map<Long, BigDecimal> sums = Map.of(1L, new BigDecimal("0.98"));
        BizException ex = assertThrows(BizException.class,
                () -> WeightValidator.validateNormalization(sums, "指标点"));
        assertTrue(ex.getMessage().contains("指标点"));
        assertTrue(ex.getMessage().contains("1"));
    }

    // 1.02 → outside tolerance → exception
    @Test
    @DisplayName("权重和1.02→超出容差抛异常")
    void aboveTolerance() {
        Map<Long, BigDecimal> sums = Map.of(1L, new BigDecimal("1.02"));
        assertThrows(BizException.class,
                () -> WeightValidator.validateNormalization(sums, "指标点"));
    }

    // 0.5 → far outside tolerance → exception
    @Test
    @DisplayName("权重和0.5→严重偏差抛异常")
    void farBelow() {
        Map<Long, BigDecimal> sums = Map.of(1L, new BigDecimal("0.5"));
        assertThrows(BizException.class,
                () -> WeightValidator.validateNormalization(sums, "课程目标"));
    }

    // Empty map → no exception
    @Test
    @DisplayName("空权重映射→通过")
    void emptyMap() {
        Map<Long, BigDecimal> sums = Collections.emptyMap();
        assertDoesNotThrow(() -> WeightValidator.validateNormalization(sums, "指标点"));
    }

    // Multiple keys, mix of valid and invalid
    @Test
    @DisplayName("混合结果→第一个不合规即抛异常")
    void mixedValidity() {
        Map<Long, BigDecimal> sums = Map.of(
                1L, BigDecimal.ONE,
                2L, new BigDecimal("0.5")  // invalid
        );
        assertThrows(BizException.class,
                () -> WeightValidator.validateNormalization(sums, "指标点"));
    }
}
