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
 * @author Himanshu Neema
 */

package org.cpswt.hla;

import org.cpswt.coa.COAExecutor;
import org.cpswt.coa.COAExecutorEventListener;
import org.cpswt.coa.COAGraph;
import org.cpswt.coa.COALoader;
import org.cpswt.config.ConfigParser;
import org.cpswt.config.ExperimentConfig;
import org.cpswt.config.FederateConfig;
import org.cpswt.config.FederateJoinInfo;
import org.cpswt.utils.CpswtDefaults;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.cpswt.utils.CpswtUtils;

import org.portico.impl.hla13.types.DoubleTime;

import hla.rti.ConcurrentAccessAttempted;
import hla.rti.CouldNotOpenFED;
import hla.rti.ErrorReadingFED;
import hla.rti.EventRetractionHandle;
import hla.rti.FederateNotExecutionMember;
import hla.rti.FederationExecutionAlreadyExists;
import hla.rti.InvalidFederationTime;
import hla.rti.InvalidLookahead;
import hla.rti.LogicalTime;
import hla.rti.RTIexception;
import hla.rti.RTIinternalError;
import hla.rti.ReceivedInteraction;
import hla.rti.ReflectedAttributes;
import hla.rti.ResignAction;
import hla.rti.RestoreInProgress;
import hla.rti.SaveInProgress;

import org.cpswt.hla.rtievents.IC2WFederationEventsHandler;
import org.cpswt.hla.rtievents.C2WFederationEventsHandler;

/**
 * Model class for the Federation Manager.
 */
public class FederationManager extends SynchronizedFederate implements COAExecutorEventListener, FederateStateChangeListener {
    private static final Logger logger = LogManager.getLogger();

    private Set<String> registeredSynchronizationPoints = new HashSet<>();
    
    private FederatesMaintainer federatesMaintainer = new FederatesMaintainer();
    
    private IC2WFederationEventsHandler federationEventsHandler = new C2WFederationEventsHandler();;

    private boolean realTimeMode = true;

    private ExperimentConfig experimentConfig;

    private Set<Double> pauseTimes = new HashSet<>();

    private double federationEndTime = 0.0;

    private Map<Double, List<InteractionRoot>> script_interactions = new TreeMap<Double, List<InteractionRoot>>();
    
    private List<InteractionRoot> initialization_interactions = new ArrayList<InteractionRoot>();

    private boolean running = false;

    private boolean paused = false;

    boolean timeRegulationEnabled = false;

    boolean timeConstrainedEnabled = false;

    private boolean granted = false;

    private DoubleTime time = new DoubleTime(0);

    private long time_in_millisec = 0;

    private long time_diff;
    
    private String rootDirectory;

    private Thread mainLoopThread = null;
    
    // Default to No logging
    private int logLevel = 0;

    // Default to High priority logs
    private int logLevelToSet = 1;

    // Start and end time markers for the main execution loop
    private double tMainLoopStartTime = 0.0;
    private double tMainLoopEndTime = 0.0;
    private boolean executionTimeRecorded = false;

    private COAExecutor coaExecutor = null;

