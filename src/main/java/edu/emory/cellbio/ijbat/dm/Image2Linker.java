package edu.emory.cellbio.ijbat.dm;

import edu.emory.cellbio.ijbat.SlideSet;

import imagej.ImageJ;
import imagej.data.Dataset;
import imagej.io.IOService;

import net.imglib2.exception.IncompatibleTypeException;
import io.scif.io.img.ImgIOException;

import java.io.File;

/**
 * Linker class for opening images ({@code Dataset}s)
 * from file references.
 * 
 * @author Benjamin Nanes
 */
@LinkerInfo( underlying = "java.lang.String", processed = "imagej.data.Dataset", 
        typeCode = "Image2", name = "Image File (read only)")
public class Image2Linker extends Linker<String, Dataset> {
     
     private IOService ios;

     public Image2Linker(ImageJ context, SlideSet owner) {
          super(context, owner);
          ios = context.get(IOService.class);
     }
     
     public Image2Linker() {
          super();
     }

     @Override
     public Dataset process(String underlying) {
          String path = String.valueOf(underlying);
          String wd = owner.getWorkingDirectory();
          wd = wd == null ? "" : wd;
          if(!(new File(path)).isAbsolute())
               path = wd + File.separator + path;
          Dataset d;
          try{ d = ios.loadDataset(path); }
          catch(ImgIOException e)
               { throw new IllegalArgumentException("Error loading img: " + e); }
          catch(IncompatibleTypeException e)
               { throw new IllegalArgumentException("Unsuported image format: " + e); }
          return d;
     }

     /** @depricated */
     public Object initialize(Object underlying) {
          Dataset d;
          try {
               d = (Dataset)underlying;
          } catch(ClassCastException e) {
               throw new IllegalArgumentException("This is not an image!" + e);
          }
          String wd = owner.getWorkingDirectory();
          if(wd == null || wd.equals(""))
               throw new IllegalArgumentException("Cannot save images if working directory is not set");
          String path = "linked-images"
                  + File.separator
                  + String.valueOf(d.hashCode());
          //<<<<<<<<<<<<<<<<<< Figure out how to save images?
          return path;
     }

     @Override
     public Class<Dataset> getProcessedClass(Object underlying) {
          return Dataset.class;
     }
     
}
