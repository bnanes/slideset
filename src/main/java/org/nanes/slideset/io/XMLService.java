package org.nanes.slideset.io;

import org.nanes.slideset.SlideSet;
import org.nanes.slideset.dm.CommandTemplate;
import org.nanes.slideset.dm.DataTypeIDService;
import org.nanes.slideset.ex.SlideSetException;

import net.imagej.ImageJ;
import org.scijava.plugin.PluginService;

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
 * Writes {@code SlideSet} data as an XML file
 * and generates a {@code SlideSet} from saved XML data.
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
          if(data.isLocked())
              xsw.writeAttribute("locked", "true");
          if(data.getCommandTemplate() != null)
              data.getCommandTemplate().writeXML(xsw, ind(level+1));
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
     public SlideSet read(File file)
             throws IOException, XMLStreamException, SlideSetException {
          if(file == null || !file.canRead()) throw new
               IllegalArgumentException("Could not read file: " +
               file == null ? "<!>" : file.getPath());
          if(dtid == null) dtid = new DataTypeIDService(ij);
          SlideSet result;
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
     private SlideSet readTable() throws XMLStreamException, SlideSetException {
          SlideSet result = new SlideSet(ij, dtid);
          result.setName(xsr.getAttributeValue(null, "name"));
          if(xsr.getAttributeValue(null, "locked") != null)
              result.setLock(true);
          xsr.next();
          int colI = -1;
          int rowI = 0;
          LinkedHashMap<String, String> props;
          do {
               switch(xsr.getEventType()) {
                    case XMLStreamReader.START_ELEMENT:
                         if(xsr.getLocalName().equals("param"))
                              result.getCreationParams().put(
                                   xsr.getAttributeValue(null, "name"),
                                   xsr.getElementText());
                         else if(xsr.getLocalName().equals("col")) {
                              props = new LinkedHashMap<String, String>();
                              for(int i=0; i<xsr.getAttributeCount(); i++)
                                   props.put(xsr.getAttributeLocalName(i), xsr.getAttributeValue(i));
                              if(props.get("name") == null)
                                   props.put("name", "X");
                              colI = result.addColumn(props);
                         }
                         else if(xsr.getLocalName().equals("e")) {
                              if(colI == -1)
                                   throw new IllegalArgumentException(
                                        "XML format error - DataElement outside of column");
                              if(colI == 0)
                                  result.addRow();
                              String val = xsr.getElementText();                            
                              result.getDataElement(colI, rowI)
                                      .setUnderlyingText(val);
                              rowI++;
                         }
                         else if(xsr.getLocalName().equals("CommandTemplate"))
                              result.setCommandTemplate(new CommandTemplate(xsr));
                         else if(xsr.getLocalName().equals("SlideSet")) {
                              SlideSet child = readTable();
                              child.setParent(result);
                              result.addChild(child);
                         }
                         break;
                    case XMLStreamReader.END_ELEMENT:
                         if(xsr.getLocalName().equals("col")) {
                              colI = -1;
                              rowI = 0;
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
