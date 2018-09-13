/*
 * Copyright (c) 2016, Institute for Software Integrated Systems, Vanderbilt University
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
 * @author Harmon Nine
 */

package org.cpswt.hla;

import hla.rti.FederateNotExecutionMember;
import hla.rti.InteractionClassNotDefined;
import hla.rti.InteractionClassNotPublished;
import hla.rti.InteractionClassNotSubscribed;
import hla.rti.LogicalTime;
import hla.rti.NameNotFound;
import hla.rti.RTIambassador;
import hla.rti.ReceivedInteraction;
import hla.rti.SuppliedParameters;
import hla.rti.jlc.RtiFactory;
import hla.rti.jlc.RtiFactoryFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cpswt.utils.CpswtUtils;
import org.portico.impl.hla13.types.DoubleTime;

/**
 * InteractionRoot is the base class for all interactions
 * defined in a given federation.  As such, an InteractionRoot
 * variable may refer to any type of interaction defined in the
 * federation.
 * 
 * This InteractionRoot class provides the following:
 * - methods for constructing any interaction in the federation, either from
 * data provided by the RTI (for example, see
 * {@link #create_interaction(int classHandle)} or from a string argument
 * specifying the name of interaction to construct (see
 * {@link #create_interaction(String className)}.
 * - methods for sending an interaction to the RTI (see
 * {@link #sendInteraction(RTIambassador rti)} for an example).
 * - methods for publishing/subscribing to any interaction
 * defined in the federation (see
 * {@link #publish(String className, RTIambassador rti)} for example).
 * - methods for getting/setting any parameter in the interaction to
 * which a given InteractionRoot variable is referring
 * (see {@link #getParameter(String datamemberName)} and
 * {@link #setParameter(String datamemberName, Object value)}
 */
public class InteractionRoot implements InteractionRootInterface {
    
    private static final Logger logger = LogManager.getLogger();

    private static int _globalUniqueID = 0;
    private static int generateUniqueID() {
        return _globalUniqueID++;
    }

    private int _uniqueID;

    public int getUniqueID() {
        return _uniqueID;
    }

    private static RtiFactory _factory;

    static {
        boolean factoryNotAcquired = true;
        while (factoryNotAcquired) {
            try {
                _factory = RtiFactoryFactory.getRtiFactory("org.portico.dlc.HLA13RTIFactory");
                factoryNotAcquired = false;
            } catch (Exception e) {
                logger.error("failed to acquire factory", e);
                CpswtUtils.sleep(100);
            }
        }
    }

    protected static Set<String> _classNameSet = new HashSet<String>();
    protected static Map<String, Class<?>> _classNameClassMap = new HashMap<String, Class<?>>();
    protected static Map<String, Set<String>> _datamemberClassNameSetMap = new HashMap<String, Set<String>>();
    protected static Map<String, Set<String>> _allDatamemberClassNameSetMap = new HashMap<String, Set<String>>();

    protected static Map<String, Integer> _classNameHandleMap = new HashMap<String, Integer>();
    protected static Map<Integer, String> _classHandleNameMap = new HashMap<Integer, String>();
    protected static Map<Integer, String> _classHandleSimpleNameMap = new HashMap<Integer, String>();

    protected static Map<String, Integer> _datamemberNameHandleMap = new HashMap<String, Integer>();
    protected static Map<Integer, String> _datamemberHandleNameMap = new HashMap<Integer, String>();
    protected static Map<String, String> _datamemberTypeMap = new HashMap<String, String>();

    private static boolean _isInitialized = false;

    private static int _handle;

    /**
     * Returns the handle (RTI assigned) of the InteractionRoot interaction class.
     * Note: As this is a static method, it is NOT polymorphic, and so, if called on
     * a reference will return the handle of the class pertaining to the reference,\
     * rather than the handle of the class for the instance referred to by the reference.
     * For the polymorphic version of this method, use {@link #getClassHandle()}.
     * 
     * @return the RTI assigned integer handle that represents this interaction class
     */
    public static int get_handle() {
        return _handle;
    }

