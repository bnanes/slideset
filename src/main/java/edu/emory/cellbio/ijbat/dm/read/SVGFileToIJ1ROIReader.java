package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.FileLinkElement;
import edu.emory.cellbio.ijbat.dm.MIME;
import edu.emory.cellbio.ijbat.dm.RoisAlias;
import edu.emory.cellbio.ijbat.ex.LinkNotFoundException;
import edu.emory.cellbio.ijbat.ex.SVGParseException;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import java.awt.geom.Path2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.imagej.overlay.EllipseOverlay;
import org.scijava.Context;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "ROI set file (SVG)",
        elementType = FileLinkElement.class,
        mimeTypes = { MIME.SVG },
        processedType = RoisAlias.class,
        hidden = true )
public class SVGFileToIJ1ROIReader 
        implements ElementReader<FileLinkElement, Roi[]> {
    
    private Context ij;
    private String warningMessage;
    
    // -- Methods --

    public Roi[] read(FileLinkElement elementToRead) throws SlideSetException {
        String path = elementToRead.getUnderlying();
        if(path == null || path.trim().isEmpty())
            throw new LinkNotFoundException(path + " does not exist!");
        String wd = elementToRead.getOwner().getWorkingDirectory();
        wd = wd == null ? "" : wd;
        if(!(new File(path)).isAbsolute())
            path = wd + File.separator + path;
        final File f = new File(path);
        if(!(f.exists()))
           throw new LinkNotFoundException(path + " does not exist!");
        ij = elementToRead.getOwner().getContext();
        Document dom = loadDocument(f);
        return parseDocument(dom).toArray(new Roi[0]);
    }
    
    // -- Helper methods --
    
    /** Load the DOM from an SVG file */
    private Document loadDocument(File file) throws SlideSetException {
        final Document dom;
        final DocumentBuilder domBuilder;
        try {
            domBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            dom = domBuilder.parse(file);
        } catch(Exception e) {
            throw new SlideSetException("Error reading SVG file:", e);
        }
        if(dom == null)
            throw new SlideSetException("Error reading SVG file.");
        return dom;
    }
    
    /** Get the list of ROIs that can be created from the DOM */
    private ArrayList<Roi> parseDocument(Document dom) {
        warningMessage = "";
        final ArrayList<Roi> rois = new ArrayList<Roi>();
        final NodeList nodes = dom.getElementsByTagName("*");
        for(int i = 0; i < nodes.getLength(); i++) {
            final Node n = nodes.item(i);
            try {
                final Roi roi = parseNode(n);
                if(roi != null)
                    rois.add(roi);
            } catch(SVGParseException e) {
                e.printStackTrace(System.err);
                warningMessage += "\n" + e.getMessage();
            }
        }
        return rois;
    }
    
    /**
     * Generate an overlay from an SVG node
     * 
     * @param n The node
     * @return The overlay, or {@code null} if an overlay cannot
     *   be created from the node
     */
    private Roi parseNode(Node n) throws SVGParseException {
        if(n.getNodeType() != Node.ELEMENT_NODE)
            return null;
        final String name = ((Element) n).getTagName();
        if(name.equals("rect"))
            return parseRect((Element) n);
        if(name.equals("circle"))
            return parseCircle((Element) n);
        if(name.equals("ellipse"))
            return parseEllipse((Element) n);
        if(name.equals("line"))
            return parseLine((Element) n);
        if(name.equals("polygon"))
            return parsePolygon((Element) n);
        if(name.equals("polyline"))
            return parsePolyline((Element) n);
        if(name.equals("path"))
            return parsePath((Element) n);
        return null;
    }
    
    /** Generate an ROI from a {@code polyline} element */
    private PolygonRoi parsePolyline(Element n) throws SVGParseException {
        if(!"polyline".equals(n.getTagName()))
            throw new SVGParseException("\'polyline\' expected, but \'" + n.getLocalName() + "\' found.");
        String points = n.getAttribute("points");
        ArrayList<double[]> pts;
        try {
            pts = parseListOfPoints(points);
        } catch(NumberFormatException e) {
            throw new SVGParseException("Bad list of points in polyline: " + points, e);
        }
        if(pts.isEmpty())
            return null;
        final float[] xs = new float[pts.size()];
        final float[] ys = new float[pts.size()];
        double[] pt = transformToDocumentSpace(pts.get(0), n);
        xs[0] = (float) pt[0];
        ys[0] = (float) pt[1];
        for(int i = 1; i < pts.size(); i++) {
            pt = transformToDocumentSpace(pts.get(i), n);
            xs[i] = (float) pt[0];
            ys[i] = (float) pt[1];
        }
        return new PolygonRoi(xs, ys, PolygonRoi.POLYLINE);
    }
    
    /** Generate an ROI from a {@code polygon} element */
    private PolygonRoi parsePolygon(Element n) throws SVGParseException {
        if(!"polygon".equals(n.getTagName()))
            throw new SVGParseException("\'polygon\' expected, but \'" + n.getLocalName() + "\' found.");
        String points = n.getAttribute("points");
        ArrayList<double[]> pts;
        try {
            pts = parseListOfPoints(points);
        } catch(NumberFormatException e) {
            throw new SVGParseException("Bad points list: " + points, e);
        }
        if(pts.isEmpty())
            return null;
        final float[] xs = new float[pts.size()];
        final float[] ys = new float[pts.size()];
        double[] pt = transformToDocumentSpace(pts.get(0), n);
        xs[0] = (float) pt[0];
        ys[0] = (float) pt[1];
        for(int i = 1; i < pts.size(); i++) {
            pt = transformToDocumentSpace(pts.get(i), n);
            xs[i] = (float) pt[0];
            ys[i] = (float) pt[1];
        }
        return new PolygonRoi(xs, ys, PolygonRoi.POLYGON);
    }
    
    /** Generate an ROI from a {@code line} element.
     *  Lines with the same start and end points will
     *  generate point overlays. */
    private Roi parseLine(Element n) throws SVGParseException {
        if(!"line".equals(n.getTagName()))
            throw new SVGParseException("\'line\' expected, but \'" + n.getLocalName() + "\' found.");
        double x1, y1, x2, y2;
        try {
            x1 = parseLength(n.getAttribute("x1"));
            y1 = parseLength(n.getAttribute("y1"));
            x2 = parseLength(n.getAttribute("x2"));
            y2 = parseLength(n.getAttribute("y2"));
        } catch(NumberFormatException e) {
            throw new SVGParseException("Bad number format in line", e);
        }
        double[] a = {x1, y1};
        double[] b = {x2, y2};
        a = transformToDocumentSpace(a, n);
        b = transformToDocumentSpace(b, n);
        Roi roi;
        if(a[0] == b[0] && a[1] == b[1])
            roi = new PointRoi(a[0], a[1]);
        else {
            roi = new Line(a[0], a[1], b[0], b[1]);
        }
        return roi;
    }
    
    /** Generate an ROI from a {@code circle} element */
    private OvalRoi parseCircle(Element n) throws SVGParseException {
        if(!"circle".equals(n.getTagName()))
            throw new SVGParseException("\'circle\' expected, but \'" + n.getLocalName() + "\' found.");
        double cx, cy, r;
        try {
            cx = parseLength(n.getAttribute("cx"));
            cy = parseLength(n.getAttribute("cy"));
            r = parseLength(n.getAttribute("r"));
        } catch(NumberFormatException e) {
            throw new SVGParseException("Bad number format in ellipse", e);
        }
        return new OvalRoi(cx, cy, r*2, r*2);
    }
    
    /** Generate an ROI from an {@code ellipse} element */
    private Roi parseEllipse(Element n) throws SVGParseException {
        if(!"ellipse".equals(n.getTagName()))
            throw new SVGParseException("\'ellipse\' expected, but \'" + n.getLocalName() + "\' found.");
        double cx, cy, rx, ry;
        try {
            cx = parseLength(n.getAttribute("cx"));
            cy = parseLength(n.getAttribute("cy"));
            rx = parseLength(n.getAttribute("rx"));
            ry = parseLength(n.getAttribute("ry"));
        } catch(NumberFormatException e) {
            throw new SVGParseException("Bad number format in ellipse", e);
        }
        return makeEllipseOverlay(cx, cy, rx, ry, n);
    }
    
    /** Generate an ROI from parsed ellipse data */
    private Roi makeEllipseOverlay(
         double cx, double cy,
         double rx, double ry, Element element)
         throws SVGParseException {
        double[] c = {cx, cy};
        double[] n = {cx, cy + ry};
        double[] e = {cx + rx, cy};
        double[] s = {cx, cy - ry};
        double[] w = {cx - rx, cy};
        c = transformToDocumentSpace(c, element);
        n = transformToDocumentSpace(n, element);
        e = transformToDocumentSpace(e, element);
        s = transformToDocumentSpace(s, element);
        w = transformToDocumentSpace(w, element);
        if(n[0] != s[0] || e[1] != w[1] 
             || Math.abs((4 * c[0]) / (n[0] + e[0] + s[0] + w[0]) - 1) > 2*Math.ulp(c[0])
             || Math.abs((4 * c[1]) / (n[1] + e[1] + s[1] + w[1]) - 1) > 2*Math.ulp(c[1]))
            throw new SVGParseException("Can't create ROI from skewed or rotated ellipse");
        EllipseOverlay eo = new EllipseOverlay(ij);
        return new OvalRoi(w[0], s[1], 2*Math.abs(e[0] - c[0]), 2*Math.abs(n[1] - c[1]));
    }
    
    /** Generate an ROI from a {@code rect} element */
    private Roi parseRect(Element n) throws SVGParseException {
        if(!"rect".equals(n.getTagName()))
            throw new SVGParseException("\'rect\' expected, but \'" + n.getLocalName() + "\' found.");
        double x, y, w, h;
        try {
            x = parseLength(n.getAttribute("x"));
            y = parseLength(n.getAttribute("y"));
            w = parseLength(n.getAttribute("width"));
            h = parseLength(n.getAttribute("height"));
        } catch(NumberFormatException e) {
            throw new SVGParseException("Bad number format in rect");
        }
        double p0[] = {x, y};
        double p1[] = {x + w, y};
        double p2[] = {x + w, y + h};
        double p3[] = {x, y + h};
        p0 = transformToDocumentSpace(p0, n);
        p1 = transformToDocumentSpace(p1, n);
        p2 = transformToDocumentSpace(p2, n);
        p3 = transformToDocumentSpace(p3, n);
        final Roi roi;
        if(p0[0] == p3[0] && p0[1] == p1[1] && p1[0] == p2[0] && p2[1] == p3[1]) {
            roi = new Roi(p0[0], p0[1], p2[0] - p0[0], p2[1] - p0[1]);
        } else {
            final float[] xs = new float[4];
            final float[] ys = new float[4];
            xs[0] = (float) p0[0];
            ys[0] = (float) p0[1];
            xs[1] = (float) p1[0];
            ys[1] = (float) p1[1];
            xs[2] = (float) p2[0];
            ys[2] = (float) p2[1];
            xs[3] = (float) p3[0];
            ys[3] = (float) p3[1];
            roi = new PolygonRoi(xs, ys, PolygonRoi.POLYGON);
        }
        return roi;
    }
    
    /**
     * Generate an ROI from a {@code path} element
     * May return a different type of ROI, depending on the data:
     */
    private Roi parsePath(Element n) throws SVGParseException {
        if(!"path".equals(n.getTagName()))
            throw new SVGParseException("\'path\' expected, but \'" + n.getLocalName() + "\' found.");
        ArrayList<String> instructions = listPathInstructions(n.getAttribute("d"));
        double[] cur = {0, 0};
        double[] lastMove = {0, 0};
        double[] reflectedBezierControl = null;
        double[] reflectedQuadControl = null;
        boolean startedDrawing = false;
        ArrayList<Float> xs = new ArrayList<Float>();
        ArrayList<Float> ys = new ArrayList<Float>();
        Path2D path = new Path2D.Double();
        boolean canUsePolygon = true;
        boolean closed = false;
        for(String i : instructions) {
            String code = i.substring(0, 1);
            i = i.substring(1);
            if(!code.equalsIgnoreCase("c") && !code.equalsIgnoreCase("s"))
                reflectedBezierControl = null;
            if(!code.equalsIgnoreCase("q") && !code.equalsIgnoreCase("t"))
                reflectedQuadControl = null;
            if(!startedDrawing) {
                if(code.equalsIgnoreCase("a")) // Arc as ellipse
                    return arcAsEllipse(code, i, cur, n.getAttribute("d"), n);
                if(!code.equalsIgnoreCase("m"))
                    startedDrawing = true;
            }
            if(code.equalsIgnoreCase("m")) { // Move
                boolean rel = code.equals("m");
                ArrayList<double[]> coords = parseListOfPoints(i);
                if(coords.isEmpty())
                    continue;
                double[] pt = coords.get(0);
                if(rel)
                    pt = vectorAdd(pt, cur);
                path.moveTo(pt[0], pt[1]);
                xs.add((float) pt[0]);
                ys.add((float) pt[1]);
                if(startedDrawing)
                    canUsePolygon = false;
                cur = Arrays.copyOf(pt, 2);
                lastMove = Arrays.copyOf(pt, 2);
                for(int j = 1; j < coords.size(); j++) {
                    pt = coords.get(j);
                    if(rel)
                        pt = vectorAdd(pt, cur);
                    path.lineTo(pt[0], pt[1]);
                    xs.add((float) pt[0]);
                    ys.add((float) pt[1]);
                    cur = pt;
                }
            } else if(code.equalsIgnoreCase("z")) { // Close path
                path.closePath();
                closed = true;
                cur = Arrays.copyOf(lastMove, 2);
            } else if(code.equalsIgnoreCase("l")) { // Line
                boolean rel = code.equals("l");
                ArrayList<double[]> coords = parseListOfPoints(i);
                if(coords.isEmpty())
                    continue;
                for(double[] pt : coords) {
                    if(rel)
                        pt = vectorAdd(pt, cur);
                    path.lineTo(pt[0], pt[1]);
                    xs.add((float) pt[0]);
                    ys.add((float) pt[1]);
                    cur = Arrays.copyOf(pt, 2);
                }
            } else if(code.equalsIgnoreCase("h")) { // Horizontal line
                boolean rel = code.equals("h");
                ArrayList<Double> coords = parseListOfNumbers(i);
                if(coords.isEmpty())
                    continue;
                for(double pt : coords) {
                    if(rel)
                        pt += cur[0];
                    path.lineTo(pt, cur[1]);
                    xs.add((float) pt);
                    ys.add((float) cur[1]);
                    cur[0] = pt;
                }
            } else if(code.equalsIgnoreCase("v")) { // Vertical line
                boolean rel = code.equals("v");
                ArrayList<Double> coords = parseListOfNumbers(i);
                if(coords.isEmpty())
                    continue;
                for(double pt : coords) {
                    if(rel)
                        pt += cur[1];
                    path.lineTo(cur[0], pt);
                    xs.add((float) cur[0]);
                    ys.add((float) pt);
                    cur[1] = pt;
                }
            } else if(code.equalsIgnoreCase("c")) { // Bezier curve
                boolean rel = code.equals("c");
                ArrayList<Double> coords = parseListOfNumbers(i);
                if(coords.isEmpty())
                    continue;
                if(coords.size() % 6 != 0)
                    throw new SVGParseException(
                       "Expect 6 parameters for a \'C\' instruction, but only found "
                       + String.valueOf(coords.size() + ": " + i));
                canUsePolygon = false;
                double[] curTrack = {cur[0], cur[1]};
                for(int j = 0; j < coords.size(); j += 6) {
                    double x1 = coords.get(j);
                    double y1 = coords.get(j + 1);
                    double x2 = coords.get(j + 2);
                    double y2 = coords.get(j + 3);
                    double x = coords.get(j + 4);
                    double y = coords.get(j + 5);
                    if(rel) {
                        x1 += curTrack[0];
                        x2 += curTrack[0];
                        x += curTrack[0];
                        y1 += curTrack[1];
                        y2 += curTrack[1];
                        y += curTrack[1];
                    }
                    path.curveTo(x1, y1, x2, y2, x, y);
                    curTrack[0] = x;
                    curTrack[1] = y;
                    reflectedBezierControl =
                            reflect(new double[] {x2, y2}, curTrack);
                }
                cur = Arrays.copyOf(curTrack, 2);
            } else if(code.equalsIgnoreCase("s")) { // Bezeir curve, shorthand
                boolean rel = code.equals("s");
                ArrayList<Double> coords = parseListOfNumbers(i);
                if(coords.isEmpty())
                    continue;
                if(coords.size() % 4 != 0)
                    throw new SVGParseException(
                       "Expect 4 parameters for an \'S\' instruction, but only found "
                       + String.valueOf(coords.size() + ": " + i));
                canUsePolygon = false;
                double[] curTrack = {cur[0], cur[1]};
                for(int j = 0; j < coords.size(); j += 4) {
                    double x2 = coords.get(j);
                    double y2 = coords.get(j + 1);
                    double x = coords.get(j + 2);
                    double y = coords.get(j + 3);
                    double x1, y1;
                    if(rel) {
                        x2 += cur[0];
                        x += cur[0];
                        y2 += cur[1];
                        y += cur[1];
                    }
                    if(reflectedBezierControl == null) {
                        x1 = x;
                        y1 = x;
                    } else {
                        x1 = reflectedBezierControl[0];
                        y1 = reflectedBezierControl[1];
                    }
                    path.curveTo(x1, y1, x2, y2, x, y);
                    curTrack[0] = x;
                    curTrack[1] = y;
                    reflectedBezierControl =
                            reflect(new double[] {x2, y2}, curTrack);
                }
                cur = Arrays.copyOf(curTrack, 2);
            } else if(code.equalsIgnoreCase("q")) { // Quadratic curve
                boolean rel = code.equals("q");
                ArrayList<Double> coords = parseListOfNumbers(i);
                if(coords.isEmpty())
                    continue;
                if(coords.size() % 4 != 0)
                    throw new SVGParseException(
                       "Expect 4 parameters for an \'Q\' instruction, but only found "
                       + String.valueOf(coords.size() + ": " + i));
                canUsePolygon = false;
                double[] curTrack = {cur[0], cur[1]};
                for(int j = 0; j < coords.size(); j += 4) {
                    double x1 = coords.get(j);
                    double y1 = coords.get(j + 1);
                    double x = coords.get(j + 2);
                    double y = coords.get(j + 3);
                    if(rel) {
                        x1 += cur[0];
                        x += cur[0];
                        y1 += cur[1];
                        y += cur[1];
                    }
                    path.quadTo(x1, y1, x, y);
                    curTrack[0] = x;
                    curTrack[1] = y;
                    reflectedQuadControl =
                            reflect(new double[] {x1, y1}, curTrack);
                }
                cur = Arrays.copyOf(curTrack, 2);
            } else if(code.equalsIgnoreCase("t")) { // Quadratic curve, shorthand
                boolean rel = code.equals("t");
                ArrayList<double[]> coords = parseListOfPoints(i);
                if(coords.isEmpty())
                    continue;
                canUsePolygon = false;
                double[] curTrack = {cur[0], cur[1]};
                for(double[] pt : coords) {
                    double x1, y1;
                    if(rel) {
                        pt[0] += cur[0];
                        pt[1] += cur[1];
                    }
                    if(reflectedQuadControl == null) {
                        x1 = cur[0];
                        y1 = cur[0];
                    } else {
                        x1 = reflectedQuadControl[0];
                        y1 = reflectedQuadControl[1];
                    }
                    path.quadTo(x1, y1, pt[0], pt[1]);
                    curTrack[0] = pt[0];
                    curTrack[1] = pt[1];
                    reflectedQuadControl =
                            reflect(new double[] {x1, y1}, curTrack);
                }
                cur = Arrays.copyOf(curTrack, 2);
            } else {
                throw new SVGParseException("Unparsable instruction: " + code + i);
            }
        }
        if(canUsePolygon) {
            float[] xa = new float[xs.size()];
            float[] ya = new float[ys.size()];
            for(int i=0; i<xs.size(); i++) {
                xa[i] = xs.get(i);
                ya[i] = ys.get(i);
            }
            if(closed)
                return new PolygonRoi(xa, ya, PolygonRoi.POLYGON);
            return new PolygonRoi(xa, ya, PolygonRoi.POLYLINE);
        }
        return new ShapeRoi(path);
    }
    
    /** Attempt to generate an ellipse ROI from an arc path */
    private Roi arcAsEllipse(
            String code,
            String param,
            double[] start,
            String pathData,
            Element n)
            throws SVGParseException {
        boolean rel = code.equals("a");
        double rx, ry, x, y;
        try {
            ArrayList<Double> points;
            points = parseListOfNumbers(param);
            if(points.size() != 14) // Must have two segments
                throw new SVGParseException("Cannot resolve arc commands that do not represent closed ellipses: " + pathData);
            rx = points.get(0);
            if(rx != points.get(7)) // X radii must be same
                throw new SVGParseException("Cannot resolve arc commands that do not represent closed ellipses: " + pathData);
            ry = points.get(1);
            if(ry != points.get(8)) // Y radii must be same
                throw new SVGParseException("Cannot resolve arc commands that do not represent closed ellipses: " + pathData);
            if(points.get(2) != 0 || points.get(9) != 0) // Radii must be axis-aligned
                throw new SVGParseException("Cannot resolve arc commands that do not represent closed ellipses: " + pathData);
            if((points.get(4) != 0) != (points.get(11) != 0)) // Each arc must travel in oposite direction
                throw new SVGParseException("Cannot resolve arc commands that do not represent closed ellipses: " + pathData);
            x = points.get(5);
            y = points.get(6);
            double x0 = points.get(12);
            double y0 = points.get(13);
            if(rel) {
                x += start[0];
                y += start[1];
                x0 += x;
                y0 += y;
            }
            if(x0 != start[0] || y0 != start[1]) // Path mist come full circle
                throw new SVGParseException("Cannot resolve arc commands that do not represent closed ellipses: " + pathData);
        } catch(NumberFormatException e) {
            throw new SVGParseException("Unable to parse arc command: " + pathData, e);
        }
        String next = pathData.substring(
                pathData.indexOf(param) + param.length()).trim();
        if(!(next.startsWith("Z") || next.startsWith("z"))) // Path must be formally closed
            throw new SVGParseException("Cannot resolve arc commands that do not represent closed ellipses: " + pathData);
        return makeEllipseOverlay((x + start[0]) / 2, (y + start[1]) / 2, rx, ry, n);
    }
    
    // -- Vector manipulation helpers --
    
    /** Point {@code a} reflected about point {@code b} */
    private double[] reflect(double[] a, double[] b) {
        return vectorAdd(b, vectorAdd(b, vectorNegate(a)));
    }
    
    /** Vectorized add function */
    private double[] vectorAdd(double[] a, double[] b) {
        if(a.length != b.length)
            throw new IllegalArgumentException(
                    "Can't add vectors of unequal length!");
        double[] c = new double[a.length];
        for(int i = 0; i < a.length; i++)
            c[i] = a[i] + b[i];
        return c;
    }
    
    /** Multiply a vector by {@code -I} */
    private double[] vectorNegate(double[] a) {
        double[] b = Arrays.copyOf(a, a.length);
        for(int i = 0; i < a.length; i++)
            b[i] = b[i] * -1;
        return b;
    }
    
    /**
     * Make a path data {@code String} into a convenient {@code List}!
     */
    private ArrayList<String> listPathInstructions(String pathData) throws SVGParseException {
        if(pathData == null)
            throw new SVGParseException("No path data!");
        pathData = pathData.trim();
        if(pathData.isEmpty())
            throw new SVGParseException("No path data!");
        ArrayList<String> l = new ArrayList<String>();
        int i = 0;
        while(i < pathData.length()) {
            pathData = pathData.substring(i).trim();
            i = getFirstPathInstructionEnd(pathData);
            l.add(pathData.substring(0, i));
        }
        return l;
    }
    
    /**
     * Get the end index of the first path data instruction in {@code pathData}.
     * Does not really verify that the instruction is correctly formed,
     * but identifies the end of the instruction, assuming it is correctly formed.
     * <em>Note that this won't work if {@code pathData} has leading white space!</em>
     */
    private int getFirstPathInstructionEnd(String pathData) {
        String[] chunks;
        int i = 0;
        while(true) {
            chunks = pathData.substring(i).split("[(\\s+)(\\s*,\\s*)]", 2);
            i += chunks[0].length() + 1;
            if(chunks.length == 1 || chunks[1].equalsIgnoreCase("z"))
                return i - 1;
            if(chunks[1].length() < 2)
                continue;
            if(chunks[1].substring(0, 2).matches("[MmLlHhVvCcSsQqTtAa][(\\s)(\\d)]|[Zz]."))
                return i - 1;
        }
    }
       
    /**
     * Parse a list of points, as used in {@code polygon} and {@code polyline} elements
     * @return A {@code List} of points: {@code (x, y)}
     */
    private ArrayList<double[]> parseListOfPoints(String list)
         throws NumberFormatException, SVGParseException {
        if(list == null || list.trim().isEmpty())
                throw new SVGParseException("No points in list.");
        list = list.trim();
        ArrayList<double[]> pts = new ArrayList<double[]>();
        String[] l = list.split("[(\\s+)(\\s*,\\s*)]");
        if(l.length % 2 != 0)
            throw new SVGParseException("Point list cannot have odd number of items: " + list);
        for(int i = 0; i < l.length; i += 2) {
            double[] pt = {parseLength(l[i]), parseLength(l[i+1])};
            pts.add(pt);
        }
        return pts;
    }
    
    /** Parse a list of numbers, possibly with units */
    private ArrayList<Double> parseListOfNumbers(String list)
            throws NumberFormatException, SVGParseException {
        if(list == null || list.trim().isEmpty())
                throw new SVGParseException("No points in list.");
        list = list.trim();
        ArrayList<Double> pts = new ArrayList<Double>();
        String[] l = list.split("[(\\s+)(\\s*,\\s*)]");
        for(String i : l)
            pts.add(new Double(parseLength(i)));
        return pts;
    }
    
    /** Parse an SVG length {@code String} (possibly with units), and convert to pixels */
    private double parseLength(String length) throws SVGParseException, NumberFormatException {
        String[] ls = length.split(
             "[([pP][xX])([iI][nN])([cC][mM])([mM][mM])([pP][tT])([pP][cC])]");
        if(ls.length > 2)
            throw new SVGParseException("Bad number format: " + length);
        double val = new Double(ls[0]);
        if(ls.length == 1 || ls[1].matches("\\s*[pP][xX]\\s*"))
            return val;
        if(ls[1].matches("\\s*[iI][nN]\\s*"))
            return val * 90;
        if(ls[1].matches("\\s*[cC][mM]\\s*"))
            return val * 35.43309;
        if(ls[1].matches("\\s*[mM][mM]\\s*"))
            return val * 0.03543309;
        if(ls[1].matches("\\s*[pP][tT]\\s*"))
            return val * 90 / 72;
        if(ls[1].matches("\\s*[pP][cC]\\s*"))
            return val * 90 / 6;
        throw new SVGParseException("Bad number format: " + length);
    }
    
    /**
     * Transform a point ({@code {x,y}}) to document space
     * by recursively looking for transform attributes in
     * the given nodes and all parent nodes.
     * @param point
     * @param n
     * @return {@code {x,y}}
     */
    private double[] transformToDocumentSpace(double[] point, Node n)
            throws SVGParseException {
        if(n.getNodeType() == Node.ELEMENT_NODE) {
            Element e = (Element)n;
            String tranAttr = e.getAttribute("transform");
            if(tranAttr != null && !tranAttr.equals("")) {
                Stack<String> ts = new Stack<String>();
                int i = getFirstTransformEnd(tranAttr);
                while(i > 0) {
                    ts.push(tranAttr.substring(0, i));
                    tranAttr = tranAttr.substring(i);
                    i = getFirstTransformEnd(tranAttr);
                }
                while(!ts.empty())
                    point = parseTransform(point, ts.pop());
            }
        }
        Node parent = n.getParentNode();
            if(parent != null)
                point = transformToDocumentSpace(point, parent);
        return point;
    }
    
    /**
     * Get the index of the end of the first transform string in a
     * {@code transform} attribute list.
     * 
     * @param transformList The {@code transform} attribute value,
     *    possibly specifying a list of transforms.
     * @return The end index of the first transform in {@code transformList}.
     *    If there are no more transforms in the list, returns {@code 0}.
     * @throws SVGParseException If a formatting error is detected,
     *    but error detection is not guaranteed.
     */
    private int getFirstTransformEnd(
            String transformList)
            throws SVGParseException {
        String[] sp = transformList.split(
                "[(matrix)(translate)(scale)(rotate)(skewX)(skewY)]", 2);
        if(sp.length == 1)
            return 0;
        if(sp[0].matches("\\S+"))
            throw new SVGParseException("Malformed transform attribute: " + transformList);
        final int startIndex = sp[0].length();
        int endIndex = transformList.indexOf(")", startIndex) + 1;
        if(endIndex < 0)
            throw new SVGParseException("Malformed transform attribute: " + transformList);
        if(transformList.length() > endIndex + 1 && transformList.charAt(endIndex) == ',')
            endIndex++;
        return endIndex;
    }
     
    /**
     * Transform a coordinate pair
     * @param point {@code {x,y}}
     * @param transform The transform
     * @return {@code {x,y}}
     */
    private double[] parseTransform(double[] point, String transform)
            throws SVGParseException {
        System.err.println("Transforming (" + point[0] + "," + point[1] + ") using attribute: " + transform);
        if(transform == null || transform.equals(""))
            return point;
        String t = transform.trim();
        
        try {
        if(t.startsWith("matrix")) {
             t = t.substring(t.indexOf("(") + 1, t.lastIndexOf(")"));
             String[] u = t.split("[(\\s*,\\s*)(\\s+)]");
             if(u.length != 6)
                 throw new SVGParseException("Bad transform format: " + transform);
             double[] m = new double[6];
             m[0] = new Double(u[0]);
             m[1] = new Double(u[2]);
             m[2] = new Double(u[4]);
             m[3] = new Double(u[1]);
             m[4] = new Double(u[3]);
             m[5] = new Double(u[5]);
             point = transformMatrix(point, m);
        }
        else if(t.startsWith("translate")) {
             t = t.substring(t.indexOf("(") + 1, t.lastIndexOf(")"));
             String[] u = t.split("[(\\s*,\\s*)(\\s+)]");
             if(u.length > 2)
                 throw new SVGParseException("Bad transform format: " + transform);
             double tx = new Double(u[0]);
             double ty;
             if(u.length > 1)
                  ty = new Double(u[1]);
             else
                  ty = 0;
             point = transformTranslate(point, tx, ty);
        }
        else if(t.startsWith("scale")) {
             t = t.substring(t.indexOf("(") + 1, t.lastIndexOf(")"));
             String[] u = t.split("[(\\s*,\\s*)(\\s+)]");
             if(u.length > 2)
                 throw new SVGParseException("Bad transform format: " + transform);
             double sx = new Double(u[0]);
             double sy;
             if(u.length > 1)
                  sy = new Double(u[1]);
             else
                  sy = 1;
             point = transformScale(point, sx, sy);
        }
        else if(t.startsWith("rotate")) {
             t = t.substring(t.indexOf("(") + 1, t.lastIndexOf(")"));
             String[] u = t.split("[(\\s*,\\s*)(\\s+)]");
             double a = new Double(u[0]);
             if(u.length == 1)
                  point = transformRotate(point, a);
             else if(u.length == 3) {
                  double cx = new Double(u[1]);
                  double cy = new Double(u[2]);
                  point = transformRotate(point, a, cx, cy);
             } else {
                 throw new SVGParseException("Bad transform format: " + transform);
             }
        }
        else if(t.startsWith("skewX")) {
             t = t.substring(t.indexOf("(") + 1, t.lastIndexOf(")"));
             point = transformSkewX(point, new Double(t));
        }
        else if(t.startsWith("skewY")) {
             t = t.substring(t.indexOf("(") + 1, t.lastIndexOf(")"));
             point = transformSkewY(point, new Double(t));
        }
        } catch(NumberFormatException e) {
            throw new SVGParseException("Bad number format in SVG: " + transform);
        }
        
        System.err.println("Result: (" + point[0] + "," + point[1] + ")");
        return point;
    }

     /**
      * Apply a matrix transformation.
      * <p><b>WARNING</b> - <em>Matrix specification (by row)
      * differs from standard SVG order (by column)</em>
      * @param point {@code {x,y}}
      * @param matrix {@code {a,c,e,b,d,f}}
      * @return {@code {x,y}}
      */
     private double[] transformMatrix(double[] point, double[] matrix) {
          if(point == null || matrix == null || point.length != 2 || matrix.length != 6)
               throw new IllegalArgumentException("Malformed matrices - point: " + point +"; matrix: " + matrix);
          double[] r = new double[2];
          r[0] = point[0]*matrix[0] + point[1]*matrix[1] + 1*matrix[2];
          r[1] = point[0]*matrix[3] + point[1]*matrix[4] + 1*matrix[5];
          return r;
     }
     
     /**
      * Apply a translation
      * @param point {@code {x,y}}
      * @param tx Delta x
      * @param ty Delta y
      * @return {@code {x,y}}
      */
     private double[] transformTranslate(double[] point, double tx, double ty) {
          double[] matrix = {1,0,tx,0,1,ty};
          return transformMatrix(point, matrix);
     }
     
     /**
      * Apply a scale
      * @param point {@code {x,y}}
      * @param sx x scale factor
      * @param sy y scale factor
      * @return {@code {x,y}}
      */
     private double[] transformScale(double[] point, double sx, double sy) {
          double[] matrix = {sx,0,0,0,sy,0};
          return transformMatrix(point, matrix);
     }
     
     /**
      * Apply a rotation about the origin
      * @param point {@code {x,y}}
      * @param a Angle, in degrees
      * @return {@code {x,y}}
      */
     private double[] transformRotate(double[] point, double a) {
          double[] matrix = {Math.cos(a), -Math.sin(a), 0, Math.sin(a), Math.cos(a), 0};
          return transformMatrix(point, matrix);
     }
     
     /**
      * Apply a rotation about center point {@code {cx,cy}}
      * @param point {@code {x,y}}
      * @param a Angle, in degrees
      * @return {@code {x,y}}
      */
     private double[] transformRotate(double[] point, double a, double cx, double cy) {
          return transformTranslate(transformRotate(transformTranslate(point, cx, cy), a), -cx, -cy);
     }
     
     /**
      * Skew along the x-axis
      * @param point {@code {x,y}}
      * @param a Angle, in degrees
      * @return {@code {x,y}}
      */
     private double[] transformSkewX(double[] point, double a) {
          double[] matrix = {1, Math.tan(a), 0, 0,1,0};
          return transformMatrix(point, matrix);
     }
     
     /**
      * Skew along the y-axis
      * @param point {@code {x,y}}
      * @param a Angle, in degrees
      * @return {@code {x,y}}
      */
     private double[] transformSkewY(double[] point, double a) {
          double[] matrix = {1,0,0, Math.tan(a),1,0};
          return transformMatrix(point, matrix);
     }
    
}
