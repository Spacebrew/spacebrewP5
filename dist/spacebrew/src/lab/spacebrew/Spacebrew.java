/**
 * spacebrew
 * Spacebrew is a toolkit for creating interactive spaces.
 * http://spacebrew.cc
 *
 * Copyright (C) 2013 Spacebrew Brett Renfer, Julio Terra http://rockwellgroup.com/lab
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 * 
 * @author      Brett Renfer
 * @modified    04/03/2013 (by Julio Terra)
 * @version     0.4.0 (3)
 */

package spacebrew;

import processing.core.*;

/**
 * 
 * @example spacebrew_base 
 *
 */

import org.json.*; //https://github.com/agoransson/JSON-processing
import java.lang.reflect.Method;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;

public class Spacebrew {

	/**
	 * Name of your app as it will appear in the Spacebrew admin.
	 * @type {String}
	 */
	public  String name;

	/**
	 * What does your app do?
	 * @type {String}
	 */
	public  String description;

	/**
	 * How loud to be (mutes debug messages)
	 * @type {Boolean}
	 */
	public  boolean verbose = false;

	private String 		hostname = "sandbox.spacebrew.cc";
	private Integer		port = 9000;

	private PApplet     parent;
	private Method      onRangeMessageMethod, onStringMessageMethod, onBooleanMessageMethod, onOtherMessageMethod, onCustomMessageMethod, onOpenMethod, onCloseMethod;
	private WsClient    wsClient;
	private boolean     connectionEstablished = false;
	private boolean     connectionRequested = false;
	private Integer     reconnectAttempt = 0;
	private Integer     reconnectInterval = 5000;

	private JSONObject  tConfig = new JSONObject();
	private JSONObject  nameConfig = new JSONObject();
	private ArrayList<SpacebrewMessage>   publishes, subscribes;
	private HashMap<String, HashMap<String, Method>> callbacks;

	/**
	 * Setup Spacebrew + try to set up default helper functions
	 * @param {PApplet}
	 */
	public Spacebrew( PApplet _parent ){
		publishes = new ArrayList<SpacebrewMessage>();
		subscribes = new ArrayList<SpacebrewMessage>();
		callbacks = new HashMap<String, HashMap<String, Method>>();
		parent = _parent; 
	    parent.registerMethod("pre", this);		
		setupMethods();   
	}
  
	//------------------------------------------------
	private void setupMethods(){
		try {
			onRangeMessageMethod = parent.getClass().getMethod("onRangeMessage", new Class[]{String.class, int.class});
		} catch (Exception e){
			//let's not print these messages, 
			//they confuse ppl and make them think they are doing something wrong
			//System.out.println("no onRangeMessage method implemented");
		}

		try {
			onStringMessageMethod = parent.getClass().getMethod("onStringMessage", new Class[]{String.class, String.class});
		} catch (Exception e){
			//System.out.println("no onStringMessage method implemented");
		}

		try {
			onBooleanMessageMethod = parent.getClass().getMethod("onBooleanMessage", new Class[]{String.class, boolean.class});
		} catch (Exception e){
			//System.out.println("no onBooleanMessage method implemented");
		}

		try {
			onOtherMessageMethod = parent.getClass().getMethod("onOtherMessage", new Class[]{String.class, String.class});
		} catch (Exception e){
			//System.out.println("no onCustomMessage method implemented");
		}

		try {
			onCustomMessageMethod = parent.getClass().getMethod("onCustomMessage", new Class[]{String.class, String.class, String.class});
		} catch (Exception e){
			//System.out.println("no onCustomMessage method implemented");
		}

		try {
			onOpenMethod = parent.getClass().getMethod("onSbOpen", new Class[]{});
		} catch (Exception e){
			// System.out.println("no onSbOpen method implemented");
		}    

		try {
			onCloseMethod = parent.getClass().getMethod("onSbClose", new Class[]{});
		} catch (Exception e){
			// System.out.println("no onSbClose method implemented");
		}    
	}

  
	/**
	 * Setup a Boolean publisher
	 * @param {String}  name of route
	 * @param {Boolean} default starting value
	 */
	public void addPublish( String name, boolean _default ){
		SpacebrewMessage m = new SpacebrewMessage();
		m.name = name; 
		m.type = "boolean"; 
		if ( _default){
			m._default = "true";
		} else {
			m._default = "false";
		}
		publishes.add(m);
		if ( connectionEstablished ) updatePubSub();
	}
  
	/**
	 * Setup a Range publisher
	 * @param {String}  name of route
	 * @param {Integer} default starting value
	 */
	public void addPublish( String name, Integer _default ){
		SpacebrewMessage m = new SpacebrewMessage();
		m.name = name; 
		m.type = "range"; 
		m._default = PApplet.str(_default);
		publishes.add(m);
		if ( connectionEstablished ) updatePubSub();
	}
  
