package edu.emory.cellbio.ijbat.pi;

import imagej.data.Dataset;
import imagej.data.overlay.AbstractOverlay;
import java.util.Arrays;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus; // This will get moved to net.imglib2.meta
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.meta.Axes;
import net.imglib2.roi.RegionOfInterest;
import net.imglib2.type.numeric.RealType;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * SlideSet command for measuring Manders' Colocalization Coefficients
 * within regions of interest.
 * 
 * Reference:
 * Dunn KW, Kamocka MM, McDonald JH. A practical guide
 * to evaluating colocalization in biological microscopy. 
 * Am J Physiol Cell Physiol. 2011 Apr;300(4):C723-42. 
 * doi: 10.1152/ajpcell.00462.2010
 * 
 * @author Benjamin Nanes
 */
@Plugin(type=SlideSetPlugin.class, label="Manders' Coloc. Coefficients")
public final class Manders extends SlideSetPlugin implements MultipleResults {
     
     @Parameter(label="Image", type=ItemIO.INPUT)
     private Dataset ds;
     
     @Parameter(label="Region of Interest", type=ItemIO.INPUT)
     private AbstractOverlay[] roi;
     
     @Parameter(label="Channel 1 (1-based index)", type=ItemIO.INPUT)
     private int c1;
     
     @Parameter(label="Channel 2 (1-based index)", type=ItemIO.INPUT)
     private int c2;
     
     @Parameter(label="Channel 1 Threshold", type=ItemIO.INPUT)
     private int t1;
     
     @Parameter(label="Channel 2 Threshold", type=ItemIO.INPUT)
     private int t2;
     
     @Parameter(label="Signal-weighted", type=ItemIO.INPUT)
     private boolean w;
     
     @Parameter(label="M1", type=ItemIO.OUTPUT)
     private float m1[];
     
     @Parameter(label="M2", type=ItemIO.OUTPUT)
     private float m2[];
     
     @Override
     public void run() {
          c1--; // Convert to 0-based index for code consistency
          c2--;
          m1 = new float[0];
          m2 = new float[0];
          if(roi == null || ds == null)
               return;
          final int n = roi.length;
          if(n == 0)
               return;
          final ImgPlus<? extends RealType<?>> imp = ds.getImgPlus();
          final Img<? extends RealType<?>> img = imp.getImg();
          final long[] dims = new long[imp.numDimensions()];
          imp.dimensions(dims);
          final int cAxis = ds.getAxisIndex(Axes.CHANNEL);  // This will change to dimensionIndex()
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
          final float[] col1 = new float[n];
          final float[] tot1 = new float[n];
          final float[] col2 = new float[n];
          final float[] tot2 = new float[n];
          Arrays.fill(col1, 0);
          Arrays.fill(tot1, 0);
          Arrays.fill(col2, 0);
          Arrays.fill(tot2, 0);
          while(ii.hasNext()) {
               ii.fwd();
               ii.localize(pos);
               for(int i=0; i<n; i++) {
                    for(int j=0; j<pos.length; j++)
                         posD[j] = pos[j];
                    if(roi2[i] != null && roi2[i].contains(posD)) {
                         ra.setPosition(pos);
                         ra.setPosition(c1, cAxis);
                         final float p1 = ra.get().getRealFloat();
                         ra.setPosition(c2, cAxis);
                         final float p2 = ra.get().getRealFloat();
                         if(p1 > t1 && p2 > t2) {
                              col1[i] += w ? Math.max(p1 - t1, 0) : 1;
                              col2[i] += w ? Math.max(p2 - t2, 0) : 1;
                         }
                         if(p1 > t1)
                              tot1[i] += w ? Math.max(p1 - t1, 0) : 1;
                         if(p2 > t2)
                              tot2[i] += w ? Math.max(p2 - t2, 0) : 1;
                    }
               }
          }
          m1 = new float[n];
          m2 = new float[n];
          Arrays.fill(m1, 0);
          Arrays.fill(m2, 0);
          for(int i=0; i<n; i++) {
               if(tot1[i] > 0)
                    m1[i] = col1[i] / tot1[i];
               if(tot2[i] > 0)
                    m2[i] = col2[i] / tot2[i];
          }
     }
     
}
