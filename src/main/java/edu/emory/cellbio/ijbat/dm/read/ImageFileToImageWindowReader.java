package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.FileLinkElement;
import edu.emory.cellbio.ijbat.dm.MIME;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
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
        return new StackWindow(iftipr.read(elementToRead));
    }

}
