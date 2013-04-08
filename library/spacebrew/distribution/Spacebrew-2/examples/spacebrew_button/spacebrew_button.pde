/*
 * Button Example
 *
 *   Spacebrew library button example that send and receives boolean messages.  
 * 
 */
import spacebrew.*;

String server="sandbox.spacebrew.cc";
String name="P5 Button Example";
String description ="Client that sends and receives boolean messages. Background turns yellow when message received.";

Spacebrew sb;

color color_on = color(255, 255, 50);
color color_off = color(255, 255, 255);
int currentColor = color_off;

void setup() {
	frameRate(240);
	size(500, 400);

	// instantiate the spacebrewConnection variable
	sb = new Spacebrew( this );

	// declare your publishers
	sb.addPublish( "button_pressed", "boolean", true ); 


	// declare your subscribers
	sb.addSubscribe( "change_background", "boolean" );

	// connect to spacebre
	sb.connect(server, name, description );
}

void draw() {
	// set background color
	background( currentColor );

	// draw button
	fill(255,0,0);
	stroke(200,0,0);
	rectMode(CENTER);
	ellipse(width/2,height/2,250,250);

	// add text to button
	fill(230);
	textAlign(CENTER);
	textSize(24);
	if (mousePressed == true) {
		text("That Feels Good", width/2, height/2 + 12);
	} else {
		text("Click Me", width/2, height/2 + 12);
	}
}

void mousePressed() {
	// send message to spacebrew
	sb.send( "button_pressed", true);
}

void mouseReleased() {
	// send message to spacebrew
	sb.send( "button_pressed", false);
}

void onBooleanMessage( String name, boolean value ){
	println("got bool message " + name + " : " + value); 

	// update background color
	if (value == true) {
		currentColor = color_on;
	} else {
		currentColor = color_off;
	}
}
