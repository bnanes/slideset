package edu.emory.cellbio.ijbat.pi;

import org.scijava.command.Command;

/**
 * <h2> Slide Set commands </h2>
 * 
 * In principle, any plugin implementing the ImageJ 2 
 * {@code Command} API (see: {@code imagej.command.Command}
 * and {@code org.scijava.plugin.Plugin}) can be executed 
 * through Slide Set, provided that there are appropriate 
 * {@code ElementReader}s and {@code ElementWriter}s for 
 * the command input and output parameters. However, plugins 
 * developed specifically for Slide Set can benefit in two 
 * ways from extending {@code SlideSetPlugin} and using the 
 * annotation, {@code @Plugin(type=SlideSetPlugin.class)}.
 * 
 * <p> First, unlike general ImageJ plugins, {@code SlideSetPlugin}s
 * are listed in the Slide Set core user interface menus. Second, 
 * {@code SlideSetPlugin}s may return multiple sets of results per 
 * set of inputs (for example, a different result for each region 
 * of interest). In order to do so, a {@code SlideSetPlugin} must 
 * implement the {@link MultipleResults} interface and use arrays 
 * or {@code List}s for the results. All the arrays and {@code List}s
 * must have the same number of elements and are assumed to be in the 
 * same order. Any results which are not arrays or Lists will be 
 * repeated to match the array or List length. To return no results,
 * the output parameters should be set to 0-length arrays or
 * {@code List}s, not {@code null}.
 * 
 * @author Benjamin Nanes
 */
public abstract class SlideSetPlugin implements Command {

}
