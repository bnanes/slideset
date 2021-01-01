package org.nanes.slideset.dm.write;

import org.nanes.slideset.dm.BooleanElement;

/**
 *
 * @author Benjamin Nanes
 */
@ElementWriterMetadata(
        name = "Logical value",
        elementType = BooleanElement.class,
        processedType = Boolean.class)
public class BooleanToBooleanWriter implements
        ElementWriter<BooleanElement, Boolean> {

    public void write(Boolean data, BooleanElement elementToWrite) {
        elementToWrite.setUnderlying(data);
    }
    
}
