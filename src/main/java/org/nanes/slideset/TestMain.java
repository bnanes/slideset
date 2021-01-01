package org.nanes.slideset;

import net.imagej.ImageJ;
import org.scijava.ui.DefaultUIService;

/**
 * Test class to run IJ2
 * @author Benjamin Nanes
 */
public class TestMain {

     /**
      * @param args the command line arguments
      */
     public static void main(String[] args) {
          net.imagej.ImageJ ij = new ImageJ(); //imagej.TestMain.launch();
          DefaultUIService duis = ij.get(DefaultUIService.class);
          duis.showUI();
          ij.command().run(SlideSetIJ2Entry.class, true, new Object[0]);
     }
}
