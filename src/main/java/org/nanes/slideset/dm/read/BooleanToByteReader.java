package org.nanes.slideset.dm.read;

import org.nanes.slideset.dm.BooleanElement;
import org.nanes.slideset.ex.SlideSetException;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "0 or 1",
        elementType = BooleanElement.class,
        processedType = Byte.class,
        hidden = true)
public class BooleanToByteReader implements
        ElementReader<BooleanElement, Byte> {
    
    private static final byte t = 1;
    private static final byte f = 0;
    
    public Byte read(BooleanElement elementToRead) throws SlideSetException {
        return elementToRead.getUnderlying() ? t : f;
    }
    
}
