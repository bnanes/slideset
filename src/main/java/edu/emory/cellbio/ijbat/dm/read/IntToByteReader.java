package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.IntegerElement;
import edu.emory.cellbio.ijbat.ex.SlideSetException;

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
