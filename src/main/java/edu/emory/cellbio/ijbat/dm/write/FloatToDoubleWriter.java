package edu.emory.cellbio.ijbat.dm.write;

import edu.emory.cellbio.ijbat.dm.DoubleElement;

/**
 *
 * @author Benjamin Nanes
 */
@ElementWriterMetadata(
        name = "Numeric value",
        elementType = DoubleElement.class,
        processedType = Float.class)
public class FloatToDoubleWriter implements
        ElementWriter<DoubleElement, Float> {

    public void write(Float data, DoubleElement elementToWrite) {
        elementToWrite.setUnderlying(data.doubleValue());
    }
    
}
