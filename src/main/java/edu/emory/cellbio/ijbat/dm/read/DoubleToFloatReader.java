package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.DoubleElement;
import edu.emory.cellbio.ijbat.ex.SlideSetException;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "Number (float)",
        elementType = DoubleElement.class,
        processedType = Float.class,
        hidden = false)
public class DoubleToFloatReader implements
        ElementReader<DoubleElement, Float> {

    public Float read(DoubleElement elementToRead) throws SlideSetException {
        return elementToRead.getUnderlying().floatValue();
    }
    
}
