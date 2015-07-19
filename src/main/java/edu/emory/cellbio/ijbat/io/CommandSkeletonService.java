package edu.emory.cellbio.ijbat.io;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.dm.CommandTemplate;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * Writes {@link edu.emory.cellbio.ijbat.dm.CommandTemplate CommandTemplate}
 * data to an XML file to create a command skeleton and generates
 * {@code CommandTemplate}s from saved command skeleton XML.
 * 
 * @author Benjamin Nanes
 */
public class CommandSkeletonService {
    
    // -- Parameters --
    
    private FileOutputStream fos;
    private XMLStreamWriter xsw;
    private FileInputStream fis;
    private XMLStreamReader xsr;
    
    // -- Methods --
    
    /**
     * Write a command skeleton to an XML file.
     * @param cts The command skeleton, consisting of a stack of {@link edu.emory.cellbio.ijbat.dm.CommandTemplate CommandTemplate}s
     * @param rootColumnTypes A list of {@code DataElement} class names
     *     specifying the column types of the root table. This will be
     *     used as a check to make sure the command skeleton is applied
     *     on appropriate data.
     * @param file The file to be written.
     * @throws IOException
     * @throws XMLStreamException 
     */
    public void write(
            final ArrayDeque<CommandTemplate> cts, 
            final List<String> rootColumnTypes, 
            final File file) 
            throws IOException, XMLStreamException {
        if(!file.exists() && !file.createNewFile()) throw
                new IllegalArgumentException("Could not create file: " + file.getPath());
        if(cts == null || !file.canWrite()) throw
                new IllegalArgumentException("Could not write to file: " + file.getPath());
        try {
            fos = new FileOutputStream(file);
            XMLOutputFactory xof = XMLOutputFactory.newFactory();
            xsw = xof.createXMLStreamWriter(fos);
            xsw.writeStartDocument();
            xsw.writeCharacters("\n");
            xsw.writeStartElement("rootColumnTypes");
            for(int i=0; i<rootColumnTypes.size(); i++) {
                xsw.writeCharacters("\n  ");
                xsw.writeEmptyElement("col");
                xsw.writeAttribute("class", rootColumnTypes.get(i));
            }
            xsw.writeCharacters("\n");
            xsw.writeEndElement();
            while(!cts.isEmpty())
                cts.pop().writeXML(xsw, "");
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
    
    /**
     * Write a command skeleton to an XML file.
     * @param data The leaf of the command skeleton.
     * @param file The file to be written.
     * @throws IOException
     * @throws XMLStreamException
     * @throws SlideSetException 
     */
    public void write(
            final SlideSet data, final File file) 
            throws IOException, XMLStreamException, SlideSetException {
        if(data == null)
            throw new SlideSetException("Table required to export data");
        ArrayDeque<CommandTemplate> cts = new ArrayDeque<CommandTemplate>();
        SlideSet t = data;
        while(t.getParent() != null) {
            CommandTemplate ct = t.getCommandTemplate();
            if(ct == null)
                throw new SlideSetException("### Unable to export command "
                        + "skeleton. ###\n" + t.getName() 
                        + " lacks command template data.");
            cts.push(ct);
            t = t.getParent();
        }
        ArrayList<String> cols = new ArrayList<String>();
        for(int i=0; i<t.getNumCols(); i++)
            cols.add(t.getColumnElementType(i).getName());
        write(cts, cols, file);
    }
    
    /**
     * Read a command skeleton from an XML file.
     * @param file The file to read.
     * @param cts A list to be filled with {@code CommandTemplate}s
     *     from the command skeleton file. The commands are intended
     *     to be executed in the order they are listed.
     * @param rootColumnTypes A list to be filled with {@code DataElement}
     *     class names representing the intended column structure
     *     of appropriate root input data tables for the command skeleton.
     * @throws IOException
     * @throws XMLStreamException
     * @throws SlideSetException 
     */
    public void read(
            final File file, 
            final ArrayList<CommandTemplate> cts, 
            final ArrayList<String> rootColumnTypes) 
            throws IOException, XMLStreamException, SlideSetException {
        if(file == null || !file.canRead()) throw new
                IllegalArgumentException("Could not read file: " +
                file == null ? "<!>" : file.getPath());
        cts.clear();
        rootColumnTypes.clear();
        try {
            fis = new FileInputStream(file);
            xsr = XMLInputFactory.newFactory().createXMLStreamReader(fis);
            while(xsr.hasNext()) {
                if(xsr.next() != XMLStreamReader.START_ELEMENT)
                    continue;
                if(xsr.getLocalName().equals("CommandTemplate"))
                    cts.add(new CommandTemplate(xsr));
                else if(xsr.getLocalName().equals("col"))
                    rootColumnTypes.add(xsr.getAttributeValue(null, "class"));
            }
        }
        catch(FileNotFoundException e) { throw new IllegalArgumentException(e.getMessage()); }
        catch(XMLStreamException e) { throw new IllegalArgumentException(e.getMessage()); }
        finally {
            if(xsr != null) xsr.close();
            if(fis != null) fis.close();
        }
    }
    
}
