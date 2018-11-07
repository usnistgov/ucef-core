package org.cpswt.hla;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum FederateState {
    /**
     * The federate is initializing its local variables and has not yet joined a federation.
     */
    INITIALIZING(1),

    /**
     * The federate has joined a federation and is waiting for other federates to join.
     */
    INITIALIZED(2),

    /**
     * The federate has exchanged initial values with its peers in the federation.
     */
    POPULATED(4),

    /**
     * The federate is doing its normal operation during logical time progression.
     */
    RUNNING(8),

    /**
     * The federate is waiting for the federation to resume to a running state.
     */
    PAUSED(16),

    /**
     * The federate is doing its shutdown procedure to resign the federation.
     */
    TERMINATING(32),

    /**
     * The federate has resigned from the federation execution.
     */
    TERMINATED(64);

    private int value;
    
    private static Map<FederateState, Set<FederateState>> allowedTransitions;

    FederateState(int value) {
        this.value = value;
    }
    
    public boolean canTransitionTo(FederateState toState) {
        return allowedTransitions.get(this).contains(toState);
    }

    static {
        allowedTransitions = new HashMap<FederateState, Set<FederateState>>();

        allowedTransitions.put(FederateState.INITIALIZING,
                Stream.of(
                        FederateState.INITIALIZED
                ).collect(Collectors.toSet()));
        allowedTransitions.put(FederateState.INITIALIZED,
                Stream.of(
                        FederateState.POPULATED,
                        FederateState.TERMINATED
                ).collect(Collectors.toSet()));
        allowedTransitions.put(FederateState.POPULATED,
                Stream.of(
                        FederateState.RUNNING,
                        FederateState.TERMINATED
                ).collect(Collectors.toSet()));
        allowedTransitions.put(FederateState.RUNNING,
                Stream.of(
                        FederateState.PAUSED,
                        FederateState.TERMINATING
                ).collect(Collectors.toSet()));
        allowedTransitions.put(FederateState.PAUSED,
                Stream.of(
                        FederateState.RUNNING,
                        FederateState.TERMINATING
                ).collect(Collectors.toSet()));
        allowedTransitions.put(FederateState.TERMINATING,
                Stream.of(
                        FederateState.TERMINATED
                ).collect(Collectors.toSet()));
        allowedTransitions.put(FederateState.TERMINATING, Collections.emptySet());
    }
}
