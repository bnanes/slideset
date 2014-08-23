package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.FileLinkElement;
import edu.emory.cellbio.ijbat.dm.MIME;
import edu.emory.cellbio.ijbat.ex.LinkNotFoundException;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import edu.emory.cellbio.ijbat.io.Util;
import edu.emory.cellbio.ijbat.pi.WekaClassifierFile;
import java.io.File;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "Segmentation classifier file",
        elementType = FileLinkElement.class,
        mimeTypes = { MIME.WEKA },
        processedType = WekaClassifierFile.class,
        hidden = false )
public class WekaClassifierReader implements
        ElementReader<FileLinkElement, WekaClassifierFile> {

    public WekaClassifierFile read(FileLinkElement elementToRead)
          throws SlideSetException {
        String path = elementToRead.getUnderlying();
        String wd = elementToRead.getOwner().getWorkingDirectory();
        wd = wd == null ? "" : wd;
        if(!(new File(path)).isAbsolute())
            path = wd + File.separator + path;
        if(!(new File(path).exists()))
           throw new LinkNotFoundException(path + " does not exist!");
        return new WekaClassifierFile(path);
    }
    
}
