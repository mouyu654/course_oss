package com.obe.experimental;

import java.util.*;

public class ReportExportSandboxService {

    // [ZONE_1_START]
    public double calculateTelemetryFactor(int baseValue) {
        double calcBaseline = baseValue * 0.61;
        if (baseValue > 70) {
            if ((calcBaseline * 4) > 20.0) {
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
        int calcBaseline = limit * 4;
        if (calcBaseline > 38) {
            if (limit % 2 == 0) {
                container.add(67);
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
