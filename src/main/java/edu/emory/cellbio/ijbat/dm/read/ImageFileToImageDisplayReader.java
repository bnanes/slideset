package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.FileLinkElement;
import edu.emory.cellbio.ijbat.dm.MIME;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import net.imagej.Dataset;
import net.imagej.display.ImageDisplay;
import org.scijava.display.DisplayService;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "Image file",
        elementType = FileLinkElement.class,
        mimeTypes = { MIME.IMAGE, MIME.TIFF, MIME.PNG, MIME.JPG, MIME.GIF },
        processedType = ImageDisplay.class,
        hidden = false )
public class ImageFileToImageDisplayReader implements
        ElementReader<FileLinkElement, ImageDisplay> {
    
    private ImageFileToDatasetReader itdr = null;

    public ImageDisplay read(
            FileLinkElement elementToRead)
            throws SlideSetException {
        if(itdr == null)
            itdr = new ImageFileToDatasetReader();
        Dataset img = itdr.read(elementToRead);
        return (ImageDisplay) img.getContext()
                .getService(DisplayService.class).createDisplay(img);
    }
    
}
