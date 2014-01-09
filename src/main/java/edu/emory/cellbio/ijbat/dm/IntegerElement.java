package edu.emory.cellbio.ijbat.dm;

import edu.emory.cellbio.ijbat.ex.SlideSetException;

/**
 * {@code DataElement} for storing integer values
 * 
 * @author Benjamin Nanes
 */
@DataElementMetadata(name = "Integer")
public class IntegerElement extends DataElement<Integer> {

    public IntegerElement() {
        super(0);
    }
    public IntegerElement(Integer underlying) {
        super(underlying);
    }

    @Override
    public String getUnderlyingText() {
        return underlying.toString();
    }

    @Override
    public void setUnderlyingText(String text) throws SlideSetException {
        try {
            underlying = new Integer(text);
        } catch(NumberFormatException e) {
            throw new SlideSetException(e);
        }
    }
    
}
