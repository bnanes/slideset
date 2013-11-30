package edu.emory.cellbio.ijbat.dm;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.java.sezpoz.Indexable;

/**
 * In order to be discoverable at run-time,
 * {@link TypeAlias}es should be marked with
 * this annotation type.
 * 
 * @author Benjamin Nanes
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Indexable(type = TypeAlias.class)
public @interface TypeAliasMetadata {
    
}
