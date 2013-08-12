package edu.emory.cellbio.ijbat.dm;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.ex.SlideSetException;

import imagej.ImageJ;

import java.io.File;

/**
 *
 * @author Benjamin Nanes
 */
public abstract class LinkLinker<P> extends Linker<String, P> {
     
     public LinkLinker(ImageJ context, SlideSet owner) {
          super(context, owner);
     }
     
     public LinkLinker() {
          super();
     }
     
     /**
      * Write the full data to the specified path.
      * 
      * @param path Location to save the data
      * @param data Data to save
      */
     public abstract void write(String path, P data) throws SlideSetException;
     
     // -- Helper methods --
     
     /**
      * Attempts to resolve a possibly relative path based on the working directory
      * associated with the parent {@code SlideSet}. Does not verify that
      * the result is valid.
      * @deprecated 
      */
     protected String resolveRelativePath(String path) {
          File f = new File(path);
          if(f.isAbsolute())
               return path;
          return owner.getWorkingDirectory() + File.separator + path;
     }
     
}
