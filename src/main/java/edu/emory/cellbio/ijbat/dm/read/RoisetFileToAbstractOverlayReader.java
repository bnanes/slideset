package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.AbstractOverlaysAlias;
import edu.emory.cellbio.ijbat.dm.FileLinkElement;
import edu.emory.cellbio.ijbat.dm.MIME;
import edu.emory.cellbio.ijbat.ex.LinkNotFoundException;
import edu.emory.cellbio.ijbat.ex.RoiLinkException;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import imagej.data.overlay.AbstractOverlay;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "ROI set file",
        elementType = FileLinkElement.class,
        mimeTypes = { MIME.ROI2 },
        processedType = AbstractOverlaysAlias.class,
        hidden = false )
public class RoisetFileToAbstractOverlayReader implements
        ElementReader<FileLinkElement, AbstractOverlay[]> {

    public AbstractOverlay[] read(FileLinkElement elementToRead) throws SlideSetException {
        String path = elementToRead.getUnderlying();
        String wd = elementToRead.getOwner().getWorkingDirectory();
        wd = wd == null ? "" : wd;
        if(!(new File(path)).isAbsolute())
            path = wd + File.separator + path;
        if(!(new File(path).exists()))
           throw new LinkNotFoundException(path + " does not exist!");
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
                    throw new RoiLinkException("Bad overlay type: "
                            + overlayClass.toString());
                overlays[i] = (AbstractOverlay) overlayClass.newInstance();
                overlays[i].readExternal(ois); // Other fields - class dependent
                overlays[i].setContext(elementToRead.getOwner().getContext());
            }
            ois.close();
            fis.close();
        } catch (FileNotFoundException e) {
            throw new LinkNotFoundException(e);
        } catch (IOException e) { 
            throw new RoiLinkException("Unable to read ROI set file", e);
        } catch (ClassNotFoundException ex) {
            throw new RoiLinkException("Unable find the specified ROI class", ex);
        } catch (Throwable t) {
            throw new RoiLinkException("Unexpected error reading ROI set file", t);
        }
        return overlays;
    }
    
}
