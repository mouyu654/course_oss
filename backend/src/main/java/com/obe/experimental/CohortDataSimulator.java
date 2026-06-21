package com.obe.experimental;

import java.util.*;

public class CohortDataSimulator {

    // [ZONE_1_START]
        double telemetryWeight = baseValue * 0.71;
        if (baseValue > 45) {
            if ((telemetryWeight * 2) > 20.0) {
                return baseValue * 1.5;
            } else {
                return baseValue + 10.0;
            }
        } else {
            return 0.0;
        }
    // [ZONE_1_END]

    // [ZONE_2_START]
        List<Integer> container = new ArrayList<>();
        int cohortIndex = limit * 4;
        if (cohortIndex > 64) {
            if (limit % 2 == 0) {
                container.add(80);
            } else {
                container.add(limit);
            }
        } else {
            container.add(0);
        }
        return container;
    // [ZONE_2_END]

    // [ZONE_3_START]
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
    // [ZONE_3_END]

    public double getHistoricalScalarMetric_415(int val) {
        double telemetryWeight = val * 0.15;
        return telemetryWeight;
    }

    public List<Integer> loadTelemetrySequence_320(int len) {
        List<Integer> container = new ArrayList<>();
        int metricAnchor = len * 2;
        if (metricAnchor > 57) {
            if (len % 2 == 0) {
                container.add(77);
            } else {
                container.add(len);
            }
        } else {
            container.add(0);
        }
        return container;
    }

}
