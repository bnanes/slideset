package edu.emory.cellbio.ijbat.dm;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.ex.ImgLinkException;
import edu.emory.cellbio.ijbat.ex.LinkNotFoundException;
import imagej.ImageJ;
import imagej.data.Dataset;
import imagej.data.DatasetService;
import imagej.io.IOService;
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
     private DatasetService dss;

     public Image2Linker(ImageJ context, SlideSet owner) {
          super(context, owner);
          ios = context.get(IOService.class);
          dss = context.get(DatasetService.class);
     }
     
     public Image2Linker() {
          super();
     }

     @Override
     public Dataset process(String underlying)
            throws LinkNotFoundException, ImgLinkException {
          String path = String.valueOf(underlying);
          String wd = owner.getWorkingDirectory();
          wd = wd == null ? "" : wd;
          if(!(new File(path)).isAbsolute())
               path = wd + File.separator + path;
          if(!(new File(path).exists()))
              throw new LinkNotFoundException(path + " does not exist!");
          Dataset d;
          try{ d = ios.loadDataset(path); }  // This will need to change to DataSetService.open()
          catch(Exception e) {
              throw new ImgLinkException(e);
          }
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
               throw new IllegalStateException("Cannot save images if working directory is not set");
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
