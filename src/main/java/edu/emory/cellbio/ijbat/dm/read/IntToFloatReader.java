package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.IntegerElement;
import edu.emory.cellbio.ijbat.ex.SlideSetException;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "Integer as float",
        elementType = IntegerElement.class,
        processedType = Float.class,
        hidden = true)
public class IntToFloatReader implements
        ElementReader<IntegerElement, Float> {

    public Float read(IntegerElement elementToRead) throws SlideSetException {
        return elementToRead.getUnderlying().floatValue();
    }
    
}