    /**
     * Returns the fully-qualified (dot-delimited) name of the InteractionRoot
     * interaction class.
     * Note: As this is a static method, it is NOT polymorphic, and so, if called on
     * a reference will return the name of the class pertaining to the reference,\
     * rather than the name of the class for the instance referred to by the reference.
     * For the polymorphic version of this method, use {@link #getClassName()}.
     * 
     * @return the fully-qualified HLA class path for this interaction class
     */
    public static String get_class_name() {
        return "InteractionRoot";
    }

    /**
     * Returns the simple name (the last name in the dot-delimited fully-qualified
     * class name) of the InteractionRoot interaction class.
     * 
     * @return the name of this interaction class
     */
    public static String get_simple_class_name() {
        return "InteractionRoot";
    }

    private static Set<String> _datamemberNames = new HashSet<>();
    private static Set<String> _allDatamemberNames = new HashSet<>();

    /**
     * Returns a set containing the names of all of the non-hidden parameters in the
     * InteractionRoot interaction class.
     * Note: As this is a static method, it is NOT polymorphic, and so, if called on
     * a reference will return a set of parameter names pertaining to the reference,\
     * rather than the parameter names of the class for the instance referred to by
     * the reference.  For the polymorphic version of this method, use
     * {@link #getParameterNames()}.
     * 
     * @return a modifiable set of the non-hidden parameter names for this interaction class
     */
    public static Set<String> get_parameter_names() {
        return new HashSet<>(_datamemberNames);
    }

    /**
     * Returns a set containing the names of all of the parameters in the
     * InteractionRoot interaction class.
     * Note: As this is a static method, it is NOT polymorphic, and so, if called on
     * a reference will return a set of parameter names pertaining to the reference,\
     * rather than the parameter names of the class for the instance referred to by
     * the reference.  For the polymorphic version of this method, use
     * {@link #getParameterNames()}.
     * 
     * @return a modifiable set of the parameter names for this interaction class
     */
    public static Set<String> get_all_parameter_names() {
        return new HashSet<>(_allDatamemberNames);
    }

    static {
        _classNameSet.add("InteractionRoot");
        _classNameClassMap.put("InteractionRoot", InteractionRoot.class);

        _datamemberClassNameSetMap.put("InteractionRoot", _datamemberNames);
        _allDatamemberClassNameSetMap.put("InteractionRoot", _allDatamemberNames);
    }

