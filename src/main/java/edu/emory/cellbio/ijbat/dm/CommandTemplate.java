package edu.emory.cellbio.ijbat.dm;

import edu.emory.cellbio.ijbat.ex.SlideSetException;
import java.util.ArrayList;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 *
 * @author Benjamin Nanes
 */
public class CommandTemplate {
    
    // -- Fields --
    
    /** Command class name */
    private String className;
    /** Input choices */
    private final ArrayList<Integer> inputChoices;
    /** Input constants as strings */
    private final ArrayList<String> inputConstants;
    /** Output choices */
    private final ArrayList<Integer> outputChoices;
    /** Output directories */
    private final ArrayList<String> outputDirs;
    /** Output file prefixes */
    private final ArrayList<String> outputPres;
    /** Output file extensions */
    private final ArrayList<String> outputExts;
    /** Parent fields to copy */
    private final ArrayList<Integer> parentFields;
    
    // -- Constructor --

    public CommandTemplate() {
        inputChoices = new ArrayList<Integer>();
        inputConstants = new ArrayList<String>();
        outputChoices = new ArrayList<Integer>();
        outputDirs = new ArrayList<String>();
        outputPres = new ArrayList<String>();
        outputExts = new ArrayList<String>();
        parentFields = new ArrayList<Integer>();
    }
    
    public CommandTemplate(XMLStreamReader xsr)
            throws SlideSetException, XMLStreamException {
        this();
        readXML(xsr);
    }
    
    // -- Methods --
    
    /** Set the command class name */
    public void setCommandClass(String cn) {
        className = cn;
    }
    
    /** Get the command class name */
    public String getCommandClass() {
        return className;
    }
    
    /**
     * Set the command input choice parameters.
     * @param ics Input choice indeces
     * @param cs Input constants
     */
    public void setInputChoices(
            final ArrayList<Integer> ics,
            final ArrayList<Object> cs)
            throws SlideSetException {
        if(ics.size() != cs.size())
            throw new SlideSetException();
        inputChoices.clear();
        inputConstants.clear();
        for(int i=0; i < cs.size(); i++) {
            inputChoices.add(ics.get(i));
            inputConstants.add(cs.get(i)==null ? null : cs.get(i).toString());
        }
    }
    
    /**
     * Get the command input choice parameters.
     * @param ics {@code ArrayList} to be filled with input choice indeces
     * @param cs {@code ArrayList} to be filled with input constants
     */
    public void getInputChoices(
            final ArrayList<Integer> ics,
            final ArrayList<Object> cs) {
        ics.clear();
        cs.clear();
        for(int i=0; i < inputChoices.size(); i++) {
            ics.add(inputChoices.get(i));
            cs.add(inputConstants.get(i));
        }
    }
    
    /**
     * Set the command output choices.
     * @param ocs Output selection indeces
     * @param spfs Selected parent field indeces
     * @param lds Link directories
     * @param lps Link prefixes
     * @param les Link extensions
     * @throws SlideSetException 
     */
    public void setOutputChoices(
            final ArrayList<Integer> ocs,
            final ArrayList<Integer> spfs,
            final ArrayList<String> lds,
            final ArrayList<String> lps,
            final ArrayList<String> les)
            throws SlideSetException {
        if(ocs.size() != lds.size() || lds.size() != lps.size() 
                || lps.size() != les.size())
            throw new SlideSetException();
        outputChoices.clear();
        outputDirs.clear();
        outputExts.clear();
        outputPres.clear();
        parentFields.clear();
        for(int i=0; i<ocs.size(); i++) {
            outputChoices.add(ocs.get(i));
            outputDirs.add(lds.get(i));
            outputExts.add(les.get(i));
            outputPres.add(lps.get(i));
        }
        for(int i=0; i<spfs.size(); i++)
            parentFields.add(spfs.get(i));
    }
    
