package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.IntegerElement;
import edu.emory.cellbio.ijbat.ex.SlideSetException;

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
