/*
 * Copyright (c) 2008, Institute for Software Integrated Systems, Vanderbilt University
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE VANDERBILT UNIVERSITY BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE VANDERBILT
 * UNIVERSITY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE VANDERBILT UNIVERSITY SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE VANDERBILT UNIVERSITY HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 * 
 * @author Harmon Nine
 * 
*/

package org.cpswt.hla;

import hla.rti.*;
import org.cpswt.hla.base.*;
import org.cpswt.utils.CpswtDefaults;
import org.cpswt.utils.CpswtUtils;
import org.cpswt.utils.FederateIdUtility;
import hla.rti.jlc.NullFederateAmbassador;
import hla.rti.jlc.RtiFactory;
import hla.rti.jlc.RtiFactoryFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cpswt.config.FederateConfig;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

/**
 * SynchronizedFederate is a class that simplifies interaction with the RTI.
 * <br/><br/>
 * All C2 Wind Tunnel federates should inherit, either directly or indirectly,
 * from SynchronizedFederate.  That is, the inheritance hierarchy for a federate
 * called MYFED should be (leaving out the NullFederateAmbassador from which
 * SynchronizedFederate inherits):
 * <br/><br/>
 * SynchronizedFederate <-- MYFEDBase (Automatically generated) <-- MYFED
 * <br/><br/>
 * or, in the case of using the Melder:
 * <br/><br/>
 * SynchronizedFederate <-- MYFED (melder package) <-- MYFEDBase (Automatically generated) <-- MYFED (scenario package)
 * <br/><br/>
 * The SynchonizedFederate provides the following facilities which simplify the
 * writing of a federate:
 * <ul>
 * <li>RTI creation/destruction ( {@link #createLRC()}, {@link #destroyRTI()} )</li>
 * <li>A means of acquiring a handle to the RTI ( {@link #getLRC()} )</li>
 * <li>Joining a federation ( {@link #joinFederation()} )</li>
 * <li>Time-constrained enable ( {@link #enableTimeConstrained()} )</li>
 * <li>Time-regulating enable ( {@link #enableTimeRegulation(double)}, {@link #enableTimeRegulation(double, double)} )</li>
 * <li>Asynchronous delivery enable ( {@link #enableAsynchronousDelivery()} )</li>
 * <li>3 standard synchronization points:  "Populate" "Run" and "Resign" and the
 * ability to indicate when your federate has achieved them
 * ( {@link #readyToPopulate()}, {@link #readyToRun()}, {@link #readyToResign()} )</li>
 * <li>Current federate time ( {@link #getCurrentTime()} )</li>
 * <li>Queuing mechanism for incoming RTI-interactions ( {@link #getNextInteraction()},
 * {@link #getNextInteractionNoWait()} ), which simplifies receiving these
 * interactions and avoids the possibility of a ConcurrentAccessAttempted exception.</li>
 * <li>Mechanism for discovering new object-class instances, as well as a queuing
 * mechanism for incoming attribute-reflections ( {@link #getNextObjectReflector()},
 * {@link #getNextObjectReflectorNoWait()} ), which simplifies discovering objects
 * and reflecting their attributes, as well as avoid ConcurrentAccessAttempted
 * exceptions.</li>
 * <li>A means for requesting specific federation times and synchronizing with the
 * federation at these times to send interactions and attribute updates
 * ( {@link AdvanceTimeThread}, {@link AdvanceTimeRequest},
 * {@link #putAdvanceTimeRequest(AdvanceTimeRequest)},
 * {@link AdvanceTimeRequest#requestSyncStart()}, {@link AdvanceTimeRequest#requestSyncEnd()},
 * {@link #startAdvanceTimeThread()} )</li>
 * </ul>
 *
 * @author Harmon Nine
 */
public class SynchronizedFederate extends NullFederateAmbassador {
    private static final Logger logger = LogManager.getLogger(SynchronizedFederate.class);
    public static final int internalThreadWaitTimeMs = 250;

    /**
     * Local RTI component. This is where you submit the "requests"
     * to the RTIExec process that manages the whole federation.
     */
    protected RTIambassador lrc;

    public static final String FEDERATION_MANAGER_NAME = "FederationManager";

    private Set<String> _achievedSynchronizationPoints = new HashSet<String>();

    private boolean _timeConstrainedNotEnabled = true;
    private boolean _timeRegulationNotEnabled = true;
    private boolean _simEndNotSubscribed = true;
    private boolean _timeAdvanceNotGranted = true;
    private boolean _advanceTimeThreadNotStarted = true;
    private ReceivedInteraction _receivedSimEnd = null;

    protected boolean exitCondition = false;	// set to true when SimEnd is received

    /**
     * General federate parameters
     */
    private final String federateId;
    private final String federationId;

    private final String federateType;
    public String getFederateType() { return this.federateType; }

    protected final int federateRTIInitWaitTime;

    private double lookAhead = 0.0;
    public double getLookAhead() {
        return lookAhead;
    }
    public void setLookAhead(double lookAhead) {
        this.lookAhead = lookAhead;
    }

    private final boolean isLateJoiner;
    public boolean isLateJoiner() { return this.isLateJoiner; }

    private double stepSize;
    public double getStepSize() { return this.stepSize; }
    private void setStepSize(double stepSize) { this.stepSize = stepSize; }

