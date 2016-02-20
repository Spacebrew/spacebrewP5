import spacebrew.*;

String server="sandbox.spacebrew.cc";
String name="PowerTail_viaProccessing";
String description ="This is an example client that has a powertail and a light atteched and it turns on the light via the power tail.";
import processing.serial.*;

Serial myPort;  // Create object from Serial class
boolean powerSwitchState = false;

Spacebrew c;
int sec0;

boolean toSend = false;

void setup() {
  frameRate(240);
  size(600, 400);
  
  c = new Spacebrew( this );
 
  c.addSubscribe( "power", "boolean" );
  
  // connect!
  c.connect("ws://"+server+":9000", name, description );
  
  // connect to serial
  myPort = new Serial(this, Serial.list()[0], 9600);
  myPort.bufferUntil('\n');
}

void draw() {
  background( 255 );
  fill(20);
  textSize(30);
  text("Listening for power messages", 20, 320);
}
  

void onRangeMessage( String name, int value ){
  println("got int message "+name +" : "+ value);
     powerSwitchState = !powerSwitchState;
          if (powerSwitchState == true){
             myPort.write('H');
          }
          else{
            myPort.write('L');
          } 
}

void onBooleanMessage( String name, boolean value ){
  println("got bool message "+name +" : "+ value); 
 
   powerSwitchState = !powerSwitchState;
          if (powerSwitchState == true){
             myPort.write('H');
          }
          else{
            myPort.write('L');
          } 
}

void onStringMessage( String name, String value ){
  println("got string message "+name +" : "+ value);  
     powerSwitchState = !powerSwitchState;
          if (powerSwitchState == true){
             myPort.write('H');
          }
          else{
            myPort.write('L');
          } 
}

/* Arduino Code/

////////////////////////////////////////////////////////////////////
const int PowerTail = 2; // the pin that the LED is attached to
int incomingByte;      // a variable to read incoming serial data into

void setup() {
  // initialize serial communication:
  Serial.begin(9600);
  // initialize the LED pin as an output:
  pinMode(ledPin, OUTPUT);
}

void loop() {
  // see if there's incoming serial data:
  if (Serial.available() > 0) {
    // read the oldest byte in the serial buffer:
    incomingByte = Serial.read();
    // if it's a capital H (ASCII 72), turn on the LED:
    if (incomingByte == 'H') {
      digitalWrite(PowerTail, HIGH);
    } 
    // if it's an L (ASCII 76) turn off the LED:
    if (incomingByte == 'L') {
      digitalWrite(PowerTail, LOW);
    }
  }
}
*/