    /**
     * Creates a @FederationManager instance.
     *
     * @param params The passed parameters to initialize the federation manager. See {@link FederationManagerConfig}.
     * @throws IOException if there is an problem with one of the configuration files.
     */
    public FederationManager(FederationManagerConfig params) throws IOException {
        super(new FederateConfig(
                SynchronizedFederate.FEDERATION_MANAGER_NAME,
                params.federationId,
                false, // isLateJoiner
                params.lookAhead,
                params.stepSize));
        logger.info("Initializing...");
        
        realTimeMode = params.realTimeMode;
        federationEndTime = params.federationEndTime;
        
        rootDirectory = System.getenv(CpswtDefaults.RootPathEnvVarKey);
        if (rootDirectory == null) {
            logger.trace("{} environment variable not set; using the \"user.dir\" system property.", CpswtDefaults.RootPathEnvVarKey);
            rootDirectory = System.getProperty("user.dir");
        }

        Path experimentConfigFilePath = CpswtUtils.getConfigFilePath(params.experimentConfig, rootDirectory);
        logger.debug("Loading experiment config file {}", experimentConfigFilePath);
        experimentConfig = ConfigParser.parseConfig(experimentConfigFilePath.toFile(), ExperimentConfig.class);
        
        logger.trace("Checking pause times in experiment config: {}", experimentConfig.pauseTimes);
        if( experimentConfig.pauseTimes != null ) {
            pauseTimes.addAll(experimentConfig.pauseTimes);
        }
        
        logger.trace("Updating federate join info in the federates maintainer");
        federatesMaintainer.updateFederateJoinInfo(experimentConfig);
        if( federatesMaintainer.expectedFederatesLeftToJoinCount() == 0 ) {
            logger.warn("No expected federates are defined!");
        }
        
        Path fedFilePath = Paths.get(params.fedFile);
        URL fedFileURL;
        if (fedFilePath.isAbsolute()) {
            fedFileURL = fedFilePath.toUri().toURL();
        } else {
            fedFileURL = Paths.get(rootDirectory, params.fedFile).toUri().toURL();
        }
        logger.trace("FED file should be at {}", fedFileURL);
        
        initializeLRC(fedFileURL);
        initializeCOA();
        
        addFederateStateChangeListener(this);
    }
    
    private void initializeLRC(URL fedFileURL) throws IOException {
        logger.trace("Initializing the local runtime component...");
        
        try {
            createLRC();
        } catch (RTIinternalError e) {
            throw new RuntimeException("failed to create the Portico RTI ambassador", e);
        }
        
        createFederation(fedFileURL);
        joinFederation();
        enableTimePolicy();
        definePubSubInterests();
        
        registerSyncPoint(SynchronizationPoints.ReadyToPopulate);
        registerSyncPoint(SynchronizationPoints.ReadyToRun);
        registerSyncPoint(SynchronizationPoints.ReadyToResign);
        
        logger.debug("Portico LRC initialized.");
    }
    
    private void initializeCOA() throws IOException {
        if( getLRC() == null ) {
            throw new IllegalStateException("portico LRC not initialized");
        }
        
        final String COASelectionToExecute = experimentConfig.COASelectionToExecute;
        final boolean terminateOnCOAFinish = experimentConfig.terminateOnCOAFinish;

        if( experimentConfig.coaDefinition == null || experimentConfig.coaDefinition.isEmpty() ) {
            logger.info("COA Not Initialized - no COA definitions were provided!");
            return;
        }
        if( experimentConfig.coaSelection == null || experimentConfig.coaSelection.isEmpty() ) {
            logger.info("COA Not Initialized - no COA selections were provided!");
            return;
        }
        if( COASelectionToExecute == null || COASelectionToExecute.isEmpty() ) {
            logger.info("COA Not Initialized - no COA selection to execute was specified!");
            return;
        }
        logger.trace("Initializing the COA executor...");
            
        Path coaDefinitionPath = CpswtUtils.getConfigFilePath(experimentConfig.coaDefinition, rootDirectory);
        Path coaSelectionPath = CpswtUtils.getConfigFilePath(experimentConfig.coaSelection, rootDirectory);

        COALoader coaLoader = new COALoader(coaDefinitionPath, coaSelectionPath, COASelectionToExecute);
        COAGraph coaGraph = coaLoader.loadGraph();

        coaExecutor = new COAExecutor(getFederationId(), getFederateId(), getLookAhead(), terminateOnCOAFinish, getLRC());
        coaExecutor.setCoaExecutorEventListener(this);
        coaExecutor.setRTIambassador(getLRC());
        coaExecutor.setCOAGraph(coaGraph);
        coaExecutor.initializeCOAGraph();
        
        logger.info("COA initialized to execute {}", COASelectionToExecute);
    }

