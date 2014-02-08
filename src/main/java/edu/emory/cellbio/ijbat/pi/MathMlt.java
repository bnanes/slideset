package edu.emory.cellbio.ijbat.pi;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 *
 * @author Benjamin Nanes
 */
@Plugin( type=SlideSetPlugin.class,
         name="Multiply",
         label="Multiply", visible = false,
         menuPath="Plugins > Slide Set > Commands > Math > Multiply")
public class MathMlt extends SlideSetPlugin {
    
    @Parameter(label = "A", type = ItemIO.INPUT)
    private double a;
    
    @Parameter(label = "B", type = ItemIO.INPUT)
    private double b;
    
    @Parameter(label = "Product", type = ItemIO.OUTPUT)
    private double c;

    public void run() {
        c = a * b;
    }
    
}
