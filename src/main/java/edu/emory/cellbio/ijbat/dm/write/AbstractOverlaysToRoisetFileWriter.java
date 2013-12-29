package edu.emory.cellbio.ijbat.dm.write;

import edu.emory.cellbio.ijbat.dm.AbstractOverlaysAlias;
import edu.emory.cellbio.ijbat.dm.FileLinkElement;
import edu.emory.cellbio.ijbat.dm.MIME;
import edu.emory.cellbio.ijbat.ex.LinkNotFoundException;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import imagej.data.overlay.AbstractOverlay;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 *
 * @author Benjamin Nanes
 */
@ElementWriterMetadata(
        name = "ROI set file",
        elementType = FileLinkElement.class,
        mimeType = MIME.ROI2,
        processedType = AbstractOverlaysAlias.class,
        linkExt = "roiset")
public class AbstractOverlaysToRoisetFileWriter implements
        ElementWriter<FileLinkElement, AbstractOverlay[]> {

    public void write(
            AbstractOverlay[] data,
            FileLinkElement elementToWrite)
            throws SlideSetException {
        String path = elementToWrite.getUnderlying();
        String wd = elementToWrite.getOwner().getWorkingDirectory();
        wd = wd == null ? "" : wd;
        if(!(new File(path)).isAbsolute())
            path = wd + File.separator + path;
        final File pathF = new File(path);
        if(!pathF.exists())
            try {
                final File pathP = pathF.getParentFile();
                if(pathP != null && (!pathP.exists()))
                    pathP.mkdirs();
                pathF.createNewFile();
            } catch(IOException ex) {
                throw new LinkNotFoundException(
                        path + " could not be created.", ex);
            }
        FileOutputStream fos;
        ObjectOutputStream oos;
        int len = data == null ? 0 : data.length;
        try {
            File dir = new File(path).getParentFile();
            if(dir != null && !dir.exists())
                dir.mkdirs();
            fos = new FileOutputStream(path);
            oos = new ObjectOutputStream(fos);
            oos.writeInt(len); // int - number of overlays
            for(int i=0; i<len; i++) {
                oos.writeUTF(data[i].getClass().getName()); // UTF string - name of overlay class
                data[i].writeExternal(oos); // Other fields - class dependent
            }
            oos.close();
            fos.close();
        } catch (FileNotFoundException e) {
            throw new LinkNotFoundException(e.getMessage());
        } catch (IOException e) { 
            throw new SlideSetException("Problem writing file: ", e);
        } catch (Throwable t) {
            throw new SlideSetException("Other error: ", t);
        }
    }
    
}
