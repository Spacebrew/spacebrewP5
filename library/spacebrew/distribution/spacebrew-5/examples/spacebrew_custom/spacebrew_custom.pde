/*
 * Custom Data Type Example - Virtual Dice
 *
 *   Click the mouse to roll the virtual dice. Receive virtual dice rolls
 *   from other apps that publish dice data.
 * 
 */

import spacebrew.*;

String server="sandbox.spacebrew.cc";
String name="P5 Custom Example - Dice";
String description ="Client that sends and receives a virtual dice roll - a number between 1 and 6.";


Spacebrew sb;

// Keep track of our current place in the range
int local_dice = 1;
int remote_dice = 1;

void setup() {
	size(400, 200);
	background(0);

	// instantiate the spacebrewConnection variable
	sb = new Spacebrew( this );

	// declare your publishers
	sb.addPublish( "roll_the_dice", "dice", local_dice ); 

	// declare your subscribers
	sb.addSubscribe( "what_did_you_roll?", "dice" );

	// connect!
	sb.connect(server, name, description );
  
}

void draw() {
	background(50);
	stroke(0);

	text("Click the mouse to roll the virtual dice: ", 30, 20);  

	// Display the current value of local and remote sliders
	fill(255);
	text("Local Roll: ", 30, 40);  
	text(local_dice, 180, 40);  

	fill(255);
	text("Remote Roll: ", 30, 60);  
	text(remote_dice, 180, 60);  


}

void mouseClicked() {
	local_dice = (floor(random(6)) + 1);
	sb.send("roll_the_dice", "dice", str(local_dice));
}


void onCustomMessage( String name, String type, String value ){
	println("got " + type + " message " + name + " : " + value);
	if (int(value) >= 1 || int(value) <= 6) {
		remote_dice = int(value);
	}
}
