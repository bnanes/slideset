package edu.emory.cellbio.ijbat.dm.write;

import edu.emory.cellbio.ijbat.dm.StringElement;

/**
 *
 * @author Benjamin Nanes
 */
@ElementWriterMetadata(
        name = "Text",
        elementType = StringElement.class,
        processedType = String.class)
public class StringToStringWriter implements ElementWriter<StringElement, String> {

    public void write(String data, StringElement elementToWrite) {
        elementToWrite.setUnderlying(data);
    }
    
}
