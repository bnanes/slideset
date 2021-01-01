package org.nanes.slideset.dm.read;

import org.nanes.slideset.dm.IntegerElement;
import org.nanes.slideset.ex.SlideSetException;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "Integer",
        elementType = IntegerElement.class,
        processedType = Integer.class,
        hidden = false)
public class IntToIntReader implements 
        ElementReader<IntegerElement, Integer> {

    public Integer read(IntegerElement elementToRead) throws SlideSetException {
        return elementToRead.getUnderlying();
    }
    
}
