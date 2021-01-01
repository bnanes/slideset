package org.nanes.slideset.dm.write;

import org.nanes.slideset.dm.FileLinkElement;
import org.nanes.slideset.dm.MIME;
import org.nanes.slideset.ex.SlideSetException;
import net.imagej.Dataset;
import net.imagej.display.ImageDisplay;

/**
 *
 * @author Benjamin Nanes
 */
@ElementWriterMetadata(
        name = "Image file (extension sets format)",
        elementType = FileLinkElement.class,
        mimeType = MIME.IMAGE,
        processedType = ImageDisplay.class,
        linkExt = "tiff")
public class ImageDisplayToImageFileWriter implements
        ElementWriter<FileLinkElement, ImageDisplay> {
    
    private DatasetToImageFileWriter difw = null;

    public void write(
            ImageDisplay data,
            FileLinkElement elementToWrite)
            throws SlideSetException {
        if(difw == null)
            difw = new DatasetToImageFileWriter();
        Dataset img;
        try {
            img = (Dataset) data.get(0).getData();
        } catch(Exception e) {
            throw new SlideSetException("An unexpectedly bad error!");
        }
        difw.write(img, elementToWrite);
        data.close();
    }
    
}
