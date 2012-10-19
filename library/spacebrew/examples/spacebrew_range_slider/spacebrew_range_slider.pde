import spacebrew.*;

/*
  This example is of a slider that sends a value in the range of 0 to 1023.
  
  Click and drag the mouse to move the slider.
*/


String server="localhost";
String name="processingRangeSlider";
String description ="This is an example client which has a red slider you can move from left to right.  It sends values in the range of 0 to 1023.  It also listens for 'currentDistance' events and will move the slider to the new place in the range 0 to 1023.";
import processing.serial.*;


Spacebrew c;

// Keep track of our current place in the range
int currentDistance = 512;

void setup() {
  size(1044, 200);
  background(0);
  
  c = new Spacebrew( this );
  
  // add each thing you publish to
  c.addPublish( "currentDistance", currentDistance ); 

  // add each thing you subscribe to
  c.addSubscribe( "currentDistance", "range" );
  
  // connect!
  c.connect("ws://"+server+":9000", name, description );
  
}

void draw() {
  background(0);
  
  // Display the current value of currentDistance
  fill(255);
  text("Current Distance: " + currentDistance, 30, 50);
  
  // White box containing slider
  fill(255);
  rect(0, height/2, width, height/2);
  
  // Line the slider moves on
  fill(150);
  line(0, height * 3/4, width, height * 3/4);
  
  // Slider
  fill(255, 0, 0);
  rect(currentDistance, (height/2) + 5, 20, (height/2) - 10);
}

void mousePressed() {
  // Leaving 20 pixels at the end prevents the slider from going off the screen
  if (mouseX >= 0 && mouseX <= width - 20) {
    currentDistance = mouseX;
    c.send("currentDistance", currentDistance);
  }   
}

void mouseDragged() {
  // Leaving 20 pixels at the end prevents the slider from going off the screen
  if (mouseX >= 0 && mouseX <= width - 20) {
    currentDistance = mouseX;
    c.send("currentDistance", currentDistance);
  }   
}


void onRangeMessage( String name, int value ){
  println("got int message " + name + " : " + value);
  
  // Only change the position of the slider if our message is of the correct name
  //  and the value is between 0 and 1023.
  if (name.equals("currentDistance") == true) {
    if (value >= 0 && value <= 1023) {
      currentDistance = value;
    }
  }
}

// Our app only responds to integers, so do nothing here
void onBooleanMessage( String name, boolean value ){
  println("got bool message "+name +" : "+ value);  
}

// Our app only responds to integers, so do nothing here
void onStringMessage( String name, String value ){
  println("got string message "+name +" : "+ value);  
}
