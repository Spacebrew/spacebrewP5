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
	 * Setup a  publisher
	 * @param {String}  name of route
	 * @param {String}  type of route ("range", "boolean", "string", or a custom name)
	 * @param {String}  default starting value
	 */
	this.addPublish = function( name, type, _default ){
		var m = new SpacebrewMessage();
		m.name = name; 
		m.type = type; 
		m._default = _default;
		if ( typeof(m._default) != "string"){
			m._default = m._default.toString();
		}
		publishes.push(m);
		if ( connectionEstablished ) updatePubSub.bind(this)();
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

		if ( methodName !== undefined){
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
		}

		if ( connectionEstablished ) updatePubSub.bind(this)();
	}
  
	/**
	 * Connect to Spacebrew admin.
	 * @param {String} URL to Spacebrew host
	 * @param {String} Name of your app as it will appear in the Spacebrew admin
	 * @param {String} What does your app do?
	 */
	this.connect = function( _hostname, _name, _description ){
		this.name = _name;
		this.description = _description;
		hostname = _hostname;
		// port = _port;
		connectionRequested = true;

		if ( this.verbose ) console.log("[Spacebrew::connect] connecting to spacebrew "+ hostname);
		wsClient = new WebSocket( "ws://" + hostname + ":" + port );  
		wsClient.onopen = onOpen.bind(this);
		wsClient.onclose = onClose.bind(this);
		wsClient.onmessage = onMessage.bind(this);
	}

	/**
	 * Method that ensure that app attempts to reconnect to Spacebrew if the connection is lost.
	 */
	function pre() {
		// attempt to reconnect
		if (connectionRequested && !connectionEstablished) {
			if (parent.millis() - reconnectAttempt > reconnectInterval) {
				if ( this.verbose ) console.log("[Spacebrew::pre] attempting to reconnect to Spacebrew");
				this.connect( hostname, this.name, this.description);
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
		    
		    subscribers.push(sub);      
		}

		tConfig = {};
		tConfig.config = {
			"name": this.name,
			"description": this.description,
			"subscribe": {
				"messages": subscribers
			}, 
			"publish": {
				"messages": publishers
			},
			"options":{}
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
		// P5 library has 2 param version of function that auto-casts
		if ( value === undefined ){
			value = type;
			switch( typeof(type) ){
				case "number":
					type = "range";
					break;
				case "boolean":
					type = "boolean";
					break;
				case "string":
					type = "string";
					break;
				default:
					console.warn("[Spacebrew::send] No type specified!")
			}
		}
		var m = {
			message: {
				clientName: this.name,
				name: messageName,
				type: type,
				value: value
			}
		};

		console.log(m);

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
		if ( this.verbose ) console.log("[Spacebrew::onOpen] spacebrew connection open!");

		// send config
		updatePubSub.bind(this)();

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
		console.log(message.data);
		var m = JSON.parse(message.data).message;

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
				method( (m.value === "true" || m.value === true ) );
			} else if ( onBooleanMessageMethod != null ){
				onBooleanMessageMethod( name, (m.value === "true" || m.value === true ) );
			}
		} else if ( type == ("range")){
			if ( method != null ){
				method( m.value );
			} else if ( onRangeMessageMethod != null ){
				onRangeMessageMethod( name, parseInt(m.value) );
			}
		} else {			

			// lets figure out our type... and cast it to a string!
			/*
			var value = "";
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
					onCustomMessageMethod( name, type, m.value);
				}
			}
		}
	}

	setup();
};

