package edu.emory.cellbio.ijbat.dm;

import edu.emory.cellbio.ijbat.ex.SlideSetException;

/**
 * {@code DataElement} for storing numeric values
 * 
 * @author Benjamin Nanes
 */
@DataElementMetadata(name = "Number")
public class DoubleElement extends DataElement<Double>{
    
    public DoubleElement() {
        super(0.0);
    }

    public DoubleElement(Double underlying) {
        super(underlying);
    }

    @Override
    public String getUnderlyingText() {
        return underlying.toString();
    }

    @Override
    public void setUnderlyingText(String text) throws SlideSetException {
        try {
            underlying = new Double(text);
        } catch(NumberFormatException e) {
            throw new SlideSetException(e);
        }
    }
    
}
