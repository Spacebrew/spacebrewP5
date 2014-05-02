/**
 * ##library.name##
 * ##library.sentence##
 * ##library.url##
 *
 * Copyright 2014 ##author##
 * 
 * @author      Brett Renfer
 * @version     ##library.prettyVersion## (##library.version##)
 */

var spacebrew = spacebrew || {};

var SpacebrewMessage = function(){
	this.name 		= "";
	this.type 		= "";
	this._default 	= "";
	this.value 		= "";
}

/**
 * [Spacebrew description]
 * @param {Processing.Sketch} PApplet pass in reference to your sketch
 */
var Spacebrew = function ( PApplet) {

	/**
	 * Name of your app as it will appear in the Spacebrew admin.
	 * @type {String}
	 */
	this.name = "";

	/**
	 * What does your app do?
	 * @type {String}
	 */
	this.description = "";

	/**
	 * How loud to be (mutes debug messages)
	 * @type {Boolean}
	 */
	this.verbose = false;

	/**
	 * Location of spacebrew server
	 * @type {String}
	 */
	var hostname = "sandbox.spacebrew.cc";

	/** Spacebrew built-in port */
	var port = 9000;

	/**
	 * @type {PApplet}
	 */
	var parent = PApplet;

	// attached to methods in sb app
	var onRangeMessageMethod 	= null;
	var onStringMessageMethod 	= null;
	var onBooleanMessageMethod 	= null; 
	var onCustomMessageMethod 	= null;
	var onOpenMethod			= null;
	var onCloseMethod			= null;
	
	/**
	 * @type {WebSocket}
	 */
	var wsClient 		= null;

	// state
	var connectionEstablished = false;
	var connectionRequested = false;

	// auto-reconnect
	var reconnectAttempt = 0;
	var reconnectInterval = 5000;

	// vars for config, publishers + subscriber
	var tConfig 	= {};
	var nameConfig 	= {};

	var publishes 	= [];
	var subscribes 	= [];

	// callback methods
	//private HashMap<String, HashMap<String, Method>> 
	var callbacks = {};
  
	//------------------------------------------------
	// called on instantiate!
	function setup(){
		// this is a little dangerous! we'll need to make sure something isn't here already in the future.
		parent.onFrameStart = pre.bind(this);
		setupMethods();
	}

	// attach to functions on your sketch
	
	function setupMethods(){
		if ( typeof(parent.onRangeMessage) === "function"){
			onRangeMessageMethod = parent.onRangeMessage.bind(parent);
		}

		if ( typeof(parent.onStringMessage) === "function"){
			onStringMessageMethod = parent.onStringMessage.bind(parent);
		}

		if ( typeof(parent.onBooleanMessage) === "function"){
			onBooleanMessageMethod = parent.onBooleanMessage.bind(parent);
		}

		if ( typeof(parent.onCustomMessage) === "function"){
			onCustomMessageMethod = parent.onCustomMessage.bind(parent);
		}

		if ( typeof(parent.onSbOpen) === "function"){
			onOpenMethod = parent.onSbOpen.bind(parent);
		}

		if ( typeof(parent.onSbClose) === "function"){
			onCloseMethod = parent.onSbClose.bind(parent);
		}
	}
  
	/**
	 * Setup a Boolean publisher
	 * @param {String}  name of route
	 * @param {Boolean} default starting value
	 */
	this.addPublish = function( name, _default ){
		var m = new SpacebrewMessage();
		m.name = name; 
		m.type = "boolean"; 
		if ( _default){
			m._default = "true";
		} else {
			m._default = "false";
		}
		publishes.push(m);
		if ( connectionEstablished ) updatePubSub();
	}
  
	/**
	 * Setup a Range publisher
	 * @param {String}  name of route
	 * @param {Integer} default starting value
	 */
	this.addPublish = function( name, _default ){
		var m = new SpacebrewMessage();
		m.name = name; 
		m.type = "range"; 
		m._default = _default.toString();
		publishes.push(m);
		if ( connectionEstablished ) updatePubSub();
	}
  
	/**
	 * Setup a String publisher
	 * @param {String}  name of route
	 * @param {String}  default starting value
	 */
	this.addPublish = function( name, _default ){
		var m = new SpacebrewMessage();
		m.name = name; 
		m.type = "string"; 
		m._default = _default;
		publishes.push(m);
		if ( connectionEstablished ) updatePubSub();
	}
  
	/**
	 * Setup a custom or string publisher
	 * @param {String}  name of route
	 * @param {String}  type of route ("range", "boolean", "string", or a custom name)
	 * @param {String}  default starting value
	 */
	this.addPublish = function( name, type, _default ){
		var m = new SpacebrewMessage();
		m.name = name; 
		m.type = type; 
		m._default = _default;
		publishes.push(m);
		if ( connectionEstablished ) updatePubSub();
	}

	/**
	 * Setup a custom or boolean publisher
	 * @param {String}  name of route
	 * @param {String}  type of route ("range", "boolean", or "string")
	 * @param {Boolean} default starting value
	 */
	this.addPublish = function( name, type, _default ){
		var m = new SpacebrewMessage();
		m.name = name; 
		m.type = type; 
		m._default = _default.toString();
		publishes.push(m);
		if ( connectionEstablished ) updatePubSub();
	}

	/**
	 * Setup a custom or integer-based publisher
	 * @param {String}  name of route
	 * @param {String}  type of route ("range", "boolean", or "string")
	 * @param {Boolean} default starting value
	 */
	this.addPublish = function( name, type, _default ){
		var m = new SpacebrewMessage();
		m.name = name; 
		m.type = type; 
		m._default = _default.toString();
		publishes.push(m);
		if ( connectionEstablished ) updatePubSub();
	}

  
	/**
	 * Add a subscriber. Note: right now this just adds to the message sent onopen;
	 * in the future, could be something like name, type, default, callback
	 * @param {String}  name of route
	 * @param {String}  type of route ("range", "boolean", or "string")
	 */
	this.addSubscribe = function( name, type ){
		var m = new SpacebrewMessage();
		m.name = name;
		m.type = type;
		subscribes.push(m);
		if ( connectionEstablished ) updatePubSub();
	}

	/**
	 * Add a subscriber + a specific callback for this route. Note: routes with a 
	 * specific callback don't call the default methods (e.g. onRangeMessage, etc)
	 * @param {String}  name of route
	 * @param {String}  name of method
	 * @param {String}  type of route ("range", "boolean", or "string")
	 */
	this.addSubscribe = function( name, type, methodName ){
		var m = new SpacebrewMessage();
		m.name = name;
		m.type = type.toLowerCase();
		subscribes.push(m);

		var method = null;
		if ( typeof(parent[methodName]) === "function"){
			method = parent[methodName];
		}

		if (method != null){
			if ( !callbacks.hasOwnProperty(name) ){
				callbacks[name] = {};
			}
			callbacks[name][type] = method;      
		}

		if ( connectionEstablished ) updatePubSub();
	}
  
	/**
	 * Connect to Spacebrew admin.
	 * @param {String} URL to Spacebrew host
	 * @param {String} Name of your app as it will appear in the Spacebrew admin
	 * @param {String} What does your app do?
	 */
	this.connect = function( hostname, _name, _description ){
		var port = 9000;
		var regex = new RegExp("ws://((?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])(?:\\.(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9]))*):([0-9]{1,5})");
	    var m = regex.exec( hostname );
	    if (m != null) {
			if (m[0].length == 3) {
				hostname = m[0][1];
				port = parseInt(m[0][2]);
				this.connect(hostname, port, _name, _description);
				console.error("Using a full websockets URL will be deprecated in future versions of the Spacebrew lib.");
				console.error("Pass just the host name or call the connect(host, port, name, description) instead");
			} else {
				console.error("Spacebrew server URL is not valid.");				
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
	this.connect = function( _hostname, _port, _name, _description ){
		this.name = _name;
		this.description = _description;
		hostname = _hostname;
		port = _port;
		connectionRequested = true;

		if ( verbose ) console.log("[Spacebrew::connect] connecting to spacebrew "+ hostname);
		wsClient = new WebSocket( "ws://" + hostname + ":" + port );  
		wsClient.onopen = onOpen.bind(this);
		wsClient.onclose = onClose.bind(this);
		wsClient.onmessage = onMessage.bind(this);
		updatePubSub();
	}

	/**
	 * Method that ensure that app attempts to reconnect to Spacebrew if the connection is lost.
	 */
	function pre() {
		// attempt to reconnect
		if (connectionRequested && !connectionEstablished) {
			if (parent.millis() - reconnectAttempt > reconnectInterval) {
				if ( verbose ) console.log("[Spacebrew::pre] attempting to reconnect to Spacebrew");
				this.connect( hostname, port, this.name, this.description);
				reconnectAttempt = parent.millis();
			}
		}
	}

	/**
	 * Close the connection to spacebrew
	 */
	this.close = function() {
		if (connectionEstablished) {
			wsClient.close();
		}
		connectionRequested = false;
	}

	/**
	 * Update publishers and subscribers.
	 */
	function updatePubSub(){

		// LOAD IN PUBLISH INFO
		var publishers = [];

		for (var i=0, len=publishes.length; i<len; i++){
		    var m = publishes[i];
		    var pub = {
		    	"name": m.name,
		    	"type": m.type,
		    	"default": m._default
		    }	    

		    publishers.push(pub);      
		}
		  
		// LOAD IN SUBSCRIBE INFO
		var subscribers = [];
		  
		for (var i=0; i<subscribes.length; i++){
		    var m = subscribes[i];
		    var sub = {
		    	"name": m.name,
		    	"type": m.type
		    }
		    
		    subscribers.push(subs);      
		}

		tConfig.config = {
			name: this.name,
			description: this.description,
			subscribe: {
				messages: subscribers
			}, 
			publish: {
				messages: publishers
			}
		};

		if ( connectionEstablished ){
			wsClient.send( JSON.stringify( tConfig ) );
		}
	}
  
	/**
	 * Send a message along a specified Route
	 * @param {String} Name of Route
	 * @param {String} Type of Route ("boolean", "range", "string")
	 * @param {String} What you're sending
	 */
	this.send = function( messageName, type, value ){
		var m = {
			message: {
				clientName: this.name,
				name: messageName,
				type: type,
				value: value
			}
		};

		if ( connectionEstablished ) wsClient.send( JSON.stringify( m ) );
		else console.warn("[Spacbrew::send] can't send message, not currently connected!");
	}

	/**
	 * @return {Boolean} Get whether websocket is connected
	 */
	this.connected = function() {
		return connectionEstablished;  
	}
  
	/**
	 * Websocket callback
	 */
	function onOpen(){
		connectionEstablished = true;
		if ( verbose ) console.log("[Spacebrew::onOpen] spacebrew connection open!");

		// send config
		wsClient.send(JSON.stringify( tConfig ));

		if ( onOpenMethod != null ){
			onOpenMethod();
		}
	}
  
	/**
	 * Websocket callback
	 */
	function onClose(){
		if ( onCloseMethod != null ){
			onCloseMethod();
		}

		connectionEstablished = false;
		console.log("[Spacebrew::onClose] spacebrew connection closed.");
	}
  
	/**
	 * Websocket callback
	 */
	function onMessage( message ){
		var m = JSON.parse(message).message;

		var name = m.name;
		var type = m.type;

		var method = null;

		if ( callbacks.hasOwnProperty(name) ){
			if ( callbacks.name.hasOwnProperty(type)){
				method = callbacks[name][type];
			}
		}
    
		if ( type == "string" ){
			if ( method != null ){
				method( m.value );
			} else if ( onStringMessageMethod != null ){
				onStringMessageMethod( name, m.value );
			}
		} else if ( type == ("boolean")){
			if ( method != null ){
				method( m.value === "true" );
			} else if ( onBooleanMessageMethod != null ){
				onBooleanMessageMethod( name, m.value === "true");
			}
		} else if ( type == ("range")){
			if ( method != null ){
				method( m.value === "true" );
			} else if ( onRangeMessageMethod != null ){
				onRangeMessageMethod( name, parseInt(m.value) );
			}
		} else {
			var value = "";			

			// lets figure out our type... and cast it to a string!
			/*
			// we don't really need to do this in JS
			var obj = m.value;
			switch( typeof(obj) ){
				case "number":
					value = value.toString();
					break;
				case "boolean":
					break;
				case "string":
					break;
				case "object":
					if ( obj.isArray() ){

					} else {

					}
					break;
			}*/

			if (method != null){
				method( value);
			} else {
				if ( onCustomMessageMethod != null ){
					onCustomMessageMethod( name, type, value);
				}
			}
		}
	}

	setup();
};

