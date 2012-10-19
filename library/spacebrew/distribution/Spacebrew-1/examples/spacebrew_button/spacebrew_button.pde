import spacebrew.*;

String server="localhost";
String name="processingButtonJSON";
String description ="This is an example client which has a big red button you can push to send a message. It also listens for color events and will change it's color based on those messages.";
import processing.serial.*;

Spacebrew c;
int numClicks = 0;
int sec0;
int currentColor = 255;

boolean buttonSend = true;
String currentText = "Click Me";

void setup() {
  frameRate(240);
  size(600, 400);
  
  c = new Spacebrew( this );
  
  // add each thing you publish and subscribe to
  c.addPublish( "buttonPress", buttonSend ); 

    
  c.addSubscribe( "color", "range" );
  c.addSubscribe( "text", "string" );
  
  // connect!
  c.connect("ws://"+server+":9000", name, description );
  
}

void draw() {
  background( currentColor );
  fill(20);
  textSize(20);
  text("Click to send a message to spacebrew", 20, 40);
  noFill();
  rect(20,80,280,180,   28,28,28,28);
  textSize(50);
  text(currentText, 50, 180);
}

void mousePressed() {
  c.send( "buttonPress", buttonSend);
}

void onRangeMessage( String name, int value ){
  println("got int message "+name +" : "+ value);
  if (name.equals("color") == true) {
      currentColor = value/4;
  }
}

void onBooleanMessage( String name, boolean value ){
  println("got bool message "+name +" : "+ value);  
}

void onStringMessage( String name, String value ){
  println("got string message "+name +" : "+ value);  
  if (name.equals("text") == true) {
     currentText = value; 
  }
}
