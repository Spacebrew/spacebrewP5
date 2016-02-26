/*
 * JSON Example 
 *
 *   This example demonstrates sending complex data--in this case, JSON--as
 *   a custom object in Spacebrew. Keep in mind, you'll need to have another
 *   app that listens to the custom "point2d" type, as it's not built into
 *   Spacebrew by default.
 * 
 */
 
import spacebrew.*;

String server= "sandbox.spacebrew.cc";
String name="P5 Custom Example - Objects";
String description ="";

Spacebrew sb;

void setup(){
  size(800,600);
  sb = new Spacebrew( this );
  
  // add a publisher and subscriber with a custom type, point2d
  // note: at the moment, the type name MUST be in all lowercase!
  sb.addPublish ("p5Point", "point2d");
  sb.addSubscribe ("p5Point", "point2d");
  sb.connect(server, name, description);
}

// vars to send/receive as JSON
PVector localPoint = new PVector(0,0);
PVector remotePoint = new PVector(0,0);
JSONObject outgoing = new JSONObject();

void draw(){
  localPoint.set(mouseX, mouseY);
  background(50);
  fill(0);
  ellipse(localPoint.x, localPoint.y, 20,20);
  fill(255);
  ellipse(remotePoint.x, remotePoint.y, 20,20);
  
  outgoing.setInt("x", mouseX);
  outgoing.setInt("y", mouseY);
  
  // since point2d is just JSON, can use P5's built-in objects
  // with the 'toString' method that converts into a string
  // note: all custom type's values MUST be converted to string!
  sb.send("p5Point", "point2d", outgoing.toString());
}

void onCustomMessage( String name, String type, String value ){
  if ( type.equals("point2d") ){
    // parse JSON!
    JSONObject m = JSONObject.parse( value );
    remotePoint.set( m.getInt("x"), m.getInt("y"));
  }
}
