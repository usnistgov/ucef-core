
package org.cpswt.hla;

import java.util.*;
import hla.rti.*;

public interface InteractionRootInterface
{
    public int getUniqueID();

    /**
    * Returns the handle (RTI assigned) of this instance's interaction class .
    * 
    * @return the handle (RTI assigned) if this instance's interaction class
    */
    public int getClassHandle();
    
    /**
    * Returns the fully-qualified (dot-delimited) name of this instance's interaction class.
    * 
    * @return the fully-qualified (dot-delimited) name of this instance's interaction class
    */
    public String getClassName();
    
    /**
    * Returns the simple name (last name in its fully-qualified dot-delimited name)
    * of this instance's interaction class.
    * 
    * @return the simple name of this instance's interaction class 
    */
    public String getSimpleClassName();
    
    /**
    * Returns a set containing the names of all of the non-hiddenparameters of an
    * interaction class instance.
    *
    * @return set containing the names of all of the parameters of an
    * interaction class instance
    */
    public Set< String > getParameterNames();
    
    /**
    * Returns a set containing the names of all of the parameters of an
    * interaction class instance.
    *
    * @return set containing the names of all of the parameters of an
    * interaction class instance
    */
    public Set< String > getAllParameterNames();
    
    /**
    * Publishes the interaction class of this instance of the class for a federate.
    *
    * @param rti handle to the RTI, usu. obtained through the
    * {@link SynchronizedFederate#getLRC()} call
    */
    public void publishInteraction( RTIambassador rti );
    
    /**
    * Unpublishes the interaction class of this instance of this class for a federate.
    *
    * @param rti handle to the RTI, usu. obtained through the
    * {@link SynchronizedFederate#getLRC()} call
    */
    public void unpublishInteraction( RTIambassador rti );
    
    /**
    * Subscribes a federate to the interaction class of this instance of this class.
    *
    * @param rti handle to the RTI, usu. obtained through the
    * {@link SynchronizedFederate#getLRC()} call
    */
    public void subscribeInteraction( RTIambassador rti );
    
    /**
    * Unsubscribes a federate from the interaction class of this instance of this class.
    *
    * @param rti handle to the RTI, usu. obtained through the
    * {@link SynchronizedFederate#getLRC()} call
    */
    public void unsubscribeInteraction( RTIambassador rti );

    /**
    * Returns the timestamp for this interaction.  "receive order" interactions
    * should have a timestamp of -1.
    *
    * @return timestamp for this interaction
    */
    public double getTime();
    
    /**
    * Sets the timestamp of this interaction to "time".
    *
    * @param time new timestamp for this interaction
    */
    public void setTime( double time );

    /**
    * Sets the timestamp of this interaction to "logicalTime".
    *
    * @param logicalTime new timestamp for this interaction
    */
    public void setTime( LogicalTime logicalTime );

    /**
    * Returns the value of the parameter named "datamemberName" for this
    * interaction.
    *
    * @param datamemberName name of parameter whose value to retrieve
    * @return the value of the parameter whose name is "datamemberName"
    */
    public Object getParameter( String datamemberName );

    /**
    * Returns the value of the parameter whose handle is "datamemberHandle"
    * (RTI assigned) for this interaction.
    *
    * @param datamemberHandle handle (RTI assigned) of parameter whose
    * value to retrieve
    * @return the value of the parameter whose handle is "datamemberHandle"
    */
    public Object getParameter( int datamemberHandle );
        
    /**
    * Set the values of the parameters in this interaction using
    * "datamemberMap".  "datamemberMap" is usually acquired as an argument to
    * an RTI federate callback method such as "receiveInteraction".
    *
    * @param datamemberMap  contains new values for the parameters of
    * this interaction
    */
    public void setParameters( ReceivedInteraction datamemberMap );

    /**
    * Sets the value of the parameter named "datamemberName" to "value"
    * in this interaction.  "value" is converted to data type of "datamemberName"
    * if needed.
    * This action can also be affected by calling the set_&lt;datamemberName&gt;( value )
    * method on the interaction using a reference to the interaction's actual
    * class.
    *
    * @param datamemberName name of parameter whose value is to be set
    * to "value"
    * @param value new value of parameter called "datamemberName"
    */
    public void setParameter( String datamemberName, String value );

    /**
    * Sets the value of the parameter named "datamemberName" to "value"
    * in this interaction.  "value" should have the same data type as that of
    * the "datamemberName" parameter.
    * This action can also be affected by calling the set_&lt;datamemberName&gt;( value )
    * method on the interaction using a reference to the interaction's actual
    * class.
    *
    * @param datamemberName name of parameter whose value is to be set
    * to "value"
    * @param value new value of parameter called "datamemberName"
    */
    public void setParameter( String datamemberName, Object value );

    /**
    * Sends this interaction to the RTI, with the specified timestamp "time".
    * This method should be used to send interactions that have "timestamp"
    * ordering.
    *
    * @param rti handle to the RTI, usu. obtained through the
    * {@link SynchronizedFederate#getLRC()} call
    * @param time timestamp for this interaction.  The timestamp should be no
    * less than the current federation time + the LOOKAHEAD value of the federate
    * sending this interaction.
    */
    public void sendInteraction( RTIambassador rti, double time );

    /**
    * Sends this interaction to the RTI (without a timestamp).
    * This method should be used to send interactions that have "receive"
    * ordering.
    *
    * @param rti handle to the RTI, usu. obtained through the
    * {@link SynchronizedFederate#getLRC()} call
    */
    public void sendInteraction( RTIambassador rti );
}
