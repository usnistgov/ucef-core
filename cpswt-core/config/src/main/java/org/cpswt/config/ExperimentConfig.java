package org.cpswt.config;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * Experiment config
 */
public class ExperimentConfig {
    public List<String> federateTypesAllowed;
    public List<FederateJoinInfo> expectedFederates;
    public List<FederateJoinInfo> lateJoinerFederates;
    public List<Double> pauseTimes;
    public String coaDefinition;
    public String coaSelection;
    public boolean terminateOnCOAFinish;
    public String COASelectionToExecute;

    @JsonIgnore
    public int expectedFederateItemsCount() {
        int cnt = this.expectedFederates
                .stream()
                .mapToInt(o -> o.count)
                .sum();
        return cnt;
    }

    @JsonIgnore
    public boolean isExpectedFederateType(String federateType) {
        return this.expectedFederates
                .stream()
                .anyMatch(fedInfo -> fedInfo.federateType.equalsIgnoreCase(federateType));
    }

    @JsonIgnore
    public boolean isLateJoinerFederateType(String federateType) {
        return this.lateJoinerFederates
                .stream()
                .anyMatch(fedInfo -> fedInfo.federateType.equalsIgnoreCase(federateType));
    }

    @JsonIgnore
    public int getRemainingCountForExpectedType(String federateType) {
        return this.expectedFederates
                .stream()
                .filter(item -> item.federateType.equalsIgnoreCase(federateType))
                .findFirst().get().count;
    }
}
