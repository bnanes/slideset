package org.nanes.slideset.pi;

import net.imagej.ImageJ;
import net.imagej.Dataset;
import net.imagej.overlay.AbstractOverlay;
import net.imagej.overlay.PolygonOverlay;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.iterator.IntervalIterator;
import net.imagej.axis.Axes;
import net.imglib2.roi.PolygonRegionOfInterest;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Segment an image into 4-neighborhood regions
 * based on threshold values.
 * 
 * @author Benjamin Nanes
 */
@HelpPath(path = "plugins/thresholdsegmentation.html")
@Plugin( type=SlideSetPlugin.class,
         name="Threshold Segmentation",
         label="Threshold Segmentation", visible = false,
         menuPath="Plugins > Slide Set > Commands > Segmentation > Threshold Segmentation")
public class ThresholdSegmentation extends SlideSetPlugin {
    
    // -- Parameters --
    
    @Parameter(label="ImageJ", type=ItemIO.INPUT)
    private ImageJ ij;
    
    @Parameter(label="Images", type=ItemIO.INPUT)
    private Dataset ds;
    
    @Parameter(label="Thresholds", type=ItemIO.INPUT)
    private String ts;
    
    @Parameter(label="Minimum size", type=ItemIO.INPUT)
    private int minSize;
    
    @Parameter(label="Maximum size", type=ItemIO.INPUT)
    private int maxSize;
    
    @Parameter(label="Combine thresholds", type=ItemIO.INPUT)
    private boolean and;
    
    @Parameter(label="Segments", type=ItemIO.OUTPUT)
    private AbstractOverlay[] segs;
    
    // -- Other fields --
    
    private final ArrayList<PolygonOverlay> v = new ArrayList<PolygonOverlay>();
    private final ArrayList<PolygonOverlay> vTemp = new ArrayList<PolygonOverlay>();
    
    // -- Methods --
    
    public void run() {
        v.clear();
        final int cAxis = ds.dimensionIndex(Axes.CHANNEL);
        final int xAxis = ds.dimensionIndex(Axes.X);
        final int yAxis = ds.dimensionIndex(Axes.Y);
        final boolean flat = cAxis < 0 && xAxis >= 0 && yAxis >= 0;
        if(!flat && cAxis < 0)
            throw new IllegalArgumentException("Unable to find channel axis.");
        long[] dims = new long[ds.numDimensions()];
        ds.dimensions(dims);
        final long nc = flat? 1 : dims[cAxis];
        //Parse threshold string
        final double[] nts = new double[(int)nc];
        Arrays.fill(nts, 0);
        String[] tss = ts.split("\\s");
        if(tss.length > 0) {
            int j=0;
            for(int i=0; i < nts.length; i++) {
                if(j >= tss.length)
                    j = 0;
                try {
                    nts[i] = new Double(tss[j]);
                } catch(NumberFormatException e) { }
                j++;
            }
        }
        //
        final long[] maskDims = Arrays.copyOf(dims, dims.length);
        final long[] origin = new long[dims.length];
        Arrays.fill(origin, 0);
        if(!flat)
            maskDims[cAxis] = 1;
        CellImg<BitType, ?> mask = new CellImgFactory<BitType>().create(maskDims, new BitType(false));
        CellImg<BitType, ?> included = new CellImgFactory<BitType>().create(maskDims, new BitType(false));
        RandomAccess<? extends RealType<?>> ra = ds.getImgPlus().randomAccess();
        RandomAccess<BitType> raMask = mask.randomAccess();
        RandomAccess<BitType> raIncl = included.randomAccess();
        IntervalIterator ii = new IntervalIterator(maskDims);
        while(ii.hasNext()) {
            ii.fwd();
            ra.setPosition(ii);
            raMask.setPosition(ii);
            boolean val = false;
            for(int c = 0; c < nc; c++) {
                if(!flat)
                    ra.setPosition(c, cAxis);
                if(c==0)
                    val = ra.get().getRealDouble() > nts[c];
                else
                    val = and ? val && ra.get().getRealFloat() > nts[c]
                              : val || ra.get().getRealFloat() > nts[c];
            }
            raMask.get().set(val);   
        }
        raMask.setPosition(origin);
        long[] pos = new long[maskDims.length];
        double[] posD = new double[maskDims.length];
        Stack<long[]> blob = new Stack<long[]>();
        ArrayList<long[][]> edges = new ArrayList<long[][]>(); // [point][axis]
        for(int x = 0; x < maskDims[xAxis]; x++) {
            for(int y = 0; y < maskDims[yAxis]; y++) {
                raMask.setPosition(x, xAxis);
                raMask.setPosition(y, yAxis);
                raMask.localize(pos);
                raIncl.setPosition(pos);
                if(!raMask.get().get() || raIncl.get().get())
                    continue;
                blob.push(pos);
                int blobSize = 0;
                edges.clear();
                while(!blob.empty()) {
                    pos = blob.pop();
                    raMask.setPosition(pos);
                    raIncl.setPosition(pos);
                    if(raIncl.get().get())
                        continue;
                    raIncl.get().set(true);
                    blobSize++;
                    ArrayList<long[]> ns = getNeighbors(pos, xAxis, yAxis);
                    for(long[] n : ns) {
                        boolean in = false;
                        if(inBounds(n, dims)) {
                            raMask.setPosition(n);
                            if(raMask.get().get())
                                in = true;
                        }
                        if(in)
                            blob.push(n);
                        else {
                            long[][] edge = new long[2][pos.length];
                            edge[0] = Arrays.copyOf(pos, pos.length);
                            edge[1] = Arrays.copyOf(pos, pos.length);
                            if(n[xAxis] == pos[xAxis])
                                edge[1][xAxis]++;
                            else if(n[xAxis] > pos[xAxis]) {
                                edge[0][xAxis]++;
                                edge[1][xAxis]++;
                            }
                            if(n[yAxis] == pos[yAxis])
                                edge[1][yAxis]++;
                            else if(n[yAxis] > pos[yAxis]) {
                                edge[0][yAxis]++;
                                edge[1][yAxis]++;
                            }
                            edges.add(edge);
                        }
                    }
                }
                if(blobSize < minSize || blobSize > maxSize)
                    continue;
                vTemp.clear();
                while(!edges.isEmpty()) {
                    PolygonOverlay po = new PolygonOverlay(ij.getContext());
                    PolygonRegionOfInterest roi = po.getRegionOfInterest();
                    long[] p0 = edges.get(0)[0];
                    long[] p = edges.get(0)[1];
                    roi.addVertex(0, new Point(p0));
                    roi.addVertex(1, new Point(p));
                    edges.remove(0);
                    p = getNextPoint(p, edges);
                    while(p != null && !vectorEquals(p0, p)) {
                        roi.addVertex(roi.getVertexCount(), new Point(p));
                        p = getNextPoint(p, edges);
                    }
                    offsetBottomRightEdges(roi, xAxis, yAxis);
                    vTemp.add(po);
                }
                boolean done = false;
                if(vTemp.size() == 1)
                    v.add(vTemp.get(0));
                else for(int i = 0; i < vTemp.size() && !done; i++) {
                    PolygonRegionOfInterest roi1 =
                            vTemp.get(i).getRegionOfInterest();
                    roi1.getVertex(0).localize(posD);
                    for(int j = 0; j < vTemp.size(); j++) {
                        if(i == j)
                            continue;
                        if(vTemp.get(j).getRegionOfInterest().contains(posD)) {
                            done = true;
                            v.add(vTemp.get(j));
                            break;
                        }
                    }
                }
            }
        }
        segs = v.toArray(new PolygonOverlay[0]);
    }
    
