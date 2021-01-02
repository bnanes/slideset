package org.nanes.slideset.dm.read;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.java.sezpoz.Indexable;

/**
 * Metadata for indexing {@link ElementReader}s.
 * An {@code ElementReader} must be marked with
 * this annotation type in order to be discovered
 * at runtime. The values in this annotation
 * type are used to identify {@code SlideSet} columns and
 * command input parameters compatible with the reader.
 * 
 * @author Benjamin Nanes
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Indexable(type = ElementReader.class)
public @interface ElementReaderMetadata {
    
    /** Human-readable name of the reader */
    String name();
    
    /** {@code DataElement} class that the reader can read */
    Class elementType();
    
    /** List of MIME types that are compatible with the reader */
    String[] mimeTypes() default {};
    
    /**
     * Type of "processed" data produced by the reader.
     * Note that array types are not compatible with the
     * library used for run-time discovery of
     * {@code ElementReader}s. To work-around this issue,
     * create a {@link org.nanes.slideset.dm.TypeAlias}.
     */
    Class processedType();
    
    /** Should the reader be hidden from constant value lists? */
    boolean hidden();
    
}
