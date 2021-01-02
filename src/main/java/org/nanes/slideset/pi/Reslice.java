package org.nanes.slideset.pi;

import org.nanes.slideset.ex.SlideSetException;
import org.nanes.slideset.ui.SlideSetLog;
import ij.ImagePlus;
import ij.gui.Roi;
import java.util.ArrayList;
import net.imagej.Dataset;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.convert.ConvertService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Reslice image along a linear ROI
 * This command is a limited reimplementation of the ImageJ Reslice command
 * (<code>ij.plugin.Slicer</code>).
 * @see org.nanes.slideset.pi.SilentSlicer
 * @author Benjamin Nanes
 */
@HelpPath(path = "plugins/stacks.html#reslice")
@Plugin( type=SlideSetPlugin.class,
         name="Reslice",
         label="Reslice", visible = false,
         menuPath="Plugins > Slide Set > Commands > Stacks > Reslice")
public class Reslice extends SlideSetPlugin implements MultipleResults {
    
    // -- Parameters --
    
    @Parameter(label = "Context", type = ItemIO.INPUT)
    private Context context;
    
    @Parameter(label = "Log", type = ItemIO.INPUT)
    private SlideSetLog log;
    
    @Parameter(label = "Image", type = ItemIO.INPUT)
    private ImagePlus image;
    
    @Parameter(label = "Slice lines", type = ItemIO.INPUT)
    private Roi[] sliceLines;
    
    @Parameter(label = "Flip verticaly", type = ItemIO.INPUT)
    private boolean flip;
    
    @Parameter(label = "Rotate 90 degrees", type = ItemIO.INPUT)
    private boolean rotate;
    
    @Parameter(label = "Slice", type = ItemIO.OUTPUT)
    private Dataset[] sliceImages;

    @Override
    public void run() {
        ConvertService cs = context.getService(ConvertService.class);
        int nSlices = sliceLines.length;
        ArrayList<ImagePlus> sliceImps = new ArrayList(nSlices);
        SilentSlicer theChopper;
        for(int i=0; i<nSlices; i++) {
            image.setRoi(sliceLines[i]);
            theChopper = new SilentSlicer(image, 1, 1, flip, rotate, true);
            try {
                sliceImps.add(theChopper.sliceSilently(image));
            } catch(SlideSetException e) {
                log.println(e.toString());
            }
        }
        sliceImages = new Dataset[sliceImps.size()];
        for(int i=0; i<sliceImps.size(); i++) {
            sliceImages[i] = cs.convert(sliceImps.get(i), Dataset.class);
        }
    }
    
}
