package org.nanes.slideset.pi;

import net.imagej.Dataset;
import net.imagej.overlay.AbstractOverlay;
import java.math.BigInteger;
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
@HelpPath(path = "plugins/regions.html")
@Plugin(type=SlideSetPlugin.class,
        label="Region Statistics", visible = false,
        menuPath = "Plugins > Slide Set > Commands > Region Statistics")
public class RegionStats extends SlideSetPlugin implements MultipleResults {
     
     @Parameter(label="Image", type=ItemIO.INPUT)
     private Dataset ds;
     
     @Parameter(label="Region of Interest", type=ItemIO.INPUT)
     private AbstractOverlay[] roi;
     
     @Parameter(label="Red Channel Threshold", type=ItemIO.INPUT)
     private double rT;
     
     @Parameter(label="Green Channel Threshold", type=ItemIO.INPUT)
     private double gT;
     
     @Parameter(label="Blue Channel Threshold", type=ItemIO.INPUT)
     private double bT;
     
     @Parameter(label="Invert", type=ItemIO.INPUT)
     private boolean inv;
     
     @Parameter(label="Red Average", type=ItemIO.OUTPUT)
     private double red[];
     
     @Parameter(label="Green Average", type=ItemIO.OUTPUT)
     private double green[];
     
     @Parameter(label="Blue Average", type=ItemIO.OUTPUT)
     private double blue[];
     
     @Parameter(label="Area", type=ItemIO.OUTPUT)
     private int size[];
         
     @Override
     public void run() {
                  
          if(roi == null || ds == null) {
              red = new double[0];
              green = new double[0];
              blue = new double[0];
              size = new int[0];
              return;
          }
          final int n = roi.length;
          red = new double[n];
          green = new double[n];
          blue = new double[n];
          size = new int[n];
          if(n == 0)
               return;
          final long[] dims = new long[ds.numDimensions()];
          long nc = dims.length - 1;
          final boolean singlet = nc == 1;
          ds.dimensions(dims);
          final int cAxis = ds.dimensionIndex(Axes.CHANNEL);
          if(!singlet) {
              if(cAxis < 0)
                 throw new IllegalArgumentException("Could not get channel axis index for " + ds.getName());
              nc = dims[cAxis];
              if(nc > 3)
                 throw new IllegalArgumentException(ds.getName() + " has too many channels");
              if(nc == 0)
                 throw new IllegalArgumentException(ds.getName() + " has zero channels");
          }
          Arrays.fill(red, 0);
          Arrays.fill(green, 0);
          Arrays.fill(blue, 0);
          Arrays.fill(size, 0);
          IntervalIterator ii;
          final double[] posD = new double[dims.length];
          final long[] min = new long[dims.length];
          final long[] max = new long[dims.length];
          RandomAccess<? extends RealType<?>> ra = ds.randomAccess();
          RegionOfInterest bin;
          BigInteger r;
          BigInteger g = BigInteger.valueOf(0);
          BigInteger b = BigInteger.valueOf(0);
          for(int i = 0; i < n; i++) {
              bin = roi[i].getRegionOfInterest();
              for(int c = 0; c < bin.numDimensions(); c++) {
                  min[c] = Math.round(Math.floor(bin.realMin(c)));
                  if(min[c] < 0 || min[c] > dims[c])
                      min[c] = 0;
                  max[c] = Math.round(Math.ceil(bin.realMax(c)));
                  if(max[c] < 0 || max[c] > dims[c])
                      max[c] = dims[c];
              }
              if(!singlet) {
                  min[cAxis] = 0;
                  max[cAxis] = 0;
              }
              r = BigInteger.valueOf(0);
              if(nc > 1)
                  g = BigInteger.valueOf(0);
              if(nc > 2)
                  b = BigInteger.valueOf(0);
              ii = new IntervalIterator(min, max);
              while(ii.hasNext()) {
                  ii.fwd();
                  ii.localize(posD);
                  if(!bin.contains(posD) || !inBounds(posD, dims))
                      continue;
                  ra.setPosition(ii);
                  ++size[i];
                  if(inv)
                      r = r.subtract(BigInteger.valueOf(Math.round(Math.min(ra.get().getRealDouble() - rT, 0))));
                  else
                      r = r.add(BigInteger.valueOf(Math.round(Math.max(ra.get().getRealDouble() - rT, 0))));
                  if(nc < 2)
                      continue;
                  ra.setPosition(1, cAxis);
                  if(inv)
                      g = g.subtract(BigInteger.valueOf(Math.round(Math.min(ra.get().getRealDouble() - gT, 0))));
                  else
                      g = g.add(BigInteger.valueOf(Math.round(Math.max(ra.get().getRealDouble() - gT, 0))));
                  if(nc < 3)
                      continue;
                  ra.setPosition(2, cAxis);
                  if(inv)
                      b = b.subtract(BigInteger.valueOf(Math.round(Math.min(ra.get().getRealDouble() - bT, 0))));
                  else
                      b = b.add(BigInteger.valueOf(Math.round(Math.max(ra.get().getRealDouble() - bT, 0))));
              }
              red[i] = r.doubleValue() / size[i];
              if(nc > 1)
                  green[i] = g.doubleValue() / size[i];
              if(nc > 2)
                  blue[i] = b.doubleValue() / size[i];
          }
     }
     
     private boolean inBounds(double[] pos, long[] dims) {
        for(int i = 0; i < pos.length; i++)
            if(pos[i] >= dims[i])
                return false;
        return true;
    }
     
}
