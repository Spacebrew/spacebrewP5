import spacebrew.*;

String server="sandbox.spacebrew.cc";
String name="givemeabettername";
String description ="This is an example client which .... It also listens to...";

Spacebrew spacebrewConnection;

void setup() {
  size(600, 400);
  
  spacebrewConnection = new Spacebrew( this );
  
  // add each thing you publish to
  spacebrewConnection.addPublish( "mouseClick", false ); 

  // add each thing you subscribe to
  // pass in a name to a function you want to use as a callback
  spacebrewConnection.addSubscribe( "wasClicked", "clickRouteCallback", "boolean" );
  
  // add a subscriber w/o a callback
  spacebrewConnection.addSubscribe( "anotherBoolean", "boolean" );
  
  // connect!
  spacebrewConnection.connect("ws://"+server+":9000", name, description );
  
}

void draw() {
}

void clickRouteCallback( boolean value ){
  println("got click route! "+value);
}

void mousePressed() {
  spacebrewConnection.send( "mouseClick", true);
}

void onRangeMessage( String name, int value ){
  println("got int message "+name +" : "+ value);
}

void onBooleanMessage( String name, boolean value ){
  println("got bool message "+name +" : "+ value);  
}

void onStringMessage( String name, String value ){
  println("got string message "+name +" : "+ value);  
}