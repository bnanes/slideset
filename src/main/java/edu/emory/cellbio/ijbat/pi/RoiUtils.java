package edu.emory.cellbio.ijbat.pi;

import edu.emory.cellbio.ijbat.ex.SlideSetException;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import net.imagej.overlay.AbstractOverlay;
import net.imagej.overlay.GeneralPathOverlay;
import net.imagej.overlay.LineOverlay;
import net.imagej.overlay.PolygonOverlay;
import net.imglib2.meta.Axes;
import net.imglib2.roi.LineRegionOfInterest;
import net.imglib2.roi.PolygonRegionOfInterest;

/**
 * Utility functions for working with regions of interest
 * 
 * @author Benjamin Nanes
 */
public class RoiUtils {
    
    /**
     * Determine if a point is near a border defined by an set of vertices.
     * @param P The point to check, {@code (x,y)}
     * @param overlay Overlay to use as the border definition. If the
     *          overlay has more than two dimensions, it is flattened
     *          to {@code X} and {@code Y} dimensions only.
     * @param radius Distance considered to be near the border
     * @throws SlideSetException The overlay type is not supported
     */
    public static boolean isNearBorder (
          final double[] P, 
          final AbstractOverlay overlay,
          final double radius) throws SlideSetException {
        if(overlay instanceof LineOverlay)
            return isNearBorder(P, (LineOverlay) overlay, radius);
        if(overlay instanceof PolygonOverlay)
            return isNearBorder(P, (PolygonOverlay) overlay, radius);
        if(overlay instanceof GeneralPathOverlay)
            return isNearBorder(P, (GeneralPathOverlay) overlay, radius);
        throw new SlideSetException(overlay.getClass().getSimpleName() + " is not supported.");
    }
    
    /** Handler for {@code PolygonOverlay}s */
    public static boolean isNearBorder(
          final double[] P, 
          final PolygonOverlay overlay,
          final double radius) {
        final int x = overlay.dimensionIndex(Axes.X);
        final int y = overlay.dimensionIndex(Axes.Y);
        if(x < 0 || y < 0)
            throw new IllegalArgumentException("Overlay must have x and y axes.");
        final PolygonRegionOfInterest roi = overlay.getRegionOfInterest();
        final int n = roi.getVertexCount();
        final double[] vertex = new double[roi.numDimensions()];
        final double[][] vertices = new double[n][2];
        for(int i = 0; i < n; i++) {
            roi.getVertex(i).localize(vertex);
            vertices[i][0] = vertex[x];
            vertices[i][1] = vertex[y];
        }
        return isNearBorder(P, vertices, radius, true);
    }
    
    /** Handler for {@code LineOverlay}s */
    public static boolean isNearBorder(
          final double[] P, 
          final LineOverlay overlay,
          final double radius) {
        final int x = overlay.dimensionIndex(Axes.X);
        final int y = overlay.dimensionIndex(Axes.Y);
        if(x < 0 || y < 0)
            throw new IllegalArgumentException("Overlay must have x and y axes.");
        final LineRegionOfInterest roi = overlay.getRegionOfInterest();
        final double[][] V = new double[2][2];
        V[0][0] = roi.getPoint1(x);
        V[0][1] = roi.getPoint1(y);
        V[1][0] = roi.getPoint2(x);
        V[1][1] = roi.getPoint2(y);
        return isNearBorder(P, V, radius, false);
    }
    
    /** Handler for {@code GeneralPathOverlay}s */
    public static boolean isNearBorder(
          final double[] P,
          final GeneralPathOverlay overlay,
          final double radius) {
        boolean closed = false;
        final int x = overlay.dimensionIndex(Axes.X);
        final int y = overlay.dimensionIndex(Axes.Y);
        if(x < 0 || y < 0)
            throw new IllegalArgumentException("Overlay must have x and y axes.");
        final GeneralPath gp = overlay.getRegionOfInterest().getGeneralPath();
        final PathIterator pi = gp.getPathIterator(null, 0.5);
        final ArrayList<double[]> pl = new ArrayList<double[]>();
        final double[] p = new double[6];
        while(!pi.isDone()) {
            if(pi.currentSegment(p) != PathIterator.SEG_CLOSE)
                pl.add(new double[] {p[0], p[1]});
            else
                closed = true;
            pi.next();
        }
        return isNearBorder(P, pl.toArray(new double[1][2]), radius, closed);
    }
    
