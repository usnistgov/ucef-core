package org.cpswt.hla;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cpswt.config.ExperimentConfig;
import org.cpswt.config.FederateJoinInfo;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * FederatesMaintainer
 */
public class FederatesMaintainer {
    private static final Logger logger = LogManager.getLogger();
    
    // map a FederateObject handle to its FederateInfo; FederateInfo may be null!
    private Map<Integer, FederateInfo> onlineFederates;
    
    // map a federate's handle to its FederateObject handle
    private Map<Integer, Integer> federateHandleToInstanceHandle;
    
    private List<FederateInfo> resignedFederates;
    
    // map a federate type to the number of expected federates of that type that still need to join; the number may be negative!
    private Map<String, Integer> expectedFederatesRemaining;
    
    public FederatesMaintainer() {
        this.onlineFederates = new HashMap<Integer, FederateInfo>();
        this.federateHandleToInstanceHandle = new HashMap<Integer, Integer>();
        this.resignedFederates = new ArrayList<>();
    }
    
    public void discoverFederate(int handle) {
        if (onlineFederates.containsKey(handle)) {
            throw new RuntimeException("discovered duplicate object instance handle: " + Integer.toString(handle));
        }
        onlineFederates.put(handle, null); // no federate info until the object attributes are reflected
    }
    
    public void updateFederate(int instanceHandle, FederateObject instance) {
        final int federateHandle = instance.get_FederateHandle();
        
        if (!onlineFederates.containsKey(instanceHandle)) {
            throw new RuntimeException("reflected undiscovered object instance: " + Integer.toString(instanceHandle));
        }
        
        if (onlineFederates.get(instanceHandle) != null) {
            // all federate object attributes relevant to the maintainer are defined as static in the HLA OMT
            // therefore, this executes only when a non-relevant attribute has updated and we can ignore it
            logger.debug("ignored attribute updates to the object instance for federate {}", federateHandle);
            return;
        }
        
        onlineFederates.put(instanceHandle, new FederateInfo(instance));
        federateHandleToInstanceHandle.put(federateHandle, instanceHandle);
        
        logCurrentStatus();
    }
    
    public void updateFederate(FederateJoinInteraction interaction) {
        final int federateHandle = interaction.get_FederateHandle();
        
        if (!federateHandleToInstanceHandle.containsKey(federateHandle)) {
            throw new RuntimeException("received join from unknown federate: " + Integer.toString(federateHandle));
        }
        
        final int instanceHandle = federateHandleToInstanceHandle.get(federateHandle);
        FederateInfo federateInfo = onlineFederates.get(instanceHandle);
        
        if (federateInfo.getFederateId() != null) {
            /*
             * This executes when one federate sends multiple instances of the federate join interaction. The current
             * behavior throws a runtime exception for the two reasons that follow.
             * 
             * It is difficult to support a federate that changes its late join status. Any such implementation must
             * handle the case where Federate A changes its status from EXPECTED to LATE JOINER at the same time that
             * Federate B changes its status from LATE JOINER to EXPECTED. This is a race condition when Federate B is
             * the last federate required to start the federation execution.
             * 
             * It is impossible to resolve this using the values from the first received federate join interaction. The
             * join interaction is sent receive-order and it is not possible to say which of two received interactions
             * from the same federate was sent first.
             * 
             * Just don't send multiple join interactions.
             */
            throw new RuntimeException("received multiple joins from federate: " + Integer.toString(federateHandle));
        }
        
        federateInfo.update(interaction);
        if (federateInfo.isExpected()) {
            int remaining = expectedFederatesRemaining.get(federateInfo.getFederateType()) - 1;
            if (remaining < 0) {
                logger.warn("More federates than expected for federate type {}!", federateInfo.getFederateType());
            }
            expectedFederatesRemaining.put(federateInfo.getFederateType(), remaining);
        }
        
        logCurrentStatus();
    }
    
    public void removeFederate(int instanceHandle) {
        if (!onlineFederates.containsKey(instanceHandle)) { // this maybe should return false instead
            throw new RuntimeException("removed unknown object instance handle: " + Integer.toString(instanceHandle));
        }
        
        FederateInfo federateInfo = onlineFederates.remove(instanceHandle);
        
        if (federateInfo == null) {
            logger.debug("unknown federate resigned: {}", instanceHandle);
            return;
        }
        
        federateHandleToInstanceHandle.remove(federateInfo.getFederateHandle());
        federateInfo.setResignTime(DateTime.now());
        resignedFederates.add(federateInfo);
        
        if (federateInfo.isExpected()) {
            int remaining = expectedFederatesRemaining.get(federateInfo.getFederateType()) + 1;
            expectedFederatesRemaining.put(federateInfo.getFederateType(), remaining);
        }
        
        logCurrentStatus();
    }
    
    public FederateInfo getOnlineFederate(int instanceHandle) {
        return this.onlineFederates.get(instanceHandle);
    }

    int expectedFederatesLeftToJoinCount() {
        return this.expectedFederatesRemaining.values()
                .stream()
                .mapToInt(Integer::intValue)
                .filter(i -> i >= 0)
                .sum();
    }

    void updateFederateJoinInfo(ExperimentConfig experimentConfig) {
        this.expectedFederatesRemaining = experimentConfig.expectedFederates
                .stream()
                .collect(Collectors.toMap(f -> f.federateType, f -> f.count));
    }

    List<FederateInfo> getOnlineFederates() {
        return this.onlineFederates.values()
                .stream()
                .collect(Collectors.toList());
    }
    
    List<FederateInfo> getOnlineExpectedFederates() {
        return this.onlineFederates.values()
                .stream()
                .filter(fi -> fi.isExpected())
                .collect(Collectors.toList());
    }

    List<FederateInfo> getOnlineLateJoinerFederates() {
        return this.onlineFederates.values()
                .stream()
                .filter(fi -> !fi.isExpected())
                .collect(Collectors.toList());
    }

    List<FederateInfo> getAllMaintainedFederates() {
        return Stream.concat(this.onlineFederates.values().stream(), this.resignedFederates.stream()).collect(Collectors.toList());
    }

    // TEMP
    void logCurrentStatus() {
        logger.trace("expectedFederateJoinInfo ::");
        for (Map.Entry<String, Integer> entry : expectedFederatesRemaining.entrySet()) {
            logger.trace("\t[{}] [{}]", entry.getValue(), entry.getKey());
        }
        if (this.expectedFederatesRemaining.size() == 0) {
            logger.trace("\t NONE");
        }

        logger.trace("onlineFederates ::");
        for (FederateInfo fi : this.onlineFederates.values()) {
            logger.trace("\t[{}] :: [JOINED @ {}] :: {}", fi.isExpected() ? "EXPECTED" : " LATEJOINER ", fi.getJoinTime(), fi.getFederateId());
        }
        if (this.onlineFederates.size() == 0) {
            logger.trace("\t NONE");
        }

        logger.trace("resignedFederates ::");
        for (FederateInfo fi : this.resignedFederates) {
            logger.trace("\t[{}] :: [RESIGNED @ {}] :: {}", fi.isExpected() ? "EXPECTED" : " LATEJOINER ", fi.getResignTime(), fi.getFederateId());
        }
        if (this.resignedFederates.size() == 0) {
            logger.trace("\t NONE");
        }
    }
}
