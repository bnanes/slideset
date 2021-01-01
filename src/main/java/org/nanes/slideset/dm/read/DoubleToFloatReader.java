package org.nanes.slideset.dm.read;

import org.nanes.slideset.dm.DoubleElement;
import org.nanes.slideset.ex.SlideSetException;

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
