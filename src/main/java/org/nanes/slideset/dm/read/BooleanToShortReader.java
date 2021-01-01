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
        processedType = Short.class,
        hidden = true)
public class BooleanToShortReader implements
        ElementReader<BooleanElement, Short> {

    static final short t = 1;
    static final short f = 0;
    
    public Short read(BooleanElement elementToRead) throws SlideSetException {
        return elementToRead.getUnderlying() ? t : f;
    }
    
}
