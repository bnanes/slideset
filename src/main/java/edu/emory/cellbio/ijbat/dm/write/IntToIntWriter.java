package edu.emory.cellbio.ijbat.dm.write;

import edu.emory.cellbio.ijbat.dm.IntegerElement;

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
