package org.nanes.slideset.pi;

import net.imagej.Dataset;
import net.imagej.overlay.AbstractOverlay;
import java.util.ArrayList;
import java.util.Arrays;
import net.imglib2.RandomAccess;
import net.imglib2.iterator.IntervalIterator;
import net.imagej.axis.Axes;
import net.imglib2.roi.RegionOfInterest;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.Parameter;
import org.scijava.ItemIO;

/**
 *
 * @author Benjamin Nanes
 */
@HelpPath(path = "plugins/correlation.html")
@Plugin(type=SlideSetPlugin.class,
        label="Pearson's Correlation", visible = false,
        menuPath="Plugins > Slide Set > Commands > Pearson's Correlation")
public class Correlation extends SlideSetPlugin implements MultipleResults {
     
     @Parameter(label="Image", type=ItemIO.INPUT)
     private Dataset ds;
     
     @Parameter(label="Region of Interest", type=ItemIO.INPUT)
     private AbstractOverlay[] roi;
     
     @Parameter(label="Channel 1 (1-based index)", type=ItemIO.INPUT)
     private int c1;
     
     @Parameter(label="Channel 2 (1-based index)", type=ItemIO.INPUT)
     private int c2;
     
     @Parameter(label="R", type=ItemIO.OUTPUT)
     private float r[];
     
     @Override
     public void run() {
          c1--; // Convert to 0-based index for code consistency
          c2--;
          r = new float[0];
          if(roi == null || ds == null)
               return;
          final int n = roi.length;
          if(n == 0)
               return;
          final long[] dims = new long[ds.numDimensions()];
          ds.dimensions(dims);
          final int cAxis = ds.dimensionIndex(Axes.CHANNEL);
          if(cAxis < 0)
               throw new IllegalArgumentException("Could not get channel axis index for " + ds.getName());
          final long nc = dims[cAxis];
          if(nc <= Math.max(c1, c2))
               throw new IllegalArgumentException(ds.getName() 
                       + " does not have enough channels. It has " 
                       + String.valueOf(nc) + ", but " 
                       + String.valueOf(Math.max(c1, c2)) 
                       + " are required based on your input.");
          final RegionOfInterest[] roi2 = new RegionOfInterest[n];
          for(int i=0; i<n; i++)
               if(roi[i] != null)
                    roi2[i] = roi[i].getRegionOfInterest();
          dims[cAxis] = 1;
          final IntervalIterator ii = new IntervalIterator(dims);
          final long[] pos = new long[ii.numDimensions()];
          final double[] posD = new double[ii.numDimensions()];
          final RandomAccess<? extends RealType<?>> ra = ds.randomAccess();
          final Object[] v1 = new Object[n];
          final Object[] v2 = new Object[n];
          for(int i=0; i<n; i++) {
               v1[i] = new ArrayList<Float>();
               v2[i] = new ArrayList<Float>();
          }
          while(ii.hasNext()) {
               ii.fwd();
               ii.localize(pos);
               for(int i=0; i<n; i++) {
                    for(int j=0; j<pos.length; j++)
                         posD[j] = pos[j];
                    if(roi2[i] != null && roi2[i].contains(posD)) {
                         ra.setPosition(pos);
                         ra.setPosition(c1, cAxis);
                         ((ArrayList<Float>) v1[i]).add(
                                 ra.get().getRealFloat());
                         ra.setPosition(c2, cAxis);
                         ((ArrayList<Float>) v2[i]).add(
                                 ra.get().getRealFloat());
                    }
               }
          }
          r = new float[n];
          Arrays.fill(r, 0);
          for(int i=0; i<n; i++) {
               final Float[] f1 = 
                       ((ArrayList<Float>) v1[i]).toArray(new Float[1]);
               final Float[] f2 = 
                       ((ArrayList<Float>) v2[i]).toArray(new Float[1]);
               if(f1.length != f2.length)
                    throw new IllegalArgumentException("Unequal number of values from each channel");
               if(f1.length == 0 || f1[0] == null)
                    continue;
               final double cor = correlate(f1, f2);
               r[i] = (new Double(cor)).floatValue();
          }
     }
     
     public final double correlate(Float[] a, Float[] b) {
          final double n = a.length + 1;
          double meana = 0;
          double meanb = 0;
          for(int i=0; i<a.length; i++) {
               meana += a[i];
               meanb += b[i];
          }
          meana = meana / a.length;
          meanb = meanb / b.length;
          double num = 0;
          double dena = 0;
          double denb = 0;
          for(int i=0; i<a.length; i++) {
               num += (a[i] - meana) * (b[i] - meanb);
               dena += Math.pow(a[i] - meana, 2);
               denb += Math.pow(b[i] - meanb, 2);
          }
          return num / (Math.sqrt(dena * denb));
     }
     
}
