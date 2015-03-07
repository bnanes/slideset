package edu.emory.cellbio.ijbat.pi;

import edu.emory.cellbio.ijbat.ex.SlideSetException;
import edu.emory.cellbio.ijbat.ui.SlideSetLog;
import net.imagej.Dataset;
import net.imagej.overlay.AbstractOverlay;
import java.math.BigInteger;
import java.util.ArrayList;
import net.imglib2.RandomAccess;
import net.imglib2.iterator.IntervalIterator;
import net.imagej.axis.Axes;
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
@HelpPath(path = "plugins/borders.html")
@Plugin(type=SlideSetPlugin.class,
    label="Border Statistics", visible = false,
    menuPath = "Plugins > Slide Set > Commands > Border Statistics")
public class BorderStats extends SlideSetPlugin implements MultipleResults {
    
    @Parameter(label="Log", type=ItemIO.INPUT)
    private SlideSetLog log;
    
    @Parameter(label = "Image", type = ItemIO.INPUT)
    private Dataset ds;

    @Parameter(label = "Region of Interest", type = ItemIO.INPUT)
    private AbstractOverlay[] roi;
    
    @Parameter(label = "Border Width", type = ItemIO.INPUT)
    private double w;

    @Parameter(label = "Red Channel Threshold", type = ItemIO.INPUT)
    private double rT;

    @Parameter(label = "Green Channel Threshold", type = ItemIO.INPUT)
    private double gT;

    @Parameter(label = "Blue Channel Threshold", type = ItemIO.INPUT)
    private double bT;

    @Parameter(label = "Invert", type = ItemIO.INPUT)
    private boolean inv;

    @Parameter(label = "Red Average", type = ItemIO.OUTPUT)
    private double[] red;

    @Parameter(label = "Green Average", type = ItemIO.OUTPUT)
    private double[] green;

    @Parameter(label = "Blue Average", type = ItemIO.OUTPUT)
    private double[] blue;
    
    @Parameter(label = "Length", type = ItemIO.OUTPUT)
    private double[] length;

    @Parameter(label = "Border Stripe Pixels", type = ItemIO.OUTPUT)
    private int[] size;

    public void run() {

        if (roi == null || ds == null || roi.length == 0) {
            red = new double[0];
            green = new double[0];
            blue = new double[0];
            length = new double[0];
            size = new int[0];
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
            if (dims[cAxis] > 3) {
                throw new IllegalArgumentException(ds.getName() + " has too many channels");
            }
            if (dims[cAxis] == 0) {
                throw new IllegalArgumentException(ds.getName() + " has zero channels");
            }
        }
        IntervalIterator ii;
        final double[] posD = new double[dims.length];
        final double[] pos2D = new double[2];
        final long[] min = new long[dims.length];
        final long[] max = new long[dims.length];
        RandomAccess<? extends RealType<?>> ra = ds.randomAccess();
        RegionOfInterest bin;
        BigInteger r;
        BigInteger g = BigInteger.valueOf(0);
        BigInteger b = BigInteger.valueOf(0);
        int s;
        ArrayList<Double> rArray = new ArrayList<Double>();
        ArrayList<Double> gArray = new ArrayList<Double>();
        ArrayList<Double> bArray = new ArrayList<Double>();
        ArrayList<Integer> sArray = new ArrayList<Integer>();
        ArrayList<Double> lArray = new ArrayList<Double>();
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
            r = BigInteger.valueOf(0);
            if (!singlet && dims[cAxis] > 1) {
                g = BigInteger.valueOf(0);
            }
            if (!singlet && dims[cAxis] > 2) {
                b = BigInteger.valueOf(0);
            }
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
                    if (inv) {
                        r = r.subtract(BigInteger.valueOf(Math.round(Math.min(ra.get().getRealDouble() - rT, 0))));
                    } else {
                        r = r.add(BigInteger.valueOf(Math.round(Math.max(ra.get().getRealDouble() - rT, 0))));
                    }
                    if (singlet || dims[cAxis] < 2) {
                        continue;
                    }
                    ra.setPosition(1, cAxis);
                    if (inv) {
                        g = g.subtract(BigInteger.valueOf(Math.round(Math.min(ra.get().getRealDouble() - gT, 0))));
                    } else {
                        g = g.add(BigInteger.valueOf(Math.round(Math.max(ra.get().getRealDouble() - gT, 0))));
                    }
                    if (dims[cAxis] < 3) {
                        continue;
                    }
                    ra.setPosition(2, cAxis);
                    if (inv) {
                        b = b.subtract(BigInteger.valueOf(Math.round(Math.min(ra.get().getRealDouble() - bT, 0))));
                    } else {
                        b = b.add(BigInteger.valueOf(Math.round(Math.max(ra.get().getRealDouble() - bT, 0))));
                    }
                }
            } catch(SlideSetException e) {
                log.println("Warning: ROI number " + String.valueOf(i) + " is not compatible");
                log.println("   with this command and will be skipped.");
                continue;
            }
            sArray.add(s);
            rArray.add(r.doubleValue() / s);
            gArray.add(g.doubleValue() / s);
            bArray.add(b.doubleValue() / s);
            double l;
            try {
                l = ROILengths.getRoiLenth(roi[i]);
            } catch(SlideSetException e) {
                log.println("Warning: " + e.getMessage());
                l = 0;
            }
            lArray.add(l);
        }
        int n = rArray.size();
        red = new double[n];
        green = new double[n];
        blue = new double[n];
        size = new int[n];
        length = new double[n];
        for(int i = 0; i < n; i++) {
            red[i] = rArray.get(i);
            green[i] = gArray.get(i);
            blue[i] = bArray.get(i);
            size[i] = sArray.get(i);
            length[i] = lArray.get(i);
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