	/**
	 * Setup a String publisher
	 * @param {String}  name of route
	 * @param {String}  default starting value
	 */
	public void addPublish( String name, String _default ){
		SpacebrewMessage m = new SpacebrewMessage();
		m.name = name; 
		m.type = "string"; 
		m._default = _default;
		publishes.add(m);
		if ( connectionEstablished ) updatePubSub();
	}
  
	/**
	 * Setup a custom or string publisher
	 * @param {String}  name of route
	 * @param {String}  type of route ("range", "boolean", or "string")
	 * @param {String}  default starting value
	 */
	public void addPublish( String name, String type, String _default ){
		SpacebrewMessage m = new SpacebrewMessage();
		m.name = name; 
		m.type = type; 
		m._default = _default;
		publishes.add(m);
		if ( connectionEstablished ) updatePubSub();
	}

	/**
	 * Setup a custom or boolean publisher
	 * @param {String}  name of route
	 * @param {String}  type of route ("range", "boolean", or "string")
	 * @param {Boolean} default starting value
	 */
	public void addPublish( String name, String type, boolean _default ){
		SpacebrewMessage m = new SpacebrewMessage();
		m.name = name; 
		m.type = type; 
		m._default = PApplet.str(_default);
		publishes.add(m);
		if ( connectionEstablished ) updatePubSub();
	}

	/**
	 * Setup a custom or integer-based publisher
	 * @param {String}  name of route
	 * @param {String}  type of route ("range", "boolean", or "string")
	 * @param {Boolean} default starting value
	 */
	public void addPublish( String name, String type, Integer _default ){
		SpacebrewMessage m = new SpacebrewMessage();
		m.name = name; 
		m.type = type; 
		m._default = PApplet.str(_default);
		publishes.add(m);
		if ( connectionEstablished ) updatePubSub();
	}

  
	/**
	 * Add a subscriber. Note: right now this just adds to the message sent onopen;
	 * in the future, could be something like name, type, default, callback
	 * @param {String}  name of route
	 * @param {String}  type of route ("range", "boolean", or "string")
	 */
	public void addSubscribe( String name, String type ){
		SpacebrewMessage m = new SpacebrewMessage();
		m.name = name;
		m.type = type;
		subscribes.add(m);
		if ( connectionEstablished ) updatePubSub();
	}

	/**
	 * Add a subscriber + a specific callback for this route. Note: routes with a 
	 * specific callback don't call the default methods (e.g. onRangeMessage, etc)
	 * @param {String}  name of route
	 * @param {String}  name of method
	 * @param {String}  type of route ("range", "boolean", or "string")
	 */
	public void addSubscribe( String name, String type, String methodName ){
		SpacebrewMessage m = new SpacebrewMessage();
		m.name = name;
		m.type = type.toLowerCase();
		subscribes.add(m);

		Method method = null;
		if ( type.equals( "boolean" ) ){

			try {
				method = parent.getClass().getMethod(methodName, new Class[]{boolean.class});
			} catch (Exception e){
				System.err.println("method "+methodName+"(boolean) doesn't exist in your Applet!");
			}
		} else if ( type.equals( "range" ) ){
			try {
				method = parent.getClass().getMethod(methodName, new Class[]{int.class});
			} catch (Exception e){
				System.err.println("Error: method " + methodName + "(int) doesn't exist in your Applet!");
			}
		} else if ( type.equals( "string" ) ){
			try {
				method = parent.getClass().getMethod(methodName, new Class[]{String.class});
			} catch (Exception e){
				System.err.println("Error: method " + methodName + "(String) doesn't exist in your Applet!");
			}
		} else {
			try{
				method = parent.getClass().getMethod(methodName, new Class[]{String.class});
			} catch (Exception e){
				System.err.println("Error: method " + methodName + "(String) doesn't exist in your Applet!");
			}
		}

		if (method != null){
			if ( !callbacks.containsKey(name) ){
				callbacks.put( name, new HashMap<String, Method>());
			}
			callbacks.get(name).put(type, method);      
		}

		if ( connectionEstablished ) updatePubSub();
	}
  
	/**
	 * Connect to Spacebrew admin.
	 * @param {String} URL to Spacebrew host
	 * @param {String} Name of your app as it will appear in the Spacebrew admin
	 * @param {String} What does your app do?
	 */
	public void connect( String hostname, String _name, String _description ){
		Integer port = 9000;
	    String[][] m = PApplet.matchAll(hostname, "ws://((?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])(?:\\.(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9]))*):([0-9]{1,5})");
	    if (m != null) {
			if (m[0].length == 3) {
				hostname = m[0][1];
				port = Integer.parseInt(m[0][2]);
				this.connect(hostname, port, _name, _description);
				System.err.println("Using a full websockets URL will be deprecated in future versions of the Spacebrew lib.");
				System.err.println("Pass just the host name or call the connect(host, port, name, description) instead");
			} else {
				System.err.println("Spacebrew server URL is not valid.");				
			}    
	    } else {
			this.connect(hostname, port, _name, _description);    	
	    }
	}
  
