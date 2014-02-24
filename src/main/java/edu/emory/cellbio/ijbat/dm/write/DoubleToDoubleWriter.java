package edu.emory.cellbio.ijbat.dm.write;

import edu.emory.cellbio.ijbat.dm.DoubleElement;

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
