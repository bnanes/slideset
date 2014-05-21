package edu.emory.cellbio.ijbat.pi;

import net.imagej.ImageJ;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.Parameter;
import org.scijava.ItemIO;
import net.imagej.Dataset;
import net.imagej.display.DefaultImageDisplay;
import net.imagej.display.OverlayService;
import net.imagej.overlay.AbstractOverlay;
import net.imagej.overlay.Overlay;
import org.scijava.ui.UserInterface;
import org.scijava.ui.swing.SwingUI;
import org.scijava.ui.viewer.DisplayWindow;
import org.scijava.ui.swing.viewer.SwingDisplayWindow;
import net.imagej.ui.swing.sdi.viewer.SwingSdiImageDisplayViewer;
import net.imagej.ui.swing.viewer.image.SwingImageDisplayViewer;
import java.util.Arrays;

/**
 * Test SlideSet compatible plugin
 * @author Ben
 */
//@Plugin(type=SlideSetPlugin.class, label="Hello World!")
public class Test extends SlideSetPlugin {
     
     @Parameter
     private ImageJ ij;
     
     @Parameter(label="An image", type=ItemIO.BOTH)
     private Dataset i;
     
     @Parameter(label="ROI", type=ItemIO.BOTH)
     private AbstractOverlay[] ao;
     
     public void run() {
          final DefaultImageDisplay imageDisplay = new DefaultImageDisplay();
          imageDisplay.setContext(ij.getContext());
          imageDisplay.display(i);
          try {
              ij.thread().invoke( new Thread() {  
                    @Override
                    public void run() {
                         UserInterface ui = ij.ui().getUI(SwingUI.NAME);
                         SwingImageDisplayViewer idv = new SwingSdiImageDisplayViewer();
                         idv.setContext(ij.getContext());
                         if(!idv.canView(imageDisplay) || !idv.isCompatible(ui))
                              throw new IllegalArgumentException("Viewer problem");
                         DisplayWindow imageWindow = ui.createDisplayWindow(imageDisplay);
                         idv.view(imageWindow, imageDisplay);
                         ij.ui().addDisplayViewer(idv);
                         if(imageWindow instanceof SwingDisplayWindow)
                             ((SwingDisplayWindow) imageWindow).setLocationRelativeTo(null);
                         imageWindow.showDisplay(true);
                    }
               });
          } catch(Exception e) {
              throw new IllegalArgumentException(e);
          }
          OverlayService os = ij.overlay();
          for(Overlay o : os.getOverlays(imageDisplay))
              os.removeOverlay(imageDisplay, o);
          if(ao != null && ao.length > 0) {
              Overlay[] po = new Overlay[ao.length];
              for(int i = 0; i < ao.length; i++)
                  po[i] = (Overlay) ao[i];
              os.addOverlays(imageDisplay, Arrays.asList(po));
          }
     }
     
}