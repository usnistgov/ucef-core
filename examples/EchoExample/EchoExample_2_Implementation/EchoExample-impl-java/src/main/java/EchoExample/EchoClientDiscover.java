package EchoExample;

import org.cpswt.utils.CpswtDefaults;
import org.cpswt.config.FederateConfig;
import org.cpswt.config.FederateConfigParser;
import org.cpswt.hla.FederateObject;
import org.cpswt.hla.InteractionRoot;
import org.cpswt.hla.ObjectRoot;
import org.cpswt.hla.base.AdvanceTimeRequest;
import org.cpswt.hla.base.ObjectReflector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cpswt.utils.CpswtUtils;

import hla.rti.EventRetractionHandle;
import hla.rti.LogicalTime;

import java.util.HashSet;
import java.util.Set;

public class EchoClientDiscover extends EchoClientBase {

    private static final Logger logger = LogManager.getLogger(EchoClient.class);
    private Set<Integer> pendingFederateObjects = new HashSet<Integer>();
    private Set<String> discoveredFederates = new HashSet<String>();

    public EchoClientDiscover(FederateConfig params) throws Exception {
        super(params);
        FederateObject.subscribe_FederateType();
        FederateObject.subscribe(getLRC());
    }

    @Override
    public void discoverObjectInstance(int objectHandle, int objectClassHandle, String objectName) {
        ObjectRoot discoveredObject = ObjectRoot.discover(objectClassHandle, objectHandle);
        if (FederateObject.match(objectClassHandle)) {
            if (discoveredObject != null) {
                logger.info("discovered object " + discoveredObject.toString());
                pendingFederateObjects.add(objectHandle);
            }
        }
    }

    @Override
    public void removeObjectInstance(int theObject, byte[] tag) {
        this.removeObjectInstance(theObject, tag, null, null);
    }

    @Override
    public void removeObjectInstance( int theObject, byte[] userSuppliedTag, LogicalTime theTime,
                                      EventRetractionHandle retractionHandle ) {
        ObjectRoot removedObject = ObjectRoot.removeObject(theObject);
        if (removedObject != null) {
            logger.info("removed object " + removedObject.toString());
            if (removedObject instanceof FederateObject) {
                // small chance this might be null if never discovered ?
                String type = ((FederateObject)removedObject).get_FederateId();
                discoveredFederates.remove(type);
                pendingFederateObjects.remove(theObject);
            }
        }
    }

    private final int sendMessageCount = 125;
    int sequenceNumber = 0;
    Set<Integer> sentSequenceNumbers = new HashSet<Integer>();
    // long waitToSendNextMessage = 10000;

    private void execute() throws Exception {

        double currentTime = 1.0;

        if (super.isLateJoiner()) {
            currentTime = super.getLBTS() - super.getLookAhead();
            super.disableTimeRegulation();
        }

        AdvanceTimeRequest atr = new AdvanceTimeRequest(currentTime);
        putAdvanceTimeRequest(atr);

        if (!super.isLateJoiner()) {
            readyToPopulate();
            readyToRun();
        }

        startAdvanceTimeThread();

        InteractionRoot interactionRoot;

        while (true) {
            // Wait for time to be granted by the RTI
            logger.debug("{}: requesting RTI to go to time: {}", this.getFederateId(), currentTime);

            currentTime += super.getStepSize();

            atr.requestSyncStart();
            enteredTimeGrantedState();

            for (int objectHandle : pendingFederateObjects) {
                ObjectRoot objectInstance = ObjectRoot.getObject(objectHandle);
                getLRC().requestObjectAttributeValueUpdate(objectHandle, objectInstance.getSubscribedAttributeHandleSet());
            }
            pendingFederateObjects.clear();

            // Waiting for incoming interactions
            while ((interactionRoot = getNextInteractionNoWait()) != null) {
                if (interactionRoot instanceof ServerReply) {

                    ServerReply reply = (ServerReply) interactionRoot;
                    if (reply.get_targetFed().equals(this.getFederateId())) {
                        int replySeqNum = reply.get_sequenceNumber();
                        if (this.sentSequenceNumbers.contains(replySeqNum)) {
                            this.sentSequenceNumbers.remove(replySeqNum);
                            logger.debug("{}: Got a server reply back with sequence number: {}", this.getFederateId(), replySeqNum);
                        } else {
                            logger.debug("{}: Server reply with sequence number unknown: {}", this.getFederateId(), replySeqNum);
                        }
                    }
                }
                else {
                    logger.trace("Interaction received is not type of ServerReply");
                }
            }

            ObjectReflector reflector = null;
            while ((reflector = getNextObjectReflectorNoWait()) != null) {
                reflector.reflect();
                ObjectRoot object = reflector.getObjectRoot();
                if (object instanceof FederateObject) {
                    logger.debug("received object update " + object.toString());
                }
            }


            if (this.sequenceNumber >= this.sendMessageCount) {
                break;
            }

            // Send interactions to RTI
            this.sendClientMessage(currentTime + this.getLookAhead());


            AdvanceTimeRequest newATR = new AdvanceTimeRequest(currentTime);
            putAdvanceTimeRequest(newATR);

            atr.requestSyncEnd();
            atr = newATR;

            // wait until next message to send
            // Thread.sleep(this.waitToSendNextMessage);
        }

        // done with sending the messages, time to resign
        super.notifyFederationOfResign();
    }

    void sendClientMessage(double currentTime) throws Exception {
        ClientMessage message = create_ClientMessage();
        this.sequenceNumber++;
        message.set_sequenceNumber(this.sequenceNumber);

        logger.debug("{}: Sending echo message interaction #{}", this.getFederateId(), this.sequenceNumber);
        message.sendInteraction(getLRC(), currentTime);

        // store sent sequenceNumber
        sentSequenceNumbers.add(this.sequenceNumber);
    }

    public static void main(String[] args) {
        try {
            FederateConfigParser federateConfigParser = new FederateConfigParser();
            FederateConfig federateConfig = federateConfigParser.parseArgs(args, FederateConfig.class);
            EchoClientDiscover echoClient = new EchoClientDiscover(federateConfig);
            echoClient.execute();

            System.exit(0);
        } catch (Exception e) {
            logger.error("There was a problem executing the EchoClient federate: {}", e.getMessage());
            logger.error(e);
        }
        System.exit(1);
    }
}