// This file was generated by the WebGME Federates Exporter plugin. Do not edit manually!

package org.cpswt.hla;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cpswt.utils.CpswtUtils;

import hla.rti.FederateNotExecutionMember;
import hla.rti.InteractionClassNotDefined;
import hla.rti.InteractionClassNotPublished;
import hla.rti.InteractionClassNotSubscribed;
import hla.rti.LogicalTime;
import hla.rti.NameNotFound;
import hla.rti.RTIambassador;
import hla.rti.ReceivedInteraction;

/**
* Implements InteractionRoot.C2WInteractionRoot
*/
public class C2WInteractionRoot extends InteractionRoot {

    private static final Logger logger = LogManager.getLogger();

    /**
    * Creates an instance of the C2WInteractionRoot interaction class with default parameter values.
    */
    public C2WInteractionRoot() {}

    private static int _actualLogicalGenerationTime_handle;
    private static int _federateFilter_handle;
    private static int _originFed_handle;
    private static int _sourceFed_handle;

    private static boolean _isInitialized = false;

    private static int _handle;

    /**
    * Returns the handle (RTI assigned) of the C2WInteractionRoot interaction class.
    * Note: As this is a static method, it is NOT polymorphic, and so, if called on
    * a reference will return the handle of the class pertaining to the reference,
    * rather than the handle of the class for the instance referred to by the reference.
    * For the polymorphic version of this method, use {@link #getClassHandle()}.
    *
    * @return the RTI assigned integer handle that represents this interaction class
    */
    public static int get_handle() {
        return _handle;
    }

    /**
    * Returns the fully-qualified (dot-delimited) name of the C2WInteractionRoot interaction class.
    * Note: As this is a static method, it is NOT polymorphic, and so, if called on
    * a reference will return the name of the class pertaining to the reference,
    * rather than the name of the class for the instance referred to by the reference.
    * For the polymorphic version of this method, use {@link #getClassName()}.
    *
    * @return the fully-qualified HLA class path for this interaction class
    */
    public static String get_class_name() {
        return "InteractionRoot.C2WInteractionRoot";
    }

    /**
    * Returns the simple name (the last name in the dot-delimited fully-qualified
    * class name) of the C2WInteractionRoot interaction class.
    *
    * @return the name of this interaction class
    */
    public static String get_simple_class_name() {
        return "C2WInteractionRoot";
    }

    private static Set< String > _datamemberNames = new HashSet< String >();
    private static Set< String > _allDatamemberNames = new HashSet< String >();

    /**
    * Returns a set containing the names of all of the non-hidden parameters in the
    * C2WInteractionRoot interaction class.
    * Note: As this is a static method, it is NOT polymorphic, and so, if called on
    * a reference will return a set of parameter names pertaining to the reference,
    * rather than the parameter names of the class for the instance referred to by
    * the reference.  For the polymorphic version of this method, use
    * {@link #getParameterNames()}.
    *
    * @return a modifiable set of the non-hidden parameter names for this interaction class
    */
    public static Set< String > get_parameter_names() {
        return new HashSet< String >(_datamemberNames);
    }

    /**
    * Returns a set containing the names of all of the parameters in the
    * C2WInteractionRoot interaction class.
    * Note: As this is a static method, it is NOT polymorphic, and so, if called on
    * a reference will return a set of parameter names pertaining to the reference,
    * rather than the parameter names of the class for the instance referred to by
    * the reference.  For the polymorphic version of this method, use
    * {@link #getParameterNames()}.
    *
    * @return a modifiable set of the parameter names for this interaction class
    */
    public static Set< String > get_all_parameter_names() {
        return new HashSet< String >(_allDatamemberNames);
    }

    static {
        _classNameSet.add("InteractionRoot.C2WInteractionRoot");
        _classNameClassMap.put("InteractionRoot.C2WInteractionRoot", C2WInteractionRoot.class);

        _datamemberClassNameSetMap.put("InteractionRoot.C2WInteractionRoot", _datamemberNames);
        _allDatamemberClassNameSetMap.put("InteractionRoot.C2WInteractionRoot", _allDatamemberNames);

        _datamemberNames.add("actualLogicalGenerationTime");
        _datamemberNames.add("federateFilter");
        _datamemberNames.add("originFed");
        _datamemberNames.add("sourceFed");

        _datamemberTypeMap.put("actualLogicalGenerationTime", "double");
        _datamemberTypeMap.put("federateFilter", "String");
        _datamemberTypeMap.put("originFed", "String");
        _datamemberTypeMap.put("sourceFed", "String");

        _allDatamemberNames.add("actualLogicalGenerationTime");
        _allDatamemberNames.add("federateFilter");
        _allDatamemberNames.add("originFed");
        _allDatamemberNames.add("sourceFed");
    }

    protected static void init(RTIambassador rti) {
        if (_isInitialized) return;
        _isInitialized = true;

        InteractionRoot.init(rti);

        boolean isNotInitialized = true;
        while(isNotInitialized) {
            try {
                _handle = rti.getInteractionClassHandle("InteractionRoot.C2WInteractionRoot");
                isNotInitialized = false;
            } catch (FederateNotExecutionMember e) {
                logger.error("could not initialize: Federate Not Execution Member", e);
                return;
            } catch (NameNotFound e) {
                logger.error("could not initialize: Name Not Found", e);
                return;
            } catch (Exception e) {
                logger.error(e);
                CpswtUtils.sleepDefault();
            }
        }

        _classNameHandleMap.put("InteractionRoot.C2WInteractionRoot", get_handle());
        _classHandleNameMap.put(get_handle(), "InteractionRoot.C2WInteractionRoot");
        _classHandleSimpleNameMap.put(get_handle(), "C2WInteractionRoot");

        isNotInitialized = true;
        while(isNotInitialized) {
            try {
                _actualLogicalGenerationTime_handle = rti.getParameterHandle("actualLogicalGenerationTime", get_handle());
                _federateFilter_handle = rti.getParameterHandle("federateFilter", get_handle());
                _originFed_handle = rti.getParameterHandle("originFed", get_handle());
                _sourceFed_handle = rti.getParameterHandle("sourceFed", get_handle());
                isNotInitialized = false;
            } catch (FederateNotExecutionMember e) {
                logger.error("could not initialize: Federate Not Execution Member", e);
                return;
            } catch (InteractionClassNotDefined e) {
                logger.error("could not initialize: Interaction Class Not Defined", e);
                return;
            } catch (NameNotFound e) {
                logger.error("could not initialize: Name Not Found", e);
                return;
            } catch (Exception e) {
                logger.error(e);
                CpswtUtils.sleepDefault();
            }
        }

        _datamemberNameHandleMap.put("InteractionRoot.C2WInteractionRoot.actualLogicalGenerationTime", _actualLogicalGenerationTime_handle);
        _datamemberNameHandleMap.put("InteractionRoot.C2WInteractionRoot.federateFilter", _federateFilter_handle);
        _datamemberNameHandleMap.put("InteractionRoot.C2WInteractionRoot.originFed", _originFed_handle);
        _datamemberNameHandleMap.put("InteractionRoot.C2WInteractionRoot.sourceFed", _sourceFed_handle);

        _datamemberHandleNameMap.put(_actualLogicalGenerationTime_handle, "actualLogicalGenerationTime");
        _datamemberHandleNameMap.put(_federateFilter_handle, "federateFilter");
        _datamemberHandleNameMap.put(_originFed_handle, "originFed");
        _datamemberHandleNameMap.put(_sourceFed_handle, "sourceFed");
    }

    private static boolean _isPublished = false;

    /**
    * Publishes the C2WInteractionRoot interaction class for a federate.
    *
    * @param rti handle to the Local RTI Component
    */
    public static void publish(RTIambassador rti) {
        if (_isPublished) return;

        init(rti);

        synchronized(rti) {
            boolean isNotPublished = true;
            while(isNotPublished) {
                try {
                    rti.publishInteractionClass(get_handle());
                    isNotPublished = false;
                } catch (FederateNotExecutionMember e) {
                    logger.error("could not publish: Federate Not Execution Member", e);
                    return;
                } catch (InteractionClassNotDefined e) {
                    logger.error("could not publish: Interaction Class Not Defined", e);
                    return;
                } catch (Exception e) {
                    logger.error(e);
                    CpswtUtils.sleepDefault();
                }
            }
        }

        _isPublished = true;
        logger.debug("publish: {}", get_class_name());
    }

    /**
    * Unpublishes the C2WInteractionRoot interaction class for a federate.
    *
    * @param rti handle to the Local RTI Component
    */
    public static void unpublish(RTIambassador rti) {
        if (!_isPublished) return;

        init(rti);

        synchronized(rti) {
            boolean isNotUnpublished = true;
            while(isNotUnpublished) {
                try {
                    rti.unpublishInteractionClass(get_handle());
                    isNotUnpublished = false;
                } catch (FederateNotExecutionMember e) {
                    logger.error("could not unpublish: Federate Not Execution Member", e);
                    return;
                } catch (InteractionClassNotDefined e) {
                    logger.error("could not unpublish: Interaction Class Not Defined", e);
                    return;
                } catch (InteractionClassNotPublished e) {
                    logger.error("could not unpublish: Interaction Class Not Published", e);
                    return;
                } catch (Exception e) {
                    logger.error(e);
                    CpswtUtils.sleepDefault();
                }
            }
        }

        _isPublished = false;
        logger.debug("unpublish: {}", get_class_name());
    }

    private static boolean _isSubscribed = false;

    /**
    * Subscribes a federate to the C2WInteractionRoot interaction class.
    *
    * @param rti handle to the Local RTI Component
    */
    public static void subscribe(RTIambassador rti) {
        if (_isSubscribed) return;

        init(rti);

        synchronized(rti) {
            boolean isNotSubscribed = true;
            while(isNotSubscribed) {
                try {
                    rti.subscribeInteractionClass(get_handle());
                    isNotSubscribed = false;
                } catch (FederateNotExecutionMember e) {
                    logger.error("could not subscribe: Federate Not Execution Member", e);
                    return;
                } catch (InteractionClassNotDefined e) {
                    logger.error("could not subscribe: Interaction Class Not Defined", e);
                    return;
                } catch (Exception e) {
                    logger.error(e);
                    CpswtUtils.sleepDefault();
                }
            }
        }

        _isSubscribed = true;
        logger.debug("subscribe: {}", get_class_name());
    }

    /**
    * Unsubscribes a federate from the C2WInteractionRoot interaction class.
    *
    * @param rti handle to the Local RTI Component
    */
    public static void unsubscribe(RTIambassador rti) {
        if (!_isSubscribed) return;

        init(rti);

        synchronized(rti) {
            boolean isNotUnsubscribed = true;
            while(isNotUnsubscribed) {
                try {
                    rti.unsubscribeInteractionClass(get_handle());
                    isNotUnsubscribed = false;
                } catch (FederateNotExecutionMember e) {
                    logger.error("could not unsubscribe: Federate Not Execution Member", e);
                    return;
                } catch (InteractionClassNotDefined e) {
                    logger.error("could not unsubscribe: Interaction Class Not Defined", e);
                    return;
                } catch (InteractionClassNotSubscribed e) {
                    logger.error("could not unsubscribe: Interaction Class Not Subscribed", e);
                    return;
                } catch (Exception e) {
                    logger.error(e);
                    CpswtUtils.sleepDefault();
                }
            }
        }

        _isSubscribed = false;
        logger.debug("unsubscribe: {}", get_class_name());
    }

    /**
    * Return true if "handle" is equal to the handle (RTI assigned) of this class
    * (that is, the C2WInteractionRoot interaction class).
    *
    * @param handle handle to compare to the value of the handle (RTI assigned) of
    * this class (the C2WInteractionRoot interaction class).
    * @return "true" if "handle" matches the value of the handle of this class
    * (that is, the C2WInteractionRoot interaction class).
    */
    public static boolean match(int handle) {
        return handle == get_handle();
    }

    /**
    * Returns the handle (RTI assigned) of this instance's interaction class .
    *
    * @return the handle (RTI assigned) if this instance's interaction class
    */
    public int getClassHandle() {
        return get_handle();
    }

    /**
    * Returns the fully-qualified (dot-delimited) name of this instance's interaction class.
    *
    * @return the fully-qualified (dot-delimited) name of this instance's interaction class
    */
    public String getClassName() {
        return get_class_name();
    }

    /**
    * Returns the simple name (last name in its fully-qualified dot-delimited name)
    * of this instance's interaction class.
    *
    * @return the simple name of this instance's interaction class
    */
    public String getSimpleClassName() {
        return get_simple_class_name();
    }

    /**
    * Returns a set containing the names of all of the non-hiddenparameters of an
    * interaction class instance.
    *
    * @return set containing the names of all of the parameters of an
    * interaction class instance
    */
    public Set< String > getParameterNames() {
        return get_parameter_names();
    }

    /**
    * Returns a set containing the names of all of the parameters of an
    * interaction class instance.
    *
    * @return set containing the names of all of the parameters of an
    * interaction class instance
    */
    public Set< String > getAllParameterNames() {
        return get_all_parameter_names();
    }

    @Override
    public String getParameterName(int datamemberHandle) {
        if (datamemberHandle == _actualLogicalGenerationTime_handle) return "actualLogicalGenerationTime";
        else if (datamemberHandle == _federateFilter_handle) return "federateFilter";
        else if (datamemberHandle == _originFed_handle) return "originFed";
        else if (datamemberHandle == _sourceFed_handle) return "sourceFed";
        else return super.getParameterName(datamemberHandle);
    }

    /**
    * Publishes the interaction class of this instance of the class for a federate.
    *
    * @param rti handle to the Local RTI Component
    */
    public void publishInteraction(RTIambassador rti) {
        publish(rti);
    }

    /**
    * Unpublishes the interaction class of this instance of this class for a federate.
    *
    * @param rti handle to the Local RTI Component
    */
    public void unpublishInteraction(RTIambassador rti) {
        unpublish(rti);
    }

    /**
    * Subscribes a federate to the interaction class of this instance of this class.
    *
    * @param rti handle to the Local RTI Component
    */
    public void subscribeInteraction(RTIambassador rti) {
        subscribe(rti);
    }

    /**
    * Unsubscribes a federate from the interaction class of this instance of this class.
    *
    * @param rti handle to the Local RTI Component
    */
    public void unsubscribeInteraction(RTIambassador rti) {
        unsubscribe(rti);
    }

    @Override
    public String toString() {
        return getClass().getName() + "("
                + "actualLogicalGenerationTime:" + get_actualLogicalGenerationTime()
                + "," + "federateFilter:" + get_federateFilter()
                + "," + "originFed:" + get_originFed()
                + "," + "sourceFed:" + get_sourceFed()
                + ")";
    }

    private double _actualLogicalGenerationTime = 0;
    private String _federateFilter = "";
    private String _originFed = "";
    private String _sourceFed = "";

    /**
    * Set the value of the "actualLogicalGenerationTime" parameter to "value" for this parameter.
    *
    * @param value the new value for the "actualLogicalGenerationTime" parameter
    */
    public void set_actualLogicalGenerationTime( double value ) {
        _actualLogicalGenerationTime = value;
    }

    /**
    * Returns the value of the "actualLogicalGenerationTime" parameter of this interaction.
    *
    * @return the value of the "actualLogicalGenerationTime" parameter
    */
    public double get_actualLogicalGenerationTime() {
        return _actualLogicalGenerationTime;
    }
    /**
    * Set the value of the "federateFilter" parameter to "value" for this parameter.
    *
    * @param value the new value for the "federateFilter" parameter
    */
    public void set_federateFilter( String value ) {
        _federateFilter = value;
    }

    /**
    * Returns the value of the "federateFilter" parameter of this interaction.
    *
    * @return the value of the "federateFilter" parameter
    */
    public String get_federateFilter() {
        return _federateFilter;
    }
    /**
    * Set the value of the "originFed" parameter to "value" for this parameter.
    *
    * @param value the new value for the "originFed" parameter
    */
    public void set_originFed( String value ) {
        _originFed = value;
    }

    /**
    * Returns the value of the "originFed" parameter of this interaction.
    *
    * @return the value of the "originFed" parameter
    */
    public String get_originFed() {
        return _originFed;
    }
    /**
    * Set the value of the "sourceFed" parameter to "value" for this parameter.
    *
    * @param value the new value for the "sourceFed" parameter
    */
    public void set_sourceFed( String value ) {
        _sourceFed = value;
    }

    /**
    * Returns the value of the "sourceFed" parameter of this interaction.
    *
    * @return the value of the "sourceFed" parameter
    */
    public String get_sourceFed() {
        return _sourceFed;
    }

    protected C2WInteractionRoot( ReceivedInteraction datamemberMap, boolean initFlag ) {
        super( datamemberMap, false );
        if ( initFlag ) setParameters( datamemberMap );
    }

