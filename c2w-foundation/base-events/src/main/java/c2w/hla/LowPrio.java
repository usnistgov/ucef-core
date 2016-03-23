
// This code has been generated by the C2W code generator.
// Do not edit manually!

package c2w.hla;

import java.util.HashSet;
import java.util.Set;

import hla.rti.*;

/**
* The LowPrio class implements the LowPrio interaction in the
* c2w.hla simulation.
*/
public class LowPrio extends SimLog {

	/**
	* Default constructor -- creates an instance of the LowPrio interaction
	* class with default parameter values.
	*/
	public LowPrio() { }

	
	
	private static int _Time_handle;
	private static int _sourceFed_handle;
	private static int _FedName_handle;
	private static int _originFed_handle;
	private static int _Comment_handle;
	private static int _actualLogicalGenerationTime_handle;
	private static int _federateFilter_handle;
	
	
	/**
	* Returns the handle (RTI assigned) of the "Time" parameter of
	* its containing interaction class.
	*
	* @return the handle (RTI assigned) of the "Time" parameter
	*/
	public static int get_Time_handle() { return _Time_handle; }
	
	/**
	* Returns the handle (RTI assigned) of the "sourceFed" parameter of
	* its containing interaction class.
	*
	* @return the handle (RTI assigned) of the "sourceFed" parameter
	*/
	public static int get_sourceFed_handle() { return _sourceFed_handle; }
	
	/**
	* Returns the handle (RTI assigned) of the "FedName" parameter of
	* its containing interaction class.
	*
	* @return the handle (RTI assigned) of the "FedName" parameter
	*/
	public static int get_FedName_handle() { return _FedName_handle; }
	
	/**
	* Returns the handle (RTI assigned) of the "originFed" parameter of
	* its containing interaction class.
	*
	* @return the handle (RTI assigned) of the "originFed" parameter
	*/
	public static int get_originFed_handle() { return _originFed_handle; }
	
	/**
	* Returns the handle (RTI assigned) of the "Comment" parameter of
	* its containing interaction class.
	*
	* @return the handle (RTI assigned) of the "Comment" parameter
	*/
	public static int get_Comment_handle() { return _Comment_handle; }
	
	/**
	* Returns the handle (RTI assigned) of the "actualLogicalGenerationTime" parameter of
	* its containing interaction class.
	*
	* @return the handle (RTI assigned) of the "actualLogicalGenerationTime" parameter
	*/
	public static int get_actualLogicalGenerationTime_handle() { return _actualLogicalGenerationTime_handle; }
	
	/**
	* Returns the handle (RTI assigned) of the "federateFilter" parameter of
	* its containing interaction class.
	*
	* @return the handle (RTI assigned) of the "federateFilter" parameter
	*/
	public static int get_federateFilter_handle() { return _federateFilter_handle; }
	
	
	
	private static boolean _isInitialized = false;

	private static int _handle;

	/**
	* Returns the handle (RTI assigned) of the LowPrio interaction class.
	* Note: As this is a static method, it is NOT polymorphic, and so, if called on
	* a reference will return the handle of the class pertaining to the reference,\
	* rather than the handle of the class for the instance referred to by the reference.
	* For the polymorphic version of this method, use {@link #getClassHandle()}.
	*/
	public static int get_handle() { return _handle; }

	/**
	* Returns the fully-qualified (dot-delimited) name of the LowPrio
	* interaction class.
	* Note: As this is a static method, it is NOT polymorphic, and so, if called on
	* a reference will return the name of the class pertaining to the reference,\
	* rather than the name of the class for the instance referred to by the reference.
	* For the polymorphic version of this method, use {@link #getClassName()}.
	*/
	public static String get_class_name() { return "InteractionRoot.C2WInteractionRoot.SimLog.LowPrio"; }

	/**
	* Returns the simple name (the last name in the dot-delimited fully-qualified
	* class name) of the LowPrio interaction class.
	*/
	public static String get_simple_class_name() { return "LowPrio"; }