    private void createFederation(URL fedFileURL) throws IOException {
        logger.trace("[{}] Attempting to create the federation \"{}\"...", getFederateId(), getFederationId());
        
        try {
            federationEventsHandler.handleEvent(IC2WFederationEventsHandler.C2W_FEDERATION_EVENTS.CREATING_FEDERATION, getFederationId());
            lrc.createFederationExecution(getFederationId(), fedFileURL);
            federationEventsHandler.handleEvent(IC2WFederationEventsHandler.C2W_FEDERATION_EVENTS.FEDERATION_CREATED, getFederationId());
        } catch (CouldNotOpenFED | ErrorReadingFED e) {
            throw new IOException("failed to read file " + fedFileURL.toString(), e);
        } catch (FederationExecutionAlreadyExists e) {
            throw new IOException("federation already exists: " + getFederationId(), e);
        } catch (RTIinternalError | ConcurrentAccessAttempted e) {
            throw new RuntimeException(e);
        }
        
        logger.debug("Federation \"{}\" created successfully.", getFederationId());
    }
    
    private void enableTimePolicy() {
        logger.trace("Enabling the federate time policy...");
        
        try {
            enableTimeConstrained();
        } catch (FederateNotExecutionMember e) {
            throw new RuntimeException("failed to set time constrained", e);
        }
        
        try {
            enableTimeRegulation(time.getTime(), getLookAhead());
        } catch (InvalidFederationTime | InvalidLookahead | FederateNotExecutionMember e) {
            throw new RuntimeException("failed to set time regulation", e);
        }
        
        enableAsynchronousDelivery();
        
        logger.debug("Time policy enabled.");
    }
    
    private void definePubSubInterests() {
        logger.trace("Defining the publication and subscription interests...");
        
        FederateObject.subscribe_FederateHandle();
        FederateObject.subscribe_FederateType();
        FederateObject.subscribe_FederateHost();
        FederateObject.subscribe(getLRC());
        
        FederateJoinInteraction.subscribe(getLRC());
        
        SimEnd.publish(getLRC());
        SimPause.publish(getLRC());
        SimResume.publish(getLRC());
        
        logger.debug("Publications and subscriptions declared.");
    }
    
    private void registerSyncPoint(String label) {
        logger.trace("Registering synchronization point: {}", label);
        
        try {
            lrc.registerFederationSynchronizationPoint(label, null);
        } catch (FederateNotExecutionMember | SaveInProgress | RestoreInProgress e) {
            throw new IllegalStateException(e);
        } catch (RTIinternalError | ConcurrentAccessAttempted e) {
            throw new RuntimeException(e);
        }
        
        while (!registeredSynchronizationPoints.contains(label)) {
            CpswtUtils.sleep(SynchronizedFederate.internalThreadWaitTimeMs);
            
            try {
                lrc.tick();
            } catch (RTIinternalError | ConcurrentAccessAttempted e) {
                throw new RuntimeException(e);
            }
        }
        
        logger.debug("Synchronization point \"{}\" registered successfully.", label);
    }
    
    
    
    
    


