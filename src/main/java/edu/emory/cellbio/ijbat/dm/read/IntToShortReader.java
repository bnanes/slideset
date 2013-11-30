package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.IntegerElement;
import edu.emory.cellbio.ijbat.ex.SlideSetException;

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
