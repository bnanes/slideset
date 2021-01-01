package org.nanes.slideset.dm.write;

import org.nanes.slideset.dm.DoubleElement;

/**
 *
 * @author Benjamin Nanes
 */
@ElementWriterMetadata(
        name = "Numeric value",
        elementType = DoubleElement.class,
        processedType = Double.class)
public class DoubleToDoubleWriter implements
        ElementWriter<DoubleElement, Double> {

    public void write(Double data, DoubleElement elementToWrite) {
        elementToWrite.setUnderlying(data);
    }
    
}
