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

import java.io.File;
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
import org.portico.impl.hla13.types.HLA13ReflectedAttributes;

import org.portico.lrc.services.object.msg.UpdateAttributes;

import hla.rti.ArrayIndexOutOfBounds;
import hla.rti.ConcurrentAccessAttempted;
import hla.rti.CouldNotOpenFED;
import hla.rti.ErrorReadingFED;
import hla.rti.EventRetractionHandle;
import hla.rti.FederateLoggingServiceCalls;
import hla.rti.FederateNotExecutionMember;
import hla.rti.FederationExecutionAlreadyExists;
import hla.rti.InteractionClassNotDefined;
import hla.rti.InteractionClassNotSubscribed;
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
public class FederationManager extends SynchronizedFederate implements COAExecutorEventListener, FederateStateChangeListener, Runnable {
    private static final Logger logger = LogManager.getLogger();

    private Set<String> registeredSynchronizationPoints = new HashSet<>();
    
    private FederatesMaintainer federatesMaintainer = new FederatesMaintainer();
    
    private IC2WFederationEventsHandler federationEventsHandler = null;

    private String federationId;

    private boolean realTimeMode = true;

    private ExperimentConfig experimentConfig;

    private Set<Double> pauseTimes = new HashSet<>();

    private double federationEndTime = 0.0;

    private Map<Double, List<InteractionRoot>> script_interactions = new TreeMap<Double, List<InteractionRoot>>();
    
    private List<InteractionRoot> initialization_interactions = new ArrayList<InteractionRoot>();

    private boolean running = false;

    private boolean paused = false;

    private boolean federationAttempted = false;

    boolean timeRegulationEnabled = false;

    boolean timeConstrainedEnabled = false;

    private boolean granted = false;

    private DoubleTime time = new DoubleTime(0);

    private long time_in_millisec = 0;

    private long time_diff;
    
    private String rootDir;
    
    private Set<Integer> federateObjectHandles = new HashSet<Integer>();

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
     * @throws Exception I have no idea why we have this here. Especially pure Exception type...
     */
    public FederationManager(FederationManagerConfig params) throws Exception {
        super(new FederateConfig(
                SynchronizedFederate.FEDERATION_MANAGER_NAME,
                params.federationId,
                false, // isLateJoiner
                params.lookAhead,
                params.stepSize));
        logger.info("Initializing...");

        // record config parameters
        this.federationId = params.federationId;
        this.federationEndTime = params.federationEndTime;
        this.realTimeMode = params.realTimeMode;

        // set project's root directory
        rootDir = System.getenv(CpswtDefaults.RootPathEnvVarKey);
        if (rootDir == null) {
            logger.trace("There was no {} environment variable set. Setting RootDir to \"user.dir\" system property.", CpswtDefaults.RootPathEnvVarKey);
            rootDir = System.getProperty("user.dir");
        }

        // build fed file URL
        Path fedFilePath = Paths.get(params.fedFile);
        URL fedFileURL;
        if (fedFilePath.isAbsolute()) {
            fedFileURL = fedFilePath.toUri().toURL();
        } else {
            fedFileURL = Paths.get(rootDir, params.fedFile).toUri().toURL();
        }
        logger.trace("FOM file should be found under {}", fedFileURL);

        // TODO: Prepare core to be able to stream events when needed #27
        this.federationEventsHandler = new C2WFederationEventsHandler();

        Path experimentConfigFilePath = CpswtUtils.getConfigFilePath(params.experimentConfig, rootDir);

        logger.trace("Loading experiment config file {}", experimentConfigFilePath);
        this.experimentConfig = ConfigParser.parseConfig(experimentConfigFilePath.toFile(), ExperimentConfig.class);
        logger.trace("Experiment config loaded");
        logger.trace("Updating Federate Join Info in the FederatesMaintainer");
        this.federatesMaintainer.updateFederateJoinInfo(this.experimentConfig);
        logger.trace("Checking pause times in experiment config: {}", this.experimentConfig.pauseTimes);
        if(this.experimentConfig.pauseTimes != null) {
            this.pauseTimes.addAll(this.experimentConfig.pauseTimes);
        }

        if(this.federatesMaintainer.expectedFederatesLeftToJoinCount() == 0) {
            logger.debug("No expected federates are defined");
        }

        initializeCOA();
        initializeLRC(fedFileURL);

        // Before beginning simulation, initialize COA sequence graph
        if(this.coaExecutor != null) {
            this.coaExecutor.setRTIambassador(getLRC());
            this.coaExecutor.initializeCOAGraph();
        }

        super.addFederateStateChangeListener(this);
    }
    
