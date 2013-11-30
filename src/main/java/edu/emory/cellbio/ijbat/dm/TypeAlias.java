package edu.emory.cellbio.ijbat.dm;

/**
 * Work-around for inability to create annotation
 * fields with array types. This affects {@code ElementReader}s
 * and {@code ElementWriter}s that use array types
 * for their "processed" data (ex., {@code AbstractOverlay[]}
 * to represent ROI sets).
 * <p>
 * To avoid the issue, implement
 * this interface for the needed array type, then use
 * the implementation class for the relevant
 * {@code ElementReaderMetadata} and {@code ElementWriterMetadata}.
 * When {@code DataTypeIDServive} encounters a {@code TypeAlias}
 * in the {@code ElementReader} or {@code ElementWriter}
 * indeces, the alias will be replaced with the real type.
 * <p>
 * {@code TypeAlias}es must be marked with
 * {@code @}{@link TypeAliasMetadata} in order to be
 * discoverable at run-time.
 * 
 * @param <T> The real type referenced by this alias type
 * 
 * @author Benjamin Nanes
 */
public interface TypeAlias<T> {
    
    /** The real type referenced by this alias type */
    public Class<T> getRealType();

    
}