	private static Set< String > _datamemberNames = new HashSet< String >();
	private static Set< String > _allDatamemberNames = new HashSet< String >();

	/**
	* Returns a set containing the names of all of the non-hidden parameters in the
	* LowPrio interaction class.
	* Note: As this is a static method, it is NOT polymorphic, and so, if called on
	* a reference will return a set of parameter names pertaining to the reference,\
	* rather than the parameter names of the class for the instance referred to by
	* the reference.  For the polymorphic version of this method, use
	* {@link #getParameterNames()}.
	*/
	public static Set< String > get_parameter_names() {
		return new HashSet< String >( _datamemberNames );
	}


	/**
	* Returns a set containing the names of all of the parameters in the
	* LowPrio interaction class.
	* Note: As this is a static method, it is NOT polymorphic, and so, if called on
	* a reference will return a set of parameter names pertaining to the reference,\
	* rather than the parameter names of the class for the instance referred to by
	* the reference.  For the polymorphic version of this method, use
	* {@link #getParameterNames()}.
	*/
	public static Set< String > get_all_parameter_names() {
		return new HashSet< String >( _allDatamemberNames );
	}


	

	static {
		_classNameSet.add( "InteractionRoot.C2WInteractionRoot.SimLog.LowPrio" );
		_classNameClassMap.put( "InteractionRoot.C2WInteractionRoot.SimLog.LowPrio", LowPrio.class );
		
		_datamemberClassNameSetMap.put( "InteractionRoot.C2WInteractionRoot.SimLog.LowPrio", _datamemberNames );
		_allDatamemberClassNameSetMap.put( "InteractionRoot.C2WInteractionRoot.SimLog.LowPrio", _allDatamemberNames );

		
		
		_datamemberNames.add( "Time" );
		_datamemberNames.add( "sourceFed" );
		_datamemberNames.add( "FedName" );
		_datamemberNames.add( "originFed" );
		_datamemberNames.add( "Comment" );
		_datamemberNames.add( "actualLogicalGenerationTime" );
		_datamemberNames.add( "federateFilter" );
		
		
		_allDatamemberNames.add( "Time" );
		_allDatamemberNames.add( "sourceFed" );
		_allDatamemberNames.add( "FedName" );
		_allDatamemberNames.add( "originFed" );
		_allDatamemberNames.add( "Comment" );
		_allDatamemberNames.add( "actualLogicalGenerationTime" );
		_allDatamemberNames.add( "federateFilter" );
		
		
		_datamemberTypeMap.put( "Time", "double" );
		_datamemberTypeMap.put( "sourceFed", "String" );
		_datamemberTypeMap.put( "FedName", "String" );
		_datamemberTypeMap.put( "originFed", "String" );
		_datamemberTypeMap.put( "Comment", "String" );
		_datamemberTypeMap.put( "actualLogicalGenerationTime", "double" );
		_datamemberTypeMap.put( "federateFilter", "String" );
	
	

	}