    private void initializeCOA() throws IOException {
        final String COASelectionToExecute = experimentConfig.COASelectionToExecute;
        final boolean terminateOnCOAFinish = experimentConfig.terminateOnCOAFinish;
        
        if( experimentConfig.coaDefinition == null || "".equals(experimentConfig.coaDefinition) ) {
            logger.info("COA Not Initialized - no COA definitions were provided!");
            return;
        }
        if( experimentConfig.coaSelection == null || "".equals(experimentConfig.coaSelection) ) {
            logger.info("COA Not Initialized - no COA selections were provided!");
            return;
        }
        if( COASelectionToExecute == null || "".equals(COASelectionToExecute) ) {
            logger.info("COA Not Initialized - no COA selection to execute was specified!");
            return;
        }
        
        logger.trace("Initializing the COA executor...");
            
        Path coaDefinitionPath = CpswtUtils.getConfigFilePath(experimentConfig.coaDefinition, rootDir);
        Path coaSelectionPath = CpswtUtils.getConfigFilePath(experimentConfig.coaSelection, rootDir);

        COALoader coaLoader = new COALoader(coaDefinitionPath, coaSelectionPath, COASelectionToExecute);
        COAGraph coaGraph = coaLoader.loadGraph();

        coaExecutor = new COAExecutor(getFederationId(), getFederateId(), getLookAhead(), terminateOnCOAFinish, getLRC());
        coaExecutor.setCoaExecutorEventListener(this);
        coaExecutor.setCOAGraph(coaGraph);
        
        logger.info("COA initialized to execute {}", COASelectionToExecute);
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
        
        logger.debug("Initialized the local runtime component.");
    }

    private void createFederation(URL fedFileURL) throws IOException {
        logger.trace("[{}] Attempting to create the federation \"{}\"...", getFederateId(), federationId);
        try {
            federationEventsHandler.handleEvent(IC2WFederationEventsHandler.C2W_FEDERATION_EVENTS.CREATING_FEDERATION, federationId);
            lrc.createFederationExecution(federationId, fedFileURL);
            federationEventsHandler.handleEvent(IC2WFederationEventsHandler.C2W_FEDERATION_EVENTS.FEDERATION_CREATED, federationId);
        } catch (CouldNotOpenFED | ErrorReadingFED e) {
            throw new IOException("failed to read file " + fedFileURL.toString(), e);
        } catch (FederationExecutionAlreadyExists e) {
            throw new RuntimeException("federation already exists: " + federationId, e);
        } catch (RTIinternalError | ConcurrentAccessAttempted e) {
            throw new RuntimeException(e);
        }
        logger.debug("Federation \"{}\" created successfully.", federationId);
    }
    
    private void enableTimePolicy() {
        // PER THE HLA BOOK, ENABLE TIME-CONSTRAINED FIRST, THEN TIME-REGULATING
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
    }
    
