/*
 * String Example
 *
 * 	 Send messages up to 50 chars long. Receive string messages
 *   from other spacebrew clients as well.
 * 
 */

import spacebrew.*;

String server="sandbox.spacebrew.cc";
String name="P5 String Example";
String description ="Client that sends and receives string messages.";


Spacebrew sb;

// Keep track of our current place in the range
String local_string = "";
String remote_string = "";
String last_string = "";

void setup() {
	size(550, 150);
	background(0);

	// instantiate the spacebrewConnection variable
	sb = new Spacebrew( this );

	// declare your publishers
	sb.addPublish( "listen_to_me", "string", local_string ); 

	// declare your subscribers
	sb.addSubscribe( "say_something", "string" );

	// connect!
	sb.connect(server, name, description );
  
}

void draw() {
	background(200, 0, 0);
	fill(255);
	stroke(250);

	// draw lines
	line(0, 35, width - 60, 35);
	line(30, 95, width, 95);

	// draw instruction text
	text("Type messages up to 50 chars long and hit return to send so Spacebrew. ", 30, 20);  

	// draw message being typed
	text("Type Message: ", 30, 60);  
	text(local_string, 150, 60);  

	// draw message that was just sent
	text("Message Sent: ", 30, 80);  
	text(last_string, 150, 80);  

	// draw latest received message
	text("Message Received: ", 30, 120);  
	text(remote_string, 150, 120);  
}

void keyPressed() {
	if (key != CODED) {
		if (key == DELETE || key == BACKSPACE) {
			if (local_string.length() - 1 >= 0) {
				local_string = local_string.substring(0, (local_string.length() - 1));	
			}
		}

		else if (key == ENTER || key == RETURN) {
			sb.send("listen_to_me", local_string);
			last_string = local_string;
			local_string = "";	
		} 

		else {
			if (local_string.length() <= 50) {
				local_string += key;
			}
		}
	} 
}

void onStringMessage( String name, String value ){
	println("got string message " + name + " : " + value);
	remote_string = value;
}
