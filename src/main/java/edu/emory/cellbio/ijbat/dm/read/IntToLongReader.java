package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.IntegerElement;
import edu.emory.cellbio.ijbat.ex.SlideSetException;

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
