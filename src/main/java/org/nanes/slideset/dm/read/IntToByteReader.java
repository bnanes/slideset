package org.nanes.slideset.dm.read;

import org.nanes.slideset.dm.IntegerElement;
import org.nanes.slideset.ex.SlideSetException;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "Byte",
        elementType = IntegerElement.class,
        processedType = Byte.class,
        hidden = false)
public class IntToByteReader implements
        ElementReader<IntegerElement, Byte> {

    public Byte read(IntegerElement elementToRead) throws SlideSetException {
        return elementToRead.getUnderlying().byteValue();
    }
    
}
