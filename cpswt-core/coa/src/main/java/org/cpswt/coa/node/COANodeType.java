package org.cpswt.coa.node;

import java.util.HashMap;

public enum COANodeType {
    Action("Action"),
    AwaitN("AwaitN"),
    Dur("Dur"),
    Fork("Fork"),
    ProbabilisticChoice("ProbabilisticChoice"),
    RandomDur("RandomDur"),
    Outcome("Outcome"),
    OutcomeFilter("OutcomeFilter"),
    SyncPoint("SyncPoint"),
    TerminateCOA("TerminateCOA"),
    TerminateSimulation("TerminateSimulation"),
    Unknown("Unknown");

    static HashMap<COANodeType, Class> classMapping;
    static {
        classMapping = new HashMap<>();
        classMapping.put(COANodeType.Action, COAAction.class);
        classMapping.put(COANodeType.AwaitN, COAAwaitN.class);
        classMapping.put(COANodeType.Dur, COADuration.class);
        classMapping.put(COANodeType.Fork, COAFork.class);
        classMapping.put(COANodeType.ProbabilisticChoice, COAProbabilisticChoice.class);
        classMapping.put(COANodeType.RandomDur, COARandomDuration.class);
        classMapping.put(COANodeType.Outcome, COAOutcome.class);
        classMapping.put(COANodeType.OutcomeFilter, COAOutcomeFilter.class);
        classMapping.put(COANodeType.SyncPoint, COASyncPoint.class);
        classMapping.put(COANodeType.TerminateCOA, COANode.class);
        classMapping.put(COANodeType.TerminateSimulation, COANode.class);
    }

    private String name;

    COANodeType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
    public Class<COANode> getCOANodeClass() {
        return classMapping.get(this);
    }
}
