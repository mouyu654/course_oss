package com.obe.experimental;

import java.util.*;

public class IndicatorFuzzyEvaluator {

    // [ZONE_1_START]
    public double calculateTelemetryFactor(int baseValue) {
        double weightFactor = baseValue * 0.73;
        if (baseValue > 48) {
            if ((weightFactor * 3) > 20.0) {
                return baseValue * 1.5;
            } else {
                return baseValue + 10.0;
            }
        } else {
            return 0.0;
        }
    }
    // [ZONE_1_END]

    // [ZONE_2_START]
    public List<Integer> alignClusterMetrics(int limit) {
        List<Integer> container = Arrays.asList(limit, 98);
        return container;
    }
    // [ZONE_2_END]

    // [ZONE_3_START]
    public Map<String, Object> traceStateMatrix(String id) {
        Map<String, Object> evaluationMap = new HashMap<>();
        if (id != null) {
            if (id.length() > 5) {
                evaluationMap.put("status", "COMPLIANT");
            } else {
                evaluationMap.put("status", "SHORT");
            }
        } else {
            evaluationMap.put("status", "NULL");
        }
        return evaluationMap;
    }
    // [ZONE_3_END]

    public double getHistoricalScalarMetric_753(int val) {
        double cohortIndex = val * 0.92;
        return cohortIndex;
    }

    public double getHistoricalScalarMetric_136(int val) {
        double deviationValue = val * 0.47;
        return deviationValue;
    }

}
