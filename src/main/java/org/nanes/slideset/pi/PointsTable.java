package org.nanes.slideset.pi;

import org.nanes.slideset.ui.SlideSetLog;
import ij.gui.PointRoi;
import ij.gui.Roi;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Extract coordinates from <code>PointRoi</code>s into a table
 * @author Benjamin Nanes
 */
@HelpPath(path = "plugins/points.html")
@Plugin( type=SlideSetPlugin.class,
         name="Create Points Table",
         label="Create Points Table", visible = false,
         menuPath="Plugins > Slide Set > Commands > Create Points Table")
public class PointsTable extends SlideSetPlugin implements MultipleResults {
    
    @Parameter(label = "Context", type = ItemIO.INPUT)
    private Context context;
    
    @Parameter(label = "Log", type = ItemIO.INPUT)
    private SlideSetLog log;
    
    @Parameter(label = "Points", type = ItemIO.INPUT)
    private Roi[] roiPoints;
    
    @Parameter(label = "X", type = ItemIO.OUTPUT)
    private double x[];

    @Parameter(label = "Y", type = ItemIO.OUTPUT)
    private double y[];

    @Override
    public void run() {
        ArrayList<Double> xl = new ArrayList(100);
        ArrayList<Double> yl = new ArrayList(100);
        for (Roi roiPoint : roiPoints) {
            if(!(roiPoint instanceof PointRoi))
                continue;
            Iterator<Point> pi = ((PointRoi)roiPoint).iterator();
            while(pi.hasNext()) {
                Point p = pi.next();
                xl.add(new Double(p.x));
                yl.add(new Double(p.y));
            }
        }
        x = new double[xl.size()];
        y = new double[yl.size()];
        for(int i=0; i<xl.size(); i++) {
            x[i] = xl.get(i);
            y[i] = yl.get(i);
        }
    }
    
}
