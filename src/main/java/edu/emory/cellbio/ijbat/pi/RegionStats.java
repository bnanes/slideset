package edu.emory.cellbio.ijbat.pi;

import imagej.data.Dataset;
import imagej.data.overlay.AbstractOverlay;
import java.util.Arrays;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
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
     private int rT;
     
     @Parameter(label="Green Channel Threshold", type=ItemIO.INPUT)
     private int gT;
     
     @Parameter(label="Blue Channel Threshold", type=ItemIO.INPUT)
     private int bT;
     
     @Parameter(label="Invert", type=ItemIO.INPUT)
     private boolean inv;
     
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
                  
          if(roi == null || ds == null) {
              red = new int[0];
              green = new int[0];
              blue = new int[0];
              size = new int[0];
              return;
          }
          final int n = roi.length;
          red = new int[n];
          green = new int[n];
          blue = new int[n];
          size = new int[n];
          if(n == 0)
               return;
          final ImgPlus<? extends RealType<?>> imp = ds.getImgPlus();
          final Img<? extends RealType<?>> img = imp.getImg();
          final long[] dims = new long[imp.numDimensions()];
          long nc = dims.length - 1;
          final boolean singlet = nc == 1;
          imp.dimensions(dims);
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
          /*if(!singlet)
              dims[cAxis] = 1;*/
          IntervalIterator ii;
          final double[] posD = new double[dims.length];
          final long[] min = new long[dims.length];
          final long[] max = new long[dims.length];
          RandomAccess<? extends RealType<?>> ra = img.randomAccess();
          RegionOfInterest bin;
          for(int i = 0; i < n; i++) {
              bin = roi[i].getRegionOfInterest();
              for(int c = 0; c < bin.numDimensions(); c++) {
                  min[c] = Math.round(Math.floor(bin.realMin(c)));
                  max[c] = Math.round(Math.ceil(bin.realMax(c)));
              }
              if(!singlet) {
                  min[cAxis] = 0;
                  max[cAxis] = 0;
              }
              ii = new IntervalIterator(min, max);
              while(ii.hasNext()) {
                  ii.fwd();
                  ii.localize(posD);
                  if(!bin.contains(posD) || !inBounds(posD, dims))
                      continue;
                  ra.setPosition(ii);
                  ++size[i];
                  if(inv)
                      red[i] -= Math.min(ra.get().getRealDouble() - rT, 0);
                  else
                      red[i] += Math.max(ra.get().getRealDouble() - rT, 0);
                  if(nc < 2)
                      continue;
                  ra.setPosition(1, cAxis);
                  if(inv)
                      green[i] -= Math.min(ra.get().getRealDouble() - gT, 0);
                  else
                      green[i] += Math.max(ra.get().getRealDouble() - gT, 0);
                  if(nc < 3)
                      continue;
                  ra.setPosition(2, cAxis);
                  if(inv)
                      blue[i] -= Math.min(ra.get().getRealDouble() - bT, 0);
                  else
                      blue[i] += Math.max(ra.get().getRealDouble() - bT, 0);
              }
          }
     }
     
     private boolean inBounds(double[] pos, long[] dims) {
        for(int i = 0; i < pos.length; i++)
            if(pos[i] >= dims[i])
                return false;
        return true;
    }
     
}
