package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.IntegerElement;
import edu.emory.cellbio.ijbat.ex.SlideSetException;

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