    private void definePubSubInterests() {
        FederateJoinInteraction.subscribe(getLRC());

        FederateObject.subscribe_FederateHandle();
        FederateObject.subscribe_FederateType();
        FederateObject.subscribe_FederateHost();
        FederateObject.subscribe(getLRC());
        
        SimEnd.publish(getLRC());
        SimPause.publish(getLRC());
        SimResume.publish(getLRC());
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

    /**
     * Start the federation run - federation that has been created already in the initializeLRC() -- TEMP comment, needs to be refactored
     *
     * @throws Exception
     */
    private synchronized void startFederationRun() throws Exception {
        federationAttempted = true;

        waitExpectedFederatesToJoin();

        logger.trace("Waiting for \"{}\"...", SynchronizationPoints.ReadyToPopulate);
        super.readyToPopulate();
        logger.trace("{} done.", SynchronizationPoints.ReadyToPopulate);

        logger.trace("Waiting for \"{}\"...", SynchronizationPoints.ReadyToRun);
        super.readyToRun();
        logger.trace("{} done.", SynchronizationPoints.ReadyToRun);

        federationEventsHandler.handleEvent(IC2WFederationEventsHandler.C2W_FEDERATION_EVENTS.FEDERATION_READY_TO_RUN, federationId);

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

        // run rti on a spearate thread
        Thread mainFederationManagerRunThread = new Thread() {
            public void run() {

                try {
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
                    federationEventsHandler.handleEvent(IC2WFederationEventsHandler.C2W_FEDERATION_EVENTS.FEDERATION_SIMULATION_FINISHED, federationId);

                    // destroy federation
                    getLRC().resignFederationExecution(ResignAction.DELETE_OBJECTS);
                    getLRC().destroyFederationExecution(federationId);
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
        mainFederationManagerRunThread.start();
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
        logger.debug("Waiting for {} federates to resign...", federateObjectHandles.size()-1);
        
        while (federateObjectHandles.size() != 1) {
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

    private void waitExpectedFederatesToJoin() throws Exception {
        for (FederateJoinInfo federateInfo : this.experimentConfig.expectedFederates) {
            logger.trace("Waiting for {} federate{} of type \"{}\" to join", federateInfo.count, federateInfo.count <= 1 ? "" : "s", federateInfo.federateType);
        }

        int numOfFedsToWaitFor = this.federatesMaintainer.expectedFederatesLeftToJoinCount();
        while (numOfFedsToWaitFor > 0) {
            try {
                super.lrc.tick();
                CpswtUtils.sleep(SynchronizedFederate.internalThreadWaitTimeMs);
                numOfFedsToWaitFor = this.federatesMaintainer.expectedFederatesLeftToJoinCount();
            }
            catch(Exception e) {
                CpswtUtils.sleep(SynchronizedFederate.internalThreadWaitTimeMs);
            }
        }
        logger.debug("All expected federates have joined the federation. Proceeding with the simulation...");
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

    public boolean federationAlreadyAttempted() {
        return federationAttempted;
    }

    public static void configureSimulation(File f) throws Exception {
    }

    public void startSimulation() throws Exception {
        logger.debug("Starting simulation");
        this.setFederateState(FederateState.INITIALIZED);
        if (!federationAlreadyAttempted()) {
            this.startFederationRun();
        }
        paused = false;
        this.setFederateState(FederateState.RUNNING);
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
    public void updateLogLevel(int selected) throws Exception {
        logLevelToSet = selected;

        if (getLRC() == null) {
            return;
        }

        if (logLevel == selected) {
            // do nothing
        } else {
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

    private void unsubscribeLogLevel(int level)
            throws Exception, InteractionClassNotDefined, InteractionClassNotSubscribed,
            FederateNotExecutionMember, SaveInProgress, RestoreInProgress,
            RTIinternalError, ConcurrentAccessAttempted {
        if (level > 0) {
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
    }

    private void subscribeLogLevel(int level)
            throws Exception, InteractionClassNotDefined, FederateNotExecutionMember,
            FederateLoggingServiceCalls, SaveInProgress, RestoreInProgress,
            RTIinternalError, ConcurrentAccessAttempted {
        if (level > 0) {
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
        super.discoverObjectInstance(objectHandle, objectClassHandle, objectName);
        
        if (FederateObject.match(objectClassHandle)) {
            if (federateObjectHandles.add(objectHandle)) {
                logger.debug("Federate {} Joined - {} federates joined", objectHandle, federateObjectHandles.size());
            } else {
                logger.error("Failed to discover joined federate [{}]{}", objectHandle, objectName);
            }
        }
    }

    @Override
    public void removeObjectInstance( int theObject, byte[] userSuppliedTag, LogicalTime theTime,
                                      EventRetractionHandle retractionHandle ) {
        logger.error(" removeObjectInstance --------------------------------------------------------- NOT IMPLEMENTED");
    }

    @Override
    public void removeObjectInstance(int theObject, byte[] tag) {
        if (federateObjectHandles.remove(theObject)) {
            logger.debug("Federate {} Resigned - {} federates remaining", theObject, federateObjectHandles.size());
        }
//        try {
//            String federateType = _discoveredFederates.get(theObject);
//            boolean registeredFederate = expectedFederateTypes.contains(federateType);
//
//            if (!registeredFederate) {
//                logger.info("Unregistered \"" + federateType + "\" federate has resigned the federation.\n");
//            } else {
//                logger.info("\"" + federateType + "\" federate has resigned the federation\n");
//                _processedFederates.remove(federateType);
//                _federationEventsHandler.handleEvent(IC2WFederationEventsHandler.C2W_FEDERATION_EVENTS.FEDERATE_RESIGNED, federateType);
//            }
//            return;
//        } catch (Exception e) {
//            logger.error("Error while parsing the Federate object: {}", e.getMessage());
//            logger.error(e);
//        }
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

        // check if current objectHandle has been already discovered by the RTI
        ObjectRoot o = ObjectRoot.getObject(objectHandle);

//        if (!this.rtiDiscoveredFederateObjects.contains(o) && !this.rtiDiscoveredCpswtFederateInfoObjects.contains(o))
//            return;

        // for user-defined objects there's no need to change to length-1
        // I have no idea why this piece of code was here
//        if(!(o instanceof CpswtFederateInfoObject)) {
            // transform attributes
            UpdateAttributes updateAttributes = new UpdateAttributes();
            try {
                for (int ix = 0; ix < reflectedAttributes.size(); ++ix) {
                    byte[] currentValue = reflectedAttributes.getValue(ix);

                    byte[] newValue = new byte[currentValue.length - 1];
                    for (int jx = 0; jx < newValue.length; ++jx) {
                        newValue[jx] = currentValue[jx];
                    }

                    updateAttributes.addFilteredAttribute(reflectedAttributes.getAttributeHandle(ix), newValue, null);
                }
            } catch (ArrayIndexOutOfBounds aioob) {
                logger.error("Error while processing reflectedAttributes. {}", aioob);
            }

            reflectedAttributes = new HLA13ReflectedAttributes(updateAttributes.getFilteredAttributes());
//        }

        try {
            ObjectRoot objRootInstance = ObjectRoot.reflect(objectHandle, reflectedAttributes);

            logger.trace("ObjectRootInstance received through reflectAttributeValues");

            // Federate info type object
//            if(objRootInstance instanceof CpswtFederateInfoObject) {
//            // if (CpswtFederateInfoObject.match(objectHandle)) {
//                logger.trace("Handling CpswtFederateInfoObject");
//                CpswtFederateInfoObject federateInfoObject = (CpswtFederateInfoObject) objRootInstance;
//
//                this.rtiDiscoveredCpswtFederateInfoObjects.remove(federateInfoObject);
//
//                if (federateInfoObject.get_FederateId().isEmpty() ||
//                        federateInfoObject.get_FederateType().isEmpty()) {
//                    logger.error("THIS SHOULDN'T HAPPEN RIGHT??");
//                    return;
//                }
//
//                String federateId = federateInfoObject.get_FederateId();
//                String federateType = federateInfoObject.get_FederateType();
//                boolean isLateJoiner = federateInfoObject.get_IsLateJoiner();
//
//                // this?
//                _federationEventsHandler.handleEvent(IC2WFederationEventsHandler.C2W_FEDERATION_EVENTS.FEDERATE_JOINED, federateId);
//
//                // federationManager case
//                if (federateType.equals(SynchronizedFederate.FEDERATION_MANAGER_NAME)) {
//                    logger.info("{} federate joined the federation", SynchronizedFederate.FEDERATION_MANAGER_NAME);
//                    return;
//                }
//
//                if (!this.experimentConfig.federateTypesAllowed.contains(federateType)) {
//                    logger.warn("{} federate type is not allowed to join this federation. Ignoring...", federateType);
//                    return;
//                }
//                // everything else
//                else {
//                    if(!isLateJoiner) {
//                        int remaining = this.workingExperimentConfig.getRemainingCountForExpectedType(federateType);
//                        if (remaining == 0) {
//                            logger.warn("{} federate is not late joiner but all expected federates already joined. Ignoring...");
//                            return;
//                        }
//                    }
//
//                    // expected
//                    if (this.workingExperimentConfig.isExpectedFederateType(federateType)) {
//                        MutableInt v = this.onlineExpectedFederateTypes.get(federateType);
//                        if (v == null) {
//                            v = new MutableInt(1);
//                            this.onlineExpectedFederateTypes.put(federateType, v);
//                        } else {
//                            v.increment();
//                        }
//
//                        logger.info("{} #{} expected federate joined the federation with ID {}", federateType, v.getValue(), federateId);
//
//                        // decrease the counter for the expected federate
//                        for (FederateJoinInfo fed : this.workingExperimentConfig.expectedFederates) {
//                            if (fed.federateType.equals(federateType)) {
//                                fed.count--;
//                                break;
//                            }
//                        }
//                    }
//                    // late joiner
//                    else if (this.workingExperimentConfig.isLateJoinerFederateType(federateType)) {
//                        MutableInt v = this.onlineLateJoinerFederateTypes.get(federateType);
//                        if (v == null) {
//                            v = new MutableInt(1);
//                            this.onlineLateJoinerFederateTypes.put(federateType, v);
//                        } else {
//                            v.increment();
//                        }
//
//                        logger.info("{} #{} late joiner federate joined the federation with ID {}", federateType, v.getValue(), federateId);
//                    }
//                    // unknown
//                    else {
//                        logger.warn("FederateType \"{}\" is neither expected nor late joiner. Ignoring...", federateType);
//                    }
//                }
//            } else {
//                FederateObject federateObject = (FederateObject) ObjectRoot.reflect(objectHandle, reflectedAttributes);
//                logger.trace("Handling FederateObject ({})", federateObject.get_FederateId());
//
//                // if any attribute of the federateObject is empty, ignore
//                if (federateObject.get_FederateHandle() == 0 ||
//                        "".equals(federateObject.get_FederateId()) ||
//                        "".equals(federateObject.get_FederateHost())
//                        ) return;
//
//                //
//                this.rtiDiscoveredFederateObjects.remove(federateObject);
//            }
        } catch (Exception e) {
            logger.error("Error while parsing the Federate object: " + e.getMessage());
            logger.error(e);
        }
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

                this.federatesMaintainer.federateJoined(new FederateInfo(federateJoinInteraction.get_FederateId(), federateJoinInteraction.get_FederateType(), federateJoinInteraction.get_IsLateJoiner()));

            }
            // "federate resign" interaction
            else if(FederateResignInteraction.match(intrHandle)) {
                FederateResignInteraction federateResignInteraction = new FederateResignInteraction(receivedIntr);
                if(federateResignInteraction == null) {
                    logger.error("FederationManager::receiveInteraction (no time): Unable to instantiate federateResignInteraction");
                    return;
                }
                logger.trace("FederateResignInteraction received :: {} resigned", federateResignInteraction.toString());

                // ??
                federationEventsHandler.handleEvent(IC2WFederationEventsHandler.C2W_FEDERATION_EVENTS.FEDERATE_RESIGNED, federateResignInteraction.get_FederateId());

                FederateInfo federateInfo = this.federatesMaintainer.getFederateInfo(federateResignInteraction.get_FederateId());
                this.federatesMaintainer.federateResigned(federateInfo);
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

                this.federatesMaintainer.federateJoined(new FederateInfo(federateJoinInteraction.get_FederateId(), federateJoinInteraction.get_FederateType(), federateJoinInteraction.get_IsLateJoiner()));

            }
            // "federate resign" interaction
            else if(FederateResignInteraction.match(intrHandle)) {
                FederateResignInteraction federateResignInteraction = new FederateResignInteraction(receivedIntr);
                if(federateResignInteraction == null) {
                    logger.error("FederationManager::receiveInteraction (with time): Unable to instantiate federateResignInteraction");
                    return;
                }
                logger.trace("FederateResignInteraction received :: {} resigned", federateResignInteraction.toString());

                // ??
                federationEventsHandler.handleEvent(IC2WFederationEventsHandler.C2W_FEDERATION_EVENTS.FEDERATE_RESIGNED, federateResignInteraction.get_FederateId());

                FederateInfo federateInfo = this.federatesMaintainer.getFederateInfo(federateResignInteraction.get_FederateId());
                this.federatesMaintainer.federateResigned(federateInfo);
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
        terminateSimulation();
    }

    @Override
    public double onCurrentTimeRequested() {
        return getCurrentTime();
    }

    public List<FederateInfo> getFederatesStatus() {
        return federatesMaintainer.getAllMaintainedFederates();
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void federateStateChanged(FederateStateChangeEvent e) {
        // TODO Auto-generated method stub
        
    }
}