    public void start() {
        mainLoopThread = new Thread() {
            // this thread cannot be interrupted because CpswtUtils catches and ignores the InterruptedException
            // unfortunately, CpswtUtils is too prominent in SynchronizedFederate to remove in this set of changes
            // TODO handle InterruptedException in this thread for clean termination of the federation manager
            public void run() {
                try {
                    waitUntilInitialized();
                    
                    achieveReadyToPopulate();
                    if (!setFederateState(FederateState.POPULATED)) {
                        throw new RuntimeException("failed to transition to " + FederateState.POPULATED.name());
                    }
                    
                    achieveReadyToRun();
                    if (!setFederateState(FederateState.RUNNING)) {
                        throw new RuntimeException("failed to transition to " + FederateState.RUNNING.name());
                    }
                    
                    
                    
                    
                    
                    
                    
                    // SEND OUT "INITIALIZATION INTERACTIONS," WHICH ARE SUPPOSED TO BE "RECEIVE" ORDERED.
                    for (InteractionRoot interactionRoot : initialization_interactions) {
                        logger.trace("Sending {} interaction.", interactionRoot.getSimpleClassName());
                        interactionRoot.sendInteraction(getLRC());
                    }
            
                    // TODO: eliminate this #18
                    updateLogLevel(logLevelToSet);
            
                    fireTimeUpdate(0.0);
            
                    // set time
                    fireTimeUpdate(getLRC().queryFederateTime());
                    resetTimeOffset();
                
                    recordMainExecutionLoopStartTime();

                    while (running) {
                        if (realTimeMode) {
                            long sleep_time = time_in_millisec - (time_diff + System.currentTimeMillis());
                            while (sleep_time > 0 && realTimeMode) {
                                long local_sleep_time = sleep_time;
                                if (local_sleep_time > 1000) local_sleep_time = 1000;
                                CpswtUtils.sleep(local_sleep_time);
                                sleep_time = time_in_millisec - (time_diff + System.currentTimeMillis());
                            }
                        }
                        executeTimeStep();
                        terminateIfNeeded();
                        if (running) { // skip if terminated
                            pauseIfNeeded();
                        }
                        if (running) { // skip if terminated during pause; do not merge with the pause if statement
                            advanceLogicalTime();
                        }
                    }
                    
                    waitForFederatesToLeave();
                    logger.info("Simulation terminated");
                    setFederateState(FederateState.TERMINATED);
                    federationEventsHandler.handleEvent(IC2WFederationEventsHandler.C2W_FEDERATION_EVENTS.FEDERATION_SIMULATION_FINISHED, getFederationId());

                    // destroy federation
                    getLRC().resignFederationExecution(ResignAction.DELETE_OBJECTS);
                    getLRC().destroyFederationExecution(getFederationId());
                    stopRTI();
                    logLevel = 0;

                    recordMainExecutionLoopEndTime();
                    System.exit(0);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        };

        running = true;
        mainLoopThread.start();
    }
    
    private void waitUntilInitialized() {
        final FederateState currentState = getFederateState();
        
        if (currentState == FederateState.INITIALIZED) {
            return;
        }
        if (currentState != FederateState.INITIALIZING) {
            throw new IllegalStateException("cannot waitUntilInitialized from " + currentState.name());
        }
        
        logger.debug("Waiting until the federation manager has been initialized...");
        while (getFederateState() != FederateState.INITIALIZED) {
            CpswtUtils.sleep(SynchronizedFederate.internalThreadWaitTimeMs);
        }
    }
    
    private void achieveReadyToPopulate() {
        waitForExpectedFederates();
        try {
            readyToPopulate();
        } catch (FederateNotExecutionMember e) {
            throw new IllegalStateException(e);
        } catch (RTIinternalError e) {
            throw new RuntimeException(e);
        }
    }
    
    private void achieveReadyToRun() {
        try {
            readyToRun();
        } catch (FederateNotExecutionMember e) {
            throw new IllegalStateException(e);
        } catch (RTIinternalError e) {
            throw new RuntimeException(e);
        }
        federationEventsHandler.handleEvent(IC2WFederationEventsHandler.C2W_FEDERATION_EVENTS.FEDERATION_READY_TO_RUN, getFederationId());
    }
    
    private void waitForExpectedFederates() {
        for (FederateJoinInfo federateInfo : experimentConfig.expectedFederates) {
            logger.debug("Waiting for {} federate(s) of type \"{}\" to join", federateInfo.count, federateInfo.federateType);
        }
        
        while (federatesMaintainer.expectedFederatesLeftToJoinCount() > 0) {
            try {
                getLRC().tick();
            } catch (RTIinternalError | ConcurrentAccessAttempted e) {
                throw new RuntimeException(e);
            }
            CpswtUtils.sleep(SynchronizedFederate.internalThreadWaitTimeMs);
        }
        logger.debug("All expected federates have joined the federation. Proceeding with the simulation...");
    }
    
    
    
    
    
    
    public void recordMainExecutionLoopStartTime() {
        logger.debug("Main execution loop of federation started at: {}", new Date());
        tMainLoopStartTime = System.currentTimeMillis();
    }

    public void recordMainExecutionLoopEndTime() {
        if (!executionTimeRecorded) {
            logger.debug("Main execution loop of federation stopped at: {}", new Date());
            tMainLoopEndTime = System.currentTimeMillis();
            executionTimeRecorded = true;
            double execTimeInSecs = (tMainLoopEndTime - tMainLoopStartTime) / 1000.0;
            if (execTimeInSecs > 0) {
                logger.debug("Total execution time of the main loop: {} seconds", execTimeInSecs);
            }
        }
    }
    
    private void executeTimeStep() {
        logger.info("Current Time = {}", time.getTime());
        
        sendScriptInteractions();
        
        if(coaExecutor != null) {
           coaExecutor.executeCOAGraph();
        }
    }
    
    private void terminateIfNeeded() {
        if (this.getFederateState() == FederateState.TERMINATING) {
            return; // termination already started
        }
        if (federationEndTime > 0 && time.getTime() >= federationEndTime) {
            logger.info("Reached federation end time at t = {}", time.getTime());
            terminateSimulation(); // this can set running to FALSE
        }
    }
    
    private void pauseIfNeeded() {
        if (!this.getFederateState().canTransitionTo(FederateState.PAUSED)) {
            logger.debug("Cannot transition to PAUSED from {}", getFederateState());
            return; // does this have a side effect when a pause_time is skipped ?
        }
        
        // Check if the federation should pause
        Iterator<Double> it = pauseTimes.iterator();
        if (it.hasNext()) {
            double pause_time = it.next();
            if (time.getTime() >= pause_time) {
                it.remove();
                pauseSimulation();
            }
        }
        
        while (paused) { // loop until resume or terminate (which sets paused to FALSE)
            CpswtUtils.sleep(10);
        }
    }
    
    private void advanceLogicalTime() throws Exception {
        synchronized (getLRC()) {
            // Request the next time step
            DoubleTime next_time = new DoubleTime(time.getTime() + super.getStepSize());
            getLRC().timeAdvanceRequest(next_time);
            if (realTimeMode) {
                time_diff = time_in_millisec - System.currentTimeMillis();
            }
            logger.debug("Requested advance to logical time t = {}", next_time);

            // Wait for time grant
            granted = false;
            while (!granted) {
                getLRC().tick();
            }
            // time.getTime() has now changed values
        }
    }
    
    private void waitForFederatesToLeave() {
        // the federation manager itself is also stored in federateObjectHandles 
        logger.debug("Waiting for {} federates to resign...", federatesMaintainer.getOnlineFederates().size()-1);
        
        while (federatesMaintainer.getOnlineFederates().size() != 1) {
            try {
                synchronized (lrc) { // i have no idea what problems this can cause
                    lrc.tick();
                }
            } catch (RTIinternalError | ConcurrentAccessAttempted e) {
                logger.error("Unable to wait for other federates to resign.", e);
                return;
            }
            CpswtUtils.sleep(SynchronizedFederate.internalThreadWaitTimeMs);
        }
        logger.debug("All other federates have resigned the federation.");
    }
    


    private void sendScriptInteractions() {
        double tmin = time.getTime() + super.getLookAhead() + (super.getLookAhead() / 10000.0);

        for (double intrtime : script_interactions.keySet()) {
            logger.trace("Interaction time = {}", intrtime);
            List<InteractionRoot> interactionRootList = script_interactions.get(intrtime);
            if (interactionRootList.size() == 0)
                continue;
            if (intrtime < tmin) {

                String interactionClassList = new String();
                boolean notFirst = false;
                for (InteractionRoot interactionRoot : interactionRootList) {
                    if (notFirst) interactionClassList += ", ";
                    notFirst = true;
                    interactionClassList += interactionRoot.getClassName();
                }
                logger.error("Error: simulation passed scheduled interaction time: {}, {}", intrtime, interactionClassList);
            } else if (intrtime >= tmin && intrtime < tmin + super.getStepSize()) {

                List<InteractionRoot> interactionsSent = new ArrayList<InteractionRoot>();
                for (InteractionRoot interactionRoot : interactionRootList) {
                    try {
                        interactionRoot.sendInteraction(getLRC(), intrtime);
                    } catch (Exception e) {
                        logger.error("Failed to send interaction: {}", interactionRoot);
                        logger.error(e.getStackTrace());
                    }
                    interactionsSent.add(interactionRoot);
                    logger.info("Sending out the injected interaction");
                }
                interactionRootList.removeAll(interactionsSent);
            }
        }
    }

    private void resetTimeOffset() {
        time_in_millisec = (long) (time.getTime() * 1000);
        time_diff = time_in_millisec - System.currentTimeMillis();
    }

    public void timeAdvanceGrant(LogicalTime t) {
        fireTimeUpdate(t);
        time_in_millisec = (long) (time.getTime() * 1000);
        granted = true;
    }

    public double getCurrentTime() {
        return time.getTime();
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }

    public void pauseSimulation() {
        logger.debug("Pausing simulation");
        this.paused = true;
        this.setFederateState(FederateState.PAUSED);
    }

    public void resumeSimulation() {
        time_diff = time_in_millisec - System.currentTimeMillis();
        logger.debug("Resuming simulation");
        this.paused = false;
        this.setFederateState(FederateState.RUNNING);
    }

    public void terminateSimulation() {
        if (this.getFederateState() == FederateState.TERMINATING) {
            return; // termination already started
        }
        logger.debug("Terminating simulation");
        this.paused=false; // resume if paused
        this.setFederateState(FederateState.TERMINATING);

        synchronized (super.lrc) {
            try {
                SimEnd simEnd = new SimEnd();
                simEnd.set_originFed(getFederateId());
                simEnd.set_sourceFed(getFederateId());
                simEnd.sendInteraction(getLRC(), time.getTime() + super.getLookAhead());
                
                lrc.synchronizationPointAchieved(SynchronizationPoints.ReadyToResign);
                lrc.tick(); // check if federation already synchronized
            } catch (RTIexception e) { // this needs better exception handling
                logger.fatal(e);
                System.exit(1);
            }
        }
    }
    
    @Override
    public void federationSynchronized(String label) {
        super.federationSynchronized(label);
        
        if (label.equals(SynchronizationPoints.ReadyToResign)) {
            running = false; // exit main loop when able
        }
    }

    public void setRealTimeMode(boolean newRealTimeMode) {
        logger.debug("Setting simulation to run in realTimeMode as: {}", newRealTimeMode);
        this.realTimeMode = newRealTimeMode;
        if (this.realTimeMode)
            this.resetTimeOffset();
    }

    /**
     * LogLevels 0: No logging 1: High priority logs 2: Up to medium priority
     * logs 3: Up to low priority logs 4: Up to very low priority logs (all
     * logs)
     */
    public void updateLogLevel(int selected) {
        if (getLRC() == null) {
            throw new IllegalStateException("cannot update log level before LRC initialized");
        }

        if (logLevel != selected) {
            if (logLevel > selected) {
                // Unsubscribe lower logger levels
                for (int i = logLevel; i > selected; i--) {
                    unsubscribeLogLevel(i);
                }
            } else {
                // Subscribe lower logger levels
                for (int i = logLevel + 1; i <= selected; i++) {
                    subscribeLogLevel(i);
                }
            }
            logLevel = selected;
        }
    }

    private void unsubscribeLogLevel(int level) {
        if (level == 1) {
            logger.debug("Unsusbcribing to High priority logs");
            HighPrio.unsubscribe(getLRC());
        } else if (level == 2) {
            logger.debug("Unsusbcribing to Medium priority logs");
            MediumPrio.unsubscribe(getLRC());
        } else if (level == 3) {
            logger.debug("Unsusbcribing to Low priority logs");
            LowPrio.unsubscribe(getLRC());
        } else if (level == 4) {
            logger.debug("Unsusbcribing to Very Low priority logs");
            VeryLowPrio.unsubscribe(getLRC());
        }
    }

    private void subscribeLogLevel(int level) {
        if (level == 1) {
            logger.debug("Susbcribing to High priority logs");
            HighPrio.subscribe(getLRC());
        } else if (level == 2) {
            logger.debug("Susbcribing to Medium priority logs");
            MediumPrio.subscribe(getLRC());
        } else if (level == 3) {
            logger.debug("Susbcribing to Low priority logs");
            LowPrio.subscribe(getLRC());
        } else if (level == 4) {
            logger.debug("Susbcribing to Very Low priority logs");
            VeryLowPrio.subscribe(getLRC());
        }
    }

    private void fireTimeUpdate(LogicalTime t) {
        DoubleTime newTime = new DoubleTime(0);
        newTime.setTo(t);
        fireTimeUpdate(newTime.getTime());
    }

    private void fireTimeUpdate(double t) {
        DoubleTime prevTime = new DoubleTime(0);
        prevTime.setTime(time.getTime());
        time.setTime(t);
    }

    @Override
    public void synchronizationPointRegistrationSucceeded(String label) {
        registeredSynchronizationPoints.add(label);
    }

    @Override
    public void discoverObjectInstance(int objectHandle, int objectClassHandle, String objectName) {
        ObjectRoot objectRoot = ObjectRoot.discover(objectClassHandle, objectHandle);
        
        if (FederateObject.match(objectClassHandle)) {
            FederateObject federateObject = (FederateObject) objectRoot;
            federatesMaintainer.discoverFederate(objectHandle);
            federationEventsHandler.handleEvent(IC2WFederationEventsHandler.C2W_FEDERATION_EVENTS.FEDERATE_JOINED, Integer.toString(objectHandle));
            federateObject.requestUpdate(getLRC());
        }
    }

    @Override
    public void removeObjectInstance( int theObject, byte[] userSuppliedTag, LogicalTime theTime,
                                      EventRetractionHandle retractionHandle ) {
        logger.error(" removeObjectInstance --------------------------------------------------------- NOT IMPLEMENTED");
    }

    @Override
    public void removeObjectInstance(int theObject, byte[] tag) {
        if (federatesMaintainer.getOnlineFederate(theObject) != null) {
            federatesMaintainer.removeFederate(theObject);
            federationEventsHandler.handleEvent(IC2WFederationEventsHandler.C2W_FEDERATION_EVENTS.FEDERATE_RESIGNED, Integer.toString(theObject));
        }
    }

    /**
     * reflectAttributeValues handles federates that join the federation
     *
     * @param objectHandle        handle (RTI assigned) of the object class instance to which
     *                            the attribute reflections are to be applied
     * @param reflectedAttributes data structure containing attribute reflections for
     *                            the object class instance, i.e. new values for the instance's attributes.
     * @param theTag
     */
    @Override
    public void reflectAttributeValues(int objectHandle, ReflectedAttributes reflectedAttributes, byte[] theTag) {
        // write equivalent version for TSO
        ObjectRoot object = ObjectRoot.reflect(objectHandle, reflectedAttributes);
        
        if (object instanceof FederateObject) {
            FederateObject federateObject = (FederateObject) object;
            federatesMaintainer.updateFederate(objectHandle, federateObject);
        }

        logger.trace("ObjectRootInstance received through reflectAttributeValues");
    }

    @Override
    public void receiveInteraction(int intrHandle, ReceivedInteraction receivedIntr, byte[] tag) {
        //some sanity checking to avoid NPE
        if(intrHandle < 0) {
            logger.error("FederationManager::receiveInteraction (no time): Invalid interaction handle received");
            return;
        }

        if(receivedIntr == null) {
            logger.error("FederationManager::receiveInteraction (no time): Invalid interaction object received");
            return;
        }

        logger.trace("FederationManager::receiveInteraction (no time): Received interaction handle as: {} and interaction as: {}", intrHandle, receivedIntr);

        try {

            // TODO: moved this from legacy 'dumpInteraction' (even though it shouldn't have been there)
            if(this.coaExecutor != null) {
                InteractionRoot interactionRoot = InteractionRoot.create_interaction(intrHandle, receivedIntr);
                if(interactionRoot == null) {
                    logger.error("FederationManager::receiveInteraction (no time): Unable to instantiate interactionRoot");
                    return;
                }
                // Inform COA orchestrator of arrival of interaction (for awaited Outcomes, if any)
                coaExecutor.updateArrivedInteractions(intrHandle, time, interactionRoot);
            }

            // "federate join" interaction
            if(FederateJoinInteraction.match(intrHandle)) {
                FederateJoinInteraction federateJoinInteraction = new FederateJoinInteraction(receivedIntr);
                if(federateJoinInteraction == null) {
                    logger.error("FederationManager::receiveInteraction (no time): Unable to instantiate federateJoinInteraction");
                    return;
                }
                logger.trace("FederateJoinInteraction received :: {} joined", federateJoinInteraction.toString());

                // ??
                federationEventsHandler.handleEvent(IC2WFederationEventsHandler.C2W_FEDERATION_EVENTS.FEDERATE_JOINED, federateJoinInteraction.get_FederateId());

                this.federatesMaintainer.updateFederate(federateJoinInteraction);

            }
        } catch (Exception e) {
            logger.error("Error while parsing the logger interaction");
            logger.error(e);
        }
    }

    @Override
    public void receiveInteraction(int intrHandle, ReceivedInteraction receivedIntr, byte[] tag, LogicalTime theTime, EventRetractionHandle retractionHandle) {
        //some sanity checking to avoid NPE
        if(intrHandle < 0) {
            logger.error("FederationManager::receiveInteraction (with time): Invalid interaction handle received");
            return;
        }

        if(receivedIntr == null) {
            logger.error("FederationManager::receiveInteraction (with time): Invalid interaction object received");
            return;
        }

        logger.trace("FederationManager::receiveInteraction (with time): Received interaction handle as: {} and interaction as: {}", intrHandle, receivedIntr);

        try {

            // TODO: moved this from legacy 'dumpInteraction' (even though it shouldn't have been there)
            if(this.coaExecutor != null) {
                InteractionRoot interactionRoot = InteractionRoot.create_interaction(intrHandle, receivedIntr);
                if(interactionRoot == null) {
                    logger.error("FederationManager::receiveInteraction (with time): Unable to instantiate interactionRoot");
                    return;
                }
                // Inform COA orchestrator of arrival of interaction (for awaited Outcomes, if any)
                coaExecutor.updateArrivedInteractions(intrHandle, time, interactionRoot);
            }

            // "federate join" interaction
            if(FederateJoinInteraction.match(intrHandle)) {
                FederateJoinInteraction federateJoinInteraction = new FederateJoinInteraction(receivedIntr);
                if(federateJoinInteraction == null) {
                    logger.error("FederationManager::receiveInteraction (with time): Unable to instantiate federateJoinInteraction");
                    return;
                }
                logger.trace("FederateJoinInteraction received :: {} joined", federateJoinInteraction.toString());

                // ??s
                federationEventsHandler.handleEvent(IC2WFederationEventsHandler.C2W_FEDERATION_EVENTS.FEDERATE_JOINED, federateJoinInteraction.get_FederateId());

                this.federatesMaintainer.updateFederate(federateJoinInteraction);

            }
        } catch (Exception e) {
            logger.error("Error while parsing the logger interaction");
            logger.error("intrHandle=" + intrHandle);
            logger.error("_federationEventsHandler=" + federationEventsHandler);
            logger.error(e);
        }
    }

    @Override
    public void onTerminateRequested() {
        if (!setFederateState(FederateState.TERMINATING)) {
            logger.error("failed to terminate using COA because of bad state transition");
        }
    }

    @Override
    public double onCurrentTimeRequested() {
        return getCurrentTime();
    }

    public List<FederateInfo> getFederatesStatus() {
        return federatesMaintainer.getAllMaintainedFederates();
    }

    @Override
    public void federateStateChanged(FederateStateChangeEvent stateChange) {
        
    }
}
