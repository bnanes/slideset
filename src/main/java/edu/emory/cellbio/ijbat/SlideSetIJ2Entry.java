package edu.emory.cellbio.ijbat;

import edu.emory.cellbio.ijbat.dm.DataTypeIDService;
import edu.emory.cellbio.ijbat.io.CSVService;
import edu.emory.cellbio.ijbat.io.XMLService;
import edu.emory.cellbio.ijbat.pi.SlideSetPluginLoader;
import edu.emory.cellbio.ijbat.ui.SlideSetLauncher;
import edu.emory.cellbio.ijbat.ui.SlideSetLog;
import imagej.ImageJ;
import imagej.command.Command;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Entry point for ImageJ2
 * @author Benjamin Nanes
 */
@Plugin(type=Command.class,
     menuPath="Plugins>Slide Set",
     description="Batch processing for ImageJ")
public class SlideSetIJ2Entry implements Command {
     
     @Parameter(label="ImageJ", type=ItemIO.INPUT)
     private Context c;
     
     @Override
     public void run() {
          final ImageJ ij = new ImageJ(c);
          final DataTypeIDService dtid = new DataTypeIDService(ij);
          final SlideSetLog log = new SlideSetLog();
          final XMLService xs = new XMLService(ij, dtid);
          final CSVService cs = new CSVService();
          final SlideSetLauncher win = new SlideSetLauncher(ij, dtid, xs, cs, log);
          win.setVisible(true);
          
     }
     
}
