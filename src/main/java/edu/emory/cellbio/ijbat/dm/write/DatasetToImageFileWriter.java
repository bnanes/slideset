package edu.emory.cellbio.ijbat.dm.write;

import edu.emory.cellbio.ijbat.dm.FileLinkElement;
import edu.emory.cellbio.ijbat.dm.MIME;
import edu.emory.cellbio.ijbat.ex.ImgLinkException;
import edu.emory.cellbio.ijbat.ex.LinkNotFoundException;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import net.imagej.Dataset;
import io.scif.services.DatasetIOService;
import java.io.File;
import java.io.IOException;
import org.scijava.Context;

/**
 *
 * @author Benjamin Nanes
 */
@ElementWriterMetadata(
        name = "Image file (extension sets format)",
        elementType = FileLinkElement.class,
        mimeType = MIME.IMAGE,
        processedType = Dataset.class,
        linkExt = "tiff" )
public class DatasetToImageFileWriter implements
        ElementWriter<FileLinkElement, Dataset> {

    public void write(Dataset data, FileLinkElement elementToWrite)
            throws SlideSetException {
        final Context context = data.getContext();
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
            if(pathF.exists())
                pathF.delete(); // This is less than ideal, but there seems to be an ImageJ bug overwriting exisiting images, especially if the new image has different dimensions.
            context.getService(DatasetIOService.class).save(data, path);
        } catch(IOException e) {
            throw new ImgLinkException(e);
        }
    }
    
}
