package org.nanes.slideset.dm.write;

import org.nanes.slideset.dm.IntegerElement;

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
