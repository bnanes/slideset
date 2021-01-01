package org.nanes.slideset.dm.read;

import org.nanes.slideset.dm.IntegerElement;
import org.nanes.slideset.ex.SlideSetException;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "Integer (short)",
        elementType = IntegerElement.class,
        processedType = Short.class,
        hidden = false)
public class IntToShortReader implements
        ElementReader<IntegerElement, Short> {

    public Short read(IntegerElement elementToRead) throws SlideSetException {
        return elementToRead.getUnderlying().shortValue();
    }
    
}
