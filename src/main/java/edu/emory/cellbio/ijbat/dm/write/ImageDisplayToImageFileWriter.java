package edu.emory.cellbio.ijbat.dm.write;

import edu.emory.cellbio.ijbat.dm.FileLinkElement;
import edu.emory.cellbio.ijbat.dm.MIME;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import imagej.data.Dataset;
import imagej.data.display.ImageDisplay;

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
