package edu.emory.cellbio.ijbat.dm;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.ex.LinkNotFoundException;

import imagej.ImageJ;
import imagej.data.overlay.AbstractOverlay;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Linker class for an ImageJ2-type ROI set
 * @author Benjamin Nanes
 */
@LinkerInfo(underlying = "java.lang.String", processed = "[Limagej.data.overlay.AbstractOverlay;", 
     typeCode = "ROISet2", name = "ROI Set (file)")
public class ROISet2Linker extends LinkLinker<AbstractOverlay[]> {

     public ROISet2Linker(ImageJ context, SlideSet owner) {
          super(context, owner);
     }
     
     public ROISet2Linker() {
          super();
     }
     
     @Override
     public AbstractOverlay[] process(String underlying)
             throws LinkNotFoundException {
          String path = resolveRelativePath(underlying);
          FileInputStream fis;
          ObjectInputStream ois;
          AbstractOverlay[] overlays;
          try {
               fis = new FileInputStream(path);
               ois = new ObjectInputStream(fis);
               int len = ois.readInt(); // int - number of overlays
               if(len == 0)
                    overlays = null;
               else
                    overlays = new AbstractOverlay[len];
               for(int i=0; i<len; i++) {
                    Class overlayClass = Class.forName(ois.readUTF()); // UTF string - name of overlay class
                    if(!AbstractOverlay.class.isAssignableFrom(overlayClass))
                         throw new IllegalArgumentException("Bad overlay type: "
                                 + overlayClass.toString());
                    overlays[i] = (AbstractOverlay) overlayClass.newInstance();
                    overlays[i].readExternal(ois); // Other fields - class dependent
                    overlays[i].setContext(context.getContext());
               }
               ois.close();
               fis.close();
          } catch (FileNotFoundException e) {
               throw new LinkNotFoundException(e.getMessage());
          } catch (IOException e) { 
               throw new IllegalArgumentException("Problem reading file: " + e);
          } catch (ClassNotFoundException ex) {
               throw new IllegalArgumentException("Class not found: " + ex);
          } catch (Throwable t) {
               throw new IllegalArgumentException("Other error: " + t);
          }
          return overlays;
     }
     
     /**
      * Write to file
      * 
      * @param path Path string
      * @param data {@code AbstractOverlay[]}
      */
     @Override
     public void write(String path, AbstractOverlay[] data) 
          throws LinkNotFoundException {
          path = resolveRelativePath(path);
          if(!AbstractOverlay[].class.isInstance(data))
               throw new IllegalArgumentException("Not a valid overlay: "
                       + data.getClass().toString());
          AbstractOverlay[] overlays = (AbstractOverlay[]) data;
          FileOutputStream fos;
          ObjectOutputStream oos;
          int len = overlays == null ? 0 : overlays.length;
          try {
               File dir = new File(path).getParentFile();
               if(dir != null && !dir.exists())
                    dir.mkdirs();
               fos = new FileOutputStream(path);
               oos = new ObjectOutputStream(fos);
               oos.writeInt(len); // int - number of overlays
               for(int i=0; i<len; i++) {
                    oos.writeUTF(overlays[i].getClass().getName()); // UTF string - name of overlay class
                    overlays[i].writeExternal(oos); // Other fields - class dependent
               }
               oos.close();
               fos.close();
          } catch (FileNotFoundException e) {
               throw new LinkNotFoundException(e.getMessage());
          } catch (IOException e) { 
               throw new IllegalArgumentException("Problem writing file: " + e);
          } catch (Throwable t) {
               throw new IllegalArgumentException("Other error: " + t);
          }
     }

     @Override
     public Class<AbstractOverlay[]> getProcessedClass(Object underlying) {
          return AbstractOverlay[].class;
     }
     
}
