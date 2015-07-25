package edu.emory.cellbio.ijbat.pi;

import java.util.Arrays;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.RandomAccess;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.img.planar.PlanarRandomAccess;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Segment an image using Otsu's method.
 * 
 * Nobuyuki Otsu (1979). "A threshold selection method 
 * from gray-level histograms". IEEE Trans. Sys., Man., Cyber.
 * 9 (1): 62–66. doi:10.1109/TSMC.1979.4310076
 * 
 * @author Benjamin Nanes
 */
@HelpPath(path = "plugins/otsu.html")
@Plugin(
        type = SlideSetPlugin.class,
        name = "Otsu Segmentation",
        label ="Otsu Segmentation", visible = false,
        menuPath = "Plugins > Slide Set > Commands > Segmentation > Otsu Segmentation")
public class OtsuSegmentation
        extends SlideSetPlugin implements MultipleResults {
    
    // -- Parameters --
    
    @Parameter(label="ImageJ", type=ItemIO.INPUT)
    private ImageJ ij;
    
    @Parameter(label="Images", type=ItemIO.INPUT)
    private Dataset ds;
    
    @Parameter(label="Channel", type=ItemIO.OUTPUT)
    private int[] channels;
    
    @Parameter(label="Threshold", type=ItemIO.OUTPUT)
    private double[] thresholds;
    
    @Parameter(label="Threshold Map", type=ItemIO.OUTPUT)
    private Dataset[] maps;
    
    // -- Other Fields --
    
    // -- Methods --
    
    public void run() {
        final int cAxis = ds.dimensionIndex(Axes.CHANNEL);
        final int xAxis = ds.dimensionIndex(Axes.X);
        final int yAxis = ds.dimensionIndex(Axes.Y);
        final boolean flat = cAxis < 0 && xAxis >= 0 && yAxis >= 0;
        if(!flat && cAxis < 0)
            throw new IllegalArgumentException("Unable to find channel axis.");
        long[] dims = new long[ds.numDimensions()];
        ds.dimensions(dims);
        final int nc = flat? 1 : new Long(dims[cAxis]).intValue();
        final long[] origin = new long[dims.length];
        Arrays.fill(origin, 0);
        final long[] planeDims = new long[flat? dims.length : dims.length-1];
        int j=0;
        for(int i=0; i<dims.length; i++) {
            if(i == cAxis)
                continue;
            planeDims[j] = dims[i];
            j++;
        }
        final RandomAccess<RealType<?>> ra = ds.randomAccess();
        final int maxValue = new Double(Math.ceil(ds.firstElement().getMaxValue())).intValue();
        final long[] hist = new long[maxValue+1];
        channels = new int[nc];
        thresholds = new double[nc];
        maps = new Dataset[nc];
        final PlanarImgFactory<UnsignedByteType> pif =
                new PlanarImgFactory<UnsignedByteType>();
        for(int c = 0; c < nc; c++) { // Loop through each image channel
            channels[c] = c;
            final long[] min = Arrays.copyOf(origin, origin.length);
            if(!flat)
                min[cAxis] = c;
            final long[] max = Arrays.copyOf(dims, dims.length);
            for(int i=0; i<max.length; i++)
                max[i]--;
            if(!flat)
                max[cAxis] = c;
            final IntervalIterator ii = new IntervalIterator(min, max);
            Arrays.fill(hist, 0);
            double total = 0;
            double pixels = 0;
            while(ii.hasNext()) {
                ii.fwd();
                ra.setPosition(ii);
                final double dVal = ra.get().getRealDouble();
                final int val = new Double(Math.round(dVal)).intValue();
                hist[val]++;
                total += val;
                pixels++;
            }
            double sumB = 0;
            double wB = 0;
            double wF;
            double mB;
            double mF;
            double btwn;
            double t1 = 0;
            double t2 = 0;
            double btwnMax = 0;
            for(int i=0; i < hist.length; i++) {
                wB += hist[i];
                if(wB == 0)
                    continue;
                wF = pixels - wB;
                if(wF == 0)
                    break;
                sumB += i * hist[i];
                mB = sumB / wB;
                mF = (total - sumB) / wF;
                btwn = wB * wF * (mB - mF) * (mB - mF);
                if(btwn >= btwnMax) {
                    t1 = i;
                    if(btwn > btwnMax)
                        t2 = i;
                    btwnMax = btwn;
                }
            }
            System.out.println("thresh done");
            thresholds[c] = (t1 + t2) / 2;
            PlanarImg<UnsignedByteType, ?> map 
                    = pif.create(planeDims, new UnsignedByteType(0));
            PlanarRandomAccess<UnsignedByteType> mra = map.randomAccess();
            final long[] mapPos = new long[flat? dims.length : dims.length-1];
            ii.reset();
            while(ii.hasNext()) {
                ii.fwd();
                ra.setPosition(ii);
                int k=0;
                for(int i=0; i<dims.length; i++) {
                    if(i == cAxis)
                        continue;
                    mapPos[k] = ii.getLongPosition(i);
                    k++;
                }
                UnsignedByteType val 
                        = ra.get().getRealDouble() > thresholds[c] 
                        ? new UnsignedByteType(255) : new UnsignedByteType(0);
                mra.setPosition(mapPos);
                mra.get().set(val);
            }
            maps[c] = new DefaultDataset(ij.context(), new ImgPlus(map, "Map"));
        }
    }
    
}
