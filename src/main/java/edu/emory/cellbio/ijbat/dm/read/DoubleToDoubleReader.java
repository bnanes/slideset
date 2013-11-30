package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.DoubleElement;
import edu.emory.cellbio.ijbat.ex.SlideSetException;

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