    /**
     * Determine if a point is near a border defined by an set of vertices.
     * @param P The point to check, {@code (x,y)}
     * @param vertices Ordered set of vertices defining the border,
     *                 {@code [index][(x,y)]}
     * @param radius Distance considered to be near the border
     * @param closed Is the border closed, i.e. is the segment connecting
     *               the last and first points in the border set considered
     *               part of the border?
     */
    public static boolean isNearBorder(
          final double[] P,
          final double[][] vertices,
          final double radius,
          final boolean closed) {
        if(P == null || vertices == null || P.length != 2 || radius < 0)
            throw new IllegalArgumentException("Requires two-dimensional coordinates");
        final int lim = closed ? vertices.length : vertices.length - 1;
        for(int i = 0; i < lim; i++) {
            int j = i + 1 < vertices.length ? i + 1 : 0;
            if((P[0] < vertices[i][0] - radius && P[0] < vertices[j][0] - radius) ||
               (P[0] > vertices[i][0] + radius && P[0] > vertices[j][0] + radius) ||
               (P[1] < vertices[i][1] - radius && P[1] < vertices[j][1] - radius) ||
               (P[1] > vertices[i][1] + radius && P[1] > vertices[j][1] + radius))
                continue;
            if(distanceFromSegment(P, vertices[i], vertices[j]) <= radius)
                return true;
        }
        return false;
    }
    
    /**
     * Compute the distance of point {@code P} from the
     * line segment bounded by points {@code A} and {@code B}.
     */
    public static double distanceFromSegment(
          final double[] P, final double[] A, final double[] B) {
        if(P == null || A == null || B == null ||
              A.length != 2 || B.length != 2 || P.length != 2)
            throw new IllegalArgumentException("Requires two-dimensional coordinates");
        if(isAbreastSegment(P, A, B))
            return distanceFromLine(P, A, B);
        double a = distance(P, A);
        double b = distance(P, B);
        return a < b ? a : b;
    }
    
    /**
     * Compute the distance of point {@code P} from
     * the line defined by points {@code A} and {@code B}.
     */
    public static double distanceFromLine(
          final double[] P, final double[] A, final double[] B) {
        if(P == null || A == null || B == null ||
              A.length != 2 || B.length != 2 || P.length != 2)
            throw new IllegalArgumentException("Requires two-dimensional coordinates");
        final double lx = B[0] - A[0];
        final double ly = B[1] - A[1];
        if(lx == 0)
            return Math.abs(P[0] - A[0]);
        if(ly == 0)
            return Math.abs(P[1] - A[1]);
        final double dx = Math.abs((P[1] - A[1]) * lx / ly + A[0] - P[0]);
        final double dy = Math.abs((P[0] - A[0]) * ly / lx + A[1] - P[1]);
        return triangleAltitude(dx, dy);
    }
    
    /**
     * Is a point next to a line segment?
     * I.e. does the shortest line connecting point {@code P}
     * to the line defined by points {@code A} and {@code B}
     * intersect with line {@code A-B} between {@code A} and {@code B}?
     */
    public static boolean isAbreastSegment(
          final double[] P, final double[] A, final double[] B) {
        if(P == null || A == null || B == null ||
              A.length != 2 || B.length != 2 || P.length != 2)
            throw new IllegalArgumentException("Requires two-dimensional coordinates");
        final double[] Q = new double[] {P[0] - A[0], P[1] - A[1]};
        final double r = distance(A, B);
        if(r == 0)
            return false;
        double theta = Math.asin((B[1] - A[1]) / r);
        if(B[0] - A[0] < 0)
            theta = Math.PI - theta;
        double t = Q[0] * Math.cos(theta) + Q[1] * Math.sin(theta);
        return 0 <= t && t <= r;
    }
    
    /**
     * Compute the linear distance between two points.
     */
    public static double distance(final double[] A, final double[] B) {
        return Math.sqrt(Math.pow(B[0] - A[0], 2) + Math.pow(B[1] - A[1], 2));
    }
    
    /**
     * Compute the squared linear distance between two points.
     */
    public static double distanceSquared(final double[] A, final double[] B) {
        return Math.pow(B[0] - A[0], 2) + Math.pow(B[1] - A[1], 2);
    }
    
    /**
     * Compute the altitude of a triangle.
     * Returns {@code 0} if one of the sides has length {@code 0}.
     */
    public static double triangleAltitude(final double a, final double b) {
        if(a == 0 || b == 0)
            return 0;
        if(a < 0 || b < 0)
            throw new IllegalArgumentException("Lengths must be greater than zero");
        final double c = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
        final double hSquared = (a+b-c)*(a-b+c)*(-a+b+c)*(a+b+c) / (4*c*c);
        return Math.sqrt(hSquared);
    }
    
}
