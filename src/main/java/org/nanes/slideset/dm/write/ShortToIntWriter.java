package org.nanes.slideset.dm.write;

import org.nanes.slideset.dm.IntegerElement;

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
