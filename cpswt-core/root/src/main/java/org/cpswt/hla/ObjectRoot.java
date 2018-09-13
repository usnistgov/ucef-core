package org.cpswt.hla;

import hla.rti.AttributeHandleSet;
import hla.rti.AttributeNotDefined;
import hla.rti.AttributeNotOwned;
import hla.rti.ConcurrentAccessAttempted;
import hla.rti.DeletePrivilegeNotHeld;
import hla.rti.FederateNotExecutionMember;
import hla.rti.InvalidFederationTime;
import hla.rti.LogicalTime;
import hla.rti.NameNotFound;
import hla.rti.ObjectAlreadyRegistered;
import hla.rti.ObjectClassNotDefined;
import hla.rti.ObjectClassNotPublished;
import hla.rti.ObjectClassNotSubscribed;
import hla.rti.ObjectNotKnown;
import hla.rti.RTIambassador;
import hla.rti.RTIinternalError;
import hla.rti.ReflectedAttributes;
import hla.rti.RestoreInProgress;
import hla.rti.SaveInProgress;
import hla.rti.SuppliedAttributes;
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
 * ObjectRoot is the base class for all objects
 * defined in a given federation.  As such, an ObjectRoot
 * variable may refer to any type of interaction defined in the
 * federation.
 *
 * This ObjectRoot class provides the following:
 * - methods for constructing any object in the federation, either from
 * data provided by the RTI (for example, see
 * {@link #create_object(int classHandle)} or from a string argument
 * specifying the name of object to construct (see
 * {@link #create_object(String className)}.
 * - methods for sending object updates to the RTI (see
 * {@link #updateAttributeValues(RTIambassador rti)} for an example).
 * - methods for publishing/subscribing to any object/object attribute
 * defined in the federation (see
 * {@link #publish(String className, RTIambassador rti)} for example).
 * - methods for getting/setting any attribute in the object to
 * which a given ObjectRoot variable is referring
 * (see {@link #getAttribute(String datamemberName)} and
 * {@link #setAttribute(String datamemberName, Object value)}
 */
public class ObjectRoot implements ObjectRootInterface {

    private static final String FACTORY_CLASS_NAME = "org.portico.dlc.HLA13RTIFactory";
    private static final String OBJECTROOT_CLASS_NAME = "ObjectRoot";

    private static final Logger logger = LogManager.getLogger();

    private static int logId = 0;
    private static int _globalUniqueID = 0;

    private static int generateUniqueID() {
        return _globalUniqueID++;
    }

    private int _uniqueID;

    public int getUniqueID() {
        return _uniqueID;
    }

    protected static RtiFactory _factory;

    static {
        boolean factoryNotAcquired = true;
        while (factoryNotAcquired) {
            try {
                _factory = RtiFactoryFactory.getRtiFactory(FACTORY_CLASS_NAME);
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

    protected static Map<String, Set<String>> _classNamePublishAttributeNameMap = new HashMap<String, Set<String>>();
    protected static Map<String, Set<String>> _classNameSubscribeAttributeNameMap = new HashMap<String, Set<String>>();

    protected static Map<String, AttributeHandleSet> _classNamePublishedAttributeMap = new HashMap<String, AttributeHandleSet>();
    protected static Map<String, AttributeHandleSet> _classNameSubscribedAttributeMap = new HashMap<String, AttributeHandleSet>();

    private static Map<Integer, ObjectRoot> _objectMap = new HashMap<Integer, ObjectRoot>();

    protected static class Attribute<T> {
        private T _value = null;
        private T _oldValue = null;
        private boolean _oldValueInit = false;
        private double _time = 0;

        public Attribute(T init) {
            _value = init;
        }

        public T getValue() {
            return _value;
        }

        public void setValue(T value) {
            if (value == null)
                return;
            _value = value;
        }

        public double getTime() {
            return _time;
        }

        public void setTime(double time) {
            _time = time;
        }

        public void setHasBeenUpdated() {
            _oldValue = _value;
            _oldValueInit = true;
        }

        public boolean shouldBeUpdated(boolean force) {
            return force || !_oldValueInit || !_oldValue.equals(_value);
        }
    }

    private static boolean _isInitialized = false;

    private static int _handle;

    /**
     * Returns the handle (RTI assigned) of the ObjectRoot object class.
     * Note: As this is a static method, it is NOT polymorphic, and so, if called on
     * a reference will return the handle of the class pertaining to the reference,\
     * rather than the handle of the class for the instance referred to by the reference.
     * For the polymorphic version of this method, use {@link #getClassHandle()}.
     * 
     * @return the RTI assigned integer handle that represents this object class
     */
    public static int get_handle() {
        return _handle;
    }

    /**
     * Returns the fully-qualified (dot-delimited) name of the ObjectRoot
     * object class.
     * Note: As this is a static method, it is NOT polymorphic, and so, if called on
     * a reference will return the name of the class pertaining to the reference,\
     * rather than the name of the class for the instance referred to by the reference.
     * For the polymorphic version of this method, use {@link #getClassName()}.
     * 
     * @return the fully-qualified HLA class path for this object class
     */
    public static String get_class_name() {
        return OBJECTROOT_CLASS_NAME;
    }

    /**
     * Returns the simple name (the last name in the dot-delimited fully-qualified
     * class name) of the ObjectRoot object class.
     * 
     * @return the name of this interaction class
     */
    public static String get_simple_class_name() {
        return OBJECTROOT_CLASS_NAME;
    }

    private static Set<String> _datamemberNames = new HashSet<String>();
    private static Set<String> _allDatamemberNames = new HashSet<String>();

    /**
     * Returns a set containing the names of all of the non-hidden attributes in the
     * ObjectRoot object class.
     * Note: As this is a static method, it is NOT polymorphic, and so, if called on
     * a reference will return a set of parameter names pertaining to the reference,\
     * rather than the parameter names of the class for the instance referred to by
     * the reference.  For the polymorphic version of this method, use
     * {@link #getAttributeNames()}.
     * 
     * @return a modifiable set of the non-hidden parameter names for this interaction class
     */
    public static Set<String> get_attribute_names() {
        return new HashSet<String>(_datamemberNames);
    }

    /**
     * Returns a set containing the names of all of the attributes in the
     * ObjectRoot object class.
     * Note: As this is a static method, it is NOT polymorphic, and so, if called on
     * a reference will return a set of parameter names pertaining to the reference,\
     * rather than the parameter names of the class for the instance referred to by
     * the reference.  For the polymorphic version of this method, use
     * {@link #getAttributeNames()}.
     * 
     * @return a modifiable set of the parameter names for this interaction class
     */
    public static Set<String> get_all_attribute_names() {
        return new HashSet<String>(_allDatamemberNames);
    }

    private static AttributeHandleSet _publishedAttributeHandleSet;
    private static Set<String> _publishAttributeNameSet = new HashSet<String>();

    private static AttributeHandleSet _subscribedAttributeHandleSet;
    private static Set<String> _subscribeAttributeNameSet = new HashSet<String>();

    static {
        _classNameSet.add(OBJECTROOT_CLASS_NAME);
        _classNameClassMap.put(OBJECTROOT_CLASS_NAME, ObjectRoot.class);

        _datamemberClassNameSetMap.put(OBJECTROOT_CLASS_NAME, _datamemberNames);
        _allDatamemberClassNameSetMap.put(OBJECTROOT_CLASS_NAME, _allDatamemberNames);

        _classNamePublishAttributeNameMap.put(OBJECTROOT_CLASS_NAME, _publishAttributeNameSet);
        _publishedAttributeHandleSet = _factory.createAttributeHandleSet();
        _classNamePublishedAttributeMap.put(OBJECTROOT_CLASS_NAME, _publishedAttributeHandleSet);

        _classNameSubscribeAttributeNameMap.put(OBJECTROOT_CLASS_NAME, _subscribeAttributeNameSet);
        _subscribedAttributeHandleSet = _factory.createAttributeHandleSet();
        _classNameSubscribedAttributeMap.put(OBJECTROOT_CLASS_NAME, _subscribedAttributeHandleSet);
    }

    protected static void init(RTIambassador rti) {
        if (_isInitialized) return;
        _isInitialized = true;

        boolean isNotInitialized = true;
        while (isNotInitialized) {
            try {
                _handle = rti.getObjectClassHandle(OBJECTROOT_CLASS_NAME);
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

        _classNameHandleMap.put(OBJECTROOT_CLASS_NAME, get_handle());
        _classHandleNameMap.put(get_handle(), OBJECTROOT_CLASS_NAME);
        _classHandleSimpleNameMap.put(get_handle(), OBJECTROOT_CLASS_NAME);
    }

    private static boolean _isPublished = false;

    /**
     * Publishes the ObjectRoot object class for a federate.
     *
     * @param rti handle to the RTI
     */
    public static void publish(RTIambassador rti) {
        if (_isPublished) return;

        init(rti);

        _publishedAttributeHandleSet.empty();
        for (String attributeName : _publishAttributeNameSet) {
            try {
                _publishedAttributeHandleSet.add(_datamemberNameHandleMap.get("ObjectRoot." + attributeName));
            } catch (Exception e) {
                logger.error("could not publish \"" + attributeName + "\" attribute.", e);
            }
        }

        synchronized (rti) {
            boolean isNotPublished = true;
            while (isNotPublished) {
                try {
                    rti.publishObjectClass(get_handle(), _publishedAttributeHandleSet);
                    isNotPublished = false;
                } catch (FederateNotExecutionMember e) {
                    logger.error("could not publish: Federate Not Execution Member", e);
                    return;
                } catch (ObjectClassNotDefined e) {
                    logger.error("could not publish: Object Class Not Defined", e);
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
     * Unpublishes the ObjectRoot object class for a federate.
     *
     * @param rti handle to the RTI
     */
    public static void unpublish(RTIambassador rti) {
        if (!_isPublished) return;

        init(rti);
        
        synchronized (rti) {
            boolean isNotUnpublished = true;
            while (isNotUnpublished) {
                try {
                    rti.unpublishObjectClass(get_handle());
                    isNotUnpublished = false;
                } catch (FederateNotExecutionMember e) {
                    logger.error("could not unpublish: Federate Not Execution Member", e);
                    return;
                } catch (ObjectClassNotDefined e) {
                    logger.error("could not unpublish: Object Class Not Defined", e);
                    return;
                } catch (ObjectClassNotPublished e) {
                    logger.error("could not unpublish: Object Class Not Published", e);
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
     * Subscribes a federate to the ObjectRoot object class.
     *
     * @param lrc handle to the RTI
     */
    public static void subscribe(RTIambassador lrc) {
        if (_isSubscribed) return;

        init(lrc);

        _subscribedAttributeHandleSet.empty();
        for (String attributeName : _subscribeAttributeNameSet) {
            try {
                _subscribedAttributeHandleSet.add(_datamemberNameHandleMap.get("ObjectRoot." + attributeName));
            } catch (Exception e) {
                logger.error("could not subscribe to \"" + attributeName + "\" attribute.", e);
            }
        }

        synchronized (lrc) {
            boolean isNotSubscribed = true;
            while (isNotSubscribed) {
                try {
                    lrc.subscribeObjectClassAttributes(get_handle(), _subscribedAttributeHandleSet);
                    isNotSubscribed = false;
                } catch (FederateNotExecutionMember e) {
                    logger.error("could not subscribe: Federate Not Execution Member", e);
                    return;
                } catch (ObjectClassNotDefined e) {
                    logger.error("could not subscribe: Object Class Not Defined", e);
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
     * Unsubscribes a federate from the ObjectRoot object class.
     *
     * @param rti handle to the RTI
     */
    public static void unsubscribe(RTIambassador rti) {
        if (!_isSubscribed) return;

        init(rti);
        
        synchronized (rti) {
            boolean isNotUnsubscribed = true;
            while (isNotUnsubscribed) {
                try {
                    rti.unsubscribeObjectClass(get_handle());
                    isNotUnsubscribed = false;
                } catch (FederateNotExecutionMember e) {
                    logger.error("could not unsubscribe: Federate Not Execution Member", e);
                    return;
                } catch (ObjectClassNotDefined e) {
                    logger.error("could not unsubscribe: Object Class Not Defined", e);
                    return;
                } catch (ObjectClassNotSubscribed e) {
                    logger.error("could not unsubscribe: Object Class Not Subscribed", e);
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
     * (that is, the ObjectRoot object class).
     *
     * @param handle handle to compare to the value of the handle (RTI assigned) of
     *               this class (the ObjectRoot object class).
     * @return "true" if "handle" matches the value of the handle of this class
     * (that is, the ObjectRoot object class).
     */
    public static boolean match(int handle) {
        return handle == get_handle();
    }

    /**
     * Returns the handle (RTI assigned) of this instance's object class .
     *
     * @return the handle (RTI assigned) if this instance's object class
     */
    public int getClassHandle() {
        return get_handle();
    }

    /**
     * Returns the fully-qualified (dot-delimited) name of this instance's object class.
     *
     * @return the fully-qualified (dot-delimited) name of this instance's object class
     */
    public String getClassName() {
        return get_class_name();
    }

    /**
     * Returns the simple name (last name in its fully-qualified dot-delimited name)
     * of this instance's object class.
     *
     * @return the simple name of this instance's object class
     */
    public String getSimpleClassName() {
        return get_simple_class_name();
    }

    /**
     * Returns a set containing the names of all of the non-hiddenattributes of an
     * object class instance.
     *
     * @return set containing the names of all of the attributes of an
     * object class instance
     */
    public Set<String> getAttributeNames() {
        return get_attribute_names();
    }

    /**
     * Returns a set containing the names of all of the attributes of an
     * object class instance.
     *
     * @return set containing the names of all of the attributes of an
     * object class instance
     */
    public Set<String> getAllAttributeNames() {
        return get_all_attribute_names();
    }

    /**
     * Returns the attribute name associated with the given handle for an object class instance.
     * 
     * @param datamemberHandle an attribute handle assigned by the RTI
     * @return the attribute name associated with the handle, or null
     */
    public String getAttributeName(int datamemberHandle) {
        return null;
    }
    
    /**
     * Returns the handle associated with the given attribute name for an object class instance
     * 
     * @param datamemberName the name of an attribute that belongs to this object class
     * @return the RTI handle associated with the attribute name, or -1 if not found
     */
    public int getAttributeHandle(String datamemberName) {
        return get_attribute_handle(getClassName(), datamemberName);
    }

    /**
     * Publishes the object class of this instance of the class for a federate.
     *
     * @param rti handle to the RTI
     */
    public void publishObject(RTIambassador rti) {
        publish(rti);
    }

    /**
     * Unpublishes the object class of this instance of this class for a federate.
     *
     * @param rti handle to the RTI
     */
    public void unpublishObject(RTIambassador rti) {
        unpublish(rti);
    }

    /**
     * Subscribes a federate to the object class of this instance of this class.
     *
     * @param rti handle to the RTI
     */
    public void subscribeObject(RTIambassador rti) {
        subscribe(rti);
    }

    /**
     * Unsubscribes a federate from the object class of this instance of this class.
     *
     * @param rti handle to the RTI
     */
    public void unsubscribeObject(RTIambassador rti) {
        unsubscribe(rti);
    }

    /**
     * Returns a data structure containing the handles of all attributes for this object
     * class that are currently marked for subscription.  To actually subscribe to these
     * attributes, a federate must call &lt;objectclassname&gt;.subscribe( RTIambassador rti ).
     *
     * @return data structure containing the handles of all attributes for this object
     * class that are currently marked for subscription
     */
    public AttributeHandleSet getSubscribedAttributeHandleSet() {
        return _subscribedAttributeHandleSet;
    }

    public String toString() {
        return getClass().getName() + "()";
    }

    /**
     * Returns a set of strings containing the names of all of the object
     * classes in the current federation.
     *
     * @return Set&lt; String &gt; containing the names of all object classes
     * in the current federation
     */
    public static Set<String> get_object_names() {
        return new HashSet<String>(_classNameSet);
    }

    /**
     * Returns a set of strings containing the names of all of the non-hidden attributes
     * in the object class specified by className.
     *
     * @param className name of object class for which to retrieve the
     *                  names of all of its attributes
     * @return Set&lt; String &gt; containing the names of all attributes in the
     * className object class
     */
    public static Set<String> get_attribute_names(String className) {
        return new HashSet<String>(_datamemberClassNameSetMap.get(className));
    }

    /**
     * Returns a set of strings containing the names of all of the attributes
     * in the object class specified by className.
     *
     * @param className name of object class for which to retrieve the
     *                  names of all of its attributes
     * @return Set&lt; String &gt; containing the names of all attributes in the
     * className object class
     */
    public static Set<String> get_all_attribute_names(String className) {
        return new HashSet<String>(_allDatamemberClassNameSetMap.get(className));
    }

    /**
     * Returns the fully-qualified name of the object class corresponding
     * to the RTI-defined classHandle.
     *
     * @param classHandle handle (defined by RTI) of object class for
     *                    which to retrieve the fully-qualified name
     * @return the fully-qualified name of the object class that
     * corresponds to the RTI-defined classHandle
     */
    public static String get_class_name(int classHandle) {
        return _classHandleNameMap.get(classHandle);
    }

    /**
     * Returns the simple name of the object class corresponding to the
     * RTI-defined classHandle.  The simple name of an object class is
     * the last name in its (dot-delimited) fully-qualified name.
     *
     * @param classHandle handle (defined by RTI) of object class for which
     *                    to retrieve the simple name
     * @return the simple name of the object class that corresponds to
     * the RTI-defined classHandle
     */
    public static String get_simple_class_name(int classHandle) {
        return _classHandleSimpleNameMap.get(classHandle);
    }

    /**
     * Returns the integer handle (RTI defined) of the object class
     * corresponding to the fully-qualified object class name in className.
     *
     * @param className fully-qualified name of object class for which to
     *                  retrieve the RTI-defined integer handle
     * @return the RTI-defined handle of the object class
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
     * Returns the name of an attribute corresponding to
     * its handle (RTI assigned) in datamemberHandle.
     *
     * @param datamemberHandle handle of attribute (RTI assigned)
     *                         for which to return the name
     * @return the name of the attribute corresponding to datamemberHandle
     */
    public static String get_attribute_name(int datamemberHandle) {
        return _datamemberHandleNameMap.get(datamemberHandle);
    }

    /**
     * Returns the handle of an attribute (RTI assigned) given
     * its object class name and attribute name
     *
     * @param className      name of object class
     * @param datamemberName name of attribute
     * @return the handle (RTI assigned) of the attribute "datamemberName" of object class "className"
     */
    public static int get_attribute_handle(String className, String datamemberName) {
        Integer datamemberHandle = _datamemberNameHandleMap.get(className + "." + datamemberName);

        if (datamemberHandle == null) {
            logger.error("Bad attribute \"{}\" for class \"{}\" on get_attribute_handle.", datamemberName, className);
            return -1;
        }
        return datamemberHandle;
    }

    private static Class<?>[] pubsubArguments = new Class<?>[]{RTIambassador.class};

    /**
     * Publishes the object class named by "className" for a federate.
     * This can also be performed by calling the publish( RTIambassador rti )
     * method directly on the object class named by "className" (for
     * example, to publish the ObjectRoot class in particular,
     * see {@link ObjectRoot#publish(RTIambassador rti)}).
     *
     * @param className name of object class to be published for the federate
     * @param rti       handle to the RTI
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
     * Unpublishes the object class named by "className" for a federate.
     * This can also be performed by calling the unpublish( RTIambassador rti )
     * method directly on the object class named by "className" (for
     * example, to unpublish the ObjectRoot class in particular,
     * see {@link ObjectRoot#unpublish(RTIambassador rti)}).
     *
     * @param className name of object class to be unpublished for the federate
     * @param rti       handle to the RTI
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
     * Subscribes federate to the object class names by "className"
     * This can also be performed by calling the subscribe( RTIambassador rti )
     * method directly on the object class named by "className" (for
     * example, to subscribe a federate to the ObjectRoot class
     * in particular, see {@link ObjectRoot#subscribe(RTIambassador rti)}).
     *
     * @param className name of object class to which to subscribe the federate
     * @param rti       handle to the RTI
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
     * Unsubscribes federate from the object class names by "className"
     * This can also be performed by calling the unsubscribe( RTIambassador rti )
     * method directly on the object class named by "className" (for
     * example, to unsubscribe a federate to the ObjectRoot class
     * in particular, see {@link ObjectRoot#unsubscribe(RTIambassador rti)}).
     *
     * @param className name of object class to which to unsubscribe the federate
     * @param rti       handle to the RTI
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

    /**
     * Publishes the attribute named by "attributeName" of the object class named
     * by "className" for a federate.  This can also be performed by calling the
     * publish_&lt;attributeName&gt;() method directly on the object class named by
     * "className".
     * <p>
     * Note:  This method only marks the attribute named by "attributeName" for
     * publication.  The attribute doesn't actually get published until the
     * "className" object class, of which it is a member, is (re)published.  See
     * {@link ObjectRoot#publish(String className, RTIambassador RTI)} and
     * {@link ObjectRoot#publish(RTIambassador RTI)} for examples of how to
     * publish the object class.
     *
     * @param className     name of object class for which the attribute named by
     *                      "attributeName" is to be published
     * @param attributeName name of the attribute to be published
     */
    public static void publish(String className, String attributeName) {
        try {
            _classNamePublishAttributeNameMap.get(className).add(attributeName);
        } catch (Exception e) {
            logger.error("could not publish class \"" + className + "\" \"" + attributeName + "\" attribute.", e);
        }
    }

    /**
     * Unpublishes the attribute named by "attributeName" of the object class named
     * by "className" for a federate.  This can also be performed by calling the
     * unpublish_&lt;attributeName&gt;() method directly on the object class named by
     * "className".
     * <p>
     * Note:  This method only marks the attribute named by "attributeName" for
     * un-publication. The attribute doesn't actually get unpublished until the
     * "className" object class, of which it is a member, is (re)published.  See
     * {@link ObjectRoot#publish(String className, RTIambassador RTI)} and
     * {@link ObjectRoot#publish(RTIambassador RTI)} for examples of how to
     * publish the object class.
     *
     * @param className     name of object class for which the attribute named by
     *                      "attributeName" is to be unpublished (by a federate)
     * @param attributeName name of the attribute to be unpublished
     */
    public static void unpublish(String className, String attributeName) {
        try {
            _classNamePublishAttributeNameMap.get(className).remove(attributeName);
        } catch (Exception e) {
            logger.error("could not unpublish class \"" + className + "\" \"" + attributeName + "\" attribute.", e);
        }
    }

    /**
     * Subscribe a federate to the attribute named by "attributeName" of the
     * object class named by "className".  This can also be performed by calling
     * the subscribe_&lt;attributeName&gt;() method directly on the object class named
     * by "className".
     * <p>
     * Note:  This method only marks the attribute named by "attributeName" for
     * subscription.  The attribute doesn't actually get subscribed to until the
     * "className" object class, of which it is a member, is (re)subscribed to.
     * See {@link ObjectRoot#subscribe(String className, RTIambassador RTI)} and
     * {@link ObjectRoot#subscribe(RTIambassador RTI)} for examples of how to
     * subscribe to the object class.
     *
     * @param className     name of object class for which the attribute named by
     *                      "attributeName" is to be subcribed
     * @param attributeName name of the attribute to be published
     */
    public static void subscribe(String className, String attributeName) {
        try {
            _classNameSubscribeAttributeNameMap.get(className).add(attributeName);
        } catch (Exception e) {
            logger.error("could not subscribe to class \"" + className + "\" \"" + attributeName + "\" attribute.", e);
        }
    }

    /**
     * Unsubscribe a federate from the attribute named by "attributeName" of the
     * object class named by "className".  This can also be performed by calling
     * the unsubscribe_&lt;attributeName&gt;() method directly on the object class named
     * by "className".
     * <p>
     * Note:  This method only marks the attribute named by "attributeName" for
     * unsubscription.  The attribute doesn't actually get unsubscribed from until the
     * "className" object class, of which it is a member, is (re)subscribed to.
     * See {@link ObjectRoot#subscribe(String className, RTIambassador RTI)} and
     * {@link ObjectRoot#subscribe(RTIambassador RTI)} for examples of how to
     * subscribe to the object class.
     *
     * @param className     name of object class for which the attribute named by
     *                      "attributeName" is to be subcribed
     * @param attributeName name of the attribute to be published
     */
    public static void unsubscribe(String className, String attributeName) {
        try {
            _classNameSubscribeAttributeNameMap.get(className).remove(attributeName);
        } catch (Exception e) {
            logger.error("could not unsubscribe class \"" + className + "\" \"" + attributeName + "\" attribute.", e);
        }
    }

    private static ObjectRoot create_object(Class<?> rtiClass) {
        ObjectRoot classRoot = null;
        try {
            classRoot = (ObjectRoot) rtiClass.newInstance();
        } catch (Exception e) {
            logger.error("could not create/cast new Object", e);
        }

        return classRoot;
    }

    private static ObjectRoot create_object(Class<?> rtiClass, LogicalTime logicalTime) {
        ObjectRoot classRoot = create_object(rtiClass);
        if (classRoot != null) classRoot.setTime(logicalTime);
        return classRoot;
    }

    private static ObjectRoot create_object(Class<?> rtiClass, ReflectedAttributes datamemberMap) {
        ObjectRoot classRoot = create_object(rtiClass);
        classRoot.setAttributes(datamemberMap);
        return classRoot;
    }

    private static ObjectRoot create_object(Class<?> rtiClass, ReflectedAttributes datamemberMap, LogicalTime logicalTime) {
        ObjectRoot classRoot = create_object(rtiClass);
        classRoot.setAttributes(datamemberMap);
        classRoot.setTime(logicalTime);
        return classRoot;
    }

    /**
     * Create an object that is in instance of object class
     * "className". An ObjectRoot reference is returned,
     * so to refer to the instance using a reference to a "className" interaction,
     * the returned reference must be cast down the object inheritance
     * hierarchy.
     * An instance of the "className" object class may also be created
     * by using the "new" operator directory on the "className" object
     * class.  For instance, two ways to create an ObjectRoot
     * instance are
     * Object.create_object( "ObjectRoot" ),
     * and
     * new ObjectRoot()
     *
     * @param className fully-qualified (dot-delimited) name of the object
     *                  class for which to create an instance
     * @return instance of "className" object class
     */
    public static ObjectRoot create_object(String className) {
        Class<?> rtiClass = _classNameClassMap.get(className);
        if (rtiClass == null) return null;

        return create_object(rtiClass);
    }

    /**
     * Like {@link #create_object(String className)}, but object
     * is created with a timestamp based on "logicalTime".
     *
     * @param className   fully-qualified (dot-delimited) name of the object
     *                    class for which to create an instance
     * @param logicalTime timestamp to place on the new object class instance
     * @return instance of "className" object class with "logicalTime" time stamp.
     */
    public static ObjectRoot create_object(String className, LogicalTime logicalTime) {
        Class<?> rtiClass = _classNameClassMap.get(className);
        if (rtiClass == null) return null;

        return create_object(rtiClass, logicalTime);
    }

    /**
     * Create an object that is in instance of object class
     * that corresponds to the "classHandle" handle (RTI assigned). An
     * ObjectRoot reference is returned, so to refer to the
     * instance using a reference to a "className" interaction, the returned
     * reference must be cast down the object inheritance hierarchy.
     *
     * @param classHandle handle of object class (RTI assigned) class for
     *                    which to create an instance
     * @return instance of object class corresponding to "classHandle"
     */
    public static ObjectRoot create_object(int classHandle) {
        Class<?> rtiClass = _classNameClassMap.get(_classHandleNameMap.get(classHandle));
        if (rtiClass == null) return null;

        return create_object(rtiClass);
    }

    /**
     * Like {@link #create_object(int classHandle)}, but the object
     * is created with a timestamp based on "logicalTime".
     *
     * @param classHandle handle of object class (RTI assigned) class for
     *                    which to create an instance
     * @param logicalTime timestamp to place on the new object class instance
     * @return instance of object class corresponding to "classHandle" with
     * "logicalTime" time stamp
     */
    public static ObjectRoot create_object(int classHandle, LogicalTime logicalTime) {
        Class<?> rtiClass = _classNameClassMap.get(_classHandleNameMap.get(classHandle));
        if (rtiClass == null) return null;

        return create_object(rtiClass, logicalTime);
    }

    /**
     * Like {@link #create_object(int classHandle)}, but the object's
     * attributes are initialized using "datamemberMap".  The "datamemberMap"
     * is usually acquired as an argument to an RTI callback method of a federate.
     *
     * @param classHandle   handle of object class (RTI assigned) class for
     *                      which to create an instance
     * @param datamemberMap contains initializing values for the attributes
     *                      of the object class instance
     * @return instance of object class corresponding to "classHandle" with
     * its attributes initialized with the "datamemberMap"
     */
    public static ObjectRoot create_object(int classHandle, ReflectedAttributes datamemberMap) {
        Class<?> rtiClass = _classNameClassMap.get(_classHandleNameMap.get(classHandle));
        if (rtiClass == null) return null;

        return create_object(rtiClass, datamemberMap);
    }

    /**
     * Like {@link #create_object(int classHandle, ReflectedAttributes datamemberMap)},
     * but the object is given a timestamp based on "logicalTime".
     *
     * @param classHandle   handle of object class (RTI assigned) class for
     *                      which to create an instance
     * @param datamemberMap initializing values for the attributes of the
     *                      object class instance
     * @param logicalTime   timestamp to place on the new object class instance
     * @return instance of object class corresponding to "classHandle" with
     * its attributes initialized with the "datamemberMap" and with
     * "logicalTime" timestamp
     */
    public static ObjectRoot create_object(int classHandle, ReflectedAttributes datamemberMap, LogicalTime logicalTime) {
        Class<?> rtiClass = _classNameClassMap.get(_classHandleNameMap.get(classHandle));
        if (rtiClass == null) return null;

        return create_object(rtiClass, datamemberMap, logicalTime);
    }

    /**
     * Creates a new instance of the object class corresponding to "objectClassHandle",
     * registers it in an map internal to the ObjectRoot class using "objectClassHandle"
     * as a key, and returns a reference to the instance.  Though the created
     * instance is of the object class corresponding to "objectClassHandle" (which is
     * a handle assigned by the RTI), it is referred to, via the return value, by
     * an ObjectRoot reference.  Thus, to refer to it as an instance of the object
     * class corresponding to "objectClassHandle", the ObjectRoot reference needs to be
     * cast down through the inheritance hierarchy.
     *
     * objectClassHandle and objectHandle are usually acquired as arguments of the
     * "discoverObjectInstance" RTI callback method of a federate.
     *
     * @param objectClassHandle  handle of object class (RTI assigned) for which to create
     *                      an instance
     * @param objectHandle handle (also RTI assigned) of this instance as it is
     *                      known to the RTI.  Any updates to the instance attributes provided by the
     *                      RTI (via a "reflectAttributeValues" federate callback) will be identified
     *                      with this objectHandle.
     * @return new instance of the object class corresponding to objectClassHandle
     */
    public static ObjectRoot discover(int objectClassHandle, int objectHandle) {
        Class<?> hlaObjectClass = _classNameClassMap.get(_classHandleNameMap.get(objectClassHandle));
        ObjectRoot objectRoot = null;
        try {
            objectRoot = (ObjectRoot) hlaObjectClass.newInstance();
            objectRoot.setObjectHandle(objectHandle);
            _objectMap.put(objectHandle, objectRoot);
        } catch (Exception e) {
            logger.error("could not discover object", e);
        }
        return objectRoot;
    }

    /**
     * Retrieves the object instance corresponding to "objectHandle" from an
     * internal table in the ObjectRoot class, updates its attribute values using
     * "reflectedAttributes", and returns the instance.  Both "objectHandle" and
     * "reflectedAttributes" are usually acquired as arguments of the
     * "reflectAttributeValues" RTI callback of a federate.
     * The return value is an ObjectRoot reference to the instance.  So, to refer
     * to the instance as an instance of its actual class, this reference will
     * have to be cast down the inheritance hierarchy.
     *
     * @param objectHandle       handle (RTI assigned) of object instance for which the
     *                            attributes are to be updated.
     * @param reflectedAttributes set of updated values for the attributes of the
     *                            object instance corresponding to objectHandle.
     * @return the object instance with updated attribute values
     */
    public static ObjectRoot reflect(int objectHandle, ReflectedAttributes reflectedAttributes) {
        ObjectRoot objectRoot = _objectMap.get(objectHandle);
        if (objectRoot == null) return null;
        objectRoot.setTime(-1);
        objectRoot.setAttributes(reflectedAttributes);
        return objectRoot;
    }

    /**
     * Like {@link #reflect(int objectHandle, ReflectedAttributes reflectedAttributes)},
     * except the updated attributes of the object instance have their timestamps
     * updated to "logicalTime".
     *
     * @param objectHandle       handle (RTI assigned) of object instance for which the
     *                            attributes are to be updated.
     * @param reflectedAttributes set of updated values for the attributes of the
     *                            object instance corresponding to objectHandle.
     * @param logicalTime         new time stamp for attributes that are updated
     * @return the object instance with updated attribute values
     */
    public static ObjectRoot reflect(int objectHandle, ReflectedAttributes reflectedAttributes, LogicalTime logicalTime) {
        ObjectRoot objectRoot = _objectMap.get(objectHandle);
        if (objectRoot == null) return null;
        objectRoot.setTime(logicalTime);
        objectRoot.setAttributes(reflectedAttributes);
        return objectRoot;
    }

    /**
     * Like {@link #reflect(int objectHandle, ReflectedAttributes reflectedAttributes)},
     * except the updated attributes of the object instance have their timestamps
     * updated to "time".
     *
     * @param objectHandle       handle (RTI assigned) of object instance for which the
     *                            attributes are to be updated.
     * @param reflectedAttributes set of updated values for the attributes of the
     *                            object instance corresponding to objectHandle.
     * @param time                new time stamp for attributes that are updated
     * @return the object instance with updated attribute values
     */
    public static ObjectRoot reflect(int objectHandle, ReflectedAttributes reflectedAttributes, double time) {
        ObjectRoot objectRoot = _objectMap.get(objectHandle);
        if (objectRoot == null) return null;
        objectRoot.setTime(time);
        objectRoot.setAttributes(reflectedAttributes);
        return objectRoot;
    }

    /**
     * Requests an attribute update for this object instance from the federate that
     * has modification rights on these attributes.
     *
     * @param rti handle to the RTI
     */
    public void requestUpdate(RTIambassador rti) {
        boolean requestNotSubmitted = true;
        while (requestNotSubmitted) {
            try {
                // TODO: LOG HERE
                rti.requestObjectAttributeValueUpdate(getObjectHandle(), getSubscribedAttributeHandleSet());
                requestNotSubmitted = false;
            } catch (FederateNotExecutionMember e) {
                logger.error("request for update failed: Federate Not Execution Member", e);
                return;
            } catch (ObjectNotKnown e) {
                logger.error("request for update failed: Object Not Known", e);
                return;
            } catch (AttributeNotDefined e) {
                logger.error("request for update failed: Name Not Found", e);
                return;
            } catch (Exception e) {
                logger.error(e);
                CpswtUtils.sleepDefault();
            }
        }
    }

    /**
     * Returns the object instance corresponding to the "objectHandle" (RTI
     * assigned) from a map internal to the ObjectRoot class.
     * The object instance is referred to, via the return value, using an
     * an ObjectRoot reference.  To reference to it using a reference of its
     * actual class, the returned reference must be cast down through the
     * inhertance hierarchy.
     *
     * @param objectHandle handle (RTI assigned) of object instance to retrieve
     *                      from the map internal to the ObjectRoot class.
     * @return object instance corresponding to the objectHandle (RTI assigned)
     * in the map that is internal to the ObjectRoot class.
     */
    public static ObjectRoot getObject(int objectHandle) {
        return _objectMap.get(objectHandle);
    }

    /**
     * Returns the object instance corresponding to the "objectHandle" (RTI
     * assigned) from a map internal to the ObjectRoot class AND REMOVES IT
     * FROM THIS MAP.
     * The object instance is referred to, via the return value, using an
     * an ObjectRoot reference.  To reference to it using a reference of its
     * actual class, the returned reference must be cast down through the
     * inhertance hierarchy.
     *
     * @param objectHandle handle (RTI assigned) of object instance to retrieve
     *                      from the map internal to the ObjectRoot class.
     * @return object instance corresponding to the objectHandle (RTI assigned)
     * in the map that is internal to the ObjectRoot class.
     */
    public static ObjectRoot removeObject(int objectHandle) {
        return _objectMap.remove(objectHandle);
    }

    private int _objectHandle;

    private void setObjectHandle(int objectHandle) {
        _objectMap.remove(objectHandle);
        _objectHandle = objectHandle;
        _objectMap.put(objectHandle, this);
    }

    /**
     * Returns the handle (RTI assigned) the corresponds to this object class
     * instance.  This handle is the instance's unique identifier to the RTI.
     *
     * @return the handle (RTI assigned) of this object class instance.
     */
    public int getObjectHandle() {
        return _objectHandle;
    }

    private double _time = -1;

    /**
     * Returns the timestamp for this object.  "receive order" objects
     * should have a timestamp of -1.
     *
     * @return timestamp for this object
     */
    public double getTime() {
        return _time;
    }

    /**
     * Sets the timestamp of this object to "time".
     *
     * @param time new timestamp for this object
     */
    public void setTime(double time) {
        _time = time;
    }

    /**
     * Sets the timestamp of this object to "logicalTime".
     *
     * @param logicalTime new timestamp for this object
     */
    public void setTime(LogicalTime logicalTime) {
        DoubleTime doubleTime = new DoubleTime();
        doubleTime.setTo(logicalTime);
        setTime(doubleTime.getTime());
    }

    /**
     * Creates a new ObjectRoot instance.
     */
    public ObjectRoot() {
        _uniqueID = generateUniqueID();
    }

    /**
     * Creates a copy of an ObjectRoot instance.  As an
     * ObjectRoot instance contains no attributes,
     * this has the same effect as the default constructor.
     * 
     * @param objectRoot the object instance to copy
     */
    public ObjectRoot(ObjectRoot objectRoot) {
        this();
    }

    protected ObjectRoot(ReflectedAttributes datamemberMap, boolean initFlag) {
        this();
        if (initFlag) setAttributes(datamemberMap);
    }

    protected ObjectRoot(ReflectedAttributes datamemberMap, LogicalTime logicalTime, boolean initFlag) {
        this();
        setTime(logicalTime);
        if (initFlag) setAttributes(datamemberMap);
    }

    /**
     * Creates a new object instance and initializes its attributes
     * using the "datamemberMap" -- this constructor is usually called as a
     * super-class constructor to create and initialize an instance of an
     * object further down in the inheritance hierarchy.  "datamemberMap"
     * is usually acquired as an argument to an RTI federate callback method, such
     * as "receiveInteraction".
     *
     * @param datamemberMap contains attribute values for the newly created
     *                      object
     */
    public ObjectRoot(ReflectedAttributes datamemberMap) {
        this(datamemberMap, true);
    }

    /**
     * Like {@link #ObjectRoot(ReflectedAttributes datamemberMap)},
     * except the new instance has an initial timestamp of "logicalTime".
     *
     * @param datamemberMap contains attribute values for the newly created
     *                      object
     * @param logicalTime   initial timestamp for newly created object instance
     */
    public ObjectRoot(ReflectedAttributes datamemberMap, LogicalTime logicalTime) {
        this(datamemberMap, logicalTime, true);
    }

    /**
     * Returns the value of the attribute named "datamemberName" for this
     * object.
     *
     * @param datamemberName name of attribute whose value to retrieve
     * @return the value of the attribute whose name is "datamemberName"
     */
    public Object getAttribute(String datamemberName) {
        return null;
    }

    /**
     * Returns the value of the attribute whose handle is "datamemberHandle"
     * (RTI assigned) for this object.
     *
     * @param datamemberHandle handle (RTI assigned) of attribute whose
     *                         value to retrieve
     * @return the value of the attribute whose handle is "datamemberHandle"
     */
    public Object getAttribute(int datamemberHandle) {
        String datamemberName = getAttributeName(datamemberHandle);
        if (datamemberName == null) {
            return null;
        }
        return getAttribute(datamemberName);
    }

    /**
     * Set the values of the attributes in this object using
     * "datamemberMap".  "datamemberMap" is usually acquired as an argument to
     * an RTI federate callback method such as "receiveInteraction".
     *
     * @param datamemberMap contains new values for the attributes of
     *                      this object
     */
    public void setAttributes(ReflectedAttributes datamemberMap) {
        int size = datamemberMap.size();
        for (int ix = 0; ix < size; ++ix) {
            try {
                setAttribute(datamemberMap.getAttributeHandle(ix), datamemberMap.getValue(ix));
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }

    private void setAttribute(int handle, byte[] val) {
        if (val == null) {
            logger.error("set: attempt to set null value");
        }
        String valAsString = new String( val, 0, val.length );
        if (valAsString != null && valAsString.length() > 0 && valAsString.charAt(valAsString.length() - 1) == '\0') {
            valAsString = valAsString.substring(0, valAsString.length() - 1);
        }

        if (!setAttributeAux(handle, valAsString)) {
            logger.error("set: bad attribute handle");
        }
    }

    /**
     * Sets the value of the attribute named "datamemberName" to "value"
     * in this object.  "value" is converted to data type of "datamemberName"
     * if needed.
     * This action can also be affected by calling the set_&lt;datamemberName&gt;( value )
     * method on the object using a reference to the object's actual
     * class.
     *
     * @param datamemberName name of attribute whose value is to be set
     *                       to "value"
     * @param value          new value of attribute called "datamemberName"
     */
    public void setAttribute(String datamemberName, String value) {
        if (!setAttributeAux(datamemberName, value)) {
            logger.error("invalid attribute \"{}\"", datamemberName);
        }
    }

    /**
     * Sets the value of the attribute named "datamemberName" to "value"
     * in this object.  "value" should have the same data type as that of
     * the "datamemberName" attribute.
     * This action can also be affected by calling the set_&lt;datamemberName&gt;( value )
     * method on the object using a reference to the object's actual
     * class.
     *
     * @param datamemberName name of attribute whose value is to be set
     *                       to "value"
     * @param value          new value of attribute called "datamemberName"
     */
    public void setAttribute(String datamemberName, Object value) {
        if (!setAttributeAux(datamemberName, value)) {
            logger.error("invalid attribute \"{}\"", datamemberName);
        }
    }

    protected boolean setAttributeAux(int datamemberHandle, String val) {
        String datamemberName = getAttributeName(datamemberHandle);
        if (datamemberName == null) {
            return false;
        }
        return setAttributeAux(datamemberName, val);
    }

    protected boolean setAttributeAux(String datamemberName, String value) {
        return false;
    }

    protected boolean setAttributeAux(String datamemberName, Object value) {
        return false;
    }

    protected SuppliedAttributes createSuppliedDatamembers(boolean force) {
        return _factory.createSuppliedAttributes();
    }

    private boolean _isRegistered = false;

    /**
     * Registers this object with the RTI.  This method is usually called by a
     * federate who "owns" this object, i.e. the federate that created it and
     * has write-privileges to its attributes (so, it is responsible for updating
     * these attribute and conveying their updated values to the RTI).
     *
     * @param rti handle to the RTI
     */
    public void registerObject(RTIambassador rti) {
        while (!_isRegistered) {
            try {
                synchronized (rti) {
                    _objectHandle = rti.registerObjectInstance(getClassHandle());
                }
                _isRegistered = true;
                _objectMap.put(getObjectHandle(), this);
            } catch (ObjectClassNotDefined | ObjectClassNotPublished | FederateNotExecutionMember e) {
                logger.error(e);
                return;
            } catch (Exception e) {
                CpswtUtils.sleep(500);
            }
        }
    }

    /**
     * Registers this object with the RTI using the given name.  This method is usually
     * called by a federate who "owns" this object, i.e. the federate that created it and
     * has write-privileges to its attributes (so, it is responsible for updating
     * these attribute and conveying their updated values to the RTI).
     *
     * @param rti handle to the RTI
     * @param name unique identifier to assign to the object instance
     * @throws ObjectAlreadyRegistered if the name is already assigned to another object instance
     */
    public void registerObject(RTIambassador rti, String name) throws ObjectAlreadyRegistered {

        while (!_isRegistered) {
            try {
                synchronized (rti) {
                    _objectHandle = rti.registerObjectInstance(getClassHandle(), name);
                }
                _isRegistered = true;
                _objectMap.put(getObjectHandle(), this);

            } catch (ObjectClassNotDefined | ObjectClassNotPublished | FederateNotExecutionMember ex) {
                logger.error(ex);
                return;
            } catch (SaveInProgress | RestoreInProgress | RTIinternalError | ConcurrentAccessAttempted e) {
                CpswtUtils.sleep(500);
            }
        }
    }

    /**
     * Unregisters this object with the RTI.  The RTI will destroy all information
     * it contains regarding this object as a result.  This method is usually
     * called by a federate who "owns" this object, i.e. the federate that created
     * it and has write-privileges to its attributes.
     *
     * @param rti handle to the RTI
     */
    public void unregisterObject(RTIambassador rti) {
        while (_isRegistered) {
            try {
                synchronized (rti) {
                    rti.deleteObjectInstance(_objectHandle, null);
                }
                _isRegistered = false;
                _objectMap.remove(getObjectHandle());
            } catch (ObjectNotKnown | DeletePrivilegeNotHeld | FederateNotExecutionMember e) {
                logger.error(e);
                return;
            } catch (Exception e) {
                CpswtUtils.sleepDefault();
            }
        }
    }

    /**
     * Broadcasts the attributes of this object and their values to the RTI, where
     * the values have "time" as their timestamp.  This call should be used for
     * objects whose attributes have "timestamp" ordering.
     *
     * @param rti   handle to the RTI
     * @param time  timestamp on attribute values of this object
     * @param force if "false", only the attributes whose values have changed since
     *              the last call to "updateAttributeValues" will be broadcast to the RTI.  If
     *              "true", all attributes and their values are broadcast to the RTI.
     */
    public void updateAttributeValues(RTIambassador rti, double time, boolean force) {
        SuppliedAttributes suppliedAttributes = createSuppliedDatamembers(force);
        if (suppliedAttributes.size() == 0) return;

        synchronized (rti) {
            try {
                rti.updateAttributeValues(getObjectHandle(), suppliedAttributes, null, new DoubleTime(time));
            } catch (ObjectNotKnown e) {
                logger.error("could not update attributes:  Object Not Known", e);
                return;
            } catch (FederateNotExecutionMember e) {
                logger.error("could not update attributes:  Federate Not Execution Member", e);
                return;
            } catch (AttributeNotDefined e) {
                logger.error("could not update attributes:  Attribute Not Defined", e);
                return;
            } catch (AttributeNotOwned e) {
                logger.error("could not update attributes:  Attribute Not Owned", e);
                return;
            } catch (ConcurrentAccessAttempted e) {
                logger.error("could not update attributes:  Concurrent Access Attempted", e);
                return;
            } catch (InvalidFederationTime e) {
                logger.error("could not update attributes:  Invalid Federation Time", e);
                return;
            } catch (Exception e) {
                logger.error("could not update attributes", e);
            }
        }
    }

    /**
     * Like {@link #updateAttributeValues(RTIambassador rti, double time, boolean force)},
     * except "force" is always false.
     *
     * @param rti  handle to the RTI
     * @param time timestamp on attribute values of this object
     */
    public void updateAttributeValues(RTIambassador rti, double time) {
        updateAttributeValues(rti, time, false);
    }

    /**
     * Broadcasts the attributes of this object and their values to the RTI (with
     * no timestamp).  This call should be used for objects whose attributes have
     * "receive" ordering.
     *
     * @param rti   handle to the RTI
     * @param force if "false", only the attributes whose values have changed since
     *              the last call to "updateAttributeValues" will be broadcast to the RTI.  If
     *              "true", all attributes and their values are broadcast to the RTI.
     */
    public void updateAttributeValues(RTIambassador rti, boolean force) {
        SuppliedAttributes suppliedAttributes = createSuppliedDatamembers(force);
        if (suppliedAttributes.size() == 0) return;

        synchronized (rti) {
            try {
                rti.updateAttributeValues(getObjectHandle(), suppliedAttributes, null);
            } catch (ObjectNotKnown e) {
                logger.error("could not update attributes:  Object Not Known", e);
                return;
            } catch (FederateNotExecutionMember e) {
                logger.error("could not update attributes:  Federate Not Execution Member", e);
                return;
            } catch (AttributeNotDefined e) {
                logger.error("could not update attributes:  Attribute Not Defined", e);
                return;
            } catch (AttributeNotOwned e) {
                logger.error("could not update attributes:  Attribute Not Owned", e);
                return;
            } catch (ConcurrentAccessAttempted e) {
                logger.error("could not update attributes:  Concurrent Access Attempted", e);
                return;
            } catch (Exception e) {
                logger.error("could not update attributes", e);
            }
        }
    }

    /**
     * Like {@link #updateAttributeValues(RTIambassador rti, boolean force)},
     * except "force" is always false.
     *
     * @param rti handle to the RTI
     */
    public void updateAttributeValues(RTIambassador rti) {
        updateAttributeValues(rti, false);
    }

    protected static String _fedName = null;
    protected static Map<String, String> _pubAttributeLogMap = new HashMap<String, String>();
    protected static Map<String, String> _subAttributeLogMap = new HashMap<String, String>();

    /**
     * For use with the melding API -- this method is used to cast
     * ObjectRoot instance reference into the
     * ObjectRootInterface interface.
     *
     * @param rootInstance ObjectRoot instance reference to be
     *                     cast into the ObjectRootInterface interface
     * @return ObjectRootInterface reference to the instance
     */
    public ObjectRootInterface cast(ObjectRoot rootInstance) {
        return rootInstance;
    }

    /**
     * For use with the melding API -- this method creates a new
     * ObjectRoot instance and returns a
     * ObjectRootInterface reference to it.
     *
     * @return ObjectRootInterface reference to a newly created
     * ObjectRoot instance
     */
    public ObjectRootInterface create() {
        return new ObjectRoot();
    }

    public void copyFrom(Object object) {
    }
}
