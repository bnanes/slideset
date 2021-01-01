package org.nanes.slideset.dm.read;

import org.nanes.slideset.dm.BooleanElement;
import org.nanes.slideset.ex.SlideSetException;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "Logical",
        elementType = BooleanElement.class,
        processedType = Boolean.class,
        hidden = false)
public class BooleanToBooleanReader implements
        ElementReader<BooleanElement, Boolean> {

    public Boolean read(BooleanElement elementToRead) throws SlideSetException {
        return elementToRead.getUnderlying();
    }
    
}
