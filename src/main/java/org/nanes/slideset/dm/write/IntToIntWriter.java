package org.nanes.slideset.dm.write;

import org.nanes.slideset.dm.IntegerElement;

/**
 *
 * @author Benjamin Nanes
 */
@ElementWriterMetadata(
        name = "Integer value",
        elementType = IntegerElement.class,
        processedType = Integer.class)
public class IntToIntWriter implements
        ElementWriter<IntegerElement, Integer> {

    public void write(Integer data, IntegerElement elementToWrite) {
        elementToWrite.setUnderlying(data);
    }
    
}
