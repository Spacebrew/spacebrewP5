/**
 * Spacebrew
 * Spacebrew is a toolkit for creating interactive spaces.
 * http://spacebrew.cc
 *
 * Copyright (C) 2012 LAB at Rockwell Group Brett Renfer http://rockwellgroup.com/lab
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
 * @modified    10/19/20120
 * @version     0.1.1 (1)
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

  private PApplet     parent;
  private Method      onRangeMessageMethod, onStringMessageMethod, onBooleanMessageMethod, onOtherMessageMethod, onOpenMethod, onCloseMethod;
  private WsClient    wsClient;
  private boolean     bConnected = false;

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
      //System.out.println("no onOtherMessage method implemented");
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
    if ( bConnected ) updatePubSub();
  }
  
  /**
   * Setup a Range publisher
   * @param {String}  name of route
   * @param {Integer} default starting value
   */
  public void addPublish( String name, int _default ){
    SpacebrewMessage m = new SpacebrewMessage();
    m.name = name; 
    m.type = "range"; 
    m._default = PApplet.str(_default);
    publishes.add(m);
    if ( bConnected ) updatePubSub();
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
    if ( bConnected ) updatePubSub();
  }
  
  /**
   * Setup a publisher
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
    if ( bConnected ) updatePubSub();
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
    if ( bConnected ) updatePubSub();
  }

  /**
   * Add a subscriber + a specific callback for this route. Note: routes with a 
   * specific callback don't call the default methods (e.g. onRangeMessage, etc)
   * @param {String}  name of route
   * @param {String}  name of method
   * @param {String}  type of route ("range", "boolean", or "string")
   */
  public void addSubscribe( String name, String methodName, String type ){
    SpacebrewMessage m = new SpacebrewMessage();
    m.name = name;
    m.type = type.toLowerCase();
    subscribes.add(m);

    Method method = null;
    if ( type == "boolean" ){
      try {
        method = parent.getClass().getMethod(methodName, new Class[]{boolean.class});
      } catch (Exception e){
        System.err.println("method "+methodName+"(boolean) doesn't exist in your Applet!");
      }
    } else if ( type == "range" ){
      try {
        method = parent.getClass().getMethod(methodName, new Class[]{int.class});
      } catch (Exception e){
        System.err.println("Error: method "+methodName+"(int) doesn't exist in your Applet!");
      }
    } else if ( type == "string" ){
      try {
        method = parent.getClass().getMethod(methodName, new Class[]{String.class});
      } catch (Exception e){
        System.err.println("Error: method "+methodName+"(String) doesn't exist in your Applet!");
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

    if ( bConnected ) updatePubSub();
  }
  
  /**
   * Connect to Spacebrew admin.
   * @param {String} URL to Spacebrew host
   * @param {String} Name of your app as it will appear in the Spacebrew admin
   * @param {String} What does your app do?
   */
  public void connect( String url, String _name, String _description ){
    name = _name;
    description = _description;
    try {
      if ( verbose ) System.out.println("connecting "+url);
      wsClient = new WsClient( this, url );    
      wsClient.connect();
      updatePubSub();
    }
    catch (Exception e){
      bConnected = false;
      System.err.println(e.getMessage());
    }
  }

  /**
   * Update publishers and subscribers.
   */
  private void updatePubSub(){
    JSONArray publishers = new JSONArray();
  
    // LOAD IN PUBLISH INFO
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
    
    if ( bConnected ){
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
    
    if ( bConnected ) wsClient.send( sM.toString() );
    else System.err.println("Can't send message, not currently connected!");
  }
  
  /**
   * Send a Range message along a specified Route
   * @param {String} Name of Route
   * @param {Integer} What you're sending
   */
  public void send( String messageName, int value ){
    
    JSONObject m = new JSONObject();
    m.put("clientName", name);
    m.put("name", messageName);
    m.put("type", "range");
    m.put("value", PApplet.str(value));
    
    JSONObject sM = new JSONObject();
    sM.put("message", m);
    
    if ( bConnected ) wsClient.send( sM.toString() );
    else System.err.println("Can't send message, not currently connected!");
  }
  
  /**
   * Send a Boolean message along a specified Route
   * @param {String} Name of Route
   * @param {boolean} What you're sending
   */
  public void send( String messageName, boolean value ){
    JSONObject m = new JSONObject();
    m.put("clientName", name);
    m.put("name", messageName);
    m.put("type", "boolean");
    m.put("value", PApplet.str(value));
    
    JSONObject sM = new JSONObject();
    sM.put("message", m);
    
    if ( bConnected ) wsClient.send( sM.toString() );
    else System.err.println("Can't send message, not currently connected!");
  }
  
  /**
   * Send a String message along a specified Route
   * @param {String} Name of Route
   * @param {String} What you're sending
   */
  public void send( String messageName, String value ){
    
    JSONObject m = new JSONObject();
    m.put("clientName", name);
    m.put("name", messageName);
    m.put("type", "string");
    m.put("value", value);
    
    JSONObject sM = new JSONObject();
    sM.put("message", m);
    
    if ( bConnected ) wsClient.send( sM.toString() );
    else System.err.println("Can't send message, not currently connected!");
  }

  public boolean connected() {
    return bConnected;  
  }
  
  /**
   * Websocket callback (don't call this please!)
   */
  public void onOpen(){
    bConnected = true;
    if ( verbose ) System.out.println("connection open!");
    // send config
    wsClient.send(nameConfig.toString());
    wsClient.send(tConfig.toString());

    if ( onOpenMethod != null ){
      try {
        onOpenMethod.invoke( parent );
      } catch( Exception e ){
        System.err.println("onOpen invoke failed, disabling :(");
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
        System.err.println("onClose invoke failed, disabling :(");
        onCloseMethod = null;
      }
    }

    bConnected = false;
    System.out.println("connection closed.");
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
          System.err.println("onStringMessageMethod invoke failed, disabling :(");
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
          System.err.println("onBooleanMessageMethod invoke failed, disabling :(");
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
          System.err.println("onRangeMessageMethod invoke failed, disabling :(");
          onRangeMessageMethod = null;
        }
      }
    } else {
      if (method != null){
        try {
          method.invoke( parent, m.getString("value"));
        } catch( Exception e ){
        }
      } else if ( onOtherMessageMethod != null ){
        try {
          onOtherMessageMethod.invoke( parent, name, m.getString("value"));
        } catch( Exception e){
          System.err.println("onOtherMessageMethod invoke failed, disabling :(");
          onOtherMessageMethod = null;
        }
      }
      System.err.println("Received message of unknown type "+type);
    }
  }
 
};

