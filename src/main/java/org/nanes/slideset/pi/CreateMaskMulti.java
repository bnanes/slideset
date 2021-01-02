package org.nanes.slideset.pi;

import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.overlay.AbstractOverlay;
import net.imglib2.RandomAccess;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.roi.RegionOfInterest;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Generate a mask image from an ROI set, with each ROI labeled individually.
 * @author Benjamin Nanes
 * 
 * Returns an image the same size as the template image with values:
 *  0 if outside of all ROIs
 *  1,2,...N if inside ROI 1,2,...N
 *  If a pixel is inside multiple ROIs, value will be the highest ROI containing the pixel
 */
@HelpPath(path = "plugins/createmask.html")
@Plugin( type=SlideSetPlugin.class,
         name="Create Multimask Image",
         label="Create Multimask Image", visible = false,
         menuPath="Plugins > Slide Set > Commands > Segmentation > Create Multimask Image")
public class CreateMaskMulti extends SlideSetPlugin {
    
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
    
    // -- Run method --

    @Override
    public void run() {
        if(regions == null)
            regions = new AbstractOverlay[0];
        long[] dim = new long[template.numDimensions()];
        //System.out.println(String.valueOf(dim.length));
        template.dimensions(dim);
        final int cAxis = template.dimensionIndex(Axes.CHANNEL);
        //System.out.println(cAxis);
        if(cAxis >= 0)
            dim[cAxis] = 1;
        PlanarImg<UnsignedByteType, ?> maskImg
                = new PlanarImgFactory<UnsignedByteType>().create(dim, new UnsignedByteType(nil));
        IntervalIterator ii;
        RandomAccess<UnsignedByteType> ra = maskImg.randomAccess();
        double[] pos = new double[dim.length];
        long[] min = new long[dim.length];
        long[] max = new long[dim.length];
        RegionOfInterest roi;
        int nRegions = regions.length;
        for(int iRegion = 0; iRegion < nRegions; iRegion++) {
            AbstractOverlay ao = regions[iRegion];
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
                ra.get().setInteger(iRegion+1);
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
