package edu.emory.cellbio.ijbat.pi;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 *
 * @author Benjamin Nanes
 */
@HelpPath(path = "plugins/math.html")
@Plugin( type=SlideSetPlugin.class,
         name="Add",
         label="Add", visible = false,
         menuPath="Plugins > Slide Set > Commands > Math > Add")
public class MathAdd extends SlideSetPlugin {
    
    @Parameter(label = "A", type = ItemIO.INPUT)
    private double a;
    
    @Parameter(label = "B", type = ItemIO.INPUT)
    private double b;
    
    @Parameter(label = "Sum", type = ItemIO.OUTPUT)
    private double c;

    public void run() {
        c = a + b;
    }
    
}
