import spacebrew.*;

String server="sandbox.spacebrew.cc";
String name="servo";
String description ="This is an example client which sends a range or boolean to a servo.";
import processing.serial.*;

Spacebrew c;
Serial servoPort;

void setup() {
  size(1, 1);
  
  c = new Spacebrew( this );

  // add each thing you subscribe to
  c.addSubscribe( "servoSpin", "range" );
  c.addSubscribe( "spinAllTheWay", "boolean" );
  
  // connect!
  c.connect("ws://"+server+":9000", name, description );
  
  // setup serial port
  String portName = Serial.list()[0];
  servoPort = new Serial(this, portName, 9600);
  delay(2000);
  servoPort.write(512+"\n");
}

void draw() {

}

//void mousePressed() {
//  c.send( "buttonPress", buttonSend);
//}

void onRangeMessage( String name, int value ){
  println("got int message "+name +" : "+ nf(value,0));
  servoPort.write(nf(value,0)+"\n");
  
  delay(1000);
  servoPort.write(512+"\n");
}

void onBooleanMessage( String name, boolean value ){
  println("got bool message "+name +" : "+ value);  
  if ( value == true ){
    servoPort.write(1024+"\n");
  } else {
    servoPort.write(0+"\n");
  }
}

void onStringMessage( String name, String value ){
  println("got string message "+name +" : "+ value);  
}
