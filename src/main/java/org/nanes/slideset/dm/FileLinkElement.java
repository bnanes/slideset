package org.nanes.slideset.dm;

/**
 * {@code DataElement} for storing file references
 * 
 * @author Benjamin Nanes
 */
@DataElementMetadata(name = "~ File")
public class FileLinkElement extends DataElement<String> implements FileLink {

    public FileLinkElement() {
        super("");
    }
    public FileLinkElement(String underlying) {
        super(underlying);
    }

    @Override
    public String getUnderlyingText() {
        return underlying;
    }

    @Override
    public void setUnderlyingText(String text) {
        underlying = text;
    }
    
}