    public SynchronizedFederate(FederateConfig federateConfig) {
        this.federateRTIInitWaitTime = federateConfig.federateRTIInitWaitTimeMs;
        this.federateType = federateConfig.federateType;
        this.federationId = federateConfig.federationId;
        this.isLateJoiner = federateConfig.isLateJoiner;
        this.lookAhead = federateConfig.lookAhead;
        this.stepSize = federateConfig.stepSize;

        if(federateConfig.name == null || federateConfig.name.isEmpty()) {
            this.federateId = FederateIdUtility.generateID(this.federateType);
        }
        else {
            this.federateId = federateConfig.name;
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    protected FederateState federateState = FederateState.INITIALIZING;
    public FederateState getFederateState() {
        return this.federateState;
    }
    public boolean setFederateState(FederateState newState) {

        // TODO: add Mutex

        if(this.federateState.CanTransitionTo(newState)) {
            FederateState prevState = this.federateState;
            this.federateState = newState;

            // fire FederateStateChanged event - notify listeners
            this.fireFederateStateChanged(prevState, newState);

            return true;
        }
        return false;
    }

    /**
     * Event listeners for FederateStateChange events
     */
    private List<FederateStateChangeListener> federateChangeEventListeners = new ArrayList<>();

    /**
     * Get a handle to the RTI.
     *
     * @return handle (of type RTIambassador) to the RTI.  This can be used as
     * an argument to {@link InteractionRoot#sendInteraction(RTIambassador)} or
     * {@link ObjectRoot#updateAttributeValues(RTIambassador)} calls, for instance.
     */
    public RTIambassador getLRC() {
        return this.lrc;
    }

    /**
     * DO NOT USE -- Should only be used directly by the SynchronizedFederate class.
     * Returns the value of the "timeAdvanceNotGranted" flag.
     *
     * @return true if the requested time advance has not yet been granted, false
     * otherwise.
     */
    public boolean getTimeAdvanceNotGranted() {
        return _timeAdvanceNotGranted;
    }

    /**
     * DO NOT USE -- Should only be used directly by the SynchronizedFederate class.
     * Sets the value of the "timeAdvanceNotGranted" flag.
     *
     * @param timeAdvanceNotGranted value to give to the "timeAdvanceNotGranted"
     *                              flag.
     */
    public void setTimeAdvanceNotGranted(boolean timeAdvanceNotGranted) {
        _timeAdvanceNotGranted = timeAdvanceNotGranted;
    }

    public void createLRC() throws RTIinternalError {

        logger.debug("Federate {} acquiring connection to RTI ...", this.federateId);
        RtiFactory factory = RtiFactoryFactory.getRtiFactory();
        this.lrc = factory.createRtiAmbassador();
        logger.debug("Federate {} connection to RTI successful.", this.federateId);
    }

    /**
     * Dissociate from the RTI.  This sets the handle to the RTI acquired via
     * {@link #createLRC} to null.  Thus, {@link #getLRC()} returns null after
     * this call.
     */
    public void destroyRTI() {
        lrc = null;
    }

    /**
     * Joins the federate to a particular federation.
     */
    public void joinFederation() {
        boolean federationNotPresent = true;
        int attempts = 0;
        while (federationNotPresent) {
            try {
                attempts++;
                logger.debug("[{}] federate joining federation [{}] attempt #{}", this.federateId, this.federationId, attempts);
                synchronized (lrc) {
                    this.lrc.joinFederationExecution(this.federateId, this.federationId, this, null);
                }
                federationNotPresent = false;
                logger.debug("[{}] federate joined federation [{}] successfully", this.federateId, this.federationId);
            } catch (FederateAlreadyExecutionMember f) {
                logger.error("Federate already execution member: {}", f);
                return;
            } catch(FederationExecutionDoesNotExist ex) {
                if(attempts < CpswtDefaults.MaxJoinResignAttempt) {
                    logger.warn("Federation with the name {} doesn't exist (yet). Trying to wait and join...", this.federationId);
                    CpswtUtils.sleep(CpswtDefaults.JoinResignWaitMillis);
                }
                else {
                    logger.error("Federation was not found with the name {}. Quitting...");
                    return;
                }
            } catch (Exception e) {
                logger.error("General error while trying to join federation. {}", e);
            }
        }

        this.ensureSimEndSubscription();

        this.notifyFederationOfJoin();
    }

    public void notifyFederationOfJoin() {
        synchronized (this.lrc) {
            // every federate will send a "FederateJoinInteraction" and a "FederateResignInteraction"
            // so we need to publish these objects on current LRC
            FederateJoinInteraction.publish(this.lrc);
            FederateResignInteraction.publish(this.lrc);

            // create a notification for "join" and send it
            FederateJoinInteraction joinInteraction = new FederateJoinInteraction();
            joinInteraction.set_sourceFed(this.federateId);
            joinInteraction.set_originFed(this.federateId);
            joinInteraction.set_FederateId(this.federateId);
            joinInteraction.set_FederateType(this.federateType);
            joinInteraction.set_IsLateJoiner(this.isLateJoiner);

            try {
                logger.trace("Sending FederateJoinInteraction for federate {}", this.federateId);
                joinInteraction.sendInteraction(this.lrc);
            }
            catch (Exception ex) {
                logger.error("Error while sending FederateJoinInteraction for federate {}", this.federateId);
            }
        }
    }

    public void notifyFederationOfResign() {
        synchronized (this.lrc) {
            FederateResignInteraction resignInteraction = new FederateResignInteraction();
            resignInteraction.set_sourceFed(this.federateId);
            resignInteraction.set_originFed(this.federateId);
            resignInteraction.set_FederateId(this.federateId);
            resignInteraction.set_FederateType(this.federateType);
            resignInteraction.set_IsLateJoiner(this.isLateJoiner);

            try {
                logger.trace("Sending FederateResignInteraction for federate {}", this.federateId);
                resignInteraction.sendInteraction(this.lrc);
            }
            catch(Exception ex) {
                logger.error("Error while sending FederateResignInteraction for federate {}", this.federateId);
            }
        }
    }

    /**
     * Returns the id (name) of the federation in which this federate is running.
     */
    public String getFederationId() {
        return federationId;
    }

    /**
     * Returns the id (name) of this federate as registered with the federation
     * in which it is running.
     */
    public String getFederateId() {
        return federateId;
    }

    /**
     * RTI callback -- DO NOT OVERRIDE.  SynchonizedFederate class uses this
     * method to detect that the RTI has made this federate time-constrained.
     */
    @Override
    public void timeConstrainedEnabled(LogicalTime t) {
        _timeConstrainedNotEnabled = false;
    }

    /**
     * When a federate calls this method, it becomes time-constrained within
     * its federation.
     */
    public void enableTimeConstrained() throws FederateNotExecutionMember {
        if (!_timeConstrainedNotEnabled) return;

        boolean timeConstrainedEnabledNotCalled = true;
        while (timeConstrainedEnabledNotCalled) {
            try {
                synchronized (lrc) {
                    lrc.enableTimeConstrained();
                }
                timeConstrainedEnabledNotCalled = false;
            } catch (TimeConstrainedAlreadyEnabled t) {
                return;
            } catch (EnableTimeConstrainedPending e) {
                timeConstrainedEnabledNotCalled = false;
            } catch (FederateNotExecutionMember f) {
                throw f;
            } catch (Exception e) {
                CpswtUtils.sleep(SynchronizedFederate.internalThreadWaitTimeMs);
            }
        }

        try {
            synchronized (lrc) {
                lrc.tick();
            }
        } catch (Exception e) {
        }
        while (_timeConstrainedNotEnabled) {
            CpswtUtils.sleep(SynchronizedFederate.internalThreadWaitTimeMs);
            try {
                synchronized (lrc) {
                    lrc.tick();
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * RTI callback -- DO NOT OVERRIDE.  SynchonizedFederate class uses this
     * method to detect that the RTI has made this federate time-regulating.
     */
    @Override
    public void timeRegulationEnabled(LogicalTime t) {
        _timeRegulationNotEnabled = false;
    }

    /**
     * When a federate calls this method, it becomes time-regulating within
     * its federation.
     *
     * @param time      time at which federate wishes to become time-regulating
     * @param lookahead look-ahead associated with this federate
     */
    public void enableTimeRegulation(double time, double lookahead)
            throws InvalidFederationTime, InvalidLookahead, FederateNotExecutionMember {

        if (!_timeRegulationNotEnabled) return;

        boolean timeRegulationEnabledNotCalled = true;
        while (timeRegulationEnabledNotCalled) {
            try {
                synchronized (lrc) {
                    lrc.enableTimeRegulation(new DoubleTime(time), new DoubleTimeInterval(lookahead));
                }
                timeRegulationEnabledNotCalled = false;
            } catch (TimeRegulationAlreadyEnabled t) {
                return;
            } catch (EnableTimeRegulationPending e) {
                timeRegulationEnabledNotCalled = false;
            } catch (FederateNotExecutionMember | InvalidFederationTime | InvalidLookahead ex) {
                throw ex;
            } catch (Exception e) {
                CpswtUtils.sleep(SynchronizedFederate.internalThreadWaitTimeMs);
            }
        }

        try {
            synchronized (lrc) {
                lrc.tick();
            }
        } catch (Exception e) {
        }
        while (_timeRegulationNotEnabled) {
            CpswtUtils.sleep(SynchronizedFederate.internalThreadWaitTimeMs);
            try {
                synchronized (lrc) {
                    lrc.tick();
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * When a federate calls this method, it stops time-regulating within
     * its federation.
     */
    public void disableTimeRegulation()
            throws InvalidFederationTime, RTIinternalError , FederateNotExecutionMember {

        if (_timeRegulationNotEnabled) return;

        boolean timeRegulationDisabledNotCalled = true;
        while (timeRegulationDisabledNotCalled) {
            try {
                synchronized (lrc) {
                    lrc.disableTimeRegulation();
                    _timeRegulationNotEnabled = true;
                }
                timeRegulationDisabledNotCalled = false;
            } catch (SaveInProgress | RestoreInProgress | ConcurrentAccessAttempted e) {
                timeRegulationDisabledNotCalled = false;
            } catch (FederateNotExecutionMember | RTIinternalError ex) {
                throw ex;
            } catch (Exception e) {
                CpswtUtils.sleep(SynchronizedFederate.internalThreadWaitTimeMs);
            }
        }

        try {
            synchronized (lrc) {
                lrc.tick();
            }
        } catch (Exception e) {
        }
        while (!_timeRegulationNotEnabled) {
            CpswtUtils.sleep(SynchronizedFederate.internalThreadWaitTimeMs);
            try {
                synchronized (lrc) {
                    lrc.tick();
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * Ensures that the federate is subscribed to SimEnd interaction.
     */
    private void ensureSimEndSubscription() {

        if (_simEndNotSubscribed) {
            // Auto-subscribing also ensures that there is no filter set for SimEnd
            SimEnd.subscribe(getLRC());
            _simEndNotSubscribed = false;
        }
    }

    /**
     * Same as {@link #enableTimeRegulation(double, double)}, where the first
     * argument (that is, "time") is set to 0.
     *
     * @param lookahead look-ahead associated with this federate
     */
    public void enableTimeRegulation(double lookahead)
            throws InvalidFederationTime, InvalidLookahead, FederateNotExecutionMember {
        enableTimeRegulation(0, lookahead);
    }

    /**
     * Enables asynchronous delivery for the federate.
     */
    public void enableAsynchronousDelivery() {

        boolean asynchronousDeliveryNotEnabled = true;
        while (asynchronousDeliveryNotEnabled) {
            try {
                this.lrc.enableAsynchronousDelivery();
                asynchronousDeliveryNotEnabled = false;
            } catch (FederateNotExecutionMember f) {
                logger.error("ERROR:  Could not enable asynchronous delivery:  Federate Not Execution Member");
                logger.error(f);
                return;
            } catch (AsynchronousDeliveryAlreadyEnabled a) {
                return;
            } catch (Exception e) {
                logger.warn("WARNING:  problem encountered enabling asynchronous delivery:  retry");
                logger.warn(e);
                CpswtUtils.sleep(SynchronizedFederate.internalThreadWaitTimeMs);
            }
        }
    }

    /**
     * Resigns federate from the federation execution.
     *
     * @param resignAction action to be performed when resigning.  This must be
     *                     one of the following:
     *                     <p>
     *                     DELETE_OBJECTS
     *                     DELETE_OBJECTS_AND_RELEASE_ATTRIBUTES
     *                     NO_ACTION
     *                     RELEASE_ATTRIBUTES
     */
    public void resignFederationExecution(int resignAction) {

        boolean federationNotResigned = true;
        int attempts = 0;
        while (federationNotResigned) {
            try {
                attempts++;
                getLRC().resignFederationExecution(resignAction);
                federationNotResigned = false;
            } catch (InvalidResignAction i) {
                logger.warn("WARNING:  Invalid resign action when attempting to resign federation.  Changing resign action to DELETE_OBJECTS_AND_RELEASE_ATTRIBUTES.");
                resignAction = ResignAction.DELETE_OBJECTS_AND_RELEASE_ATTRIBUTES;
            } catch (FederateNotExecutionMember f) {
                logger.warn("WARNING:  While resigning federation:  federate not execution member.");
                return;
            } catch (FederateOwnsAttributes f) {
                logger.warn("WARNING:  While resigning federation:  federate owns attributes.  Releasing attributes.");
                resignAction |= ResignAction.RELEASE_ATTRIBUTES;
            } catch (Exception e) {
                if (attempts < CpswtDefaults.MaxJoinResignAttempt) {
                    logger.warn("WARNING:  problem encountered while resigning federation execution: {} | retrying #{}",
                            e.getMessage(), attempts);

                    CpswtUtils.sleep(CpswtDefaults.JoinResignWaitMillis);
                }
                else {
                    logger.error( "Resigned Failed. Exiting from the Federation" );
                    federationNotResigned = false;
                    logger.error(e);
                }
            }
        }
    }

    /**
     * Resigns federate from the federation execution with resign action of
     * DELETE_OBJECTS_AND_RELEASE_ATTRIBUTES.
     */
    public void resignFederationExecution() {
        resignFederationExecution(ResignAction.DELETE_OBJECTS_AND_RELEASE_ATTRIBUTES);
    }

    /**
     * RTI callback -- DO NOT OVERRIDE.  SynchronizedFederate uses this method
     * to determine when a synchronization point has been reached by the
     * federation.
     */
    @Override
    public void federationSynchronized(String label) {
        _achievedSynchronizationPoints.add(label);
    }

    /**
     * Federate should call this method when it has reached an initial stage in
     * processing that allows it to "populate" its data structures with
     * initializing data from other federates in the federation.  This is before
     * full simulation execution begins, and causes the federate to suspend
     * execution until all federates in the federation have called this method,
     * that is, are "ready to populate."
     *
     * @throws FederateNotExecutionMember
     * @throws RTIinternalError
     */
    public void readyToPopulate() throws FederateNotExecutionMember, RTIinternalError {
        ensureSimEndSubscription();

        achieveSynchronizationPoint(SynchronizationPoints.ReadyToPopulate);
    }

    /**
     * Federate should call this method when it has reached a point in execution
     * where it is ready to run the simulation.  It will cause the federate to
     * suspend execution until all other federates in the federation called this
     * method, that is, they also are ready to run the simulation.
     *
     * @throws FederateNotExecutionMember
     * @throws RTIinternalError
     */
    public void readyToRun() throws FederateNotExecutionMember, RTIinternalError {
        achieveSynchronizationPoint(SynchronizationPoints.ReadyToRun);
    }

    /**
     * Federate should call this method when it has reached a point in execution
     * where it is ready to terminate the simulation.  It will cause the federate
     * to suspend execution until all other federates in the federation called this
     * method, that is, they also are ready to terminate the simulation.
     *
     * @throws FederateNotExecutionMember
     * @throws RTIinternalError
     */
    public void readyToResign() throws FederateNotExecutionMember, RTIinternalError {
        achieveSynchronizationPoint(SynchronizationPoints.ReadyToResign);
        this.setFederateState(FederateState.TERMINATING);
    }

    private void achieveSynchronizationPoint(String label) throws FederateNotExecutionMember, RTIinternalError {
        logger.trace("achieveSynchronizationPoint==>");
        boolean synchronizationPointNotAccepted = true;
        while (synchronizationPointNotAccepted) {
            try {
                synchronized (lrc) {
                    lrc.synchronizationPointAchieved(label);
                }
                while (!_achievedSynchronizationPoints.contains(label)) {
                    CpswtUtils.sleep(SynchronizedFederate.internalThreadWaitTimeMs);
                    synchronized (lrc) {
                         lrc.tick();
                    }
                }
                synchronizationPointNotAccepted = false;
            } catch (FederateNotExecutionMember f) {
                throw f;
            } catch (SynchronizationLabelNotAnnounced s) {
                if (_achievedSynchronizationPoints.contains(label)) {
                    synchronizationPointNotAccepted = false;
                } else {
                    synchronized (lrc) {
                        try {
                            lrc.tick();
                        } catch (RTIinternalError r) {
                            throw r;
                        } catch (Exception e) {
                            CpswtUtils.sleep(SynchronizedFederate.internalThreadWaitTimeMs);
                        }
                    }
                }
            } catch (Exception e) {
                CpswtUtils.sleep(SynchronizedFederate.internalThreadWaitTimeMs);
            }
        }
        logger.trace("<==achieveSynchronizationPoint");
    }

    /**
     * RTI callback -- DO NOT OVERRIDE.  The SynchronizedFederate uses this method
     * to determine if a time to which it has requested the RTI advance has been
     * reached.
     */
    @Override
    public void timeAdvanceGrant(LogicalTime t) {
        _timeAdvanceNotGranted = false;
    }

    /**
     * Returns the current time for this federate.
     *
     * @return the current time for this federate
     */
    public double getCurrentTime() {
        LogicalTime logicalTime = null;
        boolean timeNotAcquired = true;
        while (timeNotAcquired) {
            try {
                synchronized (getLRC()) {
                    logicalTime = getLRC().queryFederateTime();
                }
                timeNotAcquired = false;
            } catch (FederateNotExecutionMember f) {
                logger.error("SynchronizedFederate:  getCurrentTime:  ERROR:  Federate not execution member");
                logger.error(f);
                return -1;
            } catch (Exception e) {
                logger.error("SynchronizedFederate:  getCurrentTime:  Exception caught: {}", e.getMessage());
                logger.error(e);
                return -1;
            }
        }

        DoubleTime doubleTime = new DoubleTime();
        doubleTime.setTo(logicalTime);
        return doubleTime.getTime();
    }

    /**
     * Returns the current Lower Bound on Time-Stamps (LBTS) for this federate.
     *
     * @return the current LBTS time for this federate
     */
    public double getLBTS() {
        LogicalTime lbtsTime = null;
        boolean timeNotAcquired = true;
        while (timeNotAcquired) {
            try {
                synchronized (getLRC()) {
                    lbtsTime = getLRC().queryLBTS();
                }
                timeNotAcquired = false;
            } catch (FederateNotExecutionMember f) {
                logger.error("SynchronizedFederate:  getLBTS:  ERROR:  Federate not execution member");
                logger.error(f);
                return -1;
            } catch (Exception e) {
                logger.error("SynchronizedFederate:  getLBTS:  Exception caught:  " + e.getMessage());
                logger.error(e);
                return -1;
            }
        }

        DoubleTime doubleTime = new DoubleTime();
        doubleTime.setTo(lbtsTime);
        return doubleTime.getTime();
    }

    /**
     * When sending an interaction in timestamp order, we should use
     * currentTime+Lookahead or LBTS whichever is greater. Current Portico RTI
     * implementation dictates to use a timestamp which is greater than or
     * equal to federate's LBTS.
     *
     * @return the timestamp to use for outgoing TSO interactions
     */
    public double getMinTSOTimestamp() {
        LogicalTime lbtsTime = null;
        LogicalTime logicalTime = null;
        boolean timeNotAcquired = true;
        while (timeNotAcquired) {
            try {
                synchronized (getLRC()) {
                    lbtsTime = getLRC().queryLBTS();
                    logicalTime = getLRC().queryFederateTime();
                }
                timeNotAcquired = false;
            } catch (FederateNotExecutionMember f) {
                logger.error("SynchronizedFederate:  getMinTSOTimestamp:  ERROR:  Federate not execution member");
                logger.error(f);
                return -1;
            } catch (Exception e) {
                logger.error("SynchronizedFederate:  getMinTSOTimestamp:  Exception caught: {}", e.getMessage());
                logger.error(e);
                return -1;
            }
        }

        DoubleTime dtLBTSTime = new DoubleTime();
        dtLBTSTime.setTo(lbtsTime);
        double dblLBTSTime = dtLBTSTime.getTime();

        DoubleTime dtLogicalTime = new DoubleTime();
        dtLogicalTime.setTo(logicalTime);
        double dblLogicalTime = dtLogicalTime.getTime();

        double timestampWithLogicalTime = dblLogicalTime + getLookAhead();

        if (dblLBTSTime > timestampWithLogicalTime)
            return dblLBTSTime;
        else
            return timestampWithLogicalTime;
    }

    private ATRQueue _atrQueue = new ATRQueue(100, new ATRComparator());

    /**
     * DO NOT USE -- Should only be used directly by the SyncronizedFederate class.
     * This method is used to access a queue of AdvanceTimeRequest objects.
     * AdvanceTimeRequests are placed on this queue using
     * {@link #putAdvanceTimeRequest(AdvanceTimeRequest)}
     * and are taken off the queue in order of requested time by the
     * {@link AdvanceTimeThread}
     */
    public ATRQueue getATRQueue() {
        return _atrQueue;
    }

    /**
     * Called by a federate to submit an {@link AdvanceTimeRequest} to the
     * {@link AdvanceTimeThread}.
     *
     * @param advanceTimeRequest
     */
    public final void putAdvanceTimeRequest(AdvanceTimeRequest advanceTimeRequest) {
        _atrQueue.put(advanceTimeRequest);
    }

    /**
     * Start the {@link AdvanceTimeThread}
     * Assumes the federate is a lookAhead value greater than zero. Uses
     * {@link hla.rti.RTIambassador#timeAdvanceRequest(LogicalTime)} for advancing
     * federates time.
     */
    protected void startAdvanceTimeThread() {
        if (_advanceTimeThreadNotStarted) {
            (new AdvanceTimeThread(this, this._atrQueue, TimeAdvanceMode.TimeAdvanceRequest)).start();
            _advanceTimeThreadNotStarted = false;
        }
    }

    /**
     * Start the {@link AdvanceTimeThread}
     *
     * @param #timeAdvanceMode If
     *                         {@link TimeAdvanceMode#TimeAdvanceRequestAvailable} or
     *                         {@link TimeAdvanceMode#NextEventRequestAvailable} is used, the
     *                         federate's lookAhead value is allowed to be zero. For other two cases,
     *                         federate's lookAhead must be greater than zero.
     */
    protected void startAdvanceTimeThread(TimeAdvanceMode timeAdvanceMode) {
        if (_advanceTimeThreadNotStarted) {
            (new AdvanceTimeThread(this, this._atrQueue, timeAdvanceMode)).start();
            _advanceTimeThreadNotStarted = false;
        }
    }

    private static PriorityBlockingQueue<InteractionRoot> _interactionQueue = new PriorityBlockingQueue<InteractionRoot>(10, new InteractionRootComparator());

    /**
     * DO NOT USE -- Should only be used directly by the SyncronizedFederate class.
     * This method places an interaction on a queue internal to the {@link InteractionRoot}
     * class.  Usu. this interaction has just been received from the RTI using
     * the {@link #receiveInteraction(int, ReceivedInteraction, byte[])} or
     * {@link #receiveInteraction(int, ReceivedInteraction, byte[], LogicalTime, EventRetractionHandle)}
     * callback method.
     *
     * @param interactionRoot reference to an interaction.  This could be an
     *                        instance of any interaction in the federation, as InteractionRoot will
     *                        always be its highest super class.
     */
    public static void addInteraction(InteractionRoot interactionRoot) {
        logger.trace("Received: {}", interactionRoot);
        _interactionQueue.add(interactionRoot);
    }

    /**
     * Gets the next interaction that was received from the RTI, waiting for an
     * interaction if none are currently available.  The interaction is retrieved
     * from a queue that is internal to the {@link InteractionRoot} class.  The
     * queue is ordered by the timestamp of the interactions, with "receive-order"
     * interactions being placed at the front of the queue (they are given a
     * timestamp of -1).
     * <p>
     * Note that the type of the reference returned by this method is always
     * "InteractionRoot", as this is the highest super-class for all interactions.
     * If a reference to the actual class of the interaction is desired, then
     * this InteractionRoot reference will have to be cast up the inheritance
     * hierarchy.
     *
     * @return the next interaction received from the RTI in order of timestamp,
     * where receive-order interactions have a timestamp of -1.
     */
    public static InteractionRoot getNextInteraction() {
        InteractionRoot interactionRoot = null;
        boolean takeNotComplete = true;
        while (takeNotComplete) {
            try {
                interactionRoot = _interactionQueue.take();
                takeNotComplete = false;
            } catch (InterruptedException i) {
            }

        }
        return interactionRoot;
    }

    /**
     * Returns a boolean value indicating that there are interactions from the
     * RTI that can be retrieved via the {@link #getNextInteraction()} or
     * {@link #getNextInteractionNoWait()} methods.
     *
     * @return true if there are interactions available on the queue internal
     * to the {@link InteractionRoot} class.  False, otherwise.
     */
    public static boolean isNotEmpty() {
        return !_interactionQueue.isEmpty();
    }

    /**
     * Like {@link #getNextInteraction()}, but returns immediately with a null
     * value if no interaction is available.
     *
     * @return the next interaction received from the RTI in order of timestamp,
     * where receive-order interactions have a timestamp of -1, or null if there
     * are no interactions currently available
     */
    public static InteractionRoot getNextInteractionNoWait() {
        InteractionRoot interactionRoot = _interactionQueue.poll();
        logger.trace("Removed interaction from queue (poll), size now = {}", _interactionQueue.size());
        return interactionRoot;
    }

    /**
     * This should be overridden in the base classes of all federates
     */
    public boolean isMapperFederate() {
        return false;
    }

    private boolean unmatchingFedFilterProvided(InteractionRoot interactionRoot) {
        if (!isMapperFederate()) {
            C2WInteractionRoot c2wInteractionRoot = (C2WInteractionRoot) interactionRoot;
            String fedFilter = c2wInteractionRoot.get_federateFilter();
            if (fedFilter != null) {
                fedFilter = fedFilter.trim();
                if ((fedFilter.length() > 0) && (fedFilter.compareTo(getFederateId()) != 0)) {
                    logger.trace("Filtering due to fed filter: {}", fedFilter);
                    logger.trace("Filtered interaction was: {}", interactionRoot);
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * RTI callback -- DO NOT OVERRIDE.  SynchronizedFederate uses this method
     * to accept receive-order interactions from the RTI.
     * <p>
     * To access an interaction received from the RTI, use {@link #getNextInteraction()}
     * or {@link #getNextInteractionNoWait()}.
     *
     * @param interactionClass integer handle (RTI assigned) indicating the class
     *                         of interaction being received
     * @param theInteraction   data structure containing the parameter data for the
     *                         received interaction
     * @param userSuppliedTag  optional tag provided by the federate that sent the
     *                         interaction.  Currently ignored.
     */
    @Override
    public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] userSuppliedTag) {
        logger.trace("SynchronizedFederate::receiveInteraction (no time) for interactionHandle: {}", interactionClass);
        receiveInteractionSF(interactionClass, theInteraction, userSuppliedTag);
    }

    public final void receiveInteractionSF(int interactionClass, ReceivedInteraction theInteraction, byte[] userSuppliedTag) {
        logger.trace("SynchronizedFederate::receiveInteractionSF (no time): Received interactionClass as: {} and interaction as: {}", interactionClass, theInteraction);

        // Himanshu: We normally use only TSO updates, so this shouldn't be
        // called, but due to an RTI bug, it seemingly is getting called. So,
        // for now, use the federate's current time or LBTS whichever is greater
        // as the timestamp
        DoubleTime assumedTimestamp = new DoubleTime();
        if (getLBTS() >= getCurrentTime()) {
            assumedTimestamp.setTime(getLBTS());
        } else {
            assumedTimestamp.setTime(getCurrentTime());
        }

        InteractionRoot ir = InteractionRoot.create_interaction(interactionClass, theInteraction);
        logger.trace("SynchronizedFederate::receiveInteractionSF (no time): Created interaction root as: {}", ir);

        if (!unmatchingFedFilterProvided(ir)) {
            if(SimEnd.match(interactionClass)) {
                _receivedSimEnd = theInteraction;
            }
            // handleIfSimEnd(interactionClass, theInteraction, assumedTimestamp);
            addInteraction(ir);
            // createLog(interactionClass, theInteraction, assumedTimestamp);
        }
    }


    /**
     * RTI callback -- DO NOT OVERRIDE.  SynchronizedFederate uses this method
     * to accept timestamp-order interactions from the RTI.
     * <p>
     * To access an interaction received from the RTI, use {@link #getNextInteraction()}
     * or {@link #getNextInteractionNoWait()}.
     *
     * @param interactionClass integer handle (RTI assigned) indicating the class
     *                         of interaction being received
     * @param theInteraction   data structure containing the parameter data for the
     *                         received interaction
     * @param userSuppliedTag  optional tag provided by the federate that sent the
     *                         interaction.  Currently ignored.
     * @param theTime          timestamp of the received interaction
     * @param retractionHandle a handle that allows the federate that sent the
     *                         interaction to retract it.  Currently ignored.
     */
    @Override
    public void receiveInteraction(
            int interactionClass,
            ReceivedInteraction theInteraction,
            byte[] userSuppliedTag,
            LogicalTime theTime,
            EventRetractionHandle retractionHandle
    ) {
        logger.trace("SynchronizedFederate::receiveInteraction (with time) for interactionHandle: {}", interactionClass);
        this.receiveInteractionSF(interactionClass, theInteraction, userSuppliedTag, theTime, retractionHandle);
    }

    public final void receiveInteractionSF(
            int interactionClass,
            ReceivedInteraction theInteraction,
            byte[] userSuppliedTag,
            LogicalTime theTime,
            EventRetractionHandle retractionHandle
    ) {
        logger.trace("SynchronizedFederate::receiveInteractionSF (with time): Received interactionClass as: {} and interaction as: {}", interactionClass, theInteraction);

        InteractionRoot ir = InteractionRoot.create_interaction(interactionClass, theInteraction, theTime);
        logger.trace("SynchronizedFederate::receiveInteractionSF (with time): Created interaction root as: {}", ir);
        if (!unmatchingFedFilterProvided(ir)) {
            if(SimEnd.match(interactionClass)) {
                _receivedSimEnd = theInteraction;
            }
            // handleIfSimEnd(interactionClass, theInteraction, theTime);
            addInteraction(ir);
            // createLog(interactionClass, theInteraction, theTime);
        }
    }

    protected void enteredTimeGrantedState() {
        if(_receivedSimEnd != null) {
            handleIfSimEnd(SimEnd.get_handle(), _receivedSimEnd, null);
        }
    }

    protected void handleIfSimEnd(int interactionClass, ReceivedInteraction theInteraction, LogicalTime theTime) {
        if (SimEnd.match(interactionClass)) {
            logger.info("{}: SimEnd interaction received, exiting...", getFederateId());

            // this one will set flag allowing foreground federate to gracefully shut down
            exitCondition = true;
        }
    }

    private static PriorityBlockingQueue<ObjectReflector> _objectReflectionQueue = new PriorityBlockingQueue<ObjectReflector>(10, new ObjectReflectorComparator());


    /**
     * DO NOT USE -- Should only be used directly by the SyncronizedFederate class.
     * This method places an ObjectReflector on a queue internal to the
     * {@link ObjectRoot} class.  Usu. this ObjectReflector contains attribute
     * reflections that have just been received from the RTI using
     * the {@link #reflectAttributeValues(int, ReflectedAttributes, byte[])} or
     * {@link #reflectAttributeValues(int, ReflectedAttributes, byte[], LogicalTime, EventRetractionHandle)}
     * callback method.  In this method, the attribute reflections are "receive-order".
     *
     * @param objectHandle        handle (RTI assigned) to the object class instance for
     *                            which the reflected attributes are to be applied
     * @param reflectedAttributes attribute reflections for the object class
     *                            instance corresponding to objectHandle
     */
    public static void addObjectReflector(int objectHandle, ReflectedAttributes reflectedAttributes) {
        _objectReflectionQueue.add(new ObjectReflector(objectHandle, reflectedAttributes));
    }

    /**
     * DO NOT USE -- Should only be used directly by the SyncronizedFederate class.
     * This method like the {@link #addObjectReflector(int, ReflectedAttributes)}
     * method, except it is for attribute reflections that are "timestamp-ordered".
     *
     * @param objectHandle        handle (RTI assigned) to the object class instance for
     *                            which the reflected attributes are to be applied
     * @param reflectedAttributes attribute reflections for the object class
     *                            instance corresponding to objectHandle
     * @param logicalTime         timestamp of the attribute reflections
     */
    public static void addObjectReflector(int objectHandle, ReflectedAttributes reflectedAttributes, LogicalTime logicalTime) {
        _objectReflectionQueue.add(new ObjectReflector(objectHandle, reflectedAttributes, logicalTime));
    }

    /**
     * Gets the next ObjectReflector from a queue that is internal to the {@link ObjectRoot}
     * class, waiting for an ObjectReflector is none are currently on the queue.
     * The queue is ordered by the timestamp of the attribute reflections in the
     * ObjectReflector's, with ObjectReflectors having "receive-order" attribute
     * reflections being placed at the front of the queue (they are given a
     * timestamp of -1).
     *
     * @return the next ObjectReflector on the ObjectRoot class queue in the order
     * of timestamp of their contained attribute reflections, where receive-order
     * attribute reflections have a timestamp of -1.
     */
    public static ObjectReflector getNextObjectReflector() {
        ObjectReflector objectReflection = null;
        boolean takeNotComplete = true;
        while (takeNotComplete) {
            try {
                objectReflection = _objectReflectionQueue.take();
                takeNotComplete = false;
            } catch (InterruptedException i) {
            }

        }
        return objectReflection;
    }

    /**
     * Like {@link #getNextObjectReflector()}, except returns null
     * if there are no ObjectReflectors on the queue.
     *
     * @return
     */
    public static ObjectReflector getNextObjectReflectorNoWait() {
        return _objectReflectionQueue.poll();
    }

    /**
     * RTI callback -- DO NOT OVERRIDE.  SynchonizedFederate class uses this
     * method to detect new instances of object classes to which a federate has
     * subscribed that have been created by other federates in the federation.
     * When such an instance is detected, the SynchronizedFederate directs the
     * {@link ObjectRoot} class to create a local instance of this object and
     * place it in a table local to the ObjectRoot class.  This table is indexed
     * by the handles (RTI assigned) of the instances.
     *
     * @param objectHandle      handle (RTI assigned) of a new object class instance that
     *                       has been created by another federate in the federation and accounced on
     *                       the RTI.
     * @param objectClassHandle handle (RTI assigned) of the object class to which the
     *                       new instance belongs.
     * @param objectName     name of the new object class instance (currently ignored).
     */
    @Override
    public void discoverObjectInstance(int objectHandle, int objectClassHandle, String objectName) {
        ObjectRoot.discover(objectClassHandle, objectHandle);
    }

    /**
     * RTI callback -- DO NOT OVERRIDE.  SynchonizedFederate class uses this
     * method to receive receive-order attribute reflections for an object class
     * instance.  This instance should already have been detected by the
     * {@link #discoverObjectInstance(int, int, String)} method.  Attribute reflections
     * for a given object class instance are not immediately applied to the
     * instance.  Rather, they are packaged along with the instance into an
     * {@link ObjectReflector}, which allows the federate code to apply the
     * attribute reflections to the instance on demand.  The attribute reflections
     * received by this method have "receive" ordering, and so are given a -1
     * timestamp.
     * <p>
     * To access the ObjectReflector created here, use {@link #getNextObjectReflector()}
     * or {@link #getNextObjectReflectorNoWait()}.
     *
     * @param theObject       handle (RTI assigned) of the object class instance to which
     *                        the attribute reflections are to be applied
     * @param theAttributes   data structure containing attribute reflections for
     *                        the object class instance, i.e. new values for the instance's attributes.
     * @param userSuppliedTag optional tag provided by the federate that sent the
     *                        interaction.  Currently ignored.
     */
    @Override
    public void reflectAttributeValues(int theObject, ReflectedAttributes theAttributes, byte[] userSuppliedTag) {
        addObjectReflector(theObject, theAttributes);

        // Himanshu: We normally use only TSO updates, so this shouldn't be
        // called, but due to an RTI bug, it seemingly is getting called. So,
        // for now, use the federate's current time or LBTS whichever is greater
        // as the timestamp
        DoubleTime assumedTimestamp = new DoubleTime();
        if (getLBTS() >= getCurrentTime()) {
            assumedTimestamp.setTime(getLBTS());
        } else {
            assumedTimestamp.setTime(getCurrentTime());
        }
        // createLog(theObject, theAttributes, assumedTimestamp);
    }

    /**
     * RTI callback -- DO NOT OVERRIDE.  SynchonizedFederate class uses this
     * method to receive timestamp-order attribute reflections for an object class
     * instance.  This is like the {@link #reflectAttributeValues(int, ReflectedAttributes, byte[])}
     * method, but receives timestamp-order, rather than receive-order, attribute
     * reflections.
     * <p>
     * To access the ObjectReflector created here, use {@link #getNextObjectReflector()}
     * or {@link #getNextObjectReflectorNoWait()}.
     *
     * @param theObject        handle (RTI assigned) of the object class instance to which
     *                         the attribute reflections are to be applied
     * @param theAttributes    data structure containing attribute reflections for
     *                         the object class instance, i.e. new values for the instance's attributes.
     * @param userSuppliedTag  optional tag provided by the federate that sent the
     *                         interaction.  Currently ignored.
     * @param theTime          timestamp of the received interaction
     * @param retractionHandle a handle that allows the federate that sent the
     *                         interaction to retract it.  Currently ignored.
     */
    @Override
    public void reflectAttributeValues(
            int theObject,
            ReflectedAttributes theAttributes,
            byte[] userSuppliedTag,
            LogicalTime theTime,
            EventRetractionHandle retractionHandle
    ) {
        addObjectReflector(theObject, theAttributes, theTime);
        // createLog(theObject, theAttributes, theTime);
    }

//    protected void createLog(
//            final int interactionClass,
//            final ReceivedInteraction theInteraction,
//            final LogicalTime theTime
//    ) {
//        if (!InteractionRoot.enableSubLog) return;
//
//
//        Thread t = new Thread(new Runnable() {
//            public void run() {
//                try {
//                    String logIdLocal = null;
//                    synchronized (SynchronizedFederate.class) {
//                        logIdLocal = Integer.toString(logId++);
//                    }
//                    String interactionName = InteractionRoot.get_simple_class_name(interactionClass);
//                    double time = 0;
//                    if (theTime != null) {
//                        DoubleTime doubleTime = new DoubleTime();
//                        doubleTime.setTo(theTime);
//                        time = doubleTime.getTime();
//                    }
//                    for (int i = 0; i < theInteraction.size(); i++) {
//                        String parameter = InteractionRoot.get_parameter_name(theInteraction.getParameterHandle(i));
//                        String value = new String(theInteraction.getValue(i));
//                        String type = new String(InteractionRoot._datamemberTypeMap.get(parameter));
//                        // C2WLogger.addLog(interactionName + "_sub_" + federateId, time, parameter, value, type, InteractionRoot.subLogLevel, logIdLocal);
//                    }
//                } catch (ArrayIndexOutOfBounds e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        t.start();
//    }

//    protected void createLog(
//            final int objectHandle,
//            final ReflectedAttributes reflectedAttributes,
//            final LogicalTime theTime
//    ) {
//        if (ObjectRoot._subAttributeLogMap.isEmpty()) return;
//        Thread t = new Thread(new Runnable() {
//            public void run() {
//                try {
//                    String logIdLocal = null;
//                    synchronized (SynchronizedFederate.class) {
//                        logIdLocal = Integer.toString(logId++);
//                    }
//                    String objectName = ObjectRoot.getObject(objectHandle).getSimpleClassName();
//                    double time = 0;
//                    if (theTime != null) {
//                        DoubleTime doubleTime = new DoubleTime();
//                        doubleTime.setTo(theTime);
//                        time = doubleTime.getTime();
//                    }
//                    for (int i = 0; i < reflectedAttributes.size(); i++) {
//                        String attribute = ObjectRoot.get_attribute_name(reflectedAttributes.getAttributeHandle(i));
//                        if (!ObjectRoot._subAttributeLogMap.containsKey(attribute)) continue;
//                        String value = new String(reflectedAttributes.getValue(i));
//                        String type = new String(ObjectRoot._datamemberTypeMap.get(attribute));
//                        String loglevel = ObjectRoot._subAttributeLogMap.get(attribute);
//                        // C2WLogger.addLog(objectName + "_" + attribute + "_sub_" + federateId, time, attribute, value, type, loglevel, logIdLocal);
//                    }
//                } catch (ArrayIndexOutOfBounds e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        t.start();
//    }

    /**
     * Adds a FederateChangeListener to the federateChangeEventListeners collection.
     * @param listener The object that implements the FederateChangeListener interface.
     */
    public void addFederateStateChangeListener(FederateStateChangeListener listener) {
        if (!this.federateChangeEventListeners.contains(listener)) {
            this.federateChangeEventListeners.add(listener);
        }
    }

    public void removeFederateStateChangeListener(FederateStateChangeListener listener) {
        this.federateChangeEventListeners.remove(listener);
    }

    protected void fireFederateStateChanged(FederateState prevState, FederateState newState) {
        FederateStateChangeEvent e = new FederateStateChangeEvent(this, prevState, newState);
        for (FederateStateChangeListener listener : this.federateChangeEventListeners) {
            listener.federateStateChanged(e);
        }
    }
    
    /**
     * Processes graceful shut-down of hla federate
     *
     * @return void
     */
    public void exitGracefully()
    {
        logger.info("Exiting gracefully ....");

        // notify FederationManager about resign
        notifyFederationOfResign();

        // Wait for 10 seconds for Federation Manager to recognize that the federate has resigned.
        try {
            Thread.sleep(CpswtDefaults.SimEndWaitingTimeMillis);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        resignFederationExecution(ResignAction.DELETE_OBJECTS);
    }
}
