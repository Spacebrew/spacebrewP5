import spacebrew.*;

String server="localhost";
String name="processingButtonP5";
String description ="This is an example client which has a big red button you can push to send a message. It also listens for color events and will change it's color based on those messages.";

Spacebrew c;
int numClicks = 0;
int currentColor = 255;

boolean buttonSend = true;
String currentText = "Hi, Spacebrew!";

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
  fill(50);
  textAlign(CENTER);
  textSize(14);
  text("Click the button to talk to Spacebrew.", width/2, height/2-140);
  if (mousePressed == true) {
    fill(135);
  } else {
    fill(230);
  }
  rectMode(CENTER);
  rect(width/2,height/2,420,180,28);
  textSize(50);
  fill(50);
  text(currentText, width/2, height/2+15);
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
