package edu.emory.cellbio.ijbat.pi;

import edu.emory.cellbio.ijbat.ui.SlideSetLog;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 *
 * @author Benjamin Nanes
 */
@Plugin( type=SlideSetPlugin.class,
         name="Divide",
         label="Divide", visible = false,
         menuPath="Plugins > Slide Set > Commands > Math > Divide")
public class MathDiv extends SlideSetPlugin implements MultipleResults {
    
    @Parameter(label = "Log", type = ItemIO.INPUT)
    SlideSetLog log;
    
    @Parameter(label = "Numerator", type = ItemIO.INPUT)
    private double a;
    
    @Parameter(label = "Denominator", type = ItemIO.INPUT)
    private double b;
    
    @Parameter(label = "Quotient", type = ItemIO.OUTPUT)
    private double[] c;
    
    private int i = 0;

    public void run() {
        i++;
        if(b == 0) {
            c = new double[0];
            if(log != null) {
                log.println("# - - - - - - - - - - - - - - -");
                log.println("# Warning: Division by 0");
                log.println("# No result returned for row "
                        + String.valueOf(i));
            }
        }
        else
            c = new double[] {a / b};
    }
    
}
