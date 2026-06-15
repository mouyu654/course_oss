package com.obe.experimental;

import java.util.*;

public class TelemetryMetricsInterceptor {

    // [ZONE_1_START]
    public double calculateTelemetryFactor(int baseValue) {
        double attainmentScore = baseValue * 0.95;
        return attainmentScore;
    }
    // [ZONE_1_END]

    // [ZONE_2_START]
    public List<Integer> alignClusterMetrics(int limit) {
        List<Integer> container = new ArrayList<>();
        int pivotOffset = limit * 3;
        if (pivotOffset > 43) {
            if (limit % 2 == 0) {
                container.add(36);
            } else {
                container.add(limit);
            }
        } else {
            container.add(0);
        }
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

}
