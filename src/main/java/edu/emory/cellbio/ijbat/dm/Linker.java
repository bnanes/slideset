package edu.emory.cellbio.ijbat.dm;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import imagej.ImageJ;

/**
 *
 * @author Benjamin Nanes
 */
public abstract class Linker<U,P> {
     
     protected final ImageJ context;
     protected final SlideSet owner;
     
     public Linker(ImageJ context, SlideSet owner) {
          if(context == null || owner == null)
               throw new IllegalArgumentException("Can't initalize Linker with null elements");
          this.context = context;
          this.owner = owner;
     }
     
     public Linker() {
          throw new IllegalArgumentException("Can't initalize Linker with null elements");
     }
     
     /**
      * Perform some processing function to load
      * the full underlying data (i.e. read from disk)
      */
     public abstract P process(U underlying) throws SlideSetException;
        
     /**
      * Get the class of the processed underlying data
      */
     public abstract Class<P> getProcessedClass(Object underlying);
     
}