	/**
	 * Connect to Spacebrew admin.
	 * @param {String} URL to Spacebrew host
	 * @param {String} Name of your app as it will appear in the Spacebrew admin
	 * @param {String} What does your app do?
	 */
	public void connect( String _hostname, Integer _port, String _name, String _description ){
		this.name = _name;
		this.description = _description;
		this.hostname = _hostname;
		this.port = _port;
		this.connectionRequested = true;
		try {
			if ( verbose ) System.out.println("[connect] connecting to spacebrew "+ hostname);
			wsClient = new WsClient( this, ("ws://" + hostname + ":" + Integer.toString(_port)) );    
			wsClient.connect();
			updatePubSub();
		}
		catch (Exception e){
			connectionEstablished = false;
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Method that ensure that app attempts to reconnect to Spacebrew if the connection is lost.
	 */
	public void pre() {
		// attempt to reconnect
		if (connectionRequested && !connectionEstablished) {
			if (parent.millis() - reconnectAttempt > reconnectInterval) {
				if ( verbose ) System.out.println("[pre] attempting to reconnect to Spacebrew");
				this.connect(this.hostname, this.port, this.name, this.description);
				reconnectAttempt = parent.millis();
			}
		}
	}

	/**
	 * Close the connection to spacebrew
	 */
	public void close() {
		if (connectionEstablished) {
			wsClient.close();
		}
		connectionRequested = false;
	}

	/**
	 * Update publishers and subscribers.
	 */
	private void updatePubSub(){

		// LOAD IN PUBLISH INFO
		JSONArray publishers = new JSONArray();

		for (int i=0, len=publishes.size(); i<len; i++){
		    SpacebrewMessage m = publishes.get(i);
		    JSONObject pub = new JSONObject();
		    pub.put("name",m.name);
		    pub.put("type",m.type);
	        pub.put("default",m._default);		    

		    publishers.put(pub);      
		}
		  
		// LOAD IN SUBSCRIBE INFO
		JSONArray subscribers = new JSONArray();
		  
		for (int i=0; i<subscribes.size(); i++){
		    SpacebrewMessage m = subscribes.get(i);
		    JSONObject subs = new JSONObject();
		    subs.put("name",m.name);
		    subs.put("type",m.type);
		    
		    subscribers.put(subs);      
		}
		  
		JSONObject mObj = new JSONObject();
		JSONObject tMs1 = new JSONObject();
		JSONObject tMs2 = new JSONObject();
		tMs1.put("messages",subscribers);
		tMs2.put("messages",publishers);
		mObj.put("name", name);
		mObj.put("description", description);
		mObj.put("subscribe", tMs1);
		mObj.put("publish", tMs2);    
		tConfig.put("config", mObj);    

		if ( connectionEstablished ){
		  wsClient.send( tConfig.toString() );
		}
	}
  
	/**
	 * Send a message along a specified Route
	 * @param {String} Name of Route
	 * @param {String} Type of Route ("boolean", "range", "string")
	 * @param {String} What you're sending
	 */
	public void send( String messageName, String type, String value ){

		JSONObject m = new JSONObject();
		m.put("clientName", name);
		m.put("name", messageName);
		m.put("type", type);
		m.put("value", value);

		JSONObject sM = new JSONObject();
		sM.put("message", m);

		if ( connectionEstablished ) wsClient.send( sM.toString() );
		else System.err.println("[send] can't send message, not currently connected!");
	}

	/**
	 * Send a Range message along a specified Route
	 * @param {String} Name of Route
	 * @param {Integer} What you're sending
	 */
	public void send( String messageName, int value ){    
		String type = "range";
		for ( int i = 0, len = publishes.size(); i<len; i++ ){
			SpacebrewMessage m = publishes.get(i);
			if ( m.name.equals(messageName) ) { 
				type = m.type;
				break;
			}
		}
		this.send(messageName, type, PApplet.str(value));
	}
  
	/**
	 * Send a Boolean message along a specified Route
	 * @param {String} Name of Route
	 * @param {boolean} What you're sending
	 */
	public void send( String messageName, boolean value ){
		String type = "boolean";
		for ( int i = 0, len = publishes.size(); i<len; i++ ){
			SpacebrewMessage m = publishes.get(i);
			if ( m.name.equals(messageName) ) { 
				type = m.type;
				break;
			}
		}
		this.send(messageName, type, PApplet.str(value));
	}
  
	/**
	 * Send a String message along a specified Route
	 * @param {String} Name of Route
	 * @param {String} What you're sending
	 */
	public void send( String messageName, String value ){
		String type = "string";
		for ( int i = 0, len = publishes.size(); i<len; i++ ){
			SpacebrewMessage m = publishes.get(i);
			if ( m.name.equals(messageName) ) { 
				type = m.type;
				break;
			}
		}
		this.send( messageName, type, value );
	}

	public boolean connected() {
		return connectionEstablished;  
	}
  
	/**
	 * Websocket callback (don't call this please!)
	 */
	public void onOpen(){
		connectionEstablished = true;
		if ( verbose ) System.out.println("[onOpen] spacebrew connection open!");

		// send config
		wsClient.send(tConfig.toString());

		if ( onOpenMethod != null ){
			try {
				onOpenMethod.invoke( parent );
			} catch( Exception e ){
				System.err.println("[onOpen] invoke failed, disabling :(");
				onOpenMethod = null;
			}
		}
	}
  
	/**
	 * Websocket callback (don't call this please!)
	 */
	public void onClose(){
		if ( onCloseMethod != null ){
			try {
				onCloseMethod.invoke( parent );
			} catch( Exception e ){
				System.err.println("[onClose] invoke failed, disabling :(");
				onCloseMethod = null;
			}
		}

		connectionEstablished = false;
		System.out.println("[onClose] spacebrew connection closed.");
	}
  
	/**
	 * Websocket callback (don't call this please!)
	 */
	public void onMessage( String message ){
		JSONObject m = new JSONObject( message ).getJSONObject("message");

		String name = m.getString("name");
		String type = m.getString("type");
		Method method = null;

		if ( callbacks.containsKey(name) ){
			if ( callbacks.get(name).containsKey(type)){
				try {
					method = callbacks.get(name).get(type);
				} catch( Exception e ){
				}
			}
		}
    
		if ( type.equals("string") ){
			if ( method != null ){
				try {
					method.invoke( parent, m.getString("value"));
				} catch( Exception e ){
				}
			} else if ( onStringMessageMethod != null ){
				try {
					onStringMessageMethod.invoke( parent, name, m.getString("value"));
				} catch( Exception e ){
					System.err.println("[onStringMessageMethod] invoke failed, disabling :(");
					onStringMessageMethod = null;
				}
			}
		} else if ( type.equals("boolean")){
			if ( method != null ){
				try {
					method.invoke( parent, m.getBoolean("value"));
				} catch( Exception e ){
				}
			} else if ( onBooleanMessageMethod != null ){
				try {
					onBooleanMessageMethod.invoke( parent, name, m.getBoolean("value"));
				} catch( Exception e ){
					System.err.println("[onBooleanMessageMethod] invoke failed, disabling :(");
					onBooleanMessageMethod = null;
				}
			}
		} else if ( type.equals("range")){
			if ( method != null ){
				try {
					method.invoke( parent, m.getInt("value"));
				} catch( Exception e ){
				}
			} else if ( onRangeMessageMethod != null ){
				try {
					onRangeMessageMethod.invoke( parent, name, m.getInt("value"));
				} catch( Exception e ){
					System.err.println("[onRangeMessageMethod] invoke failed, disabling :(");
					onRangeMessageMethod = null;
				}
			}
		} else {
			String value = "";			

			// lets figure out our type... and cast it to a string!
			Object obj = m.get("value");
			if(obj instanceof Number){
				value = parent.str(m.getInt("value"));
			} else if ( obj instanceof Boolean ){
				value = parent.str(m.getBoolean("value"));
			} else if ( obj instanceof JSONArray ){
				value = m.getJSONArray("value").toString();
			} else if ( obj instanceof JSONObject ){
				value = m.getJSONObject("value").toString();
			} else if ( obj instanceof String ){
				value = m.get("value").toString();
			}

			if (method != null){
				try {
					method.invoke( parent, value);
				} catch( Exception e ){
				}
			} else {
				if ( onCustomMessageMethod != null ){
					try {
						onCustomMessageMethod.invoke( parent, name, type, value);
					} catch( Exception e){
						System.err.println("[onCustomMessageMethod] invoke failed, disabling :(");
						onCustomMessageMethod = null;
					}
				}
				if ( onOtherMessageMethod != null ){
					try {
						onOtherMessageMethod.invoke( parent, name, type, value);
						System.err.println("[onOtherMessageMethod] will be deprecated in future version of Spacebrew lib");
					} catch( Exception e){
						System.err.println("[onOtherMessageMethod] invoke failed, disabling :(");
						onOtherMessageMethod = null;
					}
				}
			}
		}
	}
};

