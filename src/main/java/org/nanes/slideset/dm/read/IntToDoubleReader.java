package org.nanes.slideset.dm.read;

import org.nanes.slideset.dm.IntegerElement;
import org.nanes.slideset.ex.SlideSetException;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "Integer as double",
        elementType = IntegerElement.class,
        processedType = Double.class,
        hidden = true)
public class IntToDoubleReader implements
        ElementReader<IntegerElement, Double> {

    public Double read(IntegerElement elementToRead) throws SlideSetException {
        return elementToRead.getUnderlying().doubleValue();
    }
    
}
