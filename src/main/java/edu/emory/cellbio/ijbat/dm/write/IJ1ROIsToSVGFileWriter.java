package edu.emory.cellbio.ijbat.dm.write;

import edu.emory.cellbio.ijbat.dm.FileLinkElement;
import edu.emory.cellbio.ijbat.dm.MIME;
import edu.emory.cellbio.ijbat.dm.RoisAlias;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import edu.emory.cellbio.ijbat.ex.UnsupportedOverlayException;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.FloatPolygon;
import java.awt.geom.PathIterator;
import java.io.File;
import java.io.FileOutputStream;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Write ImageJ1-style ROIs as SVG files.
 * 
 * <p>Not compatible with {@code TextRoi} or {@code EllipseRoi}
 * 
 * @author Benjamin Nanes
 */
@ElementWriterMetadata(
        name = "SVG file",
        elementType = FileLinkElement.class,
        mimeType = MIME.SVG,
        processedType = RoisAlias.class,
        linkExt = "svg" )
public class IJ1ROIsToSVGFileWriter implements
        ElementWriter<FileLinkElement, Roi[]> {
    
    // -- Fields --
    
    private FileOutputStream fos = null;
    private String errorMessages = "";
    
    // -- Methods --

    public void write(
            Roi[] data,
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
            Roi[] data,
            String path)
            throws SlideSetException {
        write(data, path, -1, -1, null);
    }
    
    public void write(
            Roi[] data,
            String path,
            int width, int height)
            throws SlideSetException {
        write(data, path, width, height, null);
    }
    
    public void write(
            Roi[] data,
            String path,
            int width, int height,
            String imgPath)
            throws SlideSetException {
        if(data == null)
            data = new Roi[0];
        writeFile(path, data, width, height, imgPath);
    }
    
    // -- Helper methods --
    
    /** Write the SVG file to {@code path} */
    private void writeFile(
            String path,
            Roi[] rois,
            int width,
            int height,
            String img)
            throws SlideSetException {
        XMLStreamWriter xsw = null;
        errorMessages = "";
        try {
            xsw = setupFile(path, width, height, img);
            for(Roi roi : rois) {
                if(roi == null)
                    continue;
                try {
                    writeOverlay(xsw, roi);
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
            f.getParentFile().mkdirs();
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
            Roi roi) 
            throws SlideSetException {
        if(roi instanceof Line)
            writeLine(xsw, (Line) roi);
        else if(roi instanceof OvalRoi)
            writeOvalRoi(xsw, (OvalRoi) roi);
        else if(roi instanceof PointRoi)
            writePointRoi(xsw, (PointRoi) roi);
        else if(roi instanceof PolygonRoi)
            writePolygonRoi(xsw, (PolygonRoi) roi);
        else if(roi instanceof ShapeRoi)
            writeShapeRoi(xsw, (ShapeRoi) roi);
        else if(roi.getType() == Roi.RECTANGLE)
            writeRectangle(xsw, roi);
        else
            throw new UnsupportedOverlayException(
                    "Unsupported ROI type: " 
                    + roi.getClass().getName());
    }
    
    //-- Element writers --
    
    /** Write a Line ROI */
    private void writeLine(
            XMLStreamWriter xsw, 
            Line roi) 
            throws SlideSetException {
        try {
            xsw.writeStartElement("line");
            xsw.writeAttribute("class", "roi Line");
            xsw.writeAttribute("x1",
                    String.valueOf(roi.x1d));
            xsw.writeAttribute("y1",
                    String.valueOf(roi.y1d));
            xsw.writeAttribute("x2",
                    String.valueOf(roi.x2d));
            xsw.writeAttribute("y2",
                    String.valueOf(roi.y2d));
            applyDefaultStyles(xsw);
            xsw.writeEndElement();
        } catch(Exception e) {
            throw new SlideSetException(e);
        }
    }
    
    /** Write an OvalRoi */
    private void writeOvalRoi(
            final XMLStreamWriter xsw, 
            final OvalRoi roi) 
            throws SlideSetException {
        final double rx = roi.getFloatWidth()/2;
        final double ry = roi.getFloatHeight()/2;
        final double x = roi.getXBase();
        final double y = roi.getYBase();
        try {
            xsw.writeStartElement("ellipse");
            xsw.writeAttribute("class", "roi OvalRoi");
            xsw.writeAttribute("cx",
                    String.valueOf(x+rx));
            xsw.writeAttribute("cy",
                    String.valueOf(y+ry));
            xsw.writeAttribute("rx",
                    String.valueOf(rx));
            xsw.writeAttribute("ry",
                    String.valueOf(ry));
            applyDefaultStyles(xsw);
            xsw.writeEndElement();
        } catch(Exception e) {
            throw new SlideSetException(e);
        }
    }
    
    /** Write a PointRoi */
    private void writePointRoi(
            final XMLStreamWriter xsw, 
            final PointRoi roi) 
            throws SlideSetException {
        final FloatPolygon fp = roi.getFloatPolygon();
        try {
            for(int i=0; i<fp.npoints; i++) {
                xsw.writeStartElement("line");
                xsw.writeAttribute("class", "roi PointRoi");
                xsw.writeAttribute("x1", String.valueOf(fp.xpoints[i]));
                xsw.writeAttribute("y1", String.valueOf(fp.ypoints[i]));
                xsw.writeAttribute("x2", String.valueOf(fp.xpoints[i]));
                xsw.writeAttribute("y2", String.valueOf(fp.ypoints[i]));
                applyDefaultStyles(xsw);
                xsw.writeAttribute("stroke-linecap", "round");
                xsw.writeEndElement();
            }
        } catch(Exception e) {
            throw new SlideSetException(e);
        }
    }
    
    /** Write a PolygonRoi */
    private void writePolygonRoi(
            final XMLStreamWriter xsw, 
            final PolygonRoi roi) 
            throws SlideSetException {
        final FloatPolygon fp = roi.getFloatPolygon();
        try {
            if(roi.getType() == Roi.POLYGON)
                xsw.writeStartElement("polygon");
            else
                xsw.writeStartElement("polyline");
            xsw.writeAttribute("class", "roi PolygonRoi");
            String coords = "";
            for(int i=0; i < fp.npoints; i++) {
                coords += String.valueOf(
                        fp.xpoints[i]) +
                        "," + String.valueOf(
                        fp.ypoints[i]) + " ";
            }
            xsw.writeAttribute("points", coords);
            applyDefaultStyles(xsw);
            xsw.writeEndElement();
        } catch(Exception e) {
            throw new SlideSetException(e);
        }
    }
    
    /** Write a ShapeRoi */
    private void writeShapeRoi(
            final XMLStreamWriter xsw, 
            final ShapeRoi roi) 
            throws SlideSetException {
        final PathIterator pi = roi.getShape().getPathIterator(null);
        final StringBuffer d = new StringBuffer("");
        final double[] cds = new double[6];
        do {
            switch(pi.currentSegment(cds)) {
                case PathIterator.SEG_MOVETO:
                    d.append("M ");
                    d.append(cds[0]);
                    d.append(" ");
                    d.append(cds[1]);
                    d.append(" ");
                    break;
                case PathIterator.SEG_LINETO:
                    d.append("L ");
                    d.append(cds[0]);
                    d.append(" ");
                    d.append(cds[1]);
                    d.append(" ");
                    break;
                case PathIterator.SEG_CUBICTO:
                    d.append("C ");
                    d.append(cds[0]);
                    d.append(" ");
                    d.append(cds[1]);
                    d.append(" ");
                    d.append(cds[2]);
                    d.append(" ");
                    d.append(cds[3]);
                    d.append(" ");
                    d.append(cds[4]);
                    d.append(" ");
                    d.append(cds[5]);
                    d.append(" ");
                    break;
                case PathIterator.SEG_QUADTO:
                    d.append("Q ");
                    d.append(cds[0]);
                    d.append(" ");
                    d.append(cds[1]);
                    d.append(" ");
                    d.append(cds[2]);
                    d.append(" ");
                    d.append(cds[3]);
                    break;
                case PathIterator.SEG_CLOSE:
                    d.append("Z");
                    break;
            }
        } while(!pi.isDone());
        try {
            xsw.writeStartElement("path");
            xsw.writeAttribute("class", "overlay generalPathOverlay");
            xsw.writeAttribute("d", d.toString());
            applyDefaultStyles(xsw);
            xsw.writeEndElement();
        } catch(XMLStreamException e) {
            throw new SlideSetException(e);
        }
    }
    
    /** Write a rectangular Roi */
    private void writeRectangle(
            final XMLStreamWriter xsw, 
            final Roi roi) 
            throws SlideSetException {
        try {
            xsw.writeStartElement("rect");
            xsw.writeAttribute("class", "roi Roi");
            xsw.writeAttribute("x",
                    String.valueOf(roi.getXBase()));
            xsw.writeAttribute("y",
                    String.valueOf(roi.getYBase()));
            xsw.writeAttribute("width",
                    String.valueOf(roi.getFloatWidth()));
            xsw.writeAttribute("height",
                    String.valueOf(roi.getFloatHeight()));
            applyDefaultStyles(xsw);
            xsw.writeEndElement();
        } catch(Exception e) {
            throw new SlideSetException(e);
        }
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
