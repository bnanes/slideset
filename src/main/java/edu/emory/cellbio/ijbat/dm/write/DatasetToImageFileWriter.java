package edu.emory.cellbio.ijbat.dm.write;

import edu.emory.cellbio.ijbat.dm.FileLinkElement;
import edu.emory.cellbio.ijbat.dm.MIME;
import edu.emory.cellbio.ijbat.ex.ImgLinkException;
import edu.emory.cellbio.ijbat.ex.LinkNotFoundException;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import imagej.core.commands.io.SaveImage;
import imagej.data.Dataset;
import io.scif.io.img.ImgSaver;
import java.io.File;
import net.imglib2.img.ImgPlus;
import org.scijava.Context;

/**
 *
 * @author Benjamin Nanes
 */
@ElementWriterMetadata(
        name = "Image file (extension sets format)",
        elementType = FileLinkElement.class,
        mimeType = MIME.IMAGE,
        processedType = Dataset.class )
public class DatasetToImageFileWriter implements
        ElementWriter<FileLinkElement, Dataset> {

    public void write(Dataset data, FileLinkElement elementToWrite)
            throws SlideSetException {
        final Context context = data.getContext();
        final SaveImage simg = new SaveImage();
        simg.setContext(context);
        String path = elementToWrite.getUnderlying();
        String wd = elementToWrite.getOwner().getWorkingDirectory();
        wd = wd == null ? "" : wd;
        if(!(new File(path)).isAbsolute())
            path = wd + File.separator + path;
        final File pathF = new File(path);
        if(!pathF.exists())
            try {
                pathF.getParentFile().mkdir();
            } catch(Exception ex) {
                throw new LinkNotFoundException(
                        path + " could not be created.", ex);
            }
        final ImgSaver iSave = new ImgSaver();  // This changes to a DatasetService method by 7.5
        iSave.setContext(data.getContext());
        try {
            final ImgPlus imp = data.getImgPlus();
            iSave.saveImg(path, imp);
        } catch(Exception e) {
            throw new ImgLinkException(e);
        }
    }
    
}
