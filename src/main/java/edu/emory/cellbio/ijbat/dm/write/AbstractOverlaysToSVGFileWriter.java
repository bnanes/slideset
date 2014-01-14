package edu.emory.cellbio.ijbat.dm.write;

import edu.emory.cellbio.ijbat.dm.AbstractOverlaysAlias;
import edu.emory.cellbio.ijbat.dm.FileLinkElement;
import edu.emory.cellbio.ijbat.dm.MIME;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import edu.emory.cellbio.ijbat.ex.UnsupportedOverlayException;
import imagej.data.overlay.AbstractOverlay;
import imagej.data.overlay.EllipseOverlay;
import imagej.data.overlay.LineOverlay;
import imagej.data.overlay.PointOverlay;
import imagej.data.overlay.PolygonOverlay;
import imagej.data.overlay.RectangleOverlay;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import net.imglib2.meta.Axes;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.roi.PolygonRegionOfInterest;

/**
 *
 * @author Benjamin Nanes
 */
@ElementWriterMetadata(
        name = "SVG file",
        elementType = FileLinkElement.class,
        mimeType = MIME.SVG,
        processedType = AbstractOverlaysAlias.class,
        linkExt = "svg" )
public class AbstractOverlaysToSVGFileWriter implements
        ElementWriter<FileLinkElement, AbstractOverlay[]> {
    
    // -- Fields --
    
    private FileOutputStream fos = null;
    private String errorMessages = "";
    
    // -- Methods --

    public void write(
            AbstractOverlay[] data,
            FileLinkElement elementToWrite)
            throws SlideSetException {
        String path = elementToWrite.getUnderlying();
        String wd = elementToWrite.getOwner().getWorkingDirectory();
        wd = wd == null ? "" : wd;
        if(!(new File(path)).isAbsolute())
            path = wd + File.separator + path;
        write(data, path);
    }
    
    public void write(
            AbstractOverlay[] data,
            String path)
            throws SlideSetException {
        write(data, path, -1, -1, null);
    }
    
    public void write(
            AbstractOverlay[] data,
            String path,
            int width, int height)
            throws SlideSetException {
        write(data, path, width, height, null);
    }
    
    public void write(
            AbstractOverlay[] data,
            String path,
            int width, int height,
            String imgPath)
            throws SlideSetException {
        if(data == null)
            data = new AbstractOverlay[0];
        writeFile(path, data, width, height, imgPath);
    }
    
    // -- Helper methods --
    
    /** Write the SVG file to {@code path} */
    private void writeFile(
            String path,
            AbstractOverlay[] overlays,
            int width,
            int height,
            String img)
            throws SlideSetException {
        XMLStreamWriter xsw = null;
        errorMessages = "";
        try {
            xsw = setupFile(path, width, height, img);
            for(AbstractOverlay overlay : overlays) {
                if(overlay == null)
                    continue;
                try {
                    writeOverlay(xsw, overlay);
                } catch(UnsupportedOverlayException e) {
                    errorMessages += "*" + e.getMessage() + "\n";
                    errorMessages += "The overlay will not be written to the file.\n";
                }
            }
        } catch(Exception e) {
            throw new SlideSetException(e);
        } finally {
            closeFile(xsw);
        }
        if(!errorMessages.isEmpty())
            throw new SlideSetException(errorMessages);
    }
    
    /** Start the SVG file */
    private XMLStreamWriter setupFile(
            String path, int width, int height, String img)
            throws SlideSetException {
        XMLStreamWriter xsw = null;
        File f = new File(path);
        if(!f.getParentFile().exists())
            f.mkdirs();
        try {
            f.createNewFile();
            fos = new FileOutputStream(f);
            xsw = XMLOutputFactory.newFactory().createXMLStreamWriter(fos);
            xsw.writeStartDocument();
            xsw.writeStartElement("svg");
            xsw.setDefaultNamespace("http://www.w3.org/2000/svg");
            xsw.writeDefaultNamespace("http://www.w3.org/2000/svg");
            xsw.setPrefix("xlink", "http://www.w3.org/1999/xlink");
            xsw.writeNamespace("xlink", "http://www.w3.org/1999/xlink");
            if(width > 0 && height > 0) {
                xsw.writeAttribute("width", String.valueOf(width));
                xsw.writeAttribute("height", String.valueOf(height));
            }
            if(img != null && (!img.trim().isEmpty())) {
                xsw.writeStartElement("image");
                xsw.writeAttribute("http://www.w3.org/1999/xlink", "href", img);
                xsw.writeEndElement();
            }
        } catch(Exception e) {
            throw new SlideSetException(e);
        } finally {
            return xsw;
        }
    }
    
    /** End the SVG file and do cleanup */
    private void closeFile(XMLStreamWriter xsw)
            throws SlideSetException {
        try {
            xsw.writeEndElement();
            xsw.writeEndDocument();
            xsw.flush();
            xsw.close();
        } catch(Exception e) {
            throw new SlideSetException(e);
        } finally {
            try {
                fos.close();
            } catch(Exception e) {
                throw new SlideSetException(e);
            }
        }
    }
    
    /** Write an overlay, if supported */
    private void writeOverlay(
            XMLStreamWriter xsw,
            AbstractOverlay overlay) 
            throws SlideSetException {
        if(overlay instanceof PointOverlay)
            writePointOverlay(xsw, (PointOverlay) overlay);
        else if(overlay instanceof LineOverlay)
            writeLineOverlay(xsw, (LineOverlay) overlay);
        else if(overlay instanceof RectangleOverlay)
            writeRectangleOverlay(xsw, (RectangleOverlay) overlay);
        else if(overlay instanceof EllipseOverlay)
            writeEllipseOverlay(xsw, (EllipseOverlay) overlay);
        else if(overlay instanceof PolygonOverlay)
            writePolygonOverlay(xsw, (PolygonOverlay) overlay);
        else
            throw new UnsupportedOverlayException(
                    "Unsupported overlay type: " 
                    + overlay.getClass().getName());
    }
    
    /**
     * Write a point overlay.
     * <P>
     * As it turns out, SVG doesn't actually support
     * point elements, so we'll draw a line with
     * identical start and end points.
     */
    private void writePointOverlay(
            XMLStreamWriter xsw,
            PointOverlay overlay)
            throws SlideSetException {
        int[] axes = new int[2];
        getPlanarAxes(overlay, axes);
        List<double[]> pts = overlay.getPoints();
        try {
            for(double[] pt : pts) {
                xsw.writeStartElement("line");
                xsw.writeAttribute("class", "overlay pointOverlay");
                xsw.writeAttribute("x1", String.valueOf(pt[axes[0]]));
                xsw.writeAttribute("y1", String.valueOf(pt[axes[1]]));
                xsw.writeAttribute("x2", String.valueOf(pt[axes[0]]));
                xsw.writeAttribute("y2", String.valueOf(pt[axes[1]]));
                applyDefaultStyles(xsw);
                xsw.writeAttribute("stroke-linecap", "round");
                xsw.writeEndElement();
            }
        } catch(Exception e) {
            throw new SlideSetException(e);
        }
    }
    
    /** Write a line overlay */
    private void writeLineOverlay(
            XMLStreamWriter xsw, 
            LineOverlay overlay)
            throws SlideSetException {
        int[] axes = new int[2];
        getPlanarAxes(overlay, axes);
        try {
            xsw.writeStartElement("line");
            xsw.writeAttribute("class", "overlay lineOverlay");
            xsw.writeAttribute("x1",
                    String.valueOf(overlay.getLineStart(axes[0])));
            xsw.writeAttribute("y1",
                    String.valueOf(overlay.getLineStart(axes[1])));
            xsw.writeAttribute("x2",
                    String.valueOf(overlay.getLineEnd(axes[0])));
            xsw.writeAttribute("y2",
                    String.valueOf(overlay.getLineEnd(axes[1])));
            applyDefaultStyles(xsw);
            xsw.writeEndElement();
        } catch(Exception e) {
            throw new SlideSetException(e);
        }
    }
    
    /** Write a rectangle overlay */
    private void writeRectangleOverlay(
            XMLStreamWriter xsw,
            RectangleOverlay overlay)
            throws SlideSetException {
        int[] xy = new int[2];
        getPlanarAxes(overlay, xy);
        try {
            xsw.writeStartElement("rect");
            xsw.writeAttribute("class", "overlay rectangleOverlay");
            xsw.writeAttribute("x",
                    String.valueOf(overlay.getOrigin(xy[0])));
            xsw.writeAttribute("y",
                    String.valueOf(overlay.getOrigin(xy[1])));
            xsw.writeAttribute("width",
                    String.valueOf(overlay.getExtent(xy[0])));
            xsw.writeAttribute("height",
                    String.valueOf(overlay.getExtent(xy[1])));
            applyDefaultStyles(xsw);
            xsw.writeEndElement();
        } catch(Exception e) {
            throw new SlideSetException(e);
        }
    }
    
    /** Write an ellipse overlay */
    private void writeEllipseOverlay(
            XMLStreamWriter xsw,
            EllipseOverlay overlay) 
            throws SlideSetException {
        int[] xy = new int[2];
        getPlanarAxes(overlay, xy);
        try {
            xsw.writeStartElement("ellipse");
            xsw.writeAttribute("class", "overlay ellipseOverlay");
            xsw.writeAttribute("cx",
                    String.valueOf(overlay.getOrigin(xy[0])));
            xsw.writeAttribute("cy",
                    String.valueOf(overlay.getOrigin(xy[1])));
            xsw.writeAttribute("rx",
                    String.valueOf(overlay.getRadius(xy[0])));
            xsw.writeAttribute("ry",
                    String.valueOf(overlay.getRadius(xy[1])));
            applyDefaultStyles(xsw);
            xsw.writeEndElement();
        } catch(Exception e) {
            throw new SlideSetException(e);
        }
    }
    
    /** Write a polygon overlay */
    private void writePolygonOverlay(
            XMLStreamWriter xsw,
            PolygonOverlay overlay)
            throws SlideSetException {
        int[] xy = new int[2];
        getPlanarAxes(overlay, xy);
        PolygonRegionOfInterest roi = overlay.getRegionOfInterest();
        try {
            xsw.writeStartElement("polygon");
            xsw.writeAttribute("class", "overlay polygonOverlay");
            String coords = "";
            for(int i=0; i < roi.getVertexCount(); i++) {
                coords += String.valueOf(
                        roi.getVertex(i).getDoublePosition(xy[0])) +
                        "," + String.valueOf(
                        roi.getVertex(i).getDoublePosition(xy[1])) + " ";
            }
            xsw.writeAttribute("points", coords);
            applyDefaultStyles(xsw);
            xsw.writeEndElement();
        } catch(Exception e) {
            throw new SlideSetException(e);
        }
    }
    
    /**
     * Get the X and Y axis indeces for an overlay
     * @param axes A 2-element {@code int} array that
     *    will be filled with the X and Y axis indeces,
     *    in that order.
     */
    private void getPlanarAxes(
            AbstractOverlay overlay,
            int[] axes)
            throws SlideSetException {
        if(axes == null || axes.length != 2)
            throw new SlideSetException(
                    "Need an array to return exactly two integers.");
        CalibratedAxis[] axs = new CalibratedAxis[overlay.numDimensions()];
        overlay.axes(axs);
        if(axs == null || axs.length == 0)
            throw new UnsupportedOverlayException("No axes found!");
        axes[0] = -1;
        axes[1] = -1;
        for(int i=0; i < axs.length; i++) {
            if(axs[i].type() == Axes.X)
                axes[0] = i;
            else if(axs[i].type() == Axes.Y)
                axes[1] = i;
        }
        if(axes[0] < 0 || axes[1] < 0)
            throw new UnsupportedOverlayException("Missing X or Y axis.");
    }
    
    /** Default SVG styling attributes for overlays */
    private void applyDefaultStyles(XMLStreamWriter xsw) 
            throws SlideSetException {
        final String stroke = "#ff0";
        final String strokeWidth = "1.5pt";
        final String fill = "none";
        try {
            xsw.writeAttribute("stroke", stroke);
            xsw.writeAttribute("stroke-width", strokeWidth);
            xsw.writeAttribute("fill", fill);
        } catch(Exception e) {
            throw new SlideSetException(e);
        }
        
    }
    
}