	private static String initErrorMessage = "Error:  InteractionRoot.C2WInteractionRoot.SimLog.LowPrio:  could not initialize:  ";
	protected static void init( RTIambassador rti ) {
		if ( _isInitialized ) return;
		_isInitialized = true;
		
		SimLog.init( rti );
		
		boolean isNotInitialized = true;
		while( isNotInitialized ) {
			try {
				_handle = rti.getInteractionClassHandle( "InteractionRoot.C2WInteractionRoot.SimLog.LowPrio" );
				isNotInitialized = false;
			} catch ( FederateNotExecutionMember f ) {
				System.err.println( initErrorMessage + "Federate Not Execution Member" );
				f.printStackTrace();
				return;				
			} catch ( NameNotFound n ) {
				System.err.println( initErrorMessage + "Name Not Found" );
				n.printStackTrace();
				return;				
			} catch ( Exception e ) {
				e.printStackTrace();
				try { Thread.sleep( 50 ); } catch( Exception e1 ) { }					
			}
		}

		_classNameHandleMap.put( "InteractionRoot.C2WInteractionRoot.SimLog.LowPrio", get_handle() );
		_classHandleNameMap.put( get_handle(), "InteractionRoot.C2WInteractionRoot.SimLog.LowPrio" );
		_classHandleSimpleNameMap.put( get_handle(), "LowPrio" );

		
		isNotInitialized = true;
		while( isNotInitialized ) {
			try {
							
				_Time_handle = rti.getParameterHandle( "Time", get_handle() );			
				_sourceFed_handle = rti.getParameterHandle( "sourceFed", get_handle() );			
				_FedName_handle = rti.getParameterHandle( "FedName", get_handle() );			
				_originFed_handle = rti.getParameterHandle( "originFed", get_handle() );			
				_Comment_handle = rti.getParameterHandle( "Comment", get_handle() );			
				_actualLogicalGenerationTime_handle = rti.getParameterHandle( "actualLogicalGenerationTime", get_handle() );			
				_federateFilter_handle = rti.getParameterHandle( "federateFilter", get_handle() );
				isNotInitialized = false;
			} catch ( FederateNotExecutionMember f ) {
				System.err.println( initErrorMessage + "Federate Not Execution Member" );
				f.printStackTrace();
				return;				
			} catch ( InteractionClassNotDefined i ) {
				System.err.println( initErrorMessage + "Interaction Class Not Defined" );
				i.printStackTrace();
				return;				
			} catch ( NameNotFound n ) {
				System.err.println( initErrorMessage + "Name Not Found" );
				n.printStackTrace();
				return;				
			} catch ( Exception e ) {
				e.printStackTrace();
				try { Thread.sleep( 50 ); } catch( Exception e1 ) { }					
			}
		}
			
			
		_datamemberNameHandleMap.put( "InteractionRoot.C2WInteractionRoot.SimLog.LowPrio,Time", get_Time_handle() );
		_datamemberNameHandleMap.put( "InteractionRoot.C2WInteractionRoot.SimLog.LowPrio,sourceFed", get_sourceFed_handle() );
		_datamemberNameHandleMap.put( "InteractionRoot.C2WInteractionRoot.SimLog.LowPrio,FedName", get_FedName_handle() );
		_datamemberNameHandleMap.put( "InteractionRoot.C2WInteractionRoot.SimLog.LowPrio,originFed", get_originFed_handle() );
		_datamemberNameHandleMap.put( "InteractionRoot.C2WInteractionRoot.SimLog.LowPrio,Comment", get_Comment_handle() );
		_datamemberNameHandleMap.put( "InteractionRoot.C2WInteractionRoot.SimLog.LowPrio,actualLogicalGenerationTime", get_actualLogicalGenerationTime_handle() );
		_datamemberNameHandleMap.put( "InteractionRoot.C2WInteractionRoot.SimLog.LowPrio,federateFilter", get_federateFilter_handle() );
			
			
		_datamemberHandleNameMap.put( get_Time_handle(), "Time" );
		_datamemberHandleNameMap.put( get_sourceFed_handle(), "sourceFed" );
		_datamemberHandleNameMap.put( get_FedName_handle(), "FedName" );
		_datamemberHandleNameMap.put( get_originFed_handle(), "originFed" );
		_datamemberHandleNameMap.put( get_Comment_handle(), "Comment" );
		_datamemberHandleNameMap.put( get_actualLogicalGenerationTime_handle(), "actualLogicalGenerationTime" );
		_datamemberHandleNameMap.put( get_federateFilter_handle(), "federateFilter" );
		
	}


	private static boolean _isPublished = false;
	private static String publishErrorMessage = "Error:  InteractionRoot.C2WInteractionRoot.SimLog.LowPrio:  could not publish:  ";

