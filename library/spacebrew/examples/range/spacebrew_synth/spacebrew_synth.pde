/*
 * Spacebrew Synth
 *
 *  Takes in ranges to three oscillators
 */

import spacebrew.*;
import processing.sound.*;

String server="sandbox.spacebrew.cc";
String name="P5 Synth exmaple";
String description ="";

Spacebrew sb;

SawOsc synth1;
SinOsc synth2;
SqrOsc synth3;

// vars controlled by spacebrew
float s1Freq = 392.00; // g
float s2Freq = 493.88; // b
float s3Freq = 587.33; // d

void setup() {
  size(600, 400);

  // instantiate the sb variable
  sb = new Spacebrew( this );

  // add each thing you publish to
  // sb.addPublish( "buttonPress", "boolean", false ); 

  // add each thing you subscribe to
  sb.addSubscribe( "synth1Freq", "range" );
  sb.addSubscribe( "synth1Amp", "range" );
  sb.addSubscribe( "synth2Freq", "range" );
  sb.addSubscribe( "synth2Amp", "range" );
  sb.addSubscribe( "synth3Freq", "range" );
  sb.addSubscribe( "synth3Amp", "range" );

  // connect to spacebrew
  sb.connect(server, name, description );

  // setup oscillators
  synth1 = new SawOsc(this);
  synth2 = new SinOsc(this);
  synth3 = new SqrOsc(this);
  
  synth1.freq(s1Freq);
  
  synth2.freq(s2Freq);
  
  synth3.freq(s3Freq);
  
  synth1.play();
  synth2.play();
  synth3.play();
}

void draw() {
  float r = s1Freq/1023 * 255.;
  float g = s2Freq/1023 * 255.;
  float b = s3Freq/1023 * 255.;
  background( r, g, b);
}


void onRangeMessage( String name, int value ) {
  if (name.equals("synth1Freq")){
    synth1.freq(value);
    s1Freq = float(value);
  } else if (name.equals("synth1Amp")){
    synth1.amp(value);
  } else if (name.equals("synth2Freq")){
    synth2.freq(value);
    s2Freq = float(value);
  } else if (name.equals("synth2Amp")){
    synth2.amp(value);
  } else if (name.equals("synth3Freq")){
    synth3.freq(value);
    s3Freq = float(value);
  } else if (name.equals("synth3Amp")){
    synth3.amp(value);
  }
}