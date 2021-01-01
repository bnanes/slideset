package org.nanes.slideset.dm.read;

import org.nanes.slideset.dm.IntegerElement;
import org.nanes.slideset.ex.SlideSetException;

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
