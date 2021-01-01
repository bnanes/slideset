package org.nanes.slideset.dm.write;

import org.nanes.slideset.dm.DataElement;
import org.nanes.slideset.ex.SlideSetException;

/**
 * Converts "processed" data from command output
 * parameters to "underlying" data that can be stored 
 * in a {@link DataElement}. Must be annotated with
 * {@code @}{@link org.nanes.slideset.dm.write.ElementWriterMetadata}.
 * 
 * @param <E> The {@code DataElement} type to which this writer
 *    can write.
 * @param <P> The "processed" data type that this writer can
 *    write to a {@code DataElement}.
 * 
 * @author Benjamin Nanes
 */
public interface ElementWriter<E extends DataElement, P> {
    
    /**
     * Convert "processed" data to "underlying" data.
     * 
     * @param data The "processed" data.
     * @param elementToWrite The {@code DataElement} to
     *    to which the data will be written.
     * 
     * @throws SlideSetException 
     */
    public void write(P data, E elementToWrite) throws SlideSetException;
    
}