    protected static void init(RTIambassador rti) {
        if (_isInitialized) return;
        _isInitialized = true;

        boolean isNotInitialized = true;
        while (isNotInitialized) {
            try {
                _handle = rti.getInteractionClassHandle("InteractionRoot");
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

        _classNameHandleMap.put("InteractionRoot", get_handle());
        _classHandleNameMap.put(get_handle(), "InteractionRoot");
        _classHandleSimpleNameMap.put(get_handle(), "InteractionRoot");
    }
    
    private static boolean _isPublished = false;

    /**
     * Publishes the InteractionRoot interaction class for a federate.
     *
     * @param rti       handle to the Local RTI Component
     */
    public static void publish(RTIambassador rti) {
        if (_isPublished) return;

        init(rti);
        
        synchronized (rti) {
            boolean isNotPublished = true;
            while (isNotPublished) {
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
        logger.debug("publish {}", get_class_name());
    }
    
    /**
     * Unpublishes the InteractionRoot interaction class for a federate.
     *
     * @param rti handle to the RTI, usu. obtained through the
     *            {@link SynchronizedFederate#getLRC()} call
     */
    public static void unpublish(RTIambassador rti) {
        if (!_isPublished) return;

        init(rti);
        
        synchronized (rti) {
            boolean isNotUnpublished = true;
            while (isNotUnpublished) {
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
        logger.debug("unpublish {}", get_class_name());
    }

    private static boolean _isSubscribed = false;

    /**
     * Subscribes a federate to the InteractionRoot interaction class.
     *
     * @param rti       handle to the Local RTI Component
     */
    public static void subscribe(RTIambassador rti) {
        if (_isSubscribed) return;

        init(rti);

        synchronized (rti) {
            boolean isNotSubscribed = true;
            while (isNotSubscribed) {
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
        logger.debug("subscribe {}", get_class_name());
    }

    /**
     * Unsubscribes a federate from the InteractionRoot interaction class.
     *
     * @param rti       handle to the Local RTI Component
     */
    public static void unsubscribe(RTIambassador rti) {
        if (!_isSubscribed) return;

        init(rti);
        
        synchronized (rti) {
            boolean isNotUnsubscribed = true;
            while (isNotUnsubscribed) {
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
        logger.debug("unsubscribe {}", get_class_name());
    }

    /**
     * Return true if "handle" is equal to the handle (RTI assigned) of this class
     * (that is, the InteractionRoot interaction class).
     *
     * @param handle handle to compare to the value of the handle (RTI assigned) of
     *               this class (the InteractionRoot interaction class).
     * @return "true" if "handle" matches the value of the handle of this class
     * (that is, the InteractionRoot interaction class).
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
    public Set<String> getParameterNames() {
        return get_parameter_names();
    }

    /**
     * Returns a set containing the names of all of the parameters of an
     * interaction class instance.
     *
     * @return set containing the names of all of the parameters of an
     * interaction class instance
     */
    public Set<String> getAllParameterNames() {
        return get_all_parameter_names();
    }
    
    /**
     * Returns the parameter name associated with the given handle for an interaction class instance.
     * 
     * @param datamemberHandle a parameter handle assigned by the RTI
     * @return the parameter name associated with the handle, or null
     */
    public String getParameterName(int datamemberHandle) {
        return null;
    }
    
    /**
     * Returns the handle associated with the given parameter name for an interaction class instance
     * 
     * @param datamemberName the name of a parameter that belongs to this interaction class
     * @return the RTI handle associated with the parameter name, or -1 if not found
     */
    public int getParameterHandle(String datamemberName) {
        return get_parameter_handle(getClassName(), datamemberName);
    }

    /**
     * Publishes the interaction class of this instance of the class for a federate.
     *
     * @param rti       handle to the Local RTI Component
     */
    public void publishInteraction(RTIambassador rti) {
        publish(rti);
    }

    /**
     * Unpublishes the interaction class of this instance of this class for a federate.
     *
     * @param rti       handle to the Local RTI Component
     */
    public void unpublishInteraction(RTIambassador rti) {
        unpublish(rti);
    }

    /**
     * Subscribes a federate to the interaction class of this instance of this class.
     *
     * @param rti       handle to the Local RTI Component
     */
    public void subscribeInteraction(RTIambassador rti) {
        subscribe(rti);
    }

    /**
     * Unsubscribes a federate from the interaction class of this instance of this class.
     *
     * @param rti       handle to the Local RTI Component
     */
    public void unsubscribeInteraction(RTIambassador rti) {
        unsubscribe(rti);
    }

    @Override
    public String toString() {
        return getClass().getName() + "()";
    }

    /**
     * Returns a set of strings containing the names of all of the interaction
     * classes in the current federation.
     *
     * @return Set&lt; String &gt; containing the names of all interaction classes
     * in the current federation
     */
    public static Set<String> get_interaction_names() {
        return new HashSet<String>(_classNameSet);
    }

    /**
     * Returns a set of strings containing the names of all of the non-hidden parameters
     * in the interaction class specified by className.
     *
     * @param className name of interaction class for which to retrieve the
     *                  names of all of its parameters
     * @return Set&lt; String &gt; containing the names of all parameters in the
     * className interaction class
     */
    public static Set<String> get_parameter_names(String className) {
        return new HashSet<String>(_datamemberClassNameSetMap.get(className));
    }

    /**
     * Returns a set of strings containing the names of all of the parameters
     * in the interaction class specified by className.
     *
     * @param className name of interaction class for which to retrieve the
     *                  names of all of its parameters
     * @return Set&lt; String &gt; containing the names of all parameters in the
     * className interaction class
     */
    public static Set<String> get_all_parameter_names(String className) {
        return new HashSet<String>(_allDatamemberClassNameSetMap.get(className));
    }

    /**
     * Returns the fully-qualified name of the interaction class corresponding
     * to the RTI-defined classHandle.
     *
     * @param classHandle handle (defined by RTI) of interaction class for
     *                    which to retrieve the fully-qualified name
     * @return the fully-qualified name of the interaction class that
     * corresponds to the RTI-defined classHandle
     */
    public static String get_class_name(int classHandle) {
        return _classHandleNameMap.get(classHandle);
    }

    /**
     * Returns the simple name of the interaction class corresponding to the
     * RTI-defined classHandle.  The simple name of an interaction class is
     * the last name in its (dot-delimited) fully-qualified name.
     *
     * @param classHandle handle (defined by RTI) of interaction class for which
     *                    to retrieve the simple name
     * @return the simple name of the interaction class that corresponds to
     * the RTI-defined classHandle
     */
    public static String get_simple_class_name(int classHandle) {
        return _classHandleSimpleNameMap.get(classHandle);
    }

    /**
     * Returns the integer handle (RTI defined) of the interaction class
     * corresponding to the fully-qualified interaction class name in className.
     *
     * @param className fully-qualified name of interaction class for which to
     *                  retrieve the RTI-defined integer handle
     * @return the RTI-defined handle of the interaction class
     */
    public static int get_handle(String className) {
        Integer classHandle = _classNameHandleMap.get(className);
        
        if (classHandle == null) {
            logger.error("Bad class name \"{}\" on get_handle.", className);
            return -1;
        }
        return classHandle;
    }

    /**
     * Returns the name of a parameter corresponding to
     * its handle (RTI assigned) in datamemberHandle.
     *
     * @param datamemberHandle handle of parameter (RTI assigned)
     *                         for which to return the name
     * @return the name of the parameter corresponding to datamemberHandle
     */
    public static String get_parameter_name(int datamemberHandle) {
        return _datamemberHandleNameMap.get(datamemberHandle);
    }

    /**
     * Returns the handle of a parameter (RTI assigned) given
     * its interaction class name and parameter name
     *
     * @param className      name of interaction class
     * @param datamemberName name of parameter
     * @return the handle (RTI assigned) of the parameter "datamemberName" of interaction class "className"
     */
    public static int get_parameter_handle(String className, String datamemberName) {
        Integer datamemberHandle = _datamemberNameHandleMap.get(className + "." + datamemberName);
        
        if (datamemberHandle == null) {
            logger.error("Bad parameter \"{}\" for class \"{}\" on get_parameter_handle.", datamemberName, className);
            return -1;
        }
        return datamemberHandle;
    }

    private static Class<?>[] pubsubArguments = new Class<?>[]{RTIambassador.class};

    /**
     * Publishes the interaction class named by "className" for a federate.
     * This can also be performed by calling the publish( RTIambassador rti )
     * method directly on the interaction class named by "className" (for
     * example, to publish the InteractionRoot class in particular,
     * see {@link InteractionRoot#publish(RTIambassador rti)}).
     *
     * @param className name of interaction class to be published for the federate
     * @param rti       handle to the Local RTI Component
     */
    public static void publish(String className, RTIambassador rti) {
        Class<?> rtiClass = _classNameClassMap.get(className);
        if (rtiClass == null) {
            logger.error("Bad class name \"{}\" on publish.", className);
            return;
        }
        try {
            Method method = rtiClass.getMethod("publish", pubsubArguments);
            method.invoke(null, new Object[]{rti});
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * Unpublishes the interaction class named by "className" for a federate.
     * This can also be performed by calling the unpublish( RTIambassador rti )
     * method directly on the interaction class named by "className" (for
     * example, to unpublish the InteractionRoot class in particular,
     * see {@link InteractionRoot#unpublish(RTIambassador rti)}).
     *
     * @param className name of interaction class to be unpublished for the federate
     * @param rti       handle to the Local RTI Component
     */
    public static void unpublish(String className, RTIambassador rti) {
        Class<?> rtiClass = _classNameClassMap.get(className);
        if (rtiClass == null) {
            logger.error("Bad class name \"{}\" on unpublish.", className);
            return;
        }
        try {
            Method method = rtiClass.getMethod("unpublish", pubsubArguments);
            method.invoke(null, new Object[]{rti});
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * Subscribes federate to the interaction class names by "className"
     * This can also be performed by calling the subscribe( RTIambassador rti )
     * method directly on the interaction class named by "className" (for
     * example, to subscribe a federate to the InteractionRoot class
     * in particular, see {@link InteractionRoot#subscribe(RTIambassador rti)}).
     *
     * @param className name of interaction class to which to subscribe the federate
     * @param rti       handle to the Local RTI Component
     */
    public static void subscribe(String className, RTIambassador rti) {
        Class<?> rtiClass = _classNameClassMap.get(className);
        if (rtiClass == null) {
            logger.error("Bad class name \"{}\" on subscribe.", className);
            return;
        }
        try {
            Method method = rtiClass.getMethod("subscribe", pubsubArguments);
            method.invoke(null, new Object[]{rti});
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * Unsubscribes federate from the interaction class names by "className"
     * This can also be performed by calling the unsubscribe( RTIambassador rti )
     * method directly on the interaction class named by "className" (for
     * example, to unsubscribe a federate to the InteractionRoot class
     * in particular, see {@link InteractionRoot#unsubscribe(RTIambassador rti)}).
     *
     * @param className name of interaction class to which to unsubscribe the federate
     * @param rti       handle to the Local RTI Component
     */
    public static void unsubscribe(String className, RTIambassador rti) {
        Class<?> rtiClass = _classNameClassMap.get(className);
        try {
            Method method = rtiClass.getMethod("unsubscribe", pubsubArguments);
            method.invoke(null, new Object[]{rti});
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private static InteractionRoot create_interaction(Class<?> rtiClass) {
        InteractionRoot classRoot = null;
        try {
            classRoot = (InteractionRoot) rtiClass.newInstance();
        } catch (Exception e) {
            logger.error("could not create/cast new Interaction", e);
        }

        return classRoot;
    }

    private static InteractionRoot create_interaction(Class<?> rtiClass, LogicalTime logicalTime) {
        InteractionRoot classRoot = create_interaction(rtiClass);
        if (classRoot != null) classRoot.setTime(logicalTime);
        return classRoot;
    }

    private static InteractionRoot create_interaction(Class<?> rtiClass, ReceivedInteraction datamemberMap) {
        InteractionRoot classRoot = create_interaction(rtiClass);
        classRoot.setParameters(datamemberMap);
        return classRoot;
    }

    private static InteractionRoot create_interaction(Class<?> rtiClass, ReceivedInteraction datamemberMap, LogicalTime logicalTime) {
        InteractionRoot classRoot = create_interaction(rtiClass);
        classRoot.setParameters(datamemberMap);
        classRoot.setTime(logicalTime);
        return classRoot;
    }

    /**
     * Create an interaction that is in instance of interaction class
     * "className". An InteractionRoot reference is returned,
     * so to refer to the instance using a reference to a "className" interaction,
     * the returned reference must be cast down the interaction inheritance
     * hierarchy.
     * An instance of the "className" interaction class may also be created
     * by using the "new" operator directory on the "className" interaction
     * class.  For instance, two ways to create an InteractionRoot
     * instance are
     * Interaction.create_interaction( "InteractionRoot" ),
     * and
     * new InteractionRoot()
     *
     * @param className fully-qualified (dot-delimited) name of the interaction
     *                  class for which to create an instance
     * @return instance of "className" interaction class
     */
    public static InteractionRoot create_interaction(String className) {
        Class<?> rtiClass = _classNameClassMap.get(className);
        if (rtiClass == null) return null;

        return create_interaction(rtiClass);
    }

    /**
     * Like {@link #create_interaction(String className)}, but interaction
     * is created with a timestamp based on "logicalTime".
     *
     * @param className   fully-qualified (dot-delimited) name of the interaction
     *                    class for which to create an instance
     * @param logicalTime timestamp to place on the new interaction class instance
     * @return instance of "className" interaction class with "logicalTime" time stamp.
     */
    public static InteractionRoot create_interaction(String className, LogicalTime logicalTime) {
        Class<?> rtiClass = _classNameClassMap.get(className);
        if (rtiClass == null) return null;

        return create_interaction(rtiClass, logicalTime);
    }

    /**
     * Create an interaction that is in instance of interaction class
     * that corresponds to the "classHandle" handle (RTI assigned). An
     * InteractionRoot reference is returned, so to refer to the
     * instance using a reference to a "className" interaction, the returned
     * reference must be cast down the interaction inheritance hierarchy.
     *
     * @param classHandle handle of interaction class (RTI assigned) class for
     *                    which to create an instance
     * @return instance of interaction class corresponding to "classHandle"
     */
    public static InteractionRoot create_interaction(int classHandle) {
        Class<?> rtiClass = _classNameClassMap.get(_classHandleNameMap.get(classHandle));
        if (rtiClass == null) return null;

        return create_interaction(rtiClass);
    }

    /**
     * Like {@link #create_interaction(int classHandle)}, but the interaction
     * is created with a timestamp based on "logicalTime".
     *
     * @param classHandle handle of interaction class (RTI assigned) class for
     *                    which to create an instance
     * @param logicalTime timestamp to place on the new interaction class instance
     * @return instance of interaction class corresponding to "classHandle" with
     * "logicalTime" time stamp
     */
    public static InteractionRoot create_interaction(int classHandle, LogicalTime logicalTime) {
        Class<?> rtiClass = _classNameClassMap.get(_classHandleNameMap.get(classHandle));
        if (rtiClass == null) return null;

        return create_interaction(rtiClass, logicalTime);
    }

    /**
     * Like {@link #create_interaction(int classHandle)}, but the interaction's
     * parameters are initialized using "datamemberMap".  The "datamemberMap"
     * is usually acquired as an argument to an RTI callback method of a federate.
     *
     * @param classHandle   handle of interaction class (RTI assigned) class for
     *                      which to create an instance
     * @param datamemberMap contains initializing values for the parameters
     *                      of the interaction class instance
     * @return instance of interaction class corresponding to "classHandle" with
     * its parameters initialized with the "datamemberMap"
     */
    public static InteractionRoot create_interaction(int classHandle, ReceivedInteraction datamemberMap) {
        Class<?> rtiClass = _classNameClassMap.get(_classHandleNameMap.get(classHandle));
        if (rtiClass == null) return null;

        return create_interaction(rtiClass, datamemberMap);
    }

    /**
     * Like {@link #create_interaction(int classHandle, ReceivedInteraction datamemberMap)},
     * but the interaction is given a timestamp based on "logicalTime".
     *
     * @param classHandle   handle of interaction class (RTI assigned) class for
     *                      which to create an instance
     * @param datamemberMap initializing values for the parameters of the
     *                      interaction class instance
     * @param logicalTime   timestamp to place on the new interaction class instance
     * @return instance of interaction class corresponding to "classHandle" with
     * its parameters initialized with the "datamemberMap" and with
     * "logicalTime" timestamp
     */
    public static InteractionRoot create_interaction(int classHandle, ReceivedInteraction datamemberMap, LogicalTime logicalTime) {
        Class<?> rtiClass = _classNameClassMap.get(_classHandleNameMap.get(classHandle));
        if (rtiClass == null) return null;

        return create_interaction(rtiClass, datamemberMap, logicalTime);
    }

    private double _time = -1;

    /**
     * Returns the timestamp for this interaction.  "receive order" interactions
     * should have a timestamp of -1.
     *
     * @return timestamp for this interaction
     */
    public double getTime() {
        return _time;
    }

    /**
     * Sets the timestamp of this interaction to "time".
     *
     * @param time new timestamp for this interaction
     */
    public void setTime(double time) {
        _time = time;
    }

    /**
     * Sets the timestamp of this interaction to "logicalTime".
     *
     * @param logicalTime new timestamp for this interaction
     */
    public void setTime(LogicalTime logicalTime) {
        DoubleTime doubleTime = new DoubleTime();
        doubleTime.setTo(logicalTime);
        setTime(doubleTime.getTime());
    }

    /**
     * Creates a new InteractionRoot instance.
     */
    public InteractionRoot() {
        _uniqueID = generateUniqueID();
    }

    /**
     * Creates a copy of an InteractionRoot instance.  As an
     * InteractionRoot instance contains no parameters,
     * this has the same effect as the default constructor.
     * 
     * @param interactionRoot the interaction class to copy
     */
    public InteractionRoot(InteractionRoot interactionRoot) {
        this();
    }

    protected InteractionRoot(ReceivedInteraction datamemberMap, boolean initFlag) {
        this();
        if (initFlag) setParameters(datamemberMap);
    }

    protected InteractionRoot(ReceivedInteraction datamemberMap, LogicalTime logicalTime, boolean initFlag) {
        this();
        setTime(logicalTime);
        if (initFlag) setParameters(datamemberMap);
    }

    /**
     * Creates a new interaction instance and initializes its parameters
     * using the "datamemberMap" -- this constructor is usually called as a
     * super-class constructor to create and initialize an instance of an
     * interaction further down in the inheritance hierarchy.  "datamemberMap"
     * is usually acquired as an argument to an RTI federate callback method, such
     * as "receiveInteraction".
     *
     * @param datamemberMap contains parameter values for the newly created
     *                      interaction
     */
    public InteractionRoot(ReceivedInteraction datamemberMap) {
        this(datamemberMap, true);
    }

    /**
     * Like {@link #InteractionRoot(ReceivedInteraction datamemberMap)},
     * except the new instance has an initial timestamp of "logicalTime".
     *
     * @param datamemberMap contains parameter values for the newly created
     *                      interaction
     * @param logicalTime   initial timestamp for newly created interaction instance
     */
    public InteractionRoot(ReceivedInteraction datamemberMap, LogicalTime logicalTime) {
        this(datamemberMap, logicalTime, true);
    }

    /**
     * Returns the value of the parameter named "datamemberName" for this
     * interaction.
     *
     * @param datamemberName name of parameter whose value to retrieve
     * @return the value of the parameter whose name is "datamemberName"
     */
    public Object getParameter(String datamemberName) {
        return null;
    }

    /**
     * Returns the value of the parameter whose handle is "datamemberHandle"
     * (RTI assigned) for this interaction.
     *
     * @param datamemberHandle handle (RTI assigned) of parameter whose
     *                         value to retrieve
     * @return the value of the parameter whose handle is "datamemberHandle"
     */
    public Object getParameter(int datamemberHandle) {
        String datamemberName = getParameterName(datamemberHandle);
        if (datamemberName == null) {
            return null;
        }
        return getParameter(datamemberName);
    }

    /**
     * Set the values of the parameters in this interaction using
     * "datamemberMap".  "datamemberMap" is usually acquired as an argument to
     * an RTI federate callback method such as "receiveInteraction".
     *
     * @param datamemberMap contains new values for the parameters of
     *                      this interaction
     */
    public void setParameters(ReceivedInteraction datamemberMap) {
        int size = datamemberMap.size();
        for (int ix = 0; ix < size; ++ix) {
            try {
                setParameter(datamemberMap.getParameterHandle(ix), datamemberMap.getValue(ix));
            } catch (Exception e) {
                logger.error("setParameters: Exception caught!", e);
            }
        }
    }

    private void setParameter(int handle, byte[] val) {
        if (val == null) {
            logger.error("set: Attempt to set null value");
        }
        String valAsString = new String( val, 0, val.length );
        if (valAsString != null && valAsString.length() > 0 && valAsString.charAt(valAsString.length() - 1) == '\0') {
            valAsString = valAsString.substring(0, valAsString.length() - 1);
        }

        if (!setParameterAux(handle, valAsString)) {
            logger.error("set: bad parameter handle");
        }
    }

    /**
     * Sets the value of the parameter named "datamemberName" to "value"
     * in this interaction.  "value" is converted to data type of "datamemberName"
     * if needed.
     * This action can also be affected by calling the set_&lt;datamemberName&gt;( value )
     * method on the interaction using a reference to the interaction's actual
     * class.
     *
     * @param datamemberName name of parameter whose value is to be set
     *                       to "value"
     * @param value          new value of parameter called "datamemberName"
     */
    public void setParameter(String datamemberName, String value) {
        if (!setParameterAux(datamemberName, value)) {
            logger.error("invalid parameter \"{}\"", datamemberName);
        }
    }

    /**
     * Sets the value of the parameter named "datamemberName" to "value"
     * in this interaction.  "value" should have the same data type as that of
     * the "datamemberName" parameter.
     * This action can also be affected by calling the set_&lt;datamemberName&gt;( value )
     * method on the interaction using a reference to the interaction's actual
     * class.
     *
     * @param datamemberName name of parameter whose value is to be set
     *                       to "value"
     * @param value          new value of parameter called "datamemberName"
     */
    public void setParameter(String datamemberName, Object value) {
        if (!setParameterAux(datamemberName, value)) {
            logger.error("invalid parameter \"{}\"", datamemberName);
        }
    }

    private boolean setParameterAux(int datamemberHandle, String val) {
        String datamemberName = getParameterName(datamemberHandle);
        if (datamemberName == null) {
            return false;
        }
        return setParameterAux(datamemberName, val);
    }

    protected boolean setParameterAux(String datamemberName, String value) {
        return false;
    }

    protected boolean setParameterAux(String datamemberName, Object value) {
        return false;
    }

    protected SuppliedParameters createSuppliedDatamembers() {
        SuppliedParameters datamembers = _factory.createSuppliedParameters();
        
        for (String datamemberName : getAllParameterNames()) {
            datamembers.add(getParameterHandle(datamemberName), getParameter(datamemberName).toString().getBytes());
        }
        return datamembers;
    }
    
    /**
     * Sends this interaction to the RTI, with the specified timestamp "time".
     * This method should be used to send interactions that have "timestamp"
     * ordering.
     *
     * @param rti  handle to the LRC
     * @param time timestamp for this interaction.  The timestamp should be no
     *             less than the current federation time + the LOOKAHEAD value of the federate
     *             sending this interaction.
     */
    public void sendInteraction(RTIambassador rti, double time) {
        synchronized (rti) {
            try {
                SuppliedParameters datamembers = createSuppliedDatamembers();
                rti.sendInteraction(getClassHandle(), datamembers, null, new DoubleTime(time));
            } catch (Exception e) {
                logger.error("could not send interaction", e);
            }
        }
    }

    /**
     * Sends this interaction to the RTI (without a timestamp).
     * This method should be used to send interactions that have "receive"
     * ordering.
     *
     * @param rti handle to the LRC
     */
    public void sendInteraction(RTIambassador rti) {
        synchronized (rti) {
            try {
                SuppliedParameters datamembers = createSuppliedDatamembers();
                rti.sendInteraction(getClassHandle(), datamembers, null);
            } catch (Exception e) {
                logger.error("could not send interaction", e);
            }
        }
    }

    protected static String fedName = null;
    public static Boolean enablePubLog = false;
    public static Boolean enableSubLog = false;
    public static String pubLogLevel = null;
    public static String subLogLevel = null;

    /**
     * For use with the melding API -- this method is used to cast
     * InteractionRoot instance reference into the
     * InteractionRootInterface interface.
     *
     * @param rootInstance InteractionRoot instance reference to be
     *                     cast into the InteractionRootInterface interface
     * @return InteractionRootInterface reference to the instance
     */
    public InteractionRootInterface cast(InteractionRoot rootInstance) {
        return rootInstance;
    }

    /**
     * For use with the melding API -- this method creates a new
     * InteractionRoot instance and returns a
     * InteractionRootInterface reference to it.
     *
     * @return InteractionRootInterface reference to a newly created
     * InteractionRoot instance
     */
    public InteractionRootInterface create() {
        return new InteractionRoot();
    }

    public void copyFrom(Object object) {
    }
}