	/**
	* Publishes the LowPrio interaction class for a federate.
	*
	* @param rti handle to the RTI, usu. obtained through the
	* {@link SynchronizedFederate#getRTI()} call
	*/
	public static void publish( RTIambassador rti ) {
		if ( _isPublished ) return;
		
		init( rti );

	

		synchronized( rti ) {
			boolean isNotPublished = true;
			while( isNotPublished ) {
				try {
					rti.publishInteractionClass( get_handle() );
					isNotPublished = false;
				} catch ( FederateNotExecutionMember f ) {
					System.err.println( publishErrorMessage + "Federate Not Execution Member" );
					f.printStackTrace();
					return;
				} catch ( InteractionClassNotDefined i ) {
					System.err.println( publishErrorMessage + "Interaction Class Not Defined" );
					i.printStackTrace();
					return;
				} catch ( Exception e ) {
					e.printStackTrace();
					try { Thread.sleep( 50 ); } catch( Exception e1 ) { }
				}
			}
		}
		
		_isPublished = true;
	}

	private static String unpublishErrorMessage = "Error:  InteractionRoot.C2WInteractionRoot.SimLog.LowPrio:  could not unpublish:  ";
	/**
	* Unpublishes the LowPrio interaction class for a federate.
	*
	* @param rti handle to the RTI, usu. obtained through the
	* {@link SynchronizedFederate#getRTI()} call
	*/
	public static void unpublish( RTIambassador rti ) {
		if ( !_isPublished ) return;
		
		init( rti );
		synchronized( rti ) {
			boolean isNotUnpublished = true;
			while( isNotUnpublished ) {
				try {
					rti.unpublishInteractionClass( get_handle() );
					isNotUnpublished = false;
				} catch ( FederateNotExecutionMember f ) {
					System.err.println( unpublishErrorMessage + "Federate Not Execution Member" );
					f.printStackTrace();
					return;
				} catch ( InteractionClassNotDefined i ) {
					System.err.println( unpublishErrorMessage + "Interaction Class Not Defined" );
					i.printStackTrace();
					return;
				} catch ( InteractionClassNotPublished i ) {
					System.err.println( unpublishErrorMessage + "Interaction Class Not Published" );
					i.printStackTrace();
					return;
				} catch ( Exception e ) {
					e.printStackTrace();
					try { Thread.sleep( 50 ); } catch( Exception e1 ) { }
				}
			}
		}
		
		_isPublished = false;
	}

	private static boolean _isSubscribed = false;
	private static String subscribeErrorMessage = "Error:  InteractionRoot.C2WInteractionRoot.SimLog.LowPrio:  could not subscribe:  ";
	/**
	* Subscribes a federate to the LowPrio interaction class.
	*
	* @param rti handle to the RTI, usu. obtained through the
	* {@link SynchronizedFederate#getRTI()} call
	*/
	public static void subscribe( RTIambassador rti ) {
		if ( _isSubscribed ) return;
		
		init( rti );
	
		
		synchronized( rti ) {
			boolean isNotSubscribed = true;
			while( isNotSubscribed ) {
				try {
					rti.subscribeInteractionClass( get_handle() );
					isNotSubscribed = false;
				} catch ( FederateNotExecutionMember f ) {
					System.err.println( subscribeErrorMessage + "Federate Not Execution Member" );
					f.printStackTrace();
					return;
				} catch ( InteractionClassNotDefined i ) {
					System.err.println( subscribeErrorMessage + "Interaction Class Not Defined" );
					i.printStackTrace();
					return;
				} catch ( Exception e ) {
					e.printStackTrace();
					try { Thread.sleep( 50 ); } catch( Exception e1 ) { }
				}
			}
		}
		
		_isSubscribed = true;
	}

