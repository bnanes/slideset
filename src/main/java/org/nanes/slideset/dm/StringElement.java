package org.nanes.slideset.dm;

/**
 * {@code DataElement} for storing text values
 * 
 * @author Benjamin Nanes
 */
@DataElementMetadata(name = "Text")
public class StringElement extends DataElement<String> {

    public StringElement() {
        super("");
    }
    public StringElement(String underlying) {
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
