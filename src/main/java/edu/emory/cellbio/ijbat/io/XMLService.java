package edu.emory.cellbio.ijbat.io;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.dm.DataElement;
import edu.emory.cellbio.ijbat.dm.DataTypeIDService;

import imagej.ImageJ;
import org.scijava.plugin.PluginService;

import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.LinkedHashMap;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;


/**
 * Class with methods for writing {@code SlideSet} data as an XML file
 * and for generating a {@code SlideSet} from saved XML data.
 * 
 * @author Benjamin Nanes
 */
public class XMLService {
     
     // -- Parameters --
     
     private ImageJ ij;
     private PluginService ps;
     private DataTypeIDService dtid;
     
     private FileOutputStream fos;
     private FileInputStream fis;
     private XMLStreamWriter xsw;
     private XMLStreamReader xsr;
     
     private boolean includeTree = true;
     
     // -- Constructor --
     
     public XMLService(ImageJ context, DataTypeIDService dtid) {
          this.ij = context;
          this.ps = ij.get(PluginService.class);
          this.dtid = dtid;
     }
     
     // -- Methods and helper methods --
     
     //  - Write -
     
     /**
      * Write {@code SlideSet} data to an XML file.
      * @throws IOException
      * @throws XMLStreamException 
      */
     public void write(SlideSet data, File file) throws IOException, XMLStreamException {
          if(!file.exists() && !file.createNewFile()) throw
               new IllegalArgumentException("Could not create file: " + file.getPath());
          if(data == null || !file.canWrite()) throw
               new IllegalArgumentException("Could not write to file: " + file.getPath());
          if(includeTree)
               while(data.getParent() != null)
                    data = data.getParent();
          try {
               fos = new FileOutputStream(file);
               XMLOutputFactory xof = XMLOutputFactory.newFactory();
               xsw = xof.createXMLStreamWriter(fos);
               xsw.writeStartDocument();
               writeTable(data, 0);
               xsw.writeEndDocument();
               xsw.flush();
          }
          catch(FileNotFoundException e) { throw new IllegalArgumentException(e.getMessage()); }
          catch(XMLStreamException e) { throw new IllegalArgumentException(e.getMessage()); }
          finally {
               if(xsw != null) xsw.close();
               if(fos != null) fos.close();
          }
     }
     
     /** Nestable utility function to write one SlideSet */
     private void writeTable(SlideSet data, int level) throws XMLStreamException {
          xsw.writeCharacters("\n" + ind(level));
          xsw.writeStartElement("SlideSet");
          xsw.writeAttribute("name", data.getName());
          for(Map.Entry<String, String> e : data.getCreationParams().entrySet()) {
               xsw.writeCharacters("\n" + ind(level+1));
               xsw.writeStartElement("param");
               xsw.writeAttribute("name", e.getKey());
               xsw.writeCharacters(e.getValue());
               xsw.writeEndElement();
          }
          for(int i=0; i<data.getNumCols(); i++) {
               xsw.writeCharacters("\n" + ind(level+1));
               xsw.writeStartElement("col");
               for(Map.Entry<String, String> e : data.getColumnProperties(i).entrySet())
                    if(e.getValue() != null)
                         xsw.writeAttribute(e.getKey(), e.getValue());
               for(int j=0; j<data.getNumRows(); j++) {
                    xsw.writeCharacters("\n" + ind(level+2));
                    xsw.writeStartElement("e");
                    xsw.writeCharacters(data.getItemText(i, j));
                    xsw.writeEndElement();
               }
               xsw.writeCharacters("\n" + ind(level+1));
               xsw.writeEndElement();
          }
          if(includeTree)
               for(SlideSet child : data.getChildren())
                    writeTable(child, level + 1);
          xsw.writeCharacters("\n" + ind(level));
          xsw.writeEndElement();
     }
     
     /** Generate an indent for "pretty" XML */
     private String ind(int level) {
          String r = "";
          for(int i=0; i<level; i++)
               r += "   ";
          return r;
     }
     
     //  - Read -
     
     /**
      * Read {@code SlideSet} data from an XML file
      * @throws IOException
      * @throws XMLStreamException 
      */
     public SlideSet read(File file) throws IOException, XMLStreamException {
          if(file == null || !file.canRead()) throw new
               IllegalArgumentException("Could not read file: " +
               file == null ? "<!>" : file.getPath());
          if(dtid == null) dtid = new DataTypeIDService(ij);
          SlideSet result = new SlideSet(ij, dtid);
          try {
               fis = new FileInputStream(file);
               xsr = XMLInputFactory.newFactory().createXMLStreamReader(fis);
               do { } while(xsr.hasNext() &&
                    ( xsr.next() != XMLStreamReader.START_ELEMENT &&
                    xsr.getLocalName().equals("SlideSet")));
               result = readTable();
          }
          catch(FileNotFoundException e) { throw new IllegalArgumentException(e.getMessage()); }
          catch(XMLStreamException e) { throw new IllegalArgumentException(e.getMessage()); }
          finally {
               if(xsr != null) xsr.close();
               if(fis != null) fis.close();
          }
          Util.setPathForTree(result, file.getParent());
          return result;
     }
     
     /** Nestable utility function to read one SlideSet */
     private SlideSet readTable() throws XMLStreamException {
          SlideSet result = new SlideSet(ij, dtid);
          result.setName(xsr.getAttributeValue(null, "name"));
          xsr.next();
          ArrayList<DataElement> col = null;
          LinkedHashMap<String, String> props = null;
          //String colName = null;
          //String typeCode = null;
          do {
               switch(xsr.getEventType()) {
                    case XMLStreamReader.START_ELEMENT:
                         if(xsr.getLocalName().equals("param"))
                              result.getCreationParams().put(
                                   xsr.getAttributeValue(null, "name"),
                                   xsr.getElementText());
                         else if(xsr.getLocalName().equals("col")) {
                              col = new ArrayList<DataElement>(16);
                              props = new LinkedHashMap<String, String>();
                              for(int i=0; i<xsr.getAttributeCount(); i++)
                                   props.put(xsr.getAttributeLocalName(i), xsr.getAttributeValue(i));
                              if(props.get("name") == null)
                                   props.put("name", "X");
                              /*colName = xsr.getAttributeValue(null, "name");
                              colName = colName == null ? "X" : colName;
                              typeCode = xsr.getAttributeValue(null, "type");*/
                         }
                         else if(xsr.getLocalName().equals("e")) {
                              if(col == null)
                                   throw new IllegalArgumentException(
                                        "XML format error - DataElement outside of column");
                              String val = xsr.getElementText();
                              DataElement element =
                                   dtid.createDataElement(val, props.get("type"), result);
                              col.add(element);
                         }
                         else if(xsr.getLocalName().equals("SlideSet")) {
                              SlideSet child = readTable();
                              child.setParent(result);
                              result.addChild(child);
                         }
                         break;
                    case XMLStreamReader.END_ELEMENT:
                         if(xsr.getLocalName().equals("col")) {
                              result.addColumn(props, col);
                              //result.addColumn(colName, typeCode, col);
                              col = null;
                              props = null;
                              /*colName = null;
                              typeCode = null;*/
                         }
                         else if(xsr.getLocalName().equals("SlideSet"))
                              return result;
                         break;
                    default:
               }
          } while(xsr.hasNext() && xsr.next() != XMLStreamReader.END_DOCUMENT);
          return result;
     }
     
     // -- Test methods --
     
}