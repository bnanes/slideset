package edu.emory.cellbio.ijbat.dm.write;

import edu.emory.cellbio.ijbat.dm.IntegerElement;

/**
 *
 * @author Benjamin Nanes
 */
@ElementWriterMetadata(
        name = "Integer value",
        elementType = IntegerElement.class,
        processedType = Long.class)
public class LongToIntWriter implements
        ElementWriter<IntegerElement, Long> {

    public void write(Long data, IntegerElement elementToWrite) {
        elementToWrite.setUnderlying(data.intValue());
    }
    
}