    /**
     * Set the command output choices.
     * @param ocs {@code ArrayList} to be filled with output selection indeces
     * @param spfs {@code ArrayList} to be filled with selected parent field indeces
     * @param lds {@code ArrayList} to be filled with link directories
     * @param lps {@code ArrayList} to be filled with link prefixes
     * @param les {@code ArrayList} to be filled with link extensions
     */
    public void getOutputChoices(
            final ArrayList<Integer> ocs,
            final ArrayList<Integer> spfs,
            final ArrayList<String> lds,
            final ArrayList<String> lps,
            final ArrayList<String> les) {
        ocs.clear();
        spfs.clear();
        lds.clear();
        lps.clear();
        les.clear();
        for(int i=0; i<outputChoices.size(); i++) {
            ocs.add(outputChoices.get(i));
            lds.add(outputDirs.get(i));
            lps.add(outputPres.get(i));
            les.add(outputExts.get(i));
        }
        for(int i=0; i<parentFields.size(); i++)
            spfs.add(parentFields.get(i));
    }
    
    /**
     * Write these properties to an XML stream.
     * @param xsw The {@code XMLStreamWriter}
     * @param indent White space to put at the beginning of each line
     * @throws XMLStreamException 
     */
    public void writeXML(
            final XMLStreamWriter xsw, 
            final String indent) 
            throws XMLStreamException {
        xsw.writeCharacters("\n" + indent);
        xsw.writeStartElement("CommandTemplate");
        xsw.writeCharacters("\n" + indent);
        xsw.writeEmptyElement("command");
        xsw.writeAttribute("class", className);
        for(int i=0; i<inputChoices.size(); i++) {
            xsw.writeCharacters("\n" + indent);
            xsw.writeEmptyElement("input");
            xsw.writeAttribute("choice", inputChoices.get(i).toString());
            xsw.writeAttribute("const", inputConstants.get(i)==null ? "" : inputConstants.get(i));
        }
        for(int i=0; i<outputChoices.size(); i++) {
            xsw.writeCharacters("\n" + indent);
            xsw.writeEmptyElement("output");
            xsw.writeAttribute("choice", outputChoices.get(i).toString());
            xsw.writeAttribute("dir", outputDirs.get(i));
            xsw.writeAttribute("pre", outputPres.get(i));
            xsw.writeAttribute("ext", outputExts.get(i));
        }
        for(int i=0; i<parentFields.size(); i++) {
            xsw.writeCharacters("\n" + indent);
            xsw.writeEmptyElement("parent");
            xsw.writeAttribute("field", parentFields.get(i).toString());
        }
        xsw.writeCharacters("\n" + indent);
        xsw.writeEndElement();
    }
    
    /**
     * Populate these properties from an XML stream.
     * @param xsr The {@code XMLStreamReader}
     * @throws SlideSetException
     * @throws XMLStreamException 
     */
    public final void readXML(
            final XMLStreamReader xsr) 
            throws SlideSetException, XMLStreamException {
        if(!xsr.getLocalName().equals("CommandTemplate"))
            throw new SlideSetException();
        outputChoices.clear();
        outputDirs.clear();
        outputExts.clear();
        outputPres.clear();
        parentFields.clear();
        String tag;
        do {
            switch(xsr.getEventType()) {
                case XMLStreamReader.START_ELEMENT:
                    tag = xsr.getLocalName();
                    if(tag.equals("command"))
                        className = xsr.getAttributeValue(null, "class");
                    else if(tag.equals("input")) {
                        inputChoices.add(new Integer(
                                xsr.getAttributeValue(null, "choice")));
                        inputConstants.add(
                                xsr.getAttributeValue(null, "const"));
                    }
                    else if(tag.equals("output")) {
                        outputChoices.add(new Integer(
                                xsr.getAttributeValue(null, "choice")));
                        outputDirs.add(
                                xsr.getAttributeValue(null, "dir"));
                        outputPres.add(
                                xsr.getAttributeValue(null, "pre"));
                        outputExts.add(
                                xsr.getAttributeValue(null, "ext"));
                    }
                    else if(tag.equals("parent"))
                        parentFields.add(new Integer(
                                xsr.getAttributeValue(null, "field")));
                    break;
                case XMLStreamReader.END_ELEMENT:
                    if(xsr.getLocalName().equals("CommandTemplate"))
                        return;
                    break;
            }
        } while(xsr.hasNext() && xsr.next() != XMLStreamReader.END_DOCUMENT);
    }
}
