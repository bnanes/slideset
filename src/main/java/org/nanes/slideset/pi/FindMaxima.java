package org.nanes.slideset.pi;

import org.nanes.slideset.ui.SlideSetLog;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.filter.MaximumFinder;
import ij.process.ImageProcessor;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Find maxima in an image using ij.plugin.filter.MaximumFinder
 * @author Benjamin Nanes
 */
@HelpPath(path = "plugins/findmaxima.html")
@Plugin( type=SlideSetPlugin.class,
         name="Find Maxima",
         label="Find Maxima", visible = false,
         menuPath="Plugins > Slide Set > Commands > Find Maxima")
public class FindMaxima extends SlideSetPlugin {
    
    // -- Parameters --
    
    @Parameter(label = "Context", type = ItemIO.INPUT)
    private Context context;
    
    @Parameter(label = "Log", type = ItemIO.INPUT)
    private SlideSetLog log;
    
    @Parameter(label = "Image", type = ItemIO.INPUT)
    private ImagePlus image;
    
    @Parameter(label = "Channel (1-index)", type = ItemIO.INPUT)
    private int chan;
    
    @Parameter(label = "Prominence", type = ItemIO.INPUT)
    private double prominence;
    
    @Parameter(label = "Strict", type = ItemIO.INPUT)
    private boolean strict;
    
    @Parameter(label = "Exclude edges", type = ItemIO.INPUT)
    private boolean exclEdges;
    
    @Parameter(label = "Maxima", type = ItemIO.OUTPUT)
    private Roi[] maxPoints;

    @Override
    public void run() {
        MaximumFinder mf = new MaximumFinder();
        ImageProcessor ipp;
        if(image.isComposite()) {
            image.setPosition(chan, image.getZ(), image.getT());
            ipp = image.getChannelProcessor();
        } else {
            ipp = image.getProcessor();
        }
        maxPoints = new Roi[1];
        maxPoints[0] = new PointRoi(mf.getMaxima(ipp, prominence, strict, exclEdges));
    }
    
}
