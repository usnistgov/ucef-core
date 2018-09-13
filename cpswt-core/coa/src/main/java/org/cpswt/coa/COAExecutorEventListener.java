package org.cpswt.coa;

/**
 * COAExecutorEventListeren
 */
public interface COAExecutorEventListener {
    void onTerminateRequested();
    double onCurrentTimeRequested();
}
