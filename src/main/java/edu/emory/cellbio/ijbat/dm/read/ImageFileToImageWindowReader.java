package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.FileLinkElement;
import edu.emory.cellbio.ijbat.ex.ImgLinkException;
import edu.emory.cellbio.ijbat.ex.LinkNotFoundException;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import java.io.File;

/**
 *
 * @author Benjamin Nanes
 */
public class ImageFileToImageWindowReader implements
        ElementReader<FileLinkElement, ImageWindow> {

    public ImageWindow read(
            FileLinkElement elementToRead) 
            throws SlideSetException {
        String path = elementToRead.getUnderlying();
        String wd = elementToRead.getOwner().getWorkingDirectory();
        wd = wd == null ? "" : wd;
        if(!(new File(path)).isAbsolute())
            path = wd + File.separator + path;
        if(!(new File(path).exists()))
           throw new LinkNotFoundException(path + " does not exist!");
        ImagePlus img = IJ.openImage(path);
        if(img == null)
            throw new ImgLinkException("Unable to read " + path);
        return new ImageWindow(img);
    }

}
