package org.nanes.slideset.io;

import org.nanes.slideset.SlideSet;
import org.nanes.slideset.dm.CommandTemplate;
import org.nanes.slideset.ex.ColumnTypeException;
import org.nanes.slideset.ex.OperationCanceledException;
import org.nanes.slideset.ex.SlideSetException;
import org.nanes.slideset.pi.PluginInputPicker;
import org.nanes.slideset.pi.PluginOutputPicker;
import org.nanes.slideset.pi.SlideSetPluginLoader;
import org.nanes.slideset.ui.HelpLoader;
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
 * Writes {@link org.nanes.slideset.dm.CommandTemplate CommandTemplate}
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
     * @param cts The command skeleton, consisting of a stack of {@link org.nanes.slideset.dm.CommandTemplate CommandTemplate}s
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
            xsw.writeStartElement("CommandSkeleton");
            xsw.writeAttribute("version", "1.0");
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
            xsw.writeCharacters("\n");
            xsw.writeEndElement();
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
    
    /**
     * Run a command skeleton on a data table.
     * @param cts The list of {@code CommandTemplate}s.
     * @param rootColTypes The list of {@code DataElement} types expected
     *     in the root table.
     * @param root The root table on which the command skeleton will be run.
     * @param sspl The {@link org.nanes.slideset.pi.SlideSetPluginLoader SlideSetPluginLoader} service.
     * @param overrideRootWarning A flag to ignore a discrepancy between
     *     the column types listed in {@code rootColTypes} and the actual
     *     column types found in {@code root}. Note that doing so could
     *     lead to unexpected behavior.
     * @throws ColumnTypeException Indicates a discrepancy between
     *     the column types listed in {@code rootColTypes} and the actual
     *     column types found in {@code root}.
     * @throws SlideSetException Indicates some other error.
     */
    public void runSkeleton(
            final List<CommandTemplate> cts, 
            final List<String> rootColTypes, 
            final SlideSet root, 
            final SlideSetPluginLoader sspl,
            final boolean overrideRootWarning ) 
            throws ColumnTypeException, SlideSetException {
        if(!overrideRootWarning) {
            if(root.getNumCols() != rootColTypes.size())
                throw new ColumnTypeException();
            for(int i=0; i<rootColTypes.size(); i++) {
                if(!root.getColumnElementType(i).getName().equals(rootColTypes.get(i)))
                    throw new ColumnTypeException();
            }
        }
        SlideSet data = root;
        String command;
        PluginInputPicker pip;
        PluginOutputPicker pop;
        CommandTemplate ct;
        for(int i=0; i<cts.size(); i++) {
            ct = cts.get(i);
            command = ct.getCommandClass();
            pip = new CommandTemplateInputPicker(ct);
            pop = new CommandTemplateOutputPicker(ct);
            data = sspl.runPlugin(command, data, pip, pop);
        }
    }
    
    // -- Helper Classes --
    
    protected class CommandTemplateInputPicker implements PluginInputPicker {

        private final CommandTemplate ct;
        
        public CommandTemplateInputPicker(final CommandTemplate ct) {
            this.ct = ct;
        }
        
        public void addInput(final String label, final String[] choices, final Object[] constantRequest, final String[] acceptableValues) {
            // Nothing to do here. The future is predetermined.
        }

        public void setHelpPath(final String helpPath, final HelpLoader helpLoader) {
            // No assistance will be given!
        }

        public void getInputChoices(
                final ArrayList<Integer> inputChoices, 
                final ArrayList<Object> constants) 
                throws OperationCanceledException {
            inputChoices.clear();
            constants.clear();
            ct.getInputChoices(inputChoices, constants);
        }
        
    }
    
    protected class CommandTemplateOutputPicker implements PluginOutputPicker {
        
        private final CommandTemplate ct;

        public CommandTemplateOutputPicker(final CommandTemplate ct) {
            this.ct = ct;
        }

        public void addOutput(final String label, final String[] choices, final boolean[] link, final String[] linkDir, final String[] linkPre, final String[] linkExt) {
            // Nothing to do here. The future is predetermined.
        }

        public void setParentFieldLabels(String[] labels) {
            // Nothing to do here. The future is predetermined.
        }

        public void getOutputChoices(
                final ArrayList<Integer> outputChoices, 
                final ArrayList<Integer> selectedParentFields, 
                final ArrayList<String> linkDir, 
                final ArrayList<String> linkPre, 
                final ArrayList<String> linkExt) 
                throws OperationCanceledException {
            outputChoices.clear();
            selectedParentFields.clear();
            linkDir.clear();
            linkPre.clear();
            linkExt.clear();
            ct.getOutputChoices(outputChoices, selectedParentFields, linkDir, linkPre, linkExt);
        }
        
    }
    
}
