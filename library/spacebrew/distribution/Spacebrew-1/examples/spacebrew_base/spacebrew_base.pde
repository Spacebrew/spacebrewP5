import spacebrew.*;

String server="sandbox.spacebrew.cc";
String name="givemeabettername";
String description ="This is an example client which .... It also listens to...";

Spacebrew spacebrewConnection;

void setup() {
  size(600, 400);
  
  spacebrewConnection = new Spacebrew( this );
  
  // add each thing you publish to
  // spacebrewConnection.addPublish( "buttonPress", buttonSend ); 

  // add each thing you subscribe to
  // spacebrewConnection.addSubscribe( "color", "range" );
  
  // connect!
  spacebrewConnection.connect("ws://"+server+":9000", name, description );
  
}

void draw() {

}

//void mousePressed() {
//  spacebrewConnection.send( "buttonPress", buttonSend);
//}

void onRangeMessage( String name, int value ){
  println("got int message "+name +" : "+ value);
  //  // check name by using equals
  //  if (name.equals("color") == true) {
  //      currentColor = value;
  //  }
}

void onBooleanMessage( String name, boolean value ){
  println("got bool message "+name +" : "+ value);  
}

void onStringMessage( String name, String value ){
  println("got string message "+name +" : "+ value);  
}