	private static String unsubscribeErrorMessage = "Error:  InteractionRoot.C2WInteractionRoot.SimLog.LowPrio:  could not unsubscribe:  ";
	/**
	* Unsubscribes a federate from the LowPrio interaction class.
	*
	* @param rti handle to the RTI, usu. obtained through the
	* {@link SynchronizedFederate#getRTI()} call
	*/
	public static void unsubscribe( RTIambassador rti ) {
		if ( !_isSubscribed ) return;

		init( rti );
		synchronized( rti ) {
			boolean isNotUnsubscribed = true;
			while( isNotUnsubscribed ) {
				try {
					rti.unsubscribeInteractionClass( get_handle() );
					isNotUnsubscribed = false;
				} catch ( FederateNotExecutionMember f ) {
					System.err.println( unsubscribeErrorMessage + "Federate Not Execution Member" );
					f.printStackTrace();
					return;
				} catch ( InteractionClassNotDefined i ) {
					System.err.println( unsubscribeErrorMessage + "Interaction Class Not Defined" );
					i.printStackTrace();
					return;
				} catch ( InteractionClassNotSubscribed i ) {
					System.err.println( unsubscribeErrorMessage + "Interaction Class Not Subscribed" );
					i.printStackTrace();
					return;
				} catch ( Exception e ) {
					e.printStackTrace();
					try { Thread.sleep( 50 ); } catch( Exception e1 ) { }
				}
			}
		}
		
		_isSubscribed = false;
	}

	/**
	* Return true if "handle" is equal to the handle (RTI assigned) of this class
	* (that is, the LowPrio interaction class).
	*
	* @param handle handle to compare to the value of the handle (RTI assigned) of
	* this class (the LowPrio interaction class).
	* @return "true" if "handle" matches the value of the handle of this class
	* (that is, the LowPrio interaction class).
	*/
	public static boolean match( int handle ) { return handle == get_handle(); }

	/**
	* Returns the handle (RTI assigned) of this instance's interaction class .
	* 
	* @return the handle (RTI assigned) if this instance's interaction class
	*/
	public int getClassHandle() { return get_handle(); }

	/**
	* Returns the fully-qualified (dot-delimited) name of this instance's interaction class.
	* 
	* @return the fully-qualified (dot-delimited) name of this instance's interaction class
	*/
	public String getClassName() { return get_class_name(); }

	/**
	* Returns the simple name (last name in its fully-qualified dot-delimited name)
	* of this instance's interaction class.
	* 
	* @return the simple name of this instance's interaction class 
	*/
	public String getSimpleClassName() { return get_simple_class_name(); }

	/**
	* Returns a set containing the names of all of the non-hiddenparameters of an
	* interaction class instance.
	*
	* @return set containing the names of all of the parameters of an
	* interaction class instance
	*/
	public Set< String > getParameterNames() { return get_parameter_names(); }

	/**
	* Returns a set containing the names of all of the parameters of an
	* interaction class instance.
	*
	* @return set containing the names of all of the parameters of an
	* interaction class instance
	*/
	public Set< String > getAllParameterNames() { return get_all_parameter_names(); }

	/**
	* Publishes the interaction class of this instance of the class for a federate.
	*
	* @param rti handle to the RTI, usu. obtained through the
	* {@link SynchronizedFederate#getRTI()} call
	*/
	public void publishInteraction( RTIambassador rti ) { publish( rti ); }

	/**
	* Unpublishes the interaction class of this instance of this class for a federate.
	*
	* @param rti handle to the RTI, usu. obtained through the
	* {@link SynchronizedFederate#getRTI()} call
	*/
	public void unpublishInteraction( RTIambassador rti ) { unpublish( rti ); }

	/**
	* Subscribes a federate to the interaction class of this instance of this class.
	*
	* @param rti handle to the RTI, usu. obtained through the
	* {@link SynchronizedFederate#getRTI()} call
	*/
	public void subscribeInteraction( RTIambassador rti ) { subscribe( rti ); }

	/**
	* Unsubscribes a federate from the interaction class of this instance of this class.
	*
	* @param rti handle to the RTI, usu. obtained through the
	* {@link SynchronizedFederate#getRTI()} call
	*/
	public void unsubscribeInteraction( RTIambassador rti ) { unsubscribe( rti ); }

	

