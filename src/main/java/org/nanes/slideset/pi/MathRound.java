package org.nanes.slideset.pi;

import org.nanes.slideset.ui.SlideSetLog;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Round a {@code double} to an {@code int}
 * 
 * @author Benjamin Nanes
 */
@HelpPath(path = "plugins/round.html")
@Plugin( type=SlideSetPlugin.class,
         name="Round",
         label="Round", visible = false,
         menuPath="Plugins > Slide Set > Commands > Math > Round")
public class MathRound extends SlideSetPlugin {

    @Parameter(label = "Number", type = ItemIO.INPUT)
    private double a;
    
    @Parameter(label = "Integer", type = ItemIO.OUTPUT)
    private int b;
    
    @Parameter(label = "Log", type = ItemIO.INPUT)
    private SlideSetLog log;
    
    public void run() {
        
        if(a > Integer.MAX_VALUE || a < Integer.MIN_VALUE) {
            log.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            log.println("Warning: " + String.valueOf(a));
            log.println("is too large or too small to store");
            log.println("as an integer. The result may not");
            log.println("make sense.");
            log.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        }
        b = new Long(Math.round(a)).intValue();
        
    }
    
}
