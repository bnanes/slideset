package org.nanes.slideset.pi;

import com.google.common.primitives.Doubles;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import org.nanes.slideset.ui.SlideSetLog;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Count intersections between ROI borders and a binary mask
 * @author Benjamin Nanes
 */
@HelpPath(path = "plugins/countIntersections.html")
@Plugin( type=SlideSetPlugin.class,
     name="Count Intersections",
     label="Count Intersections", visible = false,
     menuPath="Plugins > Slide Set > Commands > Regions > Count Intersections")
public class CountIntersections extends SlideSetPlugin implements MultipleResults {
    
    @Parameter(label = "Log", type = ItemIO.INPUT)
    private SlideSetLog log;

    @Parameter(label = "ROIs", type = ItemIO.INPUT)
    private Roi[] rois;
    
    @Parameter(label = "Image", type = ItemIO.INPUT)
    private ImagePlus imp;
    
    @Parameter(label = "Invert Image", type = ItemIO.INPUT)
    private Boolean invert;
    
    @Parameter(label = "Dilate Mask", type = ItemIO.INPUT)
    private Boolean dil;
    
    @Parameter(label = "Intersections", type = ItemIO.OUTPUT)
    private int[] isect;
    
    @Parameter(label = "Length-Ends", type = ItemIO.OUTPUT)
    private double[] lenE;
    
    @Parameter(label = "Length-Intersections", type = ItemIO.OUTPUT)
    private double[] lenI;

    @Override
    public void run() {
        if(rois == null || rois.length == 0 || imp == null) {
            lenE = new double[0];
            lenI = new double[0];
            isect = new int[0];
            return;
        }
        int N = rois.length;
        lenE = new double[N];
        lenI = new double[N];
        isect = new int[N];
        
        IJ.run(imp, "Multiply...", "value=255");
        if(invert) IJ.run(imp, "Invert", "");
        if(dil) IJ.run(imp, "Dilate", ""); // Dilate the filament mask
        
        for(int i=0; i<N; i++) {
            lenE[i] = -1;
            lenI[i] = -1;
            isect[i] = 0;
            double[] lineValues;
            if(rois[i] instanceof Line) {
                Line roi = (Line) rois[i];
                lineValues = getValuesOnLine(roi.x1d, roi.y1d, roi.x2d, roi.y2d, 10);
                lenE[i] = roi.getLength();
            } else if(rois[i] instanceof PolygonRoi) {
                PolygonRoi roi = (PolygonRoi) rois[i];
                FloatPolygon fp = roi.getFloatPolygon();                
                float[] xs = fp.xpoints;
                float[] ys = fp.ypoints;
                int Nc = xs.length;
                lineValues = new double[0];
                lenE[i] = 0;
                for(int j = 0; j < Nc - 1; j++) {
                    lineValues = Doubles.concat(
                            lineValues, getValuesOnLine(xs[j], ys[j], xs[j+1], ys[j+1], 10));
                    lenE[i] += Math.sqrt(Math.pow(xs[j] - xs[j+1], 2) + Math.pow(ys[j] - ys[j+1], 2));
                }
            } else {
                log.println(rois[i].getClass().getName() + " is not supported.*******");
                continue;
            }
            boolean lastIsect = false;
            int firstIsect = -1;
            for(int j = 0; j < lineValues.length; j++) {
                if(lineValues[j] == 0) {
                    lastIsect = false;
                } else {
                    if(firstIsect < 0)
                        firstIsect = j;
                    lenI[i] = j - firstIsect;
                    if(!lastIsect) {
                        lastIsect = true;
                        isect[i]++;
                    }
                }
            }
            lenI[i] = lenE[i] * lenI[i] / lineValues.length;
        }
    }
    
    // Adapted from ij.process.ImageProcessor.getLine, expands number of interpolation points by magFactor
    private double[] getValuesOnLine(double x1, double y1, double x2, double y2, double magFactor) {
        double dx = x2-x1;
        double dy = y2-y1;
        int n = (int)Math.round(Math.sqrt(dx*dx + dy*dy) * magFactor);
        double xinc = n>0?dx/n:0;
        double yinc = n>0?dy/n:0;
        if (!((xinc==0&&n==imp.getHeight()) || (yinc==0&&n==imp.getWidth())))
                n++;
        double[] data = new double[n];
        double rx = x1;
        double ry = y1;
        for (int i=0; i<n; i++) {
            data[i] = imp.getProcessor().getPixelValue((int)Math.round(rx), (int)Math.round(ry));
            rx += xinc;
            ry += yinc;
        }
        return data;
    }
    
}
