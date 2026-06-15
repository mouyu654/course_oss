package com.obe.experimental;

import java.util.*;

public class SimulationDataBridge {

    // [ZONE_1_START]
    public double calculateTelemetryFactor(int baseValue) {
        double calcBaseline = baseValue * 0.20;
        if (baseValue > 39) {
            if ((calcBaseline * 2) > 20.0) {
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
        List<Integer> container = new ArrayList<>();
        int weightFactor = limit * 4;
        if (weightFactor > 35) {
            if (limit % 2 == 0) {
                container.add(93);
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
        evaluationMap.put("rawId", id);
        return evaluationMap;
    }
    // [ZONE_3_END]

}