    protected C2WInteractionRoot( ReceivedInteraction datamemberMap, LogicalTime logicalTime, boolean initFlag ) {
        super( datamemberMap, logicalTime, false );
        if ( initFlag ) setParameters( datamemberMap );
    }

    /**
    * Creates an instance of the C2WInteractionRoot interaction class, using
    * "datamemberMap" to initialize its parameter values.
    * "datamemberMap" is usually acquired as an argument to an RTI federate
    * callback method, such as "receiveInteraction".
    *
    * @param datamemberMap data structure containing initial values for the
    * parameters of this new C2WInteractionRoot interaction class instance
    */
    public C2WInteractionRoot( ReceivedInteraction datamemberMap ) {
        this( datamemberMap, true );
    }

    /**
    * Like {@link #C2WInteractionRoot( ReceivedInteraction datamemberMap )}, except this
    * new C2WInteractionRoot interaction class instance is given a timestamp of
    * "logicalTime".
    *
    * @param datamemberMap data structure containing initial values for the
    * parameters of this new C2WInteractionRoot interaction class instance
    * @param logicalTime timestamp for this new C2WInteractionRoot interaction class
    * instance
    */
    public C2WInteractionRoot( ReceivedInteraction datamemberMap, LogicalTime logicalTime ) {
        this( datamemberMap, logicalTime, true );
    }

    /**
    * Creates a new C2WInteractionRoot interaction class instance that is a duplicate
    * of the instance referred to by C2WInteractionRoot_var.
    *
    * @param C2WInteractionRoot_var C2WInteractionRoot interaction class instance of which
    * this newly created C2WInteractionRoot interaction class instance will be a
    * duplicate
    */
    public C2WInteractionRoot( C2WInteractionRoot C2WInteractionRoot_var ) {
        super( C2WInteractionRoot_var );

        set_actualLogicalGenerationTime( C2WInteractionRoot_var.get_actualLogicalGenerationTime() );
        set_federateFilter( C2WInteractionRoot_var.get_federateFilter() );
        set_originFed( C2WInteractionRoot_var.get_originFed() );
        set_sourceFed( C2WInteractionRoot_var.get_sourceFed() );
    }

    /**
    * Returns the value of the parameter whose name is "datamemberName"
    * for this interaction.
    *
    * @param datamemberName name of parameter whose value is to be
    * returned
    * @return value of the parameter whose name is "datamemberName"
    * for this interaction
    */
    public Object getParameter( String datamemberName ) {
        if ( "actualLogicalGenerationTime".equals(datamemberName) ) return new Double(get_actualLogicalGenerationTime());
        else if ( "federateFilter".equals(datamemberName) ) return get_federateFilter();
        else if ( "originFed".equals(datamemberName) ) return get_originFed();
        else if ( "sourceFed".equals(datamemberName) ) return get_sourceFed();
        else return super.getParameter( datamemberName );
    }

    protected boolean setParameterAux( String datamemberName, String val ) {
        boolean retval = true;
        if ( "actualLogicalGenerationTime".equals( datamemberName) ) set_actualLogicalGenerationTime( Double.parseDouble(val) );
        else if ( "federateFilter".equals( datamemberName) ) set_federateFilter( val );
        else if ( "originFed".equals( datamemberName) ) set_originFed( val );
        else if ( "sourceFed".equals( datamemberName) ) set_sourceFed( val );
        else retval = super.setParameterAux( datamemberName, val );

        return retval;
    }

    protected boolean setParameterAux( String datamemberName, Object val ) {
        boolean retval = true;
        if ( "actualLogicalGenerationTime".equals( datamemberName) ) set_actualLogicalGenerationTime( (Double)val );
        else if ( "federateFilter".equals( datamemberName) ) set_federateFilter( (String)val );
        else if ( "originFed".equals( datamemberName) ) set_originFed( (String)val );
        else if ( "sourceFed".equals( datamemberName) ) set_sourceFed( (String)val );
        else retval = super.setParameterAux( datamemberName, val );

        return retval;
    }

    public void copyFrom( Object object ) {
        super.copyFrom( object );
        if ( object instanceof C2WInteractionRoot ) {
            C2WInteractionRoot data = (C2WInteractionRoot)object;
            _actualLogicalGenerationTime = data._actualLogicalGenerationTime;
            _federateFilter = data._federateFilter;
            _originFed = data._originFed;
            _sourceFed = data._sourceFed;
        }
    }
}

