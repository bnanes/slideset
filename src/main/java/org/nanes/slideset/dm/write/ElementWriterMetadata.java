package org.nanes.slideset.dm.write;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.java.sezpoz.Indexable;

/**
 * Metadata for indexing {@link ElementWriter}s.
 * An {@code ElementWriter} must be marked with
 * this annotation type in order to be discovered
 * at runtime. The values in this annotation type
 * are used to identify {@code SlideSet} columns
 * and command output parameters compatible with
 * the writer.
 * 
 * @author Benjamin Nanes
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Indexable(type = ElementWriter.class)
public @interface ElementWriterMetadata {
    
    /** Human-readable name of the writer */
    String name();
    
    /**
     * Type of "processed" data compatible with the writer.
     * Note that array types are not compatible with the
     * library used for run-time discovery of
     * {@code ElementWriter}s. To work-around this issue,
     * create a {@link edu.emory.cellbio.ijbat.dm.TypeAlias}.
     */
    Class processedType();
    
    /** {@code DataElement} class compatible with the writer */
    Class elementType();
    
    /** MIME type produced by the writer */
    String mimeType() default "null";
    
    /** For file links, the default file extension to use with this writer */
    String linkExt() default "null";
    
}
