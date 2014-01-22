/*
 * Base Example 
 *
 *   Sketch that features the basic building blocks of a Spacebrew client app.
 * 
 */

import spacebrew.*;

String server="sandbox.spacebrew.cc";
String name="P5 Base Example";
String description ="This is an blank example client that publishes .... and also listens to ...";

Spacebrew sb;

void setup() {
	size(600, 400);

	// instantiate the sb variable
	sb = new Spacebrew( this );

	// add each thing you publish to
	// sb.addPublish( "buttonPress", "boolean", buttonSend ); 

	// add each thing you subscribe to
	// sb.addSubscribe( "color", "range" );

	// connect to spacebrew
	sb.connect(server, name, description );

}

void draw() {
	// do whatever you want to do here	
}


void onRangeMessage( String name, int value ){
	println("got range message " + name + " : " + value);
}

void onBooleanMessage( String name, boolean value ){
	println("got boolean message " + name + " : " + value);  
}

void onStringMessage( String name, String value ){
	println("got string message " + name + " : " + value);  
}

void onCustomMessage( String name, String type, String value ){
	println("got " + type + " message " + name + " : " + value);  
}
