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
//@Plugin(type=SlideSetPlugin.class, label="Hello World!")
public class Test extends SlideSetPlugin {
     
     @Parameter
     private ImageJ context;
     
     @Parameter(label="A string", type=ItemIO.BOTH)
     private String text;
     
     @Parameter(label="An integer")
     private int number;
     
     @Parameter(label="An image")
     private Dataset d;
     
     @Parameter(type=ItemIO.OUTPUT)
     private String imgName;
     
     public void run() {
          imgName = d.getImgPlus().getName();
     }
     
}