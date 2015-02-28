package edu.emory.cellbio.ijbat.pi;

import java.util.Arrays;
import net.imagej.overlay.AbstractOverlay;
import net.imglib2.iterator.IntervalIterator;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;


/**
 * Count overlapping pixels in two sets of ROIs.
 * <p> <b>Inputs</b>
 * <ul>
 * <li> {@code roia} - ROI set A
 * <li> {@code roib} - ROI set B
 * </ul>
 * <b>Results</b>
 * <ul>
 * <li> {@code overlap} - For each region in A, the number of pixels
 *      which are within the corresponding region in B.
 * <li> {@code aoutb} - For each region in A, the number of pixels outside
 *      the corresponding region in B.
 * <li> {@code bouta} - For each region in B, the number of pixels outside
 *      the corresponding region in A.
 * </ul>
 * <b>Notes</b>
 * <p> Individual ROIs from each set are compared in sequence. Thus, there
 * must be equal numbers of regions in A and B.
 * 
 * @author Benjamin Nanes
 */
@HelpPath(path = "plugins/regionoverlap.html")
@Plugin( type=SlideSetPlugin.class,
     name="Region Overlap",
     label="Region Overlap", visible = false,
     menuPath="Plugins > Slide Set > Commands > Segmentation > Region Overlap")
public class ROIOverlap extends SlideSetPlugin implements MultipleResults {

    // -- Parameters --
    
    @Parameter(label = "Region A", type = ItemIO.INPUT)
    private AbstractOverlay[] roia;
    
    @Parameter(label = "Region B", type = ItemIO.INPUT)
    private AbstractOverlay[] roib;
    
    @Parameter(label = "Overlap", type = ItemIO.OUTPUT)
    private double[] overlap;
    
    @Parameter(label = "A outside B", type = ItemIO.OUTPUT)
    private double[] aoutb;
    
    @Parameter(label = "B outside A", type = ItemIO.OUTPUT)
    private double[] bouta;
    
    // -- Run method --
    
    public void run() {
        if(roia == null || roia.length == 0 || roib == null || roib.length != roia.length) {
            overlap = new double[0];
            aoutb = new double[0];
            bouta = new double[0];
            return;
        }
        final int n = roia.length;
        overlap = new double[n];
        aoutb = new double[n];
        bouta = new double[n];
        Arrays.fill(overlap, 0);
        Arrays.fill(aoutb, 0);
        Arrays.fill(bouta, 0);
        for(int i = 0; i < n; i++) {
            double[] minDA = new double[roia[i].numDimensions()];
            double[] minDB = new double[roia[i].numDimensions()];
            long[] min = new long[roia[i].numDimensions()];
            double[] maxDA = new double[roia[i].numDimensions()];
            double[] maxDB = new double[roia[i].numDimensions()];
            long[] max = new long[roia[i].numDimensions()];
            roia[i].realMin(minDA);
            roia[i].realMax(maxDA);
            roib[i].realMin(minDB);
            roib[i].realMax(maxDB);
            for(int q = 0; q < minDA.length; q++) {
                min[q] = Math.round(Math.floor(Math.min(minDA[q], minDB[q])));
                max[q] = Math.round(Math.ceil(Math.max(maxDA[q], minDB[q])));
            }
            IntervalIterator ii = new IntervalIterator(min, max);
            ii.reset();
            double[] pos = new double[ii.numDimensions()];
            while(ii.hasNext()) {
                ii.fwd();
                ii.localize(pos);
                boolean ina = roia[i].getRegionOfInterest().contains(pos);
                boolean inb = roib[i].getRegionOfInterest().contains(pos);
                if(ina && inb) overlap[i]++;
                else if(ina) aoutb[i]++;
                else if(inb) bouta[i]++;
            }
        }
    }
}
