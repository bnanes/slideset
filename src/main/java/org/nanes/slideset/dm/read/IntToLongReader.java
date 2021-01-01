package org.nanes.slideset.dm.read;

import org.nanes.slideset.dm.IntegerElement;
import org.nanes.slideset.ex.SlideSetException;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "Integer (long)",
        elementType = IntegerElement.class,
        processedType = Long.class,
        hidden = false)
public class IntToLongReader implements
        ElementReader<IntegerElement, Long> {

    public Long read(IntegerElement elementToRead) throws SlideSetException {
        return elementToRead.getUnderlying().longValue();
    }
    
}
