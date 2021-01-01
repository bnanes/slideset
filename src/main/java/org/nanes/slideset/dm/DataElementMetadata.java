package org.nanes.slideset.dm;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation type containing metadata for
 * {@link DataElement} classes.
 * 
 * @author Benjamin Nanes
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataElementMetadata {
    
    /** Human-readable name of the element type */
    String name();
    
}
