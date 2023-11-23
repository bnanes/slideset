package org.nanes.slideset.dm.write;

import ij.IJ;
import ij.ImagePlus;
import java.io.File;
import org.nanes.slideset.dm.FileLinkElement;
import org.nanes.slideset.dm.MIME;
import org.nanes.slideset.ex.ImgLinkException;
import org.nanes.slideset.ex.LinkNotFoundException;
import org.nanes.slideset.ex.SlideSetException;

/**
 *
 * @author Benjamin Nanes
 */
@ElementWriterMetadata(
        name = "PNG image file",
        elementType = FileLinkElement.class,
        mimeType = MIME.IMAGE,
        processedType = ImagePlus.class,
        linkExt = "png" )
public class ImagePlusToPngFileWriter implements
        ElementWriter<FileLinkElement, ImagePlus> {
    
    public void write(ImagePlus data, FileLinkElement elementToWrite)
            throws SlideSetException {
        String path = elementToWrite.getUnderlying();
        path = elementToWrite.getOwner().resolvePath(path);
        final File pathF = new File(path);
        if(!pathF.getParentFile().exists())
            try {
                pathF.getParentFile().mkdirs();
            } catch(Exception ex) {
                throw new LinkNotFoundException(
                        path + " could not be created.", ex);
            }
        try {
            IJ.save(data, path);
        } catch(Exception e) {
            throw new ImgLinkException(e);
        }
    }
}
