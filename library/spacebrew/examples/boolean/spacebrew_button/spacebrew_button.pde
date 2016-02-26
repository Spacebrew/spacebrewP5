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

// button color
color buttoncolor = #ffae00;

// vars for changing background
color color_on = color(255, 255, 50);
color color_off = color(255, 255, 255);

// var controlled by spacebrew
boolean backgroundOn = false;

void setup() {
  frameRate(240);
  size(500, 400);

  // instantiate the spacebrewConnection variable
  sb = new Spacebrew( this );

  // declare your publishers
  sb.addPublish( "button_pressed", "boolean" ); 

  // declare your subscribers
  sb.addSubscribe( "change_background", "boolean" );

  // connect to spacebre
  sb.connect(server, name, description );
}

void draw() {
  // set background color
  if ( backgroundOn ) {
    background( color_on );
  } else {
    background( color_off );
  }

  // draw button
  noStroke();
  fill(buttoncolor);
  rectMode(CENTER);
  rect(width/2, height/2, 250, 250);

  // add text to button
  fill(255);
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

void onBooleanMessage( String name, boolean value ) {
  println("got bool message " + name + " : " + value); 

  // set var to value
  if ( name.equals("change_background")){
    backgroundOn = value;
  }
}