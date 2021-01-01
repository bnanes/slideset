package org.nanes.slideset.dm.read;

import org.nanes.slideset.dm.FileLinkElement;
import org.nanes.slideset.dm.MIME;
import org.nanes.slideset.ex.LinkNotFoundException;
import org.nanes.slideset.ex.SlideSetException;
import org.nanes.slideset.io.Util;
import org.nanes.slideset.pi.WekaClassifierFile;
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
