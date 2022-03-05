package org.nanes.slideset.dm.write;

import ij.IJ;
import ij.ImagePlus;
import org.nanes.slideset.dm.FileLinkElement;
import org.nanes.slideset.dm.MIME;
import org.nanes.slideset.ex.ImgLinkException;
import org.nanes.slideset.ex.LinkNotFoundException;
import org.nanes.slideset.ex.SlideSetException;
import net.imagej.Dataset;
import java.io.File;
import org.scijava.Context;
import org.scijava.convert.ConvertService;

/**
 *
 * @author Benjamin Nanes
 */
@ElementWriterMetadata(
        name = "PNG image file",
        elementType = FileLinkElement.class,
        mimeType = MIME.IMAGE,
        processedType = Dataset.class,
        linkExt = "png" )
public class DatasetToPngFileWriter implements
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
        ConvertService cs = context.getService(ConvertService.class);
        ImagePlus imp = cs.convert(data, ImagePlus.class);
        try {
            IJ.save(imp, path);
        } catch(Exception e) {
            throw new ImgLinkException(e);
        }
    }
    
}
