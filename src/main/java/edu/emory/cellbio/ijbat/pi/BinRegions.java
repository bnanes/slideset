package edu.emory.cellbio.ijbat.pi;

import net.imagej.overlay.AbstractOverlay;
import net.imagej.overlay.LineOverlay;
import net.imagej.overlay.PointOverlay;
import java.util.Arrays;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.roi.RegionOfInterest;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Sort one set of ROIs based on a second set of ROIs.
 * <p> <b>Inputs</b>
 * <ul>
 * <li> {@code rois} - Set of ROIs to sort
 * <li> {@code bins} - Set of ROIs defining sorting bins
 * </ul>
 * <b>Results</b>
 * <ul>
 * <li> {@code binIDs} - For each region in {@code rois},
 *      the index of the first region in {@code bins}
 *      in which it is contained, or {@code -1} if no
 *      bin contains the region.
 * <li> {@code imgIDs} - Index incremented each time the
 *      plugin is run
 * </ul>
 * <b>Notes</b>
 * <p> Point and line ROIs are defined as being within a
 * bin if their (end)points are within the bin ROI.
 * Other ROIs are defined as being within a bin if
 * each integer point within the ROI is also within
 * the bin ROI.
 * 
 * @author Benjamin Nanes
 */
@HelpPath(path = "plugins/binregions.html")
@Plugin( type=SlideSetPlugin.class,
     name="Bin Regions",
     label="Bin Regions", visible = false,
     menuPath="Plugins > Slide Set > Commands > Segmentation > Bin Regions")
public class BinRegions extends SlideSetPlugin implements MultipleResults {

    // -- Parameters --
    
    @Parameter(label = "Regions", type = ItemIO.INPUT)
    private AbstractOverlay[] rois;
    
    @Parameter(label = "Bins", type = ItemIO.INPUT)
    private AbstractOverlay[] bins;
    
    @Parameter(label = "Bin Index", type = ItemIO.OUTPUT)
    private int[] binIDs;
    
    @Parameter(label = "Row Index", type = ItemIO.OUTPUT)
    private int[] imgIDs;
    
    // -- Other fields --
    
    private int runCount = 0;
    
    // -- Run method --
    
    public void run() {
        if(rois == null || rois.length == 0) {
            binIDs = new int[0];
            imgIDs = new int[0];
            return;
        }
        binIDs = new int[rois.length];
        imgIDs = new int[rois.length];
        Arrays.fill(imgIDs, runCount);
        Arrays.fill(binIDs, -1);
        if(bins == null || bins.length == 0)
            return;
        for(int i = 0; i < rois.length; i++) {
            AbstractOverlay ao = rois[i];
            if(ao instanceof PointOverlay) {
                for(int j = 0; j < bins.length; j++) {
                    boolean in = false;
                    for(double[] p : ((PointOverlay) ao).getPoints()) {
                        in = bins[j].getRegionOfInterest().contains(p);
                        if(!in)
                            break;
                    }
                    if(in) {
                        binIDs[i] = j;
                        break;
                    }
                }
            } else if (ao instanceof LineOverlay) {
                double[] p0 = new double[ao.numDimensions()];
                double[] p1 = new double[ao.numDimensions()];
                ((LineOverlay) ao).getLineStart(p0);
                ((LineOverlay) ao).getLineEnd(p1);
                for(int j = 0; j < bins.length; j++) {
                    RegionOfInterest bin = bins[j].getRegionOfInterest();
                    boolean in = bin.contains(p0) && bin.contains(p1);
                    if(in) {
                        binIDs[i] = j;
                        break;
                    }
                }
            } else { // Assume that the ROI has area
                double[] minD = new double[ao.numDimensions()];
                long[] min = new long[ao.numDimensions()];
                double[] maxD = new double[ao.numDimensions()];
                long[] max = new long[ao.numDimensions()];
                double[] pos = new double[ao.numDimensions()];
                ao.realMin(minD);
                ao.realMax(maxD);
                for(int q = 0; q < minD.length; q++) {
                    min[q] = Math.round(Math.floor(minD[q]));
                    max[q] = Math.round(Math.ceil(maxD[q]));
                }
                IntervalIterator ii = new IntervalIterator(min, max);
                for(int j = 0; j < bins.length; j++) {
                    RegionOfInterest bin = bins[j].getRegionOfInterest();
                    if(bin.numDimensions() != ii.numDimensions())
                        continue; // ROI and bin dimensions must match
                    ii.reset();
                    boolean empty = true;
                    boolean in = true;
                    while(in && ii.hasNext()) {
                        ii.fwd();
                        ii.localize(pos);
                        if(!ao.getRegionOfInterest().contains(pos))
                            continue;
                        empty = false;
                        in = bin.contains(pos);
                    }
                    if(in && !empty) {
                        binIDs[i] = j;
                        break;
                    }
                }
            }
        }
        runCount++;
    }
    
}
