package edu.emory.cellbio.ijbat.pi;

import imagej.data.Dataset;
import imagej.data.overlay.AbstractOverlay;
import java.util.Arrays;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.meta.Axes;
import net.imglib2.roi.RegionOfInterest;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.Parameter;
import org.scijava.ItemIO;

/**
 *
 * @author Benjamin Nanes
 */
@Plugin(type=SlideSetPlugin.class, label="Region Statistics (RGB)")
public class RegionStats extends SlideSetPlugin implements MultipleResults {
     
     @Parameter(label="Image", type=ItemIO.INPUT)
     private Dataset ds;
     
     @Parameter(label="Region of Interest", type=ItemIO.INPUT)
     private AbstractOverlay[] roi;
     
     @Parameter(label="Red Channel Threshold", type=ItemIO.INPUT)
     private int rT;
     
     @Parameter(label="Green Channel Threshold", type=ItemIO.INPUT)
     private int gT;
     
     @Parameter(label="Blue Channel Threshold", type=ItemIO.INPUT)
     private int bT;
     
     @Parameter(label="Red Total", type=ItemIO.OUTPUT)
     private int red[];
     
     @Parameter(label="Green Total", type=ItemIO.OUTPUT)
     private int green[];
     
     @Parameter(label="Blue Total", type=ItemIO.OUTPUT)
     private int blue[];
     
     @Parameter(label="Area", type=ItemIO.OUTPUT)
     private int size[];
     
     @Override
     public void run() {
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
          if(nc > 3)
               throw new IllegalArgumentException(ds.getName() + " has too many channels");
          if(nc == 0)
               throw new IllegalArgumentException(ds.getName() + " has zero channels");
          final int[] r = new int[n];
          final int[] g = new int[n];
          final int[] b = new int[n];
          final int[] s = new int[n];
          Arrays.fill(r, 0);
          Arrays.fill(g, 0);
          Arrays.fill(b, 0);
          Arrays.fill(s, 0);
          final RegionOfInterest[] roi2 = new RegionOfInterest[n];
          for(int i=0; i<n; i++)
               if(roi[i] != null)
                    roi2[i] = roi[i].getRegionOfInterest();
          dims[cAxis] = 1;
          final IntervalIterator ii = new IntervalIterator(dims);
          final long[] pos = new long[ii.numDimensions()];
          final double[] posD = new double[ii.numDimensions()];
          RandomAccess<? extends RealType<?>> ra = img.randomAccess();
          while(ii.hasNext()) {
               ii.fwd();
               for(int i=0; i<n; i++) {
                    ii.localize(pos);
                    for(int j=0; j<pos.length; j++)
                         posD[j] = pos[j];
                    if(roi2[i] != null && roi2[i].contains(posD)) {
                         ra.setPosition(pos);
                         ra.setPosition(0, cAxis);
                         r[i] += Math.max(
                                 ra.get().getRealDouble() - rT, 0);
                         if(nc > 1) {
                              ra.setPosition(1, cAxis);
                              g[i] += Math.max(
                                      ra.get().getRealDouble() - gT, 0);
                         }
                         if(nc > 2) {
                              ra.setPosition(2, cAxis);
                              b[i] += Math.max(
                                      ra.get().getRealDouble() - bT, 0);
                         }
                         ++s[i];
                    }
               }
          }
          red = r;
          green = g;
          blue = b;
          size = s;
     }
}
