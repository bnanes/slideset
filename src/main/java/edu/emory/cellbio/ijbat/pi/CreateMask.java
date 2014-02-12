package edu.emory.cellbio.ijbat.pi;

import imagej.data.Dataset;
import imagej.data.DefaultDataset;
import imagej.data.overlay.AbstractOverlay;
import net.imglib2.RandomAccess;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.meta.Axes;
import net.imglib2.meta.ImgPlus;
import net.imglib2.roi.RegionOfInterest;
import net.imglib2.type.numeric.integer.ByteType;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Generate a mask image from an ROI set.
 * 
 * @author Benjamin Nanes
 */
@HelpPath(path = "plugins/createmask.html")
@Plugin( type=SlideSetPlugin.class,
         name="Create Mask Image",
         label="Create Mask Image", visible = false,
         menuPath="Plugins > Slide Set > Commands > Segmentation > Create Mask Image")
public class CreateMask extends SlideSetPlugin {
    
    // -- Parameters --
    
    @Parameter(label = "Context", type = ItemIO.INPUT)
    private Context context;
    
    @Parameter(label = "Regions", type = ItemIO.INPUT)
    private AbstractOverlay[] regions;
    
    @Parameter(label = "Template", type = ItemIO.INPUT)
    private Dataset template;
    
    @Parameter(label = "Mask image", type = ItemIO.OUTPUT)
    private Dataset mask;
    
    private static final byte nil = 0;
    
    // -- run method --

    public void run() {
        if(regions == null)
            regions = new AbstractOverlay[0];
        long[] dim = new long[template.numDimensions()];
        template.dimensions(dim);
        final int cAxis = template.dimensionIndex(Axes.CHANNEL);
        if(cAxis >= 0)
            dim[cAxis] = 1;
        PlanarImg<ByteType, ?> maskImg
                = new PlanarImgFactory<ByteType>().create(dim, new ByteType(nil));
        IntervalIterator ii;
        RandomAccess<ByteType> ra = maskImg.randomAccess();
        double[] pos = new double[dim.length];
        long[] min = new long[dim.length];
        long[] max = new long[dim.length];
        RegionOfInterest roi;
        for(AbstractOverlay ao : regions) {
            roi = ao.getRegionOfInterest();
            for(int i = 0; i < dim.length; i++) {
                if(i == cAxis) {
                    min[i] = 0;
                    max[i] = 0;
                    continue;
                }
                min[i] = Math.round(Math.floor(roi.realMin(i)));
                max[i] = Math.round(Math.ceil(roi.realMax(i)));
            }
            ii = new IntervalIterator(min, max);
            while(ii.hasNext()) {
                ii.fwd();
                ii.localize(pos);
                if(!roi.contains(pos) || !inBounds(pos, dim))
                    continue;
                ra.setPosition(ii);
                ra.get().setInteger(255);
            }
        }
        mask = new DefaultDataset(context, new ImgPlus(maskImg, "mask"));
    }
    
    private boolean inBounds(double[] pos, long[] dims) {
        for(int i = 0; i < pos.length; i++)
            if(pos[i] >= dims[i])
                return false;
        return true;
    }
    
}
