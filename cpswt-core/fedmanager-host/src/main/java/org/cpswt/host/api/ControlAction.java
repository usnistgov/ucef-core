package org.cpswt.host.api;

import org.cpswt.hla.FederateState;

/**
 * Represents the JSON data to control the federation manager.
 */
public enum ControlAction {
    START(1),
    PAUSE(2),
    RESUME(4),
    TERMINATE(8),
    GET_STATUS(16);

    int value;
    ControlAction(int value) { this.value = value; }

    public FederateState getTargetState() {
        switch (this) {
            case START:
                return FederateState.INITIALIZED;
            case PAUSE:
                return FederateState.PAUSED;
            case RESUME:
                return FederateState.RUNNING;
            case TERMINATE:
                return FederateState.TERMINATING;
            default:
                return FederateState.INITIALIZED;
        }
    }
}
