package org.nanes.slideset.ui;

import net.imagej.Data;
import net.imagej.display.DataView;
import net.imagej.display.DefaultImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.overlay.Overlay;
import java.util.ArrayList;

/**
 * Performance improvements to {@code DefaultImageDisplay}
 * 
 * @author Benjamin Nanes
 */
public class FastUpdateImageDisplay extends DefaultImageDisplay {
    
    /** Add {@code Data} to the display <em>without</em> rebuilding.
     *  The display must be rebuilt with a call to {@link #rebuildNow() rebuildNow()}
     *  after {@code Data} addition is complete. This method offers
     *  a significant performance improvement over repeated calls
     *  to {@link #add(java.lang.Object) Display.add()}. */
    public void addFast(Data data, ImageDisplayService ids) {
        DataView dataView;
        if (ids == null) {
            throw new IllegalStateException(
                "An ImageDisplayService is required to display Data objects");
        }
        dataView = ids.createDataView(data);
        if (dataView == null) {
			throw new IllegalArgumentException("Unable to create data view!");
		}
        add(dataView);
    }
    
    /** Remove all {@code Overlay}s from the display without
     *  rebuilding. This is faster than calls through
     *  the {@code OverlayService}, but there can be a performance
     *  penalty when the display is rebuilt after many {@code Overlay}s
     *  have been removed. */
    public void clearOverlaysFast() {
        ArrayList<DataView> overlays = new ArrayList<DataView>();
        for(DataView dv : this) {
            Data d = dv.getData();
            if(d instanceof Overlay)
                overlays.add(dv);
        }
        removeAll(overlays);
        for(DataView dv : overlays)
            dv.dispose();       
    }
    
    /** Trigger a call to {@link #rebuild() rebuild()}.*/
    public void rebuildNow() {
        rebuild();
    }
    
}
