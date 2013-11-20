package edu.emory.cellbio.ijbat.dm;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.ex.ImgLinkException;
import edu.emory.cellbio.ijbat.ex.LinkNotFoundException;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import imagej.ImageJ;
import imagej.data.Dataset;
import imagej.data.DatasetService;
import imagej.io.IOService;
import imagej.io.event.FileSavedEvent;
import io.scif.io.img.ImgSaver;
import java.io.File;
import net.imglib2.img.ImgPlus;
import org.scijava.event.EventService;

/**
 *
 * @author Benjamin Nanes
 */
@LinkerInfo( underlying = "java.lang.String", processed = "imagej.data.Dataset", 
        typeCode = "tiff", name = "Image File (Tiff)")
public class SaveTiffLinker extends LinkLinker<Dataset> {
    
    private IOService ios;
    private DatasetService dss;
    
    // -- Constructors --
    
    public SaveTiffLinker() {
        super();
    }

    public SaveTiffLinker(ImageJ context, SlideSet owner) {
        super(context, owner);
        ios = context.io();
        dss = context.dataset();
    }
    
    // -- Methods --

    @Override
    public Dataset process(String underlying) throws SlideSetException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Class<Dataset> getProcessedClass(Object underlying) {
        return Dataset.class;
    }

    @Override
    public void write(String path, Dataset data) throws SlideSetException {
        final ImgPlus imp = data.getImgPlus();
        String wd = owner.getWorkingDirectory();
        wd = wd == null ? "" : wd;
        if(!(new File(path)).isAbsolute())
             path = wd + File.separator + path;
        if(!(new File(path).exists()))
            throw new LinkNotFoundException(path + " does not exist!");
        final ImgSaver iSave = new ImgSaver();  // This changes to a DatasetService method by 7.5
        iSave.setContext(dss.getContext());
        try {
            iSave.saveImg(path, imp);
            dss.getContext().getService(EventService.class)
                 .publish(new FileSavedEvent(data.getSource()));
        } catch(Exception e) {
            throw new ImgLinkException(e);
        }
    }
        
}
