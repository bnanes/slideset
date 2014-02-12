package edu.emory.cellbio.ijbat.pi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a {@link SlideSetPlugin} with this
 * annotation type to set an associated
 * documentation page.
 * 
 * @author Benjamin Nanes
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface HelpPath {
    /** Documentation path */
    String path() default "plugins/";
}
