package edu.emory.cellbio.ijbat.pi;

import imagej.ImageJ;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.Parameter;
import org.scijava.ItemIO;
import imagej.data.Dataset;

/**
 * Test SlideSet compatible plugin
 * @author Ben
 */
@Plugin(type=SlideSetPlugin.class, label="Hello World!")
public class Test extends SlideSetPlugin {
     
     @Parameter
     private ImageJ context;
     
     @Parameter(label="An image", type=ItemIO.INPUT)
     private Dataset i;
     
     @Parameter(label="A string", type=ItemIO.INPUT)
     private String s;
     
     @Parameter(label="Result", type=ItemIO.OUTPUT)
     private Dataset o;
     
     public void run() {
          o = i.duplicate();
     }
     
}