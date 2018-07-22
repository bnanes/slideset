package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.FileLinkElement;
import edu.emory.cellbio.ijbat.dm.MIME;
import edu.emory.cellbio.ijbat.ex.ImgLinkException;
import edu.emory.cellbio.ijbat.ex.LinkNotFoundException;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import net.imagej.Dataset;
import io.scif.services.DatasetIOService;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "Image file",
        elementType = FileLinkElement.class,
        mimeTypes = { MIME.IMAGE, MIME.TIFF, MIME.PNG, MIME.JPG, MIME.GIF },
        processedType = Dataset.class,
        hidden = false )
public class ImageFileToDatasetReader implements
        ElementReader<FileLinkElement, Dataset> {

    public Dataset read(FileLinkElement elementToRead) throws SlideSetException {
        String path = elementToRead.getUnderlying();
        String wd = elementToRead.getOwner().getWorkingDirectory();
        wd = wd == null ? "" : wd;
        if(!(new File(path)).isAbsolute())
            path = wd + File.separator + path;
        if(!(new File(path).exists()))
           throw new LinkNotFoundException(path + " does not exist!");
        Dataset d;
        DatasetIOService dios = elementToRead.getOwner().
                getContext().getService(DatasetIOService.class);
        try{ d = dios.open(path); }
        catch(IOException e) {
            throw new ImgLinkException(e);
        }
        return d;
    }
    
}
