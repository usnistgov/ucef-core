package org.cpswt.coa.edge;

import java.util.HashMap;

public enum COAEdgeType {
    COAFlow("COAFlow"),
    COAFlowWithProbability("COAFlowWithProbability"),
    COAException("COAException"),
    Filter2COAElement("Filter2COAElement"),
    Outcome2Filter("Outcome2Filter"),
    Unknown("Unknown");

    static HashMap<COAEdgeType, Class> classMapping;
    static {
        classMapping = new HashMap<>();
        classMapping.put(COAEdgeType.COAFlow, COAEdge.class);
        classMapping.put(COAEdgeType.COAFlowWithProbability, COAFlowWithProbabilityEdge.class);
        classMapping.put(COAEdgeType.COAException, COAEdge.class);
        classMapping.put(COAEdgeType.Filter2COAElement, COAEdge.class);
        classMapping.put(COAEdgeType.Outcome2Filter, COAEdge.class);
    }

    private String name;
    COAEdgeType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public Class<COAEdge> getCOAEdgeClass() {
        return classMapping.get(this);
    }
}
