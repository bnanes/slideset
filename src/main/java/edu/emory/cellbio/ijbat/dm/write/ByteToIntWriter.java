package edu.emory.cellbio.ijbat.dm.write;

import edu.emory.cellbio.ijbat.dm.IntegerElement;

/**
 *
 * @author Benjamin Nanes
 */
@ElementWriterMetadata(
        name = "Integer value",
        elementType = IntegerElement.class,
        processedType = Byte.class)
public class ByteToIntWriter implements
        ElementWriter<IntegerElement, Byte> {

    public void write(Byte data, IntegerElement elementToWrite) {
        elementToWrite.setUnderlying(data.intValue());
    }
    
}
