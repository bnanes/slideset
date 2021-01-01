package org.nanes.slideset.pi;

import org.nanes.slideset.ex.SlideSetException;
import org.nanes.slideset.ui.SlideSetLog;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.overlay.AbstractOverlay;
import net.imglib2.RandomAccess;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.roi.RegionOfInterest;
import net.imglib2.type.numeric.RealType;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Calculate average values for pixels
 * along region of interest borders
 * 
 * @author Benjamin Nanes
 */
@HelpPath(path = "plugins/borders.html#nchan")
@Plugin(type=SlideSetPlugin.class,
    label="Border Stats (multi-chan)", visible = false,
    menuPath = "Plugins > Slide Set > Commands > Regions > Border Stats (multi-chan)")
public class BorderStatsNChan extends SlideSetPlugin implements MultipleResults {
    
    @Parameter(label="Log", type=ItemIO.INPUT)
    private SlideSetLog log;
    
    @Parameter(label = "Image", type = ItemIO.INPUT)
    private Dataset ds;

    @Parameter(label = "Region of Interest", type = ItemIO.INPUT)
    private AbstractOverlay[] roi;
    
    @Parameter(label = "Border Width", type = ItemIO.INPUT)
    private double w;
    
    @Parameter(label = "Thresholds", type = ItemIO.INPUT)
    private String ts;

    @Parameter(label = "Invert", type = ItemIO.INPUT)
    private boolean inv;
    
    @Parameter(label = "Channel", type = ItemIO.OUTPUT)
    private int[] chans;
    
    @Parameter(label = "Mean", type = ItemIO.OUTPUT)
    private double[] means;
    
    @Parameter(label = "Length", type = ItemIO.OUTPUT)
    private double[] length;

    @Parameter(label = "Border Stripe Pixels", type = ItemIO.OUTPUT)
    private double[] size;

    public void run() {

        if (roi == null || ds == null || roi.length == 0) {
            chans = new int[0];
            means = new double[0];
            length = new double[0];
            size = new double[0];
            return;
        }
        final long[] dims = new long[ds.numDimensions()];
        final boolean singlet = dims.length == 2;
        ds.dimensions(dims);
        final int cAxis = ds.dimensionIndex(Axes.CHANNEL);
        final int xAxis = ds.dimensionIndex(Axes.X);
        final int yAxis = ds.dimensionIndex(Axes.Y);
        if (!singlet) {
            if (xAxis < 0 || yAxis < 0)
                throw new IllegalArgumentException("Could not get X and Y axes for " + ds.getName());
            if (cAxis < 0) {
                throw new IllegalArgumentException("Could not get channel axis index for " + ds.getName());
            }
            if (dims[cAxis] == 0) {
                throw new IllegalArgumentException(ds.getName() + " has zero channels");
            }
        }
        final int nChan = singlet ? 1 : (int)dims[cAxis];
        IntervalIterator ii;
        final double[] posD = new double[dims.length];
        final double[] pos2D = new double[2];
        final long[] min = new long[dims.length];
        final long[] max = new long[dims.length];
        RandomAccess<? extends RealType<?>> ra = ds.randomAccess();
        ArrayList<Double> vArray = new ArrayList<Double>();
        ArrayList<Double> sArray = new ArrayList<Double>();
        ArrayList<Double> lArray = new ArrayList<Double>();
        ArrayList<Integer> cArray = new ArrayList<Integer>();
        //Parse threshold string
        final double[] nts = new double[nChan];
        Arrays.fill(nts, 0);
        String[] tss = ts.split("\\s");
        if(tss.length > 0) {
            int j=0;
            for(int i=0; i < nts.length; i++) {
                if(j >= tss.length)
                    j = 0;
                try {
                    nts[i] = new Double(tss[j]);
                } catch(NumberFormatException e) { }
                j++;
            }
        }
        RegionOfInterest bin;
        BigInteger[] v = new BigInteger[nChan];
        double s;
        //
        for (int i = 0; i < roi.length; i++) {
            bin = roi[i].getRegionOfInterest();
            for (int c = 0; c < bin.numDimensions(); c++) {
                min[c] = Math.round(Math.floor(bin.realMin(c) - w));
                if(min[c] < 0 || min[c] > dims[c])
                    min[c] = 0;
                max[c] = Math.round(Math.ceil(bin.realMax(c) + w));
                if(max[c] < 0 || max[c] > dims[c])
                    max[c] = dims[c];
            }
            if (!singlet) {
                min[cAxis] = 0;
                max[cAxis] = 0;
            }
            s = 0;
            Arrays.fill(v, BigInteger.valueOf(0));
            ii = new IntervalIterator(min, max);
            try {
                while (ii.hasNext()) {
                    ii.fwd();
                    ii.localize(posD);
                    pos2D[0] = posD[xAxis];
                    pos2D[1] = posD[yAxis];
                    if(!RoiUtils.isNearBorder(pos2D, roi[i], w) || !inBounds(posD, dims))
                        continue;
                    ra.setPosition(ii);
                    ++s;
                    for(int j=0; j<nChan; j++) {
                        if(!singlet)
                            ra.setPosition(j, cAxis);
                        if (inv) {
                            v[j] = v[j].subtract(BigInteger.valueOf(Math.round(Math.min(ra.get().getRealDouble() - nts[j], 0))));
                        } else {
                            v[j] = v[j].add(BigInteger.valueOf(Math.round(Math.max(ra.get().getRealDouble() - nts[j], 0))));
                        }
                    }
                }
            } catch(SlideSetException e) {
                log.println("Warning: ROI number " + String.valueOf(i) + " is not compatible");
                log.println("   with this command and will be skipped.");
                continue;
            }
            double l;
            try {
                l = ROILengths.getRoiLenth(roi[i]);
            } catch(SlideSetException e) {
                log.println("Warning: " + e.getMessage());
                l = 0;
            }
            for(int j=0; j<nChan; j++) {
                sArray.add(s);
                vArray.add(v[j].doubleValue() / s);
                lArray.add(l);
                cArray.add(j);
            }
        }
        int n = vArray.size();
        means = new double[n];
        size = new double[n];
        length = new double[n];
        chans = new int[n];
        for(int i = 0; i < n; i++) {
            means[i] = vArray.get(i);
            size[i] = sArray.get(i);
            length[i] = lArray.get(i);
            chans[i] = cArray.get(i);
        }
    }
    
    /**
     * Check if a point is within the image dimensions
     */
    private boolean inBounds(double[] pos, long[] dims) {
        for(int i = 0; i < pos.length; i++)
            if(pos[i] >= dims[i] || pos[i] < 0)
                return false;
        return true;
    }
    
}