	public String toString() {
		return "LowPrio("
			
			
			+ "Time:" + get_Time()
			+ "," + "sourceFed:" + get_sourceFed()
			+ "," + "FedName:" + get_FedName()
			+ "," + "originFed:" + get_originFed()
			+ "," + "Comment:" + get_Comment()
			+ "," + "actualLogicalGenerationTime:" + get_actualLogicalGenerationTime()
			+ "," + "federateFilter:" + get_federateFilter()
			+ ")";
	}
	



	
	
	private double _Time = 0;
	
	private String _sourceFed = "";
	
	private String _FedName = "";
	
	private String _originFed = "";
	
	private String _Comment = "";
	
	private double _actualLogicalGenerationTime = 0;
	
	private String _federateFilter = "";

	
	
	/**
	* Set the value of the "Time" parameter to "value" for this parameter.
	*
	* @param value the new value for the "Time" parameter
	*/
	public void set_Time( double value ) { _Time = value; }
	
	/**
	* Returns the value of the "Time" parameter of this interaction.
	*
	* @return the value of the "Time" parameter
	*/
	public double get_Time() { return _Time; }
	
	
	/**
	* Set the value of the "sourceFed" parameter to "value" for this parameter.
	*
	* @param value the new value for the "sourceFed" parameter
	*/
	public void set_sourceFed( String value ) { _sourceFed = value; }
	
	/**
	* Returns the value of the "sourceFed" parameter of this interaction.
	*
	* @return the value of the "sourceFed" parameter
	*/
	public String get_sourceFed() { return _sourceFed; }
	
	
	/**
	* Set the value of the "FedName" parameter to "value" for this parameter.
	*
	* @param value the new value for the "FedName" parameter
	*/
	public void set_FedName( String value ) { _FedName = value; }
	
	/**
	* Returns the value of the "FedName" parameter of this interaction.
	*
	* @return the value of the "FedName" parameter
	*/
	public String get_FedName() { return _FedName; }
	
	
	/**
	* Set the value of the "originFed" parameter to "value" for this parameter.
	*
	* @param value the new value for the "originFed" parameter
	*/
	public void set_originFed( String value ) { _originFed = value; }
	
	/**
	* Returns the value of the "originFed" parameter of this interaction.
	*
	* @return the value of the "originFed" parameter
	*/
	public String get_originFed() { return _originFed; }
	
	
	/**
	* Set the value of the "Comment" parameter to "value" for this parameter.
	*
	* @param value the new value for the "Comment" parameter
	*/
	public void set_Comment( String value ) { _Comment = value; }
	
	/**
	* Returns the value of the "Comment" parameter of this interaction.
	*
	* @return the value of the "Comment" parameter
	*/
	public String get_Comment() { return _Comment; }
	
	
	/**
	* Set the value of the "actualLogicalGenerationTime" parameter to "value" for this parameter.
	*
	* @param value the new value for the "actualLogicalGenerationTime" parameter
	*/
	public void set_actualLogicalGenerationTime( double value ) { _actualLogicalGenerationTime = value; }
	
	/**
	* Returns the value of the "actualLogicalGenerationTime" parameter of this interaction.
	*
	* @return the value of the "actualLogicalGenerationTime" parameter
	*/
	public double get_actualLogicalGenerationTime() { return _actualLogicalGenerationTime; }
	
	
	/**
	* Set the value of the "federateFilter" parameter to "value" for this parameter.
	*
	* @param value the new value for the "federateFilter" parameter
	*/
	public void set_federateFilter( String value ) { _federateFilter = value; }
	
	/**
	* Returns the value of the "federateFilter" parameter of this interaction.
	*
	* @return the value of the "federateFilter" parameter
	*/
	public String get_federateFilter() { return _federateFilter; }
	


	protected LowPrio( ReceivedInteraction datamemberMap, boolean initFlag ) {
		super( datamemberMap, false );
		if ( initFlag ) setParameters( datamemberMap );
	}
	
	protected LowPrio( ReceivedInteraction datamemberMap, LogicalTime logicalTime, boolean initFlag ) {
		super( datamemberMap, logicalTime, false );
		if ( initFlag ) setParameters( datamemberMap );
	}


	/**
	* Creates an instance of the LowPrio interaction class, using
	* "datamemberMap" to initialize its parameter values.
	* "datamemberMap" is usually acquired as an argument to an RTI federate
	* callback method, such as "receiveInteraction".
	*
	* @param datamemberMap data structure containing initial values for the
	* parameters of this new LowPrio interaction class instance
	*/
	public LowPrio( ReceivedInteraction datamemberMap ) {
		this( datamemberMap, true );
	}
	
	/**
	* Like {@link #LowPrio( ReceivedInteraction datamemberMap )}, except this
	* new LowPrio interaction class instance is given a timestamp of
	* "logicalTime".
	*
	* @param datamemberMap data structure containing initial values for the
	* parameters of this new LowPrio interaction class instance
	* @param logicalTime timestamp for this new LowPrio interaction class
	* instance
	*/
	public LowPrio( ReceivedInteraction datamemberMap, LogicalTime logicalTime ) {
		this( datamemberMap, logicalTime, true );
	}

	/**
	* Creates a new LowPrio interaction class instance that is a duplicate
	* of the instance referred to by LowPrio_var.
	*
	* @param LowPrio_var LowPrio interaction class instance of which
	* this newly created LowPrio interaction class instance will be a
	* duplicate
	*/
	public LowPrio( LowPrio LowPrio_var ) {
		super( LowPrio_var );
		
		
		set_Time( LowPrio_var.get_Time() );
		set_sourceFed( LowPrio_var.get_sourceFed() );
		set_FedName( LowPrio_var.get_FedName() );
		set_originFed( LowPrio_var.get_originFed() );
		set_Comment( LowPrio_var.get_Comment() );
		set_actualLogicalGenerationTime( LowPrio_var.get_actualLogicalGenerationTime() );
		set_federateFilter( LowPrio_var.get_federateFilter() );
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
		
		
		
		if (  "Time".equals( datamemberName )  ) return new Double( get_Time() );
		else if (  "sourceFed".equals( datamemberName )  ) return get_sourceFed();
		else if (  "FedName".equals( datamemberName )  ) return get_FedName();
		else if (  "originFed".equals( datamemberName )  ) return get_originFed();
		else if (  "Comment".equals( datamemberName )  ) return get_Comment();
		else if (  "actualLogicalGenerationTime".equals( datamemberName )  ) return new Double( get_actualLogicalGenerationTime() );
		else if (  "federateFilter".equals( datamemberName )  ) return get_federateFilter();
		else return super.getParameter( datamemberName );
	}
	
	/**
	* Returns the value of the parameter whose handle (RTI assigned)
	* is "datamemberHandle" for this interaction.
	*
	* @param datamemberHandle handle (RTI assigned) of parameter whose
	* value is to be returned
	* @return value of the parameter whose handle (RTI assigned) is
	* "datamemberHandle" for this interaction
	*/
	public Object getParameter( int datamemberHandle ) {
		
				
		
		if ( get_Time_handle() == datamemberHandle ) return new Double( get_Time() );
		else if ( get_sourceFed_handle() == datamemberHandle ) return get_sourceFed();
		else if ( get_FedName_handle() == datamemberHandle ) return get_FedName();
		else if ( get_originFed_handle() == datamemberHandle ) return get_originFed();
		else if ( get_Comment_handle() == datamemberHandle ) return get_Comment();
		else if ( get_actualLogicalGenerationTime_handle() == datamemberHandle ) return new Double( get_actualLogicalGenerationTime() );
		else if ( get_federateFilter_handle() == datamemberHandle ) return get_federateFilter();
		else return super.getParameter( datamemberHandle );
	}
	
