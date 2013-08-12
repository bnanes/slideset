package edu.emory.cellbio.ijbat.pi;

import imagej.data.Dataset;
import imagej.data.overlay.AbstractOverlay;
import java.util.ArrayList;
import java.util.Arrays;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.meta.Axes;
import net.imglib2.roi.RegionOfInterest;
import net.imglib2.type.numeric.RealType;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.Parameter;
import org.scijava.ItemIO;

/**
 *
 * @author Benjamin Nanes
 */
@Plugin(type=SlideSetPlugin.class, label="Pearson's Correlation")
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
          if(roi == null || ds == null)
               return;
          final int n = roi.length;
          if(n == 0)
               return;
          final ImgPlus<? extends RealType<?>> imp = ds.getImgPlus();
          final Img<? extends RealType<?>> img = imp.getImg();
          final long[] dims = new long[imp.numDimensions()];
          imp.dimensions(dims);
          final int cAxis = ds.getAxisIndex(Axes.CHANNEL);
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
          final RandomAccess<? extends RealType<?>> ra = img.randomAccess();
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
          final PearsonsCorrelation pc = new PearsonsCorrelation();
          r = new float[n];
          Arrays.fill(r, 0);
          for(int i=0; i<n; i++) {
               final Float[] f1 = 
                       ((ArrayList<Float>) v1[i]).toArray(new Float[1]);
               final Float[] f2 = 
                       ((ArrayList<Float>) v2[i]).toArray(new Float[1]);
               if(f1.length != f2.length)
                    throw new IllegalArgumentException("Unequal number of values from each channel");
               if(f1.length == 0)
                    continue;
               final double[] d1 = new double[f1.length];
               final double[] d2 = new double[f2.length];
               for(int j=0; j<f1.length; j++) {
                    d1[j] = f1[j].doubleValue();
                    d2[j] = f2[j].doubleValue();
               }
               final double cor = pc.correlation(d1, d2);
               r[i] = (new Double(cor)).floatValue();
          }
     }
     
}
