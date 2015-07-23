package edu.emory.cellbio.ijbat.pi;

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
 *
 * @author Benjamin Nanes
 */
@HelpPath(path = "plugins/regions.html#nchan")
@Plugin(type=SlideSetPlugin.class,
        label="Region Stats (multi-chan)", visible = false,
        menuPath = "Plugins > Slide Set > Commands > Region Stats (multi-chan)")
public class RegionStatsNChan extends SlideSetPlugin implements MultipleResults  {
    
    @Parameter(label = "Image", type = ItemIO.INPUT)
    private Dataset ds;

    @Parameter(label = "Region of Interest", type = ItemIO.INPUT)
    private AbstractOverlay[] roi;

    @Parameter(label = "Thresholds", type = ItemIO.INPUT)
    private String ts;

    @Parameter(label = "Invert", type = ItemIO.INPUT)
    private boolean inv;

    @Parameter(label = "Mean", type = ItemIO.OUTPUT)
    private double means[];

    @Parameter(label = "Channel", type = ItemIO.OUTPUT)
    private int chans[];

    @Parameter(label = "Area", type = ItemIO.OUTPUT)
    private double size[];
    
    @Override
    public void run() {

        if (roi == null || ds == null) {
            means = new double[0];
            chans = new int[0];
            size = new double[0];
            return;
        }
        final int n = roi.length;
        if (n == 0) {
            means = new double[0];
            chans = new int[0];
            size = new double[0];
            return;
        }
        final long[] dims = new long[ds.numDimensions()];
        long nc = dims.length - 1;
        final boolean singlet = nc == 1;
        ds.dimensions(dims);
        final int cAxis = ds.dimensionIndex(Axes.CHANNEL);
        if (!singlet) {
            if (cAxis < 0) {
                throw new IllegalArgumentException("Could not get channel axis index for " + ds.getName());
            }
            nc = dims[cAxis];
            if (nc == 0) {
                throw new IllegalArgumentException(ds.getName() + " has zero channels");
            }
        }
        else
            nc = 1;
        final int nRes = singlet ? n : (int)nc*n;
        //Parse threshold string
        final double[] nts = new double[(int)nc];
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
        //
        means = new double[nRes];
        chans = new int[nRes];
        size = new double[nRes];
        Arrays.fill(means, 0);
        Arrays.fill(chans, 0);
        Arrays.fill(size, 0);
        IntervalIterator ii;
        final double[] posD = new double[dims.length];
        final long[] min = new long[dims.length];
        final long[] max = new long[dims.length];
        RandomAccess<? extends RealType<?>> ra = ds.randomAccess();
        RegionOfInterest bin;
        int count = 0;
        for (int i = 0; i < n; i++) {
            bin = roi[i].getRegionOfInterest();
            for (int c = 0; c < bin.numDimensions(); c++) {
                min[c] = Math.round(Math.floor(bin.realMin(c)));
                if (min[c] < 0 || min[c] > dims[c]) {
                    min[c] = 0;
                }
                max[c] = Math.round(Math.ceil(bin.realMax(c)));
                if (max[c] < 0 || max[c] > dims[c]) {
                    max[c] = dims[c];
                }
            }
            if (!singlet) {
                min[cAxis] = 0;
                max[cAxis] = 0;
            }
            ii = new IntervalIterator(min, max);
            while (ii.hasNext()) {
                ii.fwd();
                ii.localize(posD);
                if (!bin.contains(posD) || !inBounds(posD, dims)) {
                    continue;
                }
                ra.setPosition(ii);
                for(int j=0; j<nc; j++) {
                    if(nc > 1)
                        ra.setPosition(j, cAxis);
                    if(inv)
                        means[count+j] -= Math.min(ra.get().getRealDouble() - nts[j], 0);
                    else
                        means[count+j] += Math.max(ra.get().getRealDouble() - nts[j], 0);
                    size[count+j]++;
                    chans[count+j] = j;
                }
            }
            count += nc;
        }
        for(int i=0; i<means.length; i++) {
            means[i] = means[i] / size[i];
        }
    }

    private boolean inBounds(double[] pos, long[] dims) {
        for (int i = 0; i < pos.length; i++) {
            if (pos[i] >= dims[i]) {
                return false;
            }
        }
        return true;
    }

}