	protected boolean setParameterAux( int param_handle, String val ) {
		boolean retval = true;		
		
			
		
		if ( param_handle == get_Time_handle() ) set_Time( Double.parseDouble( val ) );
		else if ( param_handle == get_sourceFed_handle() ) set_sourceFed( val );
		else if ( param_handle == get_FedName_handle() ) set_FedName( val );
		else if ( param_handle == get_originFed_handle() ) set_originFed( val );
		else if ( param_handle == get_Comment_handle() ) set_Comment( val );
		else if ( param_handle == get_actualLogicalGenerationTime_handle() ) set_actualLogicalGenerationTime( Double.parseDouble( val ) );
		else if ( param_handle == get_federateFilter_handle() ) set_federateFilter( val );
		else retval = super.setParameterAux( param_handle, val );
		
		return retval;
	}
	
	protected boolean setParameterAux( String datamemberName, String val ) {
		boolean retval = true;
		
			
		
		if (  "Time".equals( datamemberName )  ) set_Time( Double.parseDouble( val ) );
		else if (  "sourceFed".equals( datamemberName )  ) set_sourceFed( val );
		else if (  "FedName".equals( datamemberName )  ) set_FedName( val );
		else if (  "originFed".equals( datamemberName )  ) set_originFed( val );
		else if (  "Comment".equals( datamemberName )  ) set_Comment( val );
		else if (  "actualLogicalGenerationTime".equals( datamemberName )  ) set_actualLogicalGenerationTime( Double.parseDouble( val ) );
		else if (  "federateFilter".equals( datamemberName )  ) set_federateFilter( val );	
		else retval = super.setParameterAux( datamemberName, val );
		
		return retval;
	}
	
	protected boolean setParameterAux( String datamemberName, Object val ) {
		boolean retval = true;
		
		
		
		if (  "Time".equals( datamemberName )  ) set_Time( (Double)val );
		else if (  "sourceFed".equals( datamemberName )  ) set_sourceFed( (String)val );
		else if (  "FedName".equals( datamemberName )  ) set_FedName( (String)val );
		else if (  "originFed".equals( datamemberName )  ) set_originFed( (String)val );
		else if (  "Comment".equals( datamemberName )  ) set_Comment( (String)val );
		else if (  "actualLogicalGenerationTime".equals( datamemberName )  ) set_actualLogicalGenerationTime( (Double)val );
		else if (  "federateFilter".equals( datamemberName )  ) set_federateFilter( (String)val );		
		else retval = super.setParameterAux( datamemberName, val );
		
		return retval;
	}

	protected SuppliedParameters createSuppliedDatamembers() {
		SuppliedParameters datamembers = super.createSuppliedDatamembers();

	
		
		
			datamembers.add( get_Time_handle(), Double.toString( get_Time() ).getBytes() );
		
			datamembers.add( get_sourceFed_handle(), get_sourceFed().getBytes() );
		
			datamembers.add( get_FedName_handle(), get_FedName().getBytes() );
		
			datamembers.add( get_originFed_handle(), get_originFed().getBytes() );
		
			datamembers.add( get_Comment_handle(), get_Comment().getBytes() );
		
			datamembers.add( get_actualLogicalGenerationTime_handle(), Double.toString( get_actualLogicalGenerationTime() ).getBytes() );
		
			datamembers.add( get_federateFilter_handle(), get_federateFilter().getBytes() );
		
	
		return datamembers;
	}

	
	public void copyFrom( Object object ) {
		super.copyFrom( object );
		if ( object instanceof LowPrio ) {
			LowPrio data = (LowPrio)object;
			
			
				_Time = data._Time;
				_sourceFed = data._sourceFed;
				_FedName = data._FedName;
				_originFed = data._originFed;
				_Comment = data._Comment;
				_actualLogicalGenerationTime = data._actualLogicalGenerationTime;
				_federateFilter = data._federateFilter;
			
		}
	}
}
