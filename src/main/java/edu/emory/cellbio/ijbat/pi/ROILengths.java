package edu.emory.cellbio.ijbat.pi;

import edu.emory.cellbio.ijbat.ex.SlideSetException;
import edu.emory.cellbio.ijbat.ui.SlideSetLog;
import imagej.data.overlay.AbstractOverlay;
import imagej.data.overlay.EllipseOverlay;
import imagej.data.overlay.LineOverlay;
import imagej.data.overlay.PolygonOverlay;
import imagej.data.overlay.RectangleOverlay;
import net.imglib2.roi.EllipseRegionOfInterest;
import net.imglib2.roi.PolygonRegionOfInterest;
import net.imglib2.roi.RectangleRegionOfInterest;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 *
 * @author Benjamin Nanes
 */
@HelpPath(path = "plugins/length.html")
@Plugin(type=SlideSetPlugin.class,
        label="ROI Lengths", visible = false,
        menuPath="Plugins > Slide Set > Commands > ROI Lengths")
public class ROILengths extends SlideSetPlugin implements MultipleResults {

     // -- Fields --
     
     @Parameter(label = "Log", type = ItemIO.INPUT)
     private SlideSetLog log;
     
     @Parameter(label = "ROIs", type = ItemIO.INPUT)
     private AbstractOverlay[] overlay;
     
     @Parameter(label = "Length", type = ItemIO.OUTPUT)
     private double[] length;
     
     private int index = 0;
     
     // -- Methods --
     
     @Override
     public void run() {
          index++;
          if(overlay == null) {
               length = new double[0];
               return;
          }
          length = new double[overlay.length];
          for(int i=0; i<overlay.length; i++) {
               try {
                   length[i] = getRoiLenth(overlay[i]);
               } catch(SlideSetException e) {
                   length[i] = 0;
                   log.println("~~ Warning: ~~\n"
                            + e.getMessage()
                            + "\nin image #" + String.valueOf(index) + "\n"
                            + "Result #" + String.valueOf(i+1)
                            + " for this image \nhas been recorded as 0."
                            + "\n~~~~~~");
               }
          }
     }
     
     /**
      * Calculate the length, in pixels, of an {@code AbstractOverlay} border.
      * @throws SlideSetException The method has not been implemented
      *          for the given {@code Overlay} type.
      */
     public static double getRoiLenth(AbstractOverlay overlay) throws SlideSetException {
         if(overlay instanceof LineOverlay) {
             final LineOverlay lo = (LineOverlay) overlay;
             final int d = lo.numDimensions();
             final double[] p0 = new double[d];
             final double[] p1 = new double[d];
             lo.getLineStart(p0);
             lo.getLineEnd(p1);
             return Double.valueOf(
                   Math.sqrt(distSquared(p0, p1))).floatValue();
         }
         else if(overlay instanceof PolygonOverlay) {
             final PolygonOverlay po = (PolygonOverlay) overlay;
             final PolygonRegionOfInterest pri = po.getRegionOfInterest();
             final int d = pri.numDimensions();
             final int n = pri.getVertexCount();
             double ds = 0;
             double[] p0 = new double[d];
             double[] p1 = new double[d];
             for (int j = 0; j < n; j++) {
                 pri.getVertex(j).localize(p0);
                 pri.getVertex(j + 1 < n ? j + 1 : 0).localize(p1);
                 ds += Math.sqrt(distSquared(p0, p1));
             }
             return Double.valueOf(ds).floatValue();
         }
         else if(overlay instanceof EllipseOverlay) {
             final EllipseRegionOfInterest eroi
                   = (EllipseRegionOfInterest) overlay.getRegionOfInterest();
             final int d = eroi.numDimensions();
             double[] r = new double[d];
             eroi.getRadii(r);
             double a = -1, b = -1;
             boolean ndim = false;
             for (int j = 0; j < d; j++) {
                 if (r[j] > 0) {
                     if (a < 0) {
                         a = r[j];
                     } else if (b < 0) {
                         b = r[j];
                     } else {
                         ndim = true;
                     }
                 }
             }
             if(b < 0 || ndim) {
                 throw new SlideSetException(
                       "Cannot calculate length\nfor N-dimensional ellipse");
             }
             else {
                 double h = Math.pow(a - b, 2) / Math.pow(a + b, 2);
                 return Math.PI * (a + b)
                       * (1 + (3 * h) / (10 + Math.sqrt(4 - 3 * h)));
             }
         }
         else if(overlay instanceof RectangleOverlay) {
             final RectangleRegionOfInterest rroi
                   = (RectangleRegionOfInterest) overlay.getRegionOfInterest();
             if (rroi.numDimensions() != 2)
                 throw new SlideSetException("Cannot calculate length\nfor N-dimensional rectangle");
             else {
                 double[] extents = new double[2];
                 rroi.getExtent(extents);
                 return 2 * (extents[0] + extents[1]);
             }
         }
         throw new SlideSetException("Cannot calculate length of\n" + overlay.getClass().getSimpleName());
     }
     
     // -- Helper Methods --
     
     private static double distSquared(double[] a, double[] b) {
          if(a.length != b.length)
               throw new IllegalArgumentException("Points must have same number of dimensions to calculate distance.");
          double ds = 0;
          for(int d=0; d<a.length; d++)
               ds += Math.pow(a[d] - b[d], 2);
          return ds;
     }
     
}
