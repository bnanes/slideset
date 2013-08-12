package edu.emory.cellbio.ijbat.dm;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.io.Util;

import imagej.ImageJ;

import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

import java.io.File;

/**
 * Linker class for an ImageJ1-type ROI set
 * @author Benjamin Nanes
 */
public class ROISet1Linker extends Linker {
     
     public ROISet1Linker(ImageJ context, SlideSet owner) {
          super(context, owner);
     }

     @Override
     public Object process(Object underlying) {
          String path = String.valueOf(underlying);
          String wd = owner.getWorkingDirectory();
          wd = wd == null ? "" : wd;
          if(!(new File(path)).isAbsolute())
               path = wd + File.pathSeparator;
          Roi[] roi;
          RoiManager roiMan = new RoiManager(true);
          roiMan.runCommand("open", path);
          roi = roiMan.getRoisAsArray();
          return roi;
     }

     //@Override
     public Object initialize(Object underlying) {
          Roi[] roi;
          try{ roi = (Roi[])underlying; }
          catch(ClassCastException e) {
               throw new IllegalArgumentException("Data is not an Roi array: " + e);
          }
          String wd = owner.getWorkingDirectory();
          if(wd == null || wd.equals(""))
               throw new IllegalArgumentException("Cannot save ROIs when the working directory is not set!");
          String fp = "roi-sets" + String.valueOf(underlying.hashCode()) + ".zip";
          File sf = new File( wd + File.separator + fp);
          if(!Util.writeRoiSetFile(roi, sf))
               throw new IllegalArgumentException("Could not write ROI file!");
          return fp;
     }

     @Override
     public Class<?> getProcessedClass(Object underlying) {
          return Roi[].class;
     }
     
}
