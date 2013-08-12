package edu.emory.cellbio.ijbat.dm;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.java.sezpoz.Indexable;

/**
 * Annotation to allow dynamic loading of new
 * {@link Linker} classes.
 * 
 * @author Benjamin Nanes
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Indexable(type=Linker.class)
public @interface LinkerInfo {
     
     /**
      * Class name of the underlying (table-stored) data.
      * <P> See {@link DataElement#underlying}.
      * <P> See {@link Class#getName()} for String format info.
      */
     String underlying() default "java.lang.String";
     
     /**
      * Class name of the underlying (table-stored) data.
      * <P> Should match return value of 
      * {@link Linker#getProcessedClass(java.lang.Object) Linker.getProcessedClass()}
      * for the annotated class.
      * <P> See {@link Class#getName()} for String format info.
      */
     String processed() default "java.lang.String";
     
     /**
      * {@code TypeCode} to associated with the annotated {@code Linker}
      */
     String typeCode() default "x";
     
     /**
      * Human-readable name for this data type.
      * This property is used to generate menu entries;
      * sub-menus can be specified with "/".
      */
     String name() default "x";
     
}
