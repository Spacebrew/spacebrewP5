/*
 * Spacebrew Videoplayer 
 * Takes in ranges to scrub a video.
 * It's that simple!
 */

import spacebrew.*;
import processing.video.*;

String server="sandbox.spacebrew.cc";
String name="P5 Video player";
String description ="";

// The all-important Spacebrew object!
Spacebrew sb;

// movie we're going to load/play/mess with
Movie myMovie;

// vars we're going to manipulate
float scaleX = 1.;
float scaleY = 1.;

// finally, set up
void setup() {
  size(600, 400);

  // instantiate the sb variable
  sb = new Spacebrew( this );
  
  // add each thing you subscribe to
  sb.addSubscribe( "position", "range" );
  sb.addSubscribe( "volume", "range" );
  sb.addSubscribe( "scaleX", "range" );
  sb.addSubscribe( "scaleY", "range" );

  // connect to spacebrew
  sb.connect(server, name, description );
  
  myMovie = new Movie(this, "fingers.mov");
  myMovie.loop();
  // don't play, since we'll scrub!
}

void draw() {
  if (myMovie.available()) {
    myMovie.read();
    myMovie.pause();
  }
  scale(scaleX, scaleY);
  image(myMovie, 0,0);
}


void onRangeMessage( String name, int value ){
  // we want the value from 0-1, so let's map it!
  float mappedVal = map(value, 0, 1023, 0, 1);
  
  if ( name.equals("position") ){
    // jump is in seconds, so jump to a time
    // in seconds
    float mapSeconds = map(value, 0, 1023, 0, myMovie.duration());
    myMovie.jump( mapSeconds );
    myMovie.play();
  } else if ( name.equals("volume") ){
    myMovie.volume( mappedVal );
  } else if ( name.equals("scaleX") ){
    // scale from 0 - 5
    scaleX = mappedVal * 5.;
  } else if ( name.equals("scaleY") ){
    // scale from 0 - 5
    scaleY = mappedVal * 5.;
  }
}