package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.DataElement;
import edu.emory.cellbio.ijbat.ex.SlideSetException;

/**
 * Converts {@link DataElement} "underlying" data
 * to the "processed" data required for command inputs.
 * Must be annotated with
 * {@code @}{@link edu.emory.cellbio.ijbat.dm.read.ElementReaderMetadata}.
 * 
 * @param <E> The {@code DataElement} type that this reader can read.
 * @param <P> The "processed" data type that this reader produces.
 * 
 * @author Benjamin Nanes
 */
public interface ElementReader<E extends DataElement, P> {
    
    /**
     * Convert "underlying" data to "processed" data.
     * 
     * @param elementToRead The {@code DataElement} containing
     * the "underlying" data which should be translated.
     * 
     * @return The "processed" data.
     * 
     * @throws SlideSetException 
     */
    public P read(E elementToRead) throws SlideSetException;
    
}
