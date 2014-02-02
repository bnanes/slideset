package edu.emory.cellbio.ijbat.pi;

import imagej.data.overlay.AbstractOverlay;
import java.util.ArrayList;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.roi.RegionOfInterest;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Filter one set of ROIs using another set of ROIs as a mask
 * 
 * @author Benjamin Nanes
 */
@Plugin( type=SlideSetPlugin.class,
     name="Filter Regions",
     label="Filter Regions", visible = false,
     menuPath="Plugins > Slide Set > Commands > Segmentation > Filter Regions")
public class FilterRegions extends SlideSetPlugin {
    
    // -- Parameters --
    
    @Parameter(label = "Regions", type = ItemIO.INPUT)
    private AbstractOverlay[] regions;
    
    @Parameter(label = "Mask", type = ItemIO.INPUT)
    private AbstractOverlay[] mask;
    
    @Parameter(label = "Filtered regions", type = ItemIO.OUTPUT)
    private AbstractOverlay[] filtered;
    
    // -- Other fields --
    
    private ArrayList<AbstractOverlay> keep;
    
    // -- Plugin run method --
    
    public void run() {
        if(regions == null || mask == null) {
            filtered = new AbstractOverlay[0];
            return;
        }
        keep = new ArrayList<AbstractOverlay>();
        for(AbstractOverlay ao : regions) {
            RegionOfInterest roi = ao.getRegionOfInterest();
            int nDims = roi.numDimensions();
            double[] minD = new double[nDims];
            long[] min = new long[nDims];
            double[] maxD = new double[nDims];
            long[] max = new long[nDims];
            double[] pos = new double[nDims];
            roi.realMin(minD);
            roi.realMax(maxD);
            for(int i = 0; i < nDims; i++) {
                min[i] = Math.round(Math.floor(minD[i]));
                max[i] = Math.round(Math.ceil(maxD[i]));
            }
            IntervalIterator ii = new IntervalIterator(min, max);
            boolean included = false;
            while(ii.hasNext()) {
                ii.fwd();
                ii.localize(pos);
                if(!roi.contains(pos))
                    continue;
                included = false;
                for(AbstractOverlay m : mask ) {
                    if(m.getRegionOfInterest().contains(pos)) {
                        included = true;
                        break;
                    }
                }
                if(!included)
                    break;
            }
            if(included)
                keep.add(ao);
        }
        filtered = keep.toArray(new AbstractOverlay[0]);
    }
    
}
