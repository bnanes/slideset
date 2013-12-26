package edu.emory.cellbio.ijbat.pi;

import imagej.ImageJ;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.Parameter;
import org.scijava.ItemIO;
import imagej.data.Dataset;
import imagej.data.display.DefaultImageDisplay;
import imagej.data.display.OverlayService;
import imagej.data.overlay.AbstractOverlay;
import imagej.data.overlay.Overlay;
import imagej.ui.swing.sdi.SwingUI;
import imagej.ui.swing.sdi.viewer.SwingDisplayWindow;
import imagej.ui.swing.sdi.viewer.SwingSdiImageDisplayViewer;
import imagej.ui.swing.viewer.image.SwingImageDisplayViewer;
import java.util.Arrays;

/**
 * Test SlideSet compatible plugin
 * @author Ben
 */
@Plugin(type=SlideSetPlugin.class, label="Hello World!")
public class Test extends SlideSetPlugin {
     
     @Parameter
     private ImageJ ij;
     
     @Parameter(label="An image", type=ItemIO.INPUT)
     private Dataset i;
     
     @Parameter(label="ROI", type=ItemIO.INPUT)
     private AbstractOverlay[] ao;
     
     public void run() {
          final DefaultImageDisplay imageDisplay = new DefaultImageDisplay();
          imageDisplay.setContext(ij.getContext());
          imageDisplay.display(i);
          try {
              ij.thread().invoke( new Thread() {  
                    @Override
                    public void run() {
                         SwingUI ui = (SwingUI) ij.ui().getUI(SwingUI.NAME);
                         SwingImageDisplayViewer idv = new SwingSdiImageDisplayViewer();
                         idv.setContext(ij.getContext());
                         if(!idv.canView(imageDisplay) || !idv.isCompatible(ui))
                              throw new IllegalArgumentException("Viewer problem");
                         SwingDisplayWindow imageWindow = ui.createDisplayWindow(imageDisplay);
                         idv.view(imageWindow, imageDisplay);
                         ij.ui().addDisplayViewer(idv);
                         imageWindow.setLocationRelativeTo(null);
                         imageWindow.showDisplay(true);
                    }
               });
          } catch(Exception e) {
              throw new IllegalArgumentException(e);
          }
          OverlayService os = ij.overlay();
          for(Overlay o : os.getOverlays(imageDisplay))
              os.removeOverlay(imageDisplay, o);
          Overlay[] po = ao;
          if(ao != null && ao.length > 0)
              os.addOverlays(imageDisplay, Arrays.asList(po));
     }
     
}