package org.nanes.slideset.dm.read;

import org.nanes.slideset.dm.FileLinkElement;
import org.nanes.slideset.dm.MIME;
import org.nanes.slideset.ex.ImgLinkException;
import org.nanes.slideset.ex.LinkNotFoundException;
import org.nanes.slideset.ex.SlideSetException;
import net.imagej.Dataset;
import io.scif.services.DatasetIOService;
import java.io.File;
import java.io.IOException;
import org.scijava.convert.ConvertService;

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
    
    private ImageFileToImagePlusReader iftipr;
    private ConvertService cs;

    public Dataset read(FileLinkElement elementToRead) throws SlideSetException {
        String path = elementToRead.getUnderlying();
        String wd = elementToRead.getOwner().getWorkingDirectory();
        wd = wd == null ? "" : wd;
        path = path.replaceFirst("^~", System.getProperty("user.home"));  // need to expand home dir relative paths
        if(!(new File(path)).isAbsolute())
            path = wd + File.separator + path;
        if(!(new File(path).exists()))
           throw new LinkNotFoundException(path + " does not exist!");
        Dataset d;
        if(iftipr == null)
            iftipr = new ImageFileToImagePlusReader();
        if(cs == null)
            cs = elementToRead.getOwner().getContext().getService(ConvertService.class);
        try {
            d = cs.convert(iftipr.read(elementToRead), Dataset.class);
        }
        catch(Exception e) {
            d = null; // Fallback to SCFIO
        }
        if(d == null) {
            DatasetIOService dios = elementToRead.getOwner().
                    getContext().getService(DatasetIOService.class);
            try{ 
                d = dios.open(path); 
                
            }
            catch(IOException e) {
                throw new ImgLinkException(e);
            }
        }
        if(d == null)
            throw new ImgLinkException("Unable to read " + path);
        return d;
    }
    
}
