package edu.emory.cellbio.ijbat.pi;

import edu.emory.cellbio.ijbat.ui.SlideSetLog;
import imagej.data.Dataset;
import imagej.data.overlay.AbstractOverlay;
import imagej.data.overlay.LineOverlay;
import imagej.data.overlay.PolygonOverlay;
import net.imglib2.roi.PolygonRegionOfInterest;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 *
 * @author Benjamin Nanes
 */
@Plugin(type=SlideSetPlugin.class, label="ROI Lengths")
public class ROILengths extends SlideSetPlugin implements MultipleResults {

     // -- Fields --
     
     @Parameter(label = "Log", type = ItemIO.INPUT)
     private SlideSetLog log;
     
     @Parameter(label = "Image", type = ItemIO.INPUT)
     private Dataset image;
     
     @Parameter(label = "ROIs", type = ItemIO.INPUT)
     private AbstractOverlay[] overlay;
     
     @Parameter(label = "Length", type = ItemIO.OUTPUT)
     private float[] length;
     
     // -- Methods --
     
     @Override
     public void run() {
          
          if(overlay == null) {
               length = new float[0];
               return;
          }
          
          length = new float[overlay.length];
          for(int i=0; i<overlay.length; i++) {
               
               if(overlay[i] instanceof LineOverlay) {
                    final LineOverlay lo = (LineOverlay)overlay[i];
                    final int d = lo.numDimensions();
                    final double[] p0 = new double[d];
                    final double[] p1 = new double[d];
                    lo.getLineStart(p0);
                    lo.getLineEnd(p1);
                    length[i] = Double.valueOf(
                            Math.sqrt(distSquared(p0, p1))).floatValue();
               }
               
               else if(overlay[i] instanceof PolygonOverlay) {
                    final PolygonOverlay po = (PolygonOverlay) overlay[i];
                    final PolygonRegionOfInterest pri = po.getRegionOfInterest();
                    final int d = pri.numDimensions();
                    final int n = pri.getVertexCount();
                    double ds = 0;
                    for(int j=0; j<n; j++) {
                         final double[] p0 = new double[d];
                         final double[] p1 = new double[d];
                         pri.getVertex(j).localize(p0);
                         pri.getVertex(j+1 < n ? j+1 : 0).localize(p1);
                         ds += distSquared(p0, p1);
                    }
                    length[i] = Double.valueOf(Math.sqrt(ds)).floatValue();
               }
               
               else {
                    length[i] = 0;
                    log.println("~~ Warning: ~~\n"
                            + "Cannot calculate length for \noverlay type " 
                            + overlay[i].getClass().getSimpleName()
                            + "\nin image " + image.getName() + "\n"
                            + "Result #" + String.valueOf(i)
                            + " for this image \nhas been recorded as 0."
                            + "\n~~~~~~");
               }
               
          }
          
     }
     
     // -- Helper Methods --
     
     private double distSquared(double[] a, double[] b) {
          if(a.length != b.length)
               throw new IllegalArgumentException("Points must have same number of dimensions to calculate distance.");
          double ds = 0;
          for(int d=0; d<a.length; d++)
               ds += Math.pow(a[d] - b[d], 2);
          return ds;
     }
     
}
