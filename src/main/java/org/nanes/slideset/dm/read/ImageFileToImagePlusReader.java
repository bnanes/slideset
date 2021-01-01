package org.nanes.slideset.dm.read;

import org.nanes.slideset.dm.FileLinkElement;
import org.nanes.slideset.dm.MIME;
import org.nanes.slideset.ex.ImgLinkException;
import org.nanes.slideset.ex.LinkNotFoundException;
import org.nanes.slideset.ex.SlideSetException;
import ij.IJ;
import ij.ImagePlus;
import java.io.File;
import java.io.IOException;
import loci.formats.FormatException;
import loci.plugins.BF;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "Image file",
        elementType = FileLinkElement.class,
        mimeTypes = { MIME.IMAGE, MIME.TIFF, MIME.PNG, MIME.JPG, MIME.GIF },
        processedType = ImagePlus.class,
        hidden = false )
public class ImageFileToImagePlusReader implements
        ElementReader<FileLinkElement, ImagePlus> {

    public ImagePlus read(
            FileLinkElement elementToRead) 
            throws SlideSetException {
        String path = elementToRead.getUnderlying();
        String wd = elementToRead.getOwner().getWorkingDirectory();
        wd = wd == null ? "" : wd;
        path = path.replaceFirst("^~", System.getProperty("user.home"));  // need to expand home dir relative paths
        if(!(new File(path)).isAbsolute())
            path = wd + File.separator + path;
        if(!(new File(path).exists()))
           throw new LinkNotFoundException(path + " does not exist!");
        ImagePlus img;
        try {
            ImagePlus[] imgs = BF.openImagePlus(path);
            if(imgs.length > 1)
                throw new ImgLinkException("Unable to read " + path +
                    "\n Multiple sequences are not supported.");
            img = imgs[0];
        } catch(FormatException e) {
            img = null;
        } catch(IOException e) {
            throw new ImgLinkException("Unable to read " + path +
                    "\n" + e.getMessage());
        }
        if(img == null)
             img = IJ.openImage(path);
        if(img == null)
            throw new ImgLinkException("Unable to read " + path);
        return img;
    }

}
