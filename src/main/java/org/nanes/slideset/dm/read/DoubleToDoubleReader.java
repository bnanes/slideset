package org.nanes.slideset.dm.read;

import org.nanes.slideset.dm.DoubleElement;
import org.nanes.slideset.ex.SlideSetException;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "Number (double)",
        elementType = DoubleElement.class,
        processedType = Double.class,
        hidden = false)
public class DoubleToDoubleReader implements
        ElementReader<DoubleElement, Double> {

    public Double read(DoubleElement elementToRead) throws SlideSetException {
        return elementToRead.getUnderlying();
    }
    
}
