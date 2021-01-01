package org.nanes.slideset.dm;

import org.nanes.slideset.ex.SlideSetException;

/**
 * {@code DataElement} for storing logical values
 * 
 * @author Benjamin Nanes
 */
@DataElementMetadata(name = "Logical")
public class BooleanElement extends DataElement<Boolean> {

    public BooleanElement() {
        super(false);
    }
    public BooleanElement(Boolean underlying) {
        super(underlying);
    }

    @Override
    public String getUnderlyingText() {
        return underlying.toString();
    }

    @Override
    public void setUnderlyingText(String text) throws SlideSetException {
        if( text.equals("t") || text.equals("T")
                || text.equals("true") || text.equals("True")
                || text.equals("TRUE") || text.equals("1"))
            underlying = true;
        else if( text.equals("f") || text.equals("F")
                || text.equals("false") || text.equals("False")
                || text.equals("FALSE") || text.equals("0"))
            underlying = false;
        else throw new SlideSetException(
                "\"" + text + "\" is not a logical value.");
    }
    
}
