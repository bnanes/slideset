package edu.emory.cellbio.ijbat.dm.write;

import edu.emory.cellbio.ijbat.dm.IntegerElement;

/**
 *
 * @author Benjamin Nanes
 */
@ElementWriterMetadata(
        name = "Integer value",
        elementType = IntegerElement.class,
        processedType = Short.class)
public class ShortToIntWriter implements
        ElementWriter<IntegerElement, Short> {

    public void write(Short data, IntegerElement elementToWrite) {
        elementToWrite.setUnderlying(data.intValue());
    }
    
}