    /**
     * Find the edge in {@code set} with one point matching {@code p}.
     * Return that edge's other point, and remove it from the set.
     * @param set [point][axis]
     */
    private long[] getNextPoint(long[] p, ArrayList<long[][]> set) {
        for(long[][] edge : set) {
            long[] q = edge[0];
            long[] r = edge[1];
            boolean match = true;
            for(int i = 0; i < p.length; i++)
                match = match && p[i] == q[i];
            if(match) {
                set.remove(edge);
                return r;
            }
            match = true;
            for(int i = 0; i < p.length; i++)
                match = match && p[i] == r[i];
            if(match) {
                set.remove(edge);
                return q;
            }
        }
        return null;
    }
    
    /** Get the 4-connected neighborhood in the x-y plane */
    private ArrayList<long[]> getNeighbors(long[] pos, int xAxis, int yAxis) {
        ArrayList<long[]> neighbors = new ArrayList<long[]>();
        long[] n = Arrays.copyOf(pos, pos.length);
        n[yAxis]++;
        neighbors.add(n);
        n = Arrays.copyOf(pos, pos.length);
        n[yAxis]--;
        neighbors.add(n);
        n = Arrays.copyOf(pos, pos.length);
        n[xAxis]++;
        neighbors.add(n);
        n = Arrays.copyOf(pos, pos.length);
        n[xAxis]--;
        neighbors.add(n);
        return neighbors;
    }
    
    private boolean vectorEquals(long[] a, long[] b) {
        boolean c = a.length == b.length;
        if(!c)
            return false;
        for(int i = 0; i < a.length; i++)
            c = c && a[i] == b[i];
        return c;
    }
    
    private boolean inBounds(long[] point, long[] dims) {
        if(point.length != dims.length)
            throw new IllegalArgumentException();
        for(int i = 0; i < point.length; i++) {
            if(point[i] < 0 || point[i] >= dims[i])
                return false;
        }
        return true;
    }
    
    /**
     * Offset the bottom and right edges of an ROI
     * outlining pixels as a workaround for issues
     * with the {@code contains()} method. This
     * should ensure that polygon ROIs actually
     * contain only the pixels which they appear
     * to outline.
     */
    private void offsetBottomRightEdges(
            PolygonRegionOfInterest roi,
            int xAxis, int yAxis) {
        final int n = roi.getVertexCount();
        final int nDims = roi.getVertex(0).numDimensions();
        double[] a = new double[nDims];
        double[] t = new double[nDims];
        boolean tr, br, bl, tl;
        for(int i = 0; i < n; i++) {
            roi.getVertex(i).localize(a);
            t = Arrays.copyOf(a, a.length);
            t[xAxis] += 0.5;
            t[yAxis] -= 0.5;
            tr = roi.contains(t);
            t[yAxis]++;
            br = roi.contains(t);
            t[xAxis]--;
            bl = roi.contains(t);
            t[yAxis]--;
            tl = roi.contains(t);
            if((tl && !tr) || (bl && !br))
                a[xAxis] -= 0.001;
            if((tl && !bl) || (tr && !br))
                a[yAxis] -= 0.001;
            roi.setVertexPosition(i, a);
        }
    }
}
