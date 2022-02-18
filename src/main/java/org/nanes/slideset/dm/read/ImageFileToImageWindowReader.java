package org.nanes.slideset.dm.read;

import ij.IJ;
import org.nanes.slideset.dm.FileLinkElement;
import org.nanes.slideset.dm.MIME;
import org.nanes.slideset.ex.SlideSetException;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "Image file",
        elementType = FileLinkElement.class,
        mimeTypes = { MIME.IMAGE, MIME.TIFF, MIME.PNG, MIME.JPG, MIME.GIF },
        processedType = ImageWindow.class,
        hidden = false )
public class ImageFileToImageWindowReader implements
        ElementReader<FileLinkElement, ImageWindow> {
    
    private ImageFileToImagePlusReader iftipr;

    public ImageWindow read(
            FileLinkElement elementToRead) 
            throws SlideSetException {
        if(iftipr == null)
            iftipr = new ImageFileToImagePlusReader();
        ij.ImagePlus imp = iftipr.read(elementToRead);
        if(imp.getBitDepth() == 24) {
            IJ.log("Detected RGB image");
            return new ImageWindow(imp);
        }
        return new StackWindow(imp);
    }

}
