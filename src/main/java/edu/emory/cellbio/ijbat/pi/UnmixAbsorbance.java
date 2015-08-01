package edu.emory.cellbio.ijbat.pi;

import Jama.Matrix;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import edu.emory.cellbio.ijbat.ui.SlideSetLog;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imglib2.RandomAccess;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.img.planar.PlanarRandomAccess;
import net.imglib2.iterator.IntervalIterator;
import net.imagej.axis.Axes;
import net.imagej.ImgPlus;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Separate RGB images into two absorbance components.
 * 
 * @author Benjamin Nanes
 */
@HelpPath(path = "plugins/unmix.html")
@Plugin( type=SlideSetPlugin.class,
    name="Unmix Absorbances",
    label="Unmix Absorbances", visible = false,
    menuPath="Plugins > Slide Set > Commands > Unmix Absorbances")
public class UnmixAbsorbance extends SlideSetPlugin implements MultipleResults {
    
    @Parameter(label="Context", type=ItemIO.INPUT)
    private Context context;
    
    @Parameter(label="Log", type=ItemIO.INPUT)
    private SlideSetLog log;
    
    @Parameter(label="Images", type=ItemIO.INPUT)
    private Dataset ds;
    
    @Parameter(label="Pigment 1 color (R G B)", type=ItemIO.INPUT)
    private String pigmentOne;
    
    @Parameter(label="Pigment 2 color (R G B)", type=ItemIO.INPUT)
    private String pigmentTwo;
    
    @Parameter(label="Absorbance 1", type=ItemIO.OUTPUT)
    private Dataset[] p1ds;
    
    @Parameter(label="Absorbance 2", type=ItemIO.OUTPUT)
    private Dataset[] p2ds;
    
    @Parameter(label="Residual", type=ItemIO.OUTPUT)
    private Dataset[] rds;
    
    public void run() {
        int max = new Double(ds.getType().getMaxValue()).intValue();
        final double logMax = Math.log(max);
        final double[] p1 = parseRGBtoPigment(pigmentOne, max);
        final double[] p2 = parseRGBtoPigment(pigmentTwo, max);
        Dataset[] result;
        try {
            result = unmix(ds, p1, p2);
            if(result == null || result.length != 3)
                throw new SlideSetException("Null or unexpected result!");
        } catch(SlideSetException e) {
            if(log != null) {
                log.println("-------------------------");
                log.println("Error: " + e.getMessage());
                log.println("  " + ds.getName());
                log.println("  will be skipped.");
                log.println("-------------------------");
            }
            p1ds = new Dataset[0];
            p2ds = new Dataset[0];
            rds = new Dataset[0];
            return;
        }
        p1ds = new Dataset[] {result[0]};
        p2ds = new Dataset[] {result[1]};
        rds = new Dataset[] {result[2]};
    }
    
    /**
     * Separate an RGB image into two absorbance components.
     * 
     * @param input Image to unmix
     * @param absOne First absorbance vector, {@code R G B}.
     *     Higher values correspond to more absorbance
     *     (resulting in lower pixel values) on each channel.
     * @param absTwo Second absorbance vector, {@code R G B}
     * @return An array of three {@code Dataset}s:
     *      <li> First absorbance component
     *      <li> Second absorbance component
     *      <li> Residual component, normalized
     * @throws SlideSetException 
     */
    public Dataset[] unmix(
          final Dataset input, 
          final double[] absOne, final double[] absTwo)
          throws SlideSetException {
        int max = new Double(input.getType().getMaxValue()).intValue();
        double logMax = Math.log(max);

        final Matrix P = new Matrix(concat(absOne, absTwo), 3);
        
        final int nDims = ds.numDimensions();
        final int cDim = ds.dimensionIndex(Axes.CHANNEL);
        final long[] dims = new long[nDims];
        ds.dimensions(dims);
        if(cDim < 0 || dims[cDim] < 3)
            throw new SlideSetException("RGB image required");
        dims[cDim] = 1;
        
        final IntervalIterator ii = new IntervalIterator(dims);
        final RandomAccess<? extends RealType<?>> ra = ds.randomAccess();
        final PlanarImgFactory<DoubleType> pifd =
                new PlanarImgFactory<DoubleType>();
        final PlanarImg<DoubleType, ?> p1d =
                pifd.create(dims, new DoubleType(0));
        final PlanarRandomAccess<DoubleType> p1ra = p1d.randomAccess();
        final double[] p1lim = new double[] {0, 0}; // Min, Max
        final PlanarImg<DoubleType, ?> p2d =
                pifd.create(dims, new DoubleType(0));
        final PlanarRandomAccess<DoubleType> p2ra = p2d.randomAccess();
        final double[] p2lim = new double[] {0, 0};
        final PlanarImg<DoubleType, ?> rd =
                pifd.create(dims, new DoubleType(0));
        final PlanarRandomAccess<DoubleType> rra = rd.randomAccess();
        final double[] rlim = new double[] {0, 0};
        boolean first = true;
        
        while(ii.hasNext()) { // Do unmixing
            ii.fwd();
            ra.setPosition(ii);
            p1ra.setPosition(ii);
            p2ra.setPosition(ii);
            rra.setPosition(ii);
            final double[] I = new double[3];
            for(int i = 0; i < 3; i++) {
                ra.setPosition(i, cDim);
                I[i] = ra.get().getRealDouble();
            }
            final double[] a = new double[3];
            for(int i = 0; i < 3; i++)
                a[i] = -1 * Math.log(I[i] / max);
            final Matrix A = new Matrix(a, 3); // Total absorbance
            final Matrix x = P.solve(A); // Solution
            final double residual = P.times(x).minus(A).normF();
            final double p1 = x.get(0,0); // Absorbance apportioned to each pigment
            final double p2 = x.get(1,0);
            p1ra.get().set(p1);
            if(first || p1 < p1lim[0])
                p1lim[0] = p1;
            if(first || p1 > p1lim[1])
                p1lim[1] = p1;
            p2ra.get().set(p2);
            if(first || p2 < p2lim[0])
                p2lim[0] = p2;
            if(first || p2 > p2lim[1])
                p2lim[1] = p2;
            rra.get().set(residual);
            if(first || residual < rlim[0])
                rlim[0] = residual;
            if(first || residual > rlim[1])
                rlim[1] = residual;
            first = false;
        }
        ii.reset();
        
        final PlanarImgFactory<UnsignedShortType> pifnorm =
                new PlanarImgFactory<UnsignedShortType>();
        final PlanarImg<UnsignedShortType, ?> p1n =
                pifnorm.create(dims, new UnsignedShortType(0));
        PlanarRandomAccess<UnsignedShortType> p1nra = p1n.randomAccess();
        final PlanarImg<UnsignedShortType, ?> p2n =
                pifnorm.create(dims, new UnsignedShortType(0));
        PlanarRandomAccess<UnsignedShortType> p2nra = p2n.randomAccess();
        final PlanarImg<UnsignedShortType, ?> rn =
                pifnorm.create(dims, new UnsignedShortType(0));
        PlanarRandomAccess<UnsignedShortType> rnra = rn.randomAccess();
        
        while(ii.hasNext()) { // Normalize output images
            ii.fwd();
            p1ra.setPosition(ii);
            p1nra.setPosition(ii);
            p2ra.setPosition(ii);
            p2nra.setPosition(ii);
            rra.setPosition(ii);
            rnra.setPosition(ii);
            p1nra.get().setInteger(Math.round(
                  Math.max(p1ra.get().getRealDouble(), 0) / logMax * 65535));
            p2nra.get().setInteger(Math.round(
                  Math.max(p2ra.get().getRealDouble(), 0) / logMax * 65535));
            rnra.get().setInteger(Math.round(
                 ((rra.get().getRealDouble() - rlim[0]) / (rlim[1] - rlim[0])) * 65535));
        }
        
        final DefaultDataset pigment1 =
              new DefaultDataset(context, new ImgPlus(p1n, "Pigment 1"));
        final DefaultDataset pigment2 =
              new DefaultDataset(context, new ImgPlus(p2n, "Pigment 2"));
        final DefaultDataset residual =
              new DefaultDataset(context, new ImgPlus(rn, "Residual"));
        return new Dataset[] {pigment1, pigment2, residual};
    }
    
    // -- Helper methods --
    
    /**
     * Parse a whitespace-separated RGB string to an array of absorbance values
     */
    private double[] parseRGBtoPigment(String rgb, int maxVal) {
        if(rgb == null || rgb.isEmpty())
            throw new IllegalArgumentException("Invalid RGB string");
        double[] val = new double[3];
        String[] tokens = rgb.split("\\s+");
        if(tokens.length != 3)
            throw new IllegalArgumentException("Invalid RGB string");
        double sum = 0;
        for(int i = 0; i < 3; i++) {
            val[i] = -1 * Math.log( new Double(tokens[i]) / maxVal );
            sum += val[i];
        }
        for(int i = 0; i < 3; i++)
            val[i] = val[i] / sum;
        return val;
    }
    
    /** Concatenate two vectors */
    private double[] concat(double[] a, double[] b) {
        double[] c = new double[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
    
}
