package edu.emory.cellbio.ijbat.dm;

import edu.emory.cellbio.ijbat.SlideSet;
import imagej.ImageJ;

/**
 * {@link Linker} class for any {@link DataElement} that is not a link.
 * 
 * @author Benjamin Nanes
 */
public class ObjectLinker extends Linker {

     public ObjectLinker(ImageJ context, SlideSet owner) {
          super(context, owner);
     }

     @Override
     public Object process(Object underlying) {
          return underlying;
     }

     @Override
     public Class<?> getProcessedClass(Object underlying) {
          return underlying.getClass();
     }
     
}