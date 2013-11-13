package edu.emory.cellbio.ijbat.ui;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.dm.DataTypeIDService;
import edu.emory.cellbio.ijbat.ex.DefaultPathNotSetException;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import imagej.ImageJ;
import imagej.data.Dataset;
import imagej.data.display.DefaultImageDisplay;
import imagej.data.display.DefaultOverlayService;
import imagej.data.display.ImageDisplay;
import imagej.data.overlay.AbstractOverlay;
import imagej.data.overlay.Overlay;
import imagej.display.Display;
import imagej.ui.swing.sdi.SwingUI;
import imagej.ui.swing.sdi.viewer.SwingDisplayWindow;
import imagej.ui.swing.sdi.viewer.SwingSdiImageDisplayViewer;
import imagej.ui.swing.viewer.image.SwingImageDisplayViewer;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 *
 * @author Benjamin Nanes
 */
public class RoiEditor extends JFrame 
     implements ActionListener, SlideSetWindow {
     
     // -- Fields --
     
     private SlideSet slideSet;
     private DataTypeIDService dtid;
     private ImageJ ij;
     private DefaultOverlayService dos;
     private SwingUI ui;
     /** DataSet column from which to draw the images */
     private int imageColumn = -1;
     /** DataSet columns corresponding to the ROI sets */
     private ArrayList<Integer> roiSetIndeces;
     /** Names of the ROI sets */
     private ArrayList<String> roiSetNames;
     /** ROI sets {@code AbstractOverlay[image#][Roi#]} */
     private ArrayList<AbstractOverlay[][]> roiSets;
     /** Current ROI set index */
     private int curRoiSet = -1;
     /** Current image index */
     private int curImage = 0;
     
     private JComboBox roiSetList;
     private JButton addRoiSet;
     private JButton deleteRoiSet;
     private JComboBox imageList;
     private JButton goImageNext;
     private JButton goImageBack;
     private JButton saveChanges;
     private JButton undoChanges;
     /** The image display */
     private Display imageDisplay;
     /** The image window */
     private SwingDisplayWindow imageWindow;
     
     /** Have changes been made? */
     private boolean changed = false;
     /** Active flag */
     private boolean active = false;
     
     // -- Constructor --
     
     public RoiEditor(SlideSet slideSet, DataTypeIDService dtid, ImageJ ij) {
          if(slideSet == null || dtid == null || ij == null)
               throw new IllegalArgumentException("Can't initiate with null elements");
          this.slideSet = slideSet;
          this.dtid = dtid;
          this.ij = ij;
          dos = ij.get(DefaultOverlayService.class);
          ui = (SwingUI) ij.ui().getUI(SwingUI.NAME);
          roiSetNames = new ArrayList<String>();
          roiSets = new ArrayList<AbstractOverlay[][]>();
          setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
          buildLayout();
          setActionListeners();
     }
     
     // -- Methods --
     
     /** Run the ROI editor. Returns when finished. Use separate thread. */
     public void showAndWait() {
          synchronized(this) {
               active = true;
               loadData();
               if(!active) return;
               updateControls();
               setVisible(true);
          }
          loadImage();
          Point p = imageWindow.getLocationOnScreen();
          setLocation(Math.max(p.x - getWidth(), 0), Math.max(p.y, 0));
          synchronized(this) {
               while(active) {
                    try{ wait(); }
                    catch(InterruptedException e){}
               }
               setVisible(false);
          }
     }

     /** Action handler */
     @Override
     public void actionPerformed(ActionEvent e) {
          handleActionEvent(e);
     }
     
     // -- Helper methods --
     
     /** Build the window */
     private void buildLayout() {
          setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
          add(Box.createVerticalStrut(5));
          
          roiSetList = new JComboBox();
          add(roiSetList);
          add(Box.createVerticalStrut(5));
          JPanel rsetButtons = new JPanel();
          rsetButtons.setLayout(new BoxLayout(rsetButtons, BoxLayout.X_AXIS));
          addRoiSet = new JButton("Add ROI Set");
          deleteRoiSet = new JButton("Delete");
          rsetButtons.add(addRoiSet);
          //rsetButtons.add(deleteRoiSet);
          add(rsetButtons);
          add(Box.createVerticalStrut(10));
          
          imageList = new JComboBox();
          add(imageList);
          add(Box.createVerticalStrut(5));
          goImageBack = new JButton("<<");
          goImageNext = new JButton(">>");
          JPanel imageButtons = new JPanel();
          imageButtons.setLayout(new BoxLayout(imageButtons, BoxLayout.X_AXIS));
          imageButtons.add(goImageBack);
          imageButtons.add(goImageNext);
          add(imageButtons);
          add(Box.createVerticalStrut(10));
          
          saveChanges = new JButton("Save");
          undoChanges = new JButton("Undo");
          JPanel roiButtons = new JPanel();
          roiButtons.setLayout(new BoxLayout(roiButtons, BoxLayout.X_AXIS));
          roiButtons.add(saveChanges);
          roiButtons.add(undoChanges);
          add(roiButtons);
          add(Box.createVerticalStrut(5));
          
          pack();
     }
     
     /** Set action listeners for the controls */
     private void setActionListeners() {
          addWindowListener(new WindowAdapter() {
               @Override
               public void windowClosing(WindowEvent e) { kill(); }
          });
          goImageBack.setActionCommand("imageBack");
          goImageBack.addActionListener(this);
          goImageNext.setActionCommand("imageNext");
          goImageNext.addActionListener(this);
          imageList.setActionCommand("imageListSelection");
          imageList.addActionListener(this);
          addRoiSet.setActionCommand("roiSetNew");
          addRoiSet.addActionListener(this);
          roiSetList.setActionCommand("roiSetListSelection");
          roiSetList.addActionListener(this);
          saveChanges.setActionCommand("writeRoiSets");
          saveChanges.addActionListener(this);
          undoChanges.setActionCommand("revertRoiSets");
          undoChanges.addActionListener(this);
     }
     
     /** Handle an {@code ActionEvent} */
     private void handleActionEvent(final ActionEvent e) {
          (new Thread() {
               @Override
               public void run() {
                    String ac = e.getActionCommand();
                    System.out.println("Action command: " + ac);
                    if(ac.equals("imageBack"))
                         setImage(curImage - 1);
                    else if(ac.equals("imageNext"))
                         setImage(curImage + 1);
                    else if(ac.equals("imageListSelection"))
                         setImage(imageList.getSelectedIndex());
                    else if(ac.equals("roiSetNew"))
                         createRoiSet();
                    else if(ac.equals("roiSetListSelection"))
                         setROISet(roiSetList.getSelectedIndex());
                    else if(ac.equals("writeRoiSets"))
                         writeOverlays();
                    else if(ac.equals("revertRoiSets"))
                         revertOverlays();
               }
          }).start();
     }
     
     /** Load the internal data */
     private void loadData() {
          List<Integer> imageColumns =
                  dtid.getMatchingColumns(Dataset.class, slideSet);
          if(imageColumns == null || imageColumns.isEmpty()) {
               JOptionPane.showMessageDialog(this,
                       "This table does not contain any images. "
                       + "Cannot create ROIs.",
                       "SlideSet - ROI Editor",
                       JOptionPane.ERROR_MESSAGE);
               active = false;
               kill();
               return;
          }
          if(imageColumns.size() > 1) {
               int choices = imageColumns.size();
               String[] names = new String[choices];
               for(int i=0; i<choices; i++)
                    names[i] = String.valueOf(i+1)
                            + ": " + slideSet.getColumnName(i);
               Object choice = JOptionPane.showInputDialog(this,
                       "Select images on which ROIs will be drawn:",
                       "SlideSet - ROI Editor",
                       JOptionPane.PLAIN_MESSAGE,
                       null, names, names[0]);
               if(choice == null)
               { kill(); return; }
               for(int i=0; i<choices; i++) {
                    imageColumn = imageColumns.get(i);
                    if(names[i].equals(choice))
                         break;
               }
          }
          else imageColumn = imageColumns.get(0);
          loadOverlays();
     }
     
     /** Load overlay data from disk */
     private void loadOverlays() {
          roiSetIndeces = dtid.getMatchingColumns(AbstractOverlay[].class, slideSet);
          for(int i : roiSetIndeces) {
               roiSetNames.add(slideSet.getColumnName(i));
               final String defp = slideSet.getColumnDefaultPath(i);
               if(defp == null || defp.isEmpty())
                    slideSet.setColumnDefaultPath(i, "roi");
               slideSet.setDefaultLinkPrefix(i, slideSet.getColumnName(i).replaceAll("\\W", "-"));
               slideSet.setDefaultLinkExtension(i, "roiset");
               AbstractOverlay[][] set = new AbstractOverlay[slideSet.getNumRows()][];
               for(int j=0; j<slideSet.getNumRows(); j++) {
                    try{
                         set[j] = (AbstractOverlay[])slideSet.getProcessedUnderlying(i, j);
                    } catch (SlideSetException ex) { set[j] = null; }
               }
               roiSets.add(set);
          }
     }
     
     /** Update the state of the controls */
     private void updateControls() {
          roiSetList.setModel(
                  new DefaultComboBoxModel(getRoiSetNames()));
          roiSetList.setSelectedIndex(curRoiSet);
          imageList.setModel(
                  new DefaultComboBoxModel(getImageNames()));
          imageList.setSelectedIndex(curImage);
     }
     
     /**
      * Get the names of available ROI sets to put in the list,
      * prefixed by the row number to avoid name duplications
      * which cause problems with {@code DefaultComboBoxModel}.
      */
     private String[] getRoiSetNames() {
          String[] names = new String[roiSetNames.size()];
          names = roiSetNames.toArray(names);
          for(int i=0; i<names.length; i++)
               names[i] = String.valueOf(i+1) + ": " + names[i];
          return names;
     }
     
     /** 
      * Get the short names of image files to put in the list,
      * prefixed by the row number to avoid name duplications
      * which cause problems with {@code DefaultComboBoxModel}.
      */
     private String[] getImageNames() {
          if(imageColumn < 0 || imageColumn >= slideSet.getNumCols()
                  || !slideSet.getColumnTypeCode(imageColumn).equals("Image2"))
               throw new IllegalArgumentException("Bad image column index");
          String[] names = new String[slideSet.getNumRows()];
          for(int i=0; i<slideSet.getNumRows(); i++)
               names[i] = String.valueOf(i+1) + ": " +
                       new File(slideSet.getUnderlying(imageColumn, i)
                       .toString()).getName();
          return names;
     }
     
     /** Load and display the selected image */
     private void loadImage() {
          if(curImage < 0 || curImage >= slideSet.getNumRows())
               return;
          Dataset ds;
          try{
               ds = (Dataset)slideSet
                       .getProcessedUnderlying(imageColumn, curImage);
          } catch(Throwable t) {
               throw new IllegalArgumentException("Problem loading image: ", t);
          }
          
          if(imageDisplay == null) {
               imageDisplay = new DefaultImageDisplay();
               imageDisplay.setContext(ij.getContext());
          }
          else {
               imageDisplay.clear();
          }
          imageDisplay.display(ds);
          
          if(imageWindow == null)
               createImageWindow();
          
          drawOverlays();
          imageWindow.setTitle("ROI Editor");
     }
     
     /** Create the image window */
     private void createImageWindow() {
          try {
               ij.thread().invoke( new Thread() {  
                    @Override
                    public void run() {
                         SwingImageDisplayViewer idv = new SwingSdiImageDisplayViewer();
                         idv.setContext(ij.getContext());
                         if(!idv.canView(imageDisplay) || !idv.isCompatible(ui))
                              throw new IllegalArgumentException("Viewer problem");
                         imageWindow = ui.createDisplayWindow(imageDisplay);
                         idv.view(imageWindow, imageDisplay);
                         ij.ui().addDisplayViewer(idv);
                         imageWindow.addWindowListener(
                                 new WindowAdapter() {
                                      @Override
                                      public void windowClosing(WindowEvent e) {
                                           if(active) kill();
                                 }});
                         imageWindow.showDisplay(true);
                    }
               });
          } catch (InterruptedException e) {
               throw new IllegalArgumentException(e);
          } catch (InvocationTargetException e) {
               throw new IllegalArgumentException(e);
          }
          imageWindow.setLocationRelativeTo(null);
     }
     
     /** Draw appropriate overlays on the image (and clears any already drawn overlays) */
     private void drawOverlays() {
          if(imageDisplay == null)
               return;
          if(curRoiSet < 0 || curRoiSet >= roiSets.size())
               return;
          if(!ImageDisplay.class.isInstance(imageDisplay))
               throw new IllegalArgumentException("Bad display type.");
          for(Overlay o : dos.getOverlays((ImageDisplay) imageDisplay))
               dos.removeOverlay((ImageDisplay) imageDisplay, o);
          Overlay[] overlays = roiSets.get(curRoiSet)[curImage];
          if(overlays != null)
               dos.addOverlays((ImageDisplay) imageDisplay, Arrays.asList(overlays));
          imageDisplay.update();
     }
     
     /** Save overlays drawn on the current image to memory, not to disk. */
     private void saveOverlays() {
          if(curRoiSet < 0 || curRoiSet >= roiSets.size())
               return;
          if(!ImageDisplay.class.isInstance(imageDisplay))
               throw new IllegalArgumentException("Bad display type.");
          List<Overlay> overlays = dos.getOverlays((ImageDisplay) imageDisplay);
          if(overlays.isEmpty()) {
               roiSets.get(curRoiSet)[curImage] = null;
               return;
          }
          ArrayList<AbstractOverlay> overCast =
                  new ArrayList<AbstractOverlay>(overlays.size()+2);
          for(Overlay o : overlays)
               if(AbstractOverlay.class.isInstance(o) &&
                       !overCast.contains((AbstractOverlay) o)) //<<< A bit hacky...
                    overCast.add((AbstractOverlay) o);
          roiSets.get(curRoiSet)[curImage] =
                  overCast.toArray(new AbstractOverlay[overCast.size()]);
     }
     
     /** Save all overlays to disk */
     private void writeOverlays() {
          saveOverlays();
          if(roiSets.isEmpty())
               return;
          for(int i=0; i<roiSets.size(); i++) {
               for(int row=0; row < slideSet.getNumRows(); row++) {
                    try {
                         final String dest = 
                              (String) slideSet.getUnderlying(roiSetIndeces.get(i), row);
                         if(dest == null || dest.isEmpty())
                              slideSet.makeDefaultLink(roiSetIndeces.get(i), row);
                         slideSet.setProcessedUnderlying(roiSetIndeces.get(i), row, roiSets.get(i)[row]);
                    } catch(SlideSetException e) {
                         throw new IllegalArgumentException(e);
                    }
               }
          }
     }
     
     /** With user confirmation, revert overlays to last saved version. */
     private void revertOverlays() {
          if( JOptionPane.showConfirmDialog(this,
                  "Revert all regions of interest to last saved version?",
                  "ROI Editor", JOptionPane.YES_NO_OPTION)
                  != JOptionPane.YES_OPTION )
               return;
          loadOverlays();
          drawOverlays();
     }
     
     /** Create a new set of overlays (ROIs) */
     private void createRoiSet() {
          String name = JOptionPane.showInputDialog(this, "New ROI set name:");
          if(name == null)
               return;
          name = name.trim();
          name = name.equals("") ? "ROI" : name;
          int index = slideSet.addColumn(name, "ROISet2");
          roiSetIndeces.add(index);
          roiSetNames.add(name);
          roiSets.add(new AbstractOverlay[slideSet.getNumRows()][]);
          slideSet.setColumnDefaultPath(index, "roi");
          slideSet.setDefaultLinkPrefix(index, name.replaceAll("\\W", "-"));
          slideSet.setDefaultLinkExtension(index, "roiset");
          try {
               for(int i=0; i<slideSet.getNumRows(); i++)
                    slideSet.makeDefaultLink(index, i);
          } catch(DefaultPathNotSetException e) {
               throw new IllegalStateException("Can't generate new links: " + e);
          }
          curRoiSet = roiSets.size() - 1;
          updateControls();
          drawOverlays();
     }
     
     /** Clean up and close the editor */
     @Override
     public void kill() {
          System.out.println("Closing ROI editor");
          if(active && 
                  JOptionPane.showConfirmDialog(this, "Save changes?", 
                  "ROI Editor", JOptionPane.YES_NO_OPTION) 
                  == JOptionPane.YES_OPTION) {
               saveOverlays();
               writeOverlays();
          }
          synchronized(this) {
               active = false;
               setVisible(false);
               if(imageWindow != null && imageWindow.isVisible())
                    imageWindow.dispose();
               notifyAll();
          }
     }
     
     /**
      * Change display to the image with the given {@code index} in the list.
      */
     private void setImage(int index) {
          if(index >= imageList.getItemCount())
               index = 0;
          if(index < 0)
               index = imageList.getItemCount() - 1;
          if(index == curImage)
               return;
          saveOverlays();
          curImage = index;
          updateControls();
          loadImage();
     }
     
     /**
      * Load the selected ROI set into the display.
      */
     private void setROISet(int index) {
          if(index >= roiSetList.getItemCount())
               index = 0;
          else if(index < 0)
               index = roiSetList.getItemCount() - 1;
          if(index == curRoiSet)
               return;
          saveOverlays();
          curRoiSet = index;
          updateControls();
          drawOverlays();
     }
     
}
