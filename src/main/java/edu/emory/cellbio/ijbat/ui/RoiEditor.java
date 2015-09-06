package edu.emory.cellbio.ijbat.ui;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.dm.ColumnBoundReader;
import edu.emory.cellbio.ijbat.dm.ColumnBoundWriter;
import edu.emory.cellbio.ijbat.dm.DataElement;
import edu.emory.cellbio.ijbat.dm.DataTypeIDService;
import edu.emory.cellbio.ijbat.dm.FileLinkElement;
import edu.emory.cellbio.ijbat.dm.MIME;
import edu.emory.cellbio.ijbat.dm.read.SVGFileToAbstractOverlayReader;
import edu.emory.cellbio.ijbat.dm.write.AbstractOverlaysToSVGFileWriter;
import edu.emory.cellbio.ijbat.ex.ImgLinkException;
import edu.emory.cellbio.ijbat.ex.LinkNotFoundException;
import edu.emory.cellbio.ijbat.ex.OperationCanceledException;
import edu.emory.cellbio.ijbat.ex.RoiLinkException;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.imagej.Data;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.display.ColorMode;
import net.imagej.display.DataView;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.display.OverlayService;
import net.imagej.event.OverlayCreatedEvent;
import net.imagej.event.OverlayDeletedEvent;
import net.imagej.event.OverlayRestructuredEvent;
import net.imagej.event.OverlayUpdatedEvent;
import net.imagej.overlay.AbstractOverlay;
import net.imagej.overlay.Overlay;
import net.imagej.ui.swing.commands.OverlayManager;
import net.imagej.ui.swing.sdi.viewer.SwingSdiImageDisplayViewer;
import net.imagej.ui.swing.viewer.image.SwingImageDisplayViewer;
import org.scijava.Context;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.event.EventHandler;
import org.scijava.event.EventSubscriber;
import org.scijava.object.event.ObjectEvent;
import org.scijava.ui.UserInterface;
import org.scijava.ui.swing.SwingUI;
import org.scijava.ui.swing.viewer.SwingDisplayWindow;
import org.scijava.ui.viewer.DisplayWindow;

/**
 * Editor for ROI set files.
 * 
 * @author Benjamin Nanes
 */
public class RoiEditor extends JFrame 
     implements ActionListener, SlideSetWindow {
     
     // -- Fields --
     
     private SlideSet slideSet;
     private DataTypeIDService dtid;
     private ImageJ ij;
     private OverlayService os;
     private UserInterface ui;
     
     private ColumnBoundReader<? extends DataElement, Dataset> images = null;
     
     private ArrayList<ColumnBoundReader> roiReaders;
     private ArrayList<ColumnBoundWriter> roiWriters;
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
     // private JButton deleteRoiSet;
     private JButton openROIManager;
     private JComboBox displayMode;
     private JButton changeLevels;
     private JButton exportSVG;
     private JComboBox imageList;
     private JButton goImageNext;
     private JButton goImageBack;
     private JButton saveChanges;
     private JButton undoChanges;
     /** The image display */
     private FastUpdateImageDisplay imageDisplay;
     /** The image window */
     private SwingDisplayWindow imageWindow;
     /** The brightness/contrast dialog */
     private BrightnessContrastRoi bcDialog;
     
     /** Active flag */
     private boolean active = false;
     /** Busy loading an image flag */
     private boolean loadingImage = false;
     /** The log */
     private SlideSetLog log;
     /** Read-only mode */
     private boolean locked = false;
     /** Changed flag */
     private boolean changed = false;
     
     // -- Constructor --
     
     public RoiEditor(SlideSet slideSet, DataTypeIDService dtid,
             ImageJ ij, SlideSetLog log) {
          if(slideSet == null || dtid == null || ij == null || log == null)
               throw new IllegalArgumentException("Can't initiate with null elements");
          this.slideSet = slideSet;
          this.dtid = dtid;
          this.ij = ij;
          this.log = log;
          os = ij.get(OverlayService.class);
          ui = ij.ui().getUI(SwingUI.NAME);
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
               try {
                   loadData();
               } catch(SlideSetException ex) {
                   handleError(ex);
                   active = false;
                   return;
               }
               if(!active) return;
               updateControls();
               setVisible(true);
          }
          List<EventSubscriber<?>> subscribers = ij.event().subscribe(this);
          loadImage(curImage);
          if(imageWindow != null && imageWindow.isVisible()) {
              Point p = imageWindow.getLocationOnScreen();
              setLocation(Math.max(p.x - getWidth(), 0), Math.max(p.y, 0));
          }
          synchronized(this) {
               while(active) {
                    try{ wait(); }
                    catch(InterruptedException e){}
               }
               setVisible(false);
          }
          ij.event().unsubscribe(subscribers);
     }

     /** Action handler */
     @Override
     public void actionPerformed(ActionEvent e) {
          handleActionEvent(e);
     }
     
     /** Register a Brightness/Contrast dialog */
     public void registerBrightnessContrast(BrightnessContrastRoi bc) {
         bcDialog = bc;
         if(imageDisplay == null || bcDialog == null) return;
         for(DataView v : imageDisplay) {
             if(!DatasetView.class.isInstance(v)) continue;
             bcDialog.setView((DatasetView)v);
             return;
         }
     }
     
     /** Activate read-only mode and prevent changes to ROIs */
     public void lock() {
         locked = true;
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
          rsetButtons.setLayout(new BoxLayout(rsetButtons, BoxLayout.Y_AXIS));
          addRoiSet = new JButton("Add ROI Set");
          // deleteRoiSet = new JButton("Delete");
          openROIManager = new JButton("ROI Manager");
          exportSVG = new JButton("Export SVG");
          Box addRoiSetBox = Box.createHorizontalBox();
          addRoiSetBox.add(Box.createHorizontalGlue());
          addRoiSetBox.add(addRoiSet);
          addRoiSetBox.add(Box.createHorizontalGlue());
          rsetButtons.add(addRoiSetBox);
          rsetButtons.add(Box.createVerticalStrut(5));
          Box openROIManagerBox = Box.createHorizontalBox();
          openROIManagerBox.add(Box.createHorizontalGlue());
          openROIManagerBox.add(openROIManager);
          openROIManagerBox.add(Box.createHorizontalGlue());
          rsetButtons.add(openROIManagerBox);
          rsetButtons.add(Box.createVerticalStrut(5));
          Box exportSVGBox = Box.createHorizontalBox();
          exportSVGBox.add(Box.createHorizontalGlue());
          exportSVGBox.add(exportSVG);
          exportSVGBox.add(Box.createHorizontalGlue());
          rsetButtons.add(exportSVGBox);
          // rsetButtons.add(deleteRoiSet);
          add(rsetButtons);
          add(Box.createVerticalStrut(10));
          
          JPanel dispButtons = new JPanel();
          dispButtons.setLayout(new BoxLayout(dispButtons, BoxLayout.Y_AXIS));
          displayMode = new JComboBox();
          displayMode.addItem("Composite");
          displayMode.addItem("Grayscale");
          displayMode.addItem("Color");
          Box modeBox = Box.createHorizontalBox();
          modeBox.add(Box.createHorizontalGlue());
          modeBox.add(displayMode);
          modeBox.add(Box.createHorizontalGlue());
          dispButtons.add(modeBox);
          changeLevels = new JButton("Levels");
          Box levBox = Box.createHorizontalBox();
          levBox.add(Box.createHorizontalGlue());
          levBox.add(changeLevels);
          levBox.add(Box.createHorizontalGlue());
          dispButtons.add(Box.createVerticalStrut(5));
          dispButtons.add(levBox);
          add(dispButtons);
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
          openROIManager.setActionCommand("openROIManager");
          openROIManager.addActionListener(this);
          exportSVG.setActionCommand("exportSVG");
          exportSVG.addActionListener(this);
          roiSetList.setActionCommand("roiSetListSelection");
          roiSetList.addActionListener(this);
          saveChanges.setActionCommand("writeRoiSets");
          saveChanges.addActionListener(this);
          undoChanges.setActionCommand("revertRoiSets");
          undoChanges.addActionListener(this);
          displayMode.setActionCommand("changeColorMode");
          displayMode.addActionListener(this);
          changeLevels.setActionCommand("changeLevels");
          changeLevels.addActionListener(this);
     }
     
     /** Handle an {@code ActionEvent} */
     private void handleActionEvent(final ActionEvent e) {
          (new Thread() {
               @Override
               public void run() {
                    if(loadingImage)
                        return;
                    String ac = e.getActionCommand();
                    ij.log().debug("Action command: " + ac);
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
                    else if(ac.equals("openROIManager"))
                         openROIManager();
                    else if(ac.equals("exportSVG"))
                         exportSVG();
                    else if(ac.equals("changeColorMode"))
                         changeDisplayMode();
                    else if(ac.equals("changeLevels"))
                         changeLevels();
               }
          }).start();
     }
     
     /** Load the internal data */
     private void loadData() throws SlideSetException {
          ArrayList<ColumnBoundReader> iCbrs;
          iCbrs = dtid.getCompatableColumnReaders(Dataset.class, slideSet);
          if(iCbrs == null || iCbrs.isEmpty()) {
               JOptionPane.showMessageDialog(this,
                       "This table does not contain any images. "
                       + "Cannot create ROIs.",
                       "SlideSet - ROI Editor",
                       JOptionPane.ERROR_MESSAGE);
               active = false;
               throw new OperationCanceledException("No images in table.");
          }
          if(iCbrs.size() > 1) {
               int choices = iCbrs.size();
               String[] names = new String[choices];
               for(int i=0; i<choices; i++)
                    names[i] = String.valueOf(i+1)
                            + ": " + iCbrs.get(i).getColumnName();
               Object choice = JOptionPane.showInputDialog(this,
                       "Select images on which ROIs will be drawn:",
                       "SlideSet - ROI Editor",
                       JOptionPane.PLAIN_MESSAGE,
                       null, names, names[0]);
               if(choice == null)
               { throw new OperationCanceledException("No images selected"); }
               for(int i=0; i<choices; i++) {
                    images = iCbrs.get(i);
                    if(names[i].equals(choice))
                         break;
               }
          }
          else images = iCbrs.get(0);
          loadOverlays();
     }
     
     /** Load overlay data from disk */
     private void loadOverlays() throws SlideSetException {
          if(roiReaders == null)
              roiReaders = new ArrayList<ColumnBoundReader>();
          if(roiWriters == null)
              roiWriters = new ArrayList<ColumnBoundWriter>();
          dtid.getColumnReadWritePairs(
                 AbstractOverlay[].class, slideSet, roiReaders, roiWriters);
          for(int u = 0; u < roiReaders.size(); u++) {
               final int i = roiReaders.get(u).getColumnNum();
               roiSetNames.add(roiReaders.get(u).getColumnName());
               final String defp = slideSet.getColumnDefaultPath(i);
               if(defp == null || defp.isEmpty())
                    slideSet.setColumnDefaultPath(i, "roi");
               final String dlp = slideSet.getDefaultLinkPrefix(i);
               if(dlp == null || dlp.isEmpty())
                   slideSet.setDefaultLinkPrefix(
                         i, slideSet.getColumnName(i).replaceAll("\\W", "-"));
               final String dlex = slideSet.getDefaultLinkExtension(i);
               if(dlex == null || dlex.isEmpty()) {
                   if(slideSet.getColumnMimeType(i).equals(MIME.SVG))
                       slideSet.setDefaultLinkExtension(i, "svg");
                   else
                       slideSet.setDefaultLinkExtension(i, "roiset");
               }
               AbstractOverlay[][] set = new AbstractOverlay[slideSet.getNumRows()][];
               for(int j=0; j<slideSet.getNumRows(); j++) {
                    try{
                         set[j] = (AbstractOverlay[]) roiReaders.get(u).read(j);
                    } catch(LinkNotFoundException e) {
                         log.println("\nWarning: Could not find ROI set file \""
                                + slideSet.getItemText(i, j) + "\"");
                         set[j] = null;
                    } catch (RoiLinkException e) {
                         log.println("\nError: Could not read ROI set file!");
                         log.println("# This could be because the file specified");
                         log.println("# is not really an ROI set file.");
                         handleError(e);
                         set[j] = null;
                    } catch (Exception ex) {
                         log.println("\nWarning: Unable to read ROI set.");
                         handleError(ex);
                         set[j] = null;
                    }
                    
               }
               roiSets.add(set);
          }
          if(!roiSets.isEmpty())
              curRoiSet = 0;
     }
     
     /**
      * Update the state of the controls
      * Do NOT call from the event dispatch thread.
      */
     private void updateControls() {
          try {
            SwingUtilities.invokeAndWait( new Thread() {
                @Override
                public void run() {
                  roiSetList.setModel(
                          new DefaultComboBoxModel(getRoiSetNames()));
                  roiSetList.setSelectedIndex(curRoiSet);
                  imageList.setModel(
                          new DefaultComboBoxModel(getImageNames()));
                  imageList.setSelectedIndex(curImage);
                }
            });
          } catch(Exception e) {
              throw new IllegalArgumentException(e);
          }
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
          String[] names = new String[slideSet.getNumRows()];
          for(int i=0; i<slideSet.getNumRows(); i++)
               names[i] = String.valueOf(i+1) + ": " +
                       new File(slideSet.getItemText(images.getColumnNum(), i)
                       .toString()).getName();
          return names;
     }
     
     /** Load and display the selected image */
     private void loadImage(int imageIndex) {
          Dataset ds = null;
          synchronized(this) {
              if( loadingImage
                      || imageIndex < 0
                      || imageIndex >= slideSet.getNumRows())
                  return;
              loadingImage = true;
              if(imageIndex == curImage && imageDisplay != null) {
                    for(DataView dv : imageDisplay) {
                        Data d = dv.getData();
                        if(d instanceof Dataset)
                            ds = (Dataset) d;
                    }
              }
              curImage = imageIndex;
          }
          updateControls();
          
          if(ds == null) try {
               ds = images.read(imageIndex);
          } catch(LinkNotFoundException e) {
               log.println("\nError: Unable to find image \""
                    + slideSet.getItemText(
                    images.getColumnNum(), imageIndex) + "\"");
               if(imageWindow != null)
                   imageWindow.close();
               loadingImage = false;
               return;
          } catch(ImgLinkException e) {
               log.println("\nError: Unable to load image");
               log.println("# \"" +
                       slideSet.getItemText(
                       images.getColumnNum(), imageIndex) + "\"");
               log.println("# It may not be a valid image file!");
               ij.log().debug(e);
               if(imageWindow != null)
                   imageWindow.close();
               loadingImage = false;
               return;
          } catch(Throwable t) {
               log.println("\nFatal error: Unexpected problem loading image!");
               ij.log().debug(t);
               kill();
               return;
          }
          if(imageDisplay != null)
              imageDisplay.close();
          imageDisplay = new FastUpdateImageDisplay();
          ij.getContext().inject(imageDisplay);
          imageDisplay.display(ds);

          createImageWindow();
          imageDisplay.update();
          
          drawOverlays();
          changeDisplayMode();
          registerBrightnessContrast(bcDialog);
          imageWindow.setTitle("ROI Editor");
          loadingImage = false;
     }
     
     /** Update the color mode */
     private void changeDisplayMode() {
         ColorMode m;
         switch(displayMode.getSelectedIndex()) {
            default:
            case 0:
                m = ColorMode.COMPOSITE;
                break;
            case 1:
                m = ColorMode.GRAYSCALE;
                break;
            case 2:
                m = ColorMode.COLOR;
        }
         for(DataView v : imageDisplay) {
             if(!DatasetView.class.isInstance(v)) continue;
             ((DatasetView)v).setColorMode(m);
         }
         imageDisplay.update();
     }
     
     /** Open the Brightness/Contrast dialog */
     private void changeLevels() {
         CommandInfo bci = ij.command().getCommand(BrightnessContrastRoi.class);
         //CommandInfo bci = ij.command().getCommand(net.imagej.plugins.commands.display.interactive.BrightnessContrast.class);
         ij.command().run(bci, true, "roiEditor", this);
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
                         final DisplayWindow dw = ui.createDisplayWindow(imageDisplay);
                         if(!(dw instanceof SwingDisplayWindow))
                             throw new IllegalArgumentException("Must run in a windowed environment!");
                         imageWindow = (SwingDisplayWindow) dw;
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
     
     /** Draw appropriate overlays on the image */
     private void drawOverlays() {
          if(imageDisplay == null)
               return;
          if(curRoiSet < 0 || curRoiSet >= roiSets.size())
               return;
          if(!ImageDisplay.class.isInstance(imageDisplay))
               throw new IllegalArgumentException("Bad display type.");
          imageDisplay.clearOverlaysFast();
          imageDisplay.update();
          Overlay[] overlays = roiSets.get(curRoiSet)[curImage];
          ImageDisplayService ids = ij.get(ImageDisplayService.class);
          if(overlays != null)
               for(int i = 0; i < overlays.length; i++)
                   imageDisplay.addFast(overlays[i], ids);
          imageDisplay.rebuildNow();
          imageDisplay.update();
     }
     
     /** Save overlays drawn on the current image to memory, not to disk. */
     private void saveOverlays() {
          if(locked) return; // Don't save changes if read-only
          if(curRoiSet < 0 || curRoiSet >= roiSets.size())
               return;
          if(imageDisplay == null || imageDisplay.isEmpty())
              return;
          if(!ImageDisplay.class.isInstance(imageDisplay)) {
               log.println("\nError: Unable to record overlays.");
               log.println("# There is not a valid display open.");
          }
          List<Overlay> overlays = os.getOverlays((ImageDisplay) imageDisplay);
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
          if(locked) { // Don't save changes if read-only. Here we'll let the user know.
              JOptionPane.showMessageDialog(this, "This ROI set is locked. Unable to save changes.", "Slide Set", JOptionPane.ERROR_MESSAGE);
              return;
          }
          saveOverlays();
          if(roiSets.isEmpty())
               return;
          for(int i=0; i<roiSets.size(); i++) {
               for(int row=0; row < slideSet.getNumRows(); row++) {
                    final ColumnBoundWriter w = roiWriters.get(i);
                    try {
                         String dest = 
                              slideSet.getItemText(w.getColumnNum(), row);
                         if(dest == null || dest.isEmpty()) {
                              slideSet.makeDefaultLink(w.getColumnNum(), row);
                              dest =
                                  slideSet.getItemText(w.getColumnNum(), row);
                         }
                         if(w.getWriter()
                                 instanceof AbstractOverlaysToSVGFileWriter) {
                             final String imgpath
                                 = slideSet.resolvePath(
                                   slideSet.getItemText(images.getColumnNum(), row));
                             dest = slideSet.resolvePath(dest);
                             final AbstractOverlaysToSVGFileWriter aosvg
                                 = (AbstractOverlaysToSVGFileWriter) w.getWriter();
                             aosvg.write(roiSets.get(i)[row], dest, -1, -1, imgpath);
                         }
                         else
                             w.write(roiSets.get(i)[row], row);
                         changed = false;
                    } catch(LinkNotFoundException e) {
                         log.println("\nError: \""
                             + slideSet.getItemText(w.getColumnNum(), row)
                             + "\"");
                         log.println("# is not a valid path, so the");
                         log.println("# ROI set cannot be saved!");
                    } catch(SlideSetException e) {
                         log.println("\nError: Unable to save ROI set!");
                         handleError(e);
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
          try {
               loadOverlays();
          } catch(Exception e) {
               handleError(e);
          }
          loadImage(curImage);
     }
     
     /** Create a new set of overlays (ROIs) */
     private void createRoiSet() {
          String name = JOptionPane.showInputDialog(this, "New ROI set name:");
          if(name == null)
               return;
          name = name.trim();
          name = name.equals("") ? "ROI" : name;
          int colI;
          try {
              colI = slideSet.addColumn(name, FileLinkElement.class, MIME.SVG);
          } catch(Exception e) {
              handleError(e);
              return;
          }
          roiSetNames.add(name);
          roiSets.add(new AbstractOverlay[slideSet.getNumRows()][]);
          roiReaders.add(
                  new ColumnBoundReader(slideSet, colI,
                  new SVGFileToAbstractOverlayReader()));
          roiWriters.add(
                  new ColumnBoundWriter(slideSet, colI,
                  new AbstractOverlaysToSVGFileWriter()));
          slideSet.setColumnDefaultPath(colI, "roi");
          slideSet.setDefaultLinkPrefix(colI, name.replaceAll("\\W", "-"));
          slideSet.setDefaultLinkExtension(colI, "svg");
          try {
               for(int i=0; i<slideSet.getNumRows(); i++)
                    slideSet.makeDefaultLink(colI, i);
          } catch(SlideSetException e) {
               handleError(e);
          }
          curRoiSet = roiSets.size() - 1;
          updateControls();
          loadImage(curImage);
     }
     
     /** Clean up and close the editor */
     @Override
     public void kill() {
          ij.log().debug("Closing ROI editor");
          if(active && (!locked) && changed &&
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
               dispose();
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
          loadImage(index);
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
          loadImage(curImage);
     }
     
     /** Open the ImageJ overlay manager window */
     private void openROIManager() {
          CommandService cs = os.getContext().getService(CommandService.class);
          try {
              cs.run(OverlayManager.class, true, new Object[0]);
          } catch(Exception e) {
              log.println("\nUnable to open ROI Manager window.");
              handleError(e);
          }
     }
     
     /** Check to see if one of the ROIs has been changed */
     @EventHandler
     private void onEvent(OverlayUpdatedEvent e) {
         flagOverlayChanges(e);
     }
     
     /** Check to see if one of the ROIs has been restructured */
     @EventHandler
     private void onEvent(OverlayRestructuredEvent e) {
         flagOverlayChanges(e);
     }
     
     /** Handle an ROI creation */
     @EventHandler
     private void onEvent(OverlayCreatedEvent e) {
         flagOverlayChanges(e);
     }
     
     /** Handle an ROI deletion */
     @EventHandler
     private void onEvent(OverlayDeletedEvent e) {
         flagOverlayChanges(e);
     }
     
     /** Check to see if an ROI change should set the {@code changed} flag */
     private void flagOverlayChanges(ObjectEvent e) {
         if(imageDisplay==null)
             return;
         if(e instanceof OverlayCreatedEvent && (!loadingImage)) {
             changed = true; // A bit hacky and non-specific, but the OverlayCreatedEvent is fired before the overlay is added to the display!
             return;
         }
         for(DataView dv : imageDisplay) {
             if(e.getObject() == dv.getData())
                 changed = true;
         }
     }
     
     private void exportSVG() {
         saveOverlays();
         JFileChooser fc = new JFileChooser(slideSet.getWorkingDirectory());
         fc.setDialogType(JFileChooser.SAVE_DIALOG);
         fc.setDialogTitle("Save ROIs as...");
         fc.setFileFilter(new FileNameExtensionFilter("SVG file", "svg"));
         fc.setSelectedFile(new File("ROI" + ".svg"));
         final int r = fc.showDialog(this, "Save");
         if(r != JFileChooser.APPROVE_OPTION)
             return;
         final File path = fc.getSelectedFile();
         if(path == null)
             return;
         if(path.exists()
               && JOptionPane.showConfirmDialog(this, 
               "File exists. OK to overwrite?", 
               "Slide Set", JOptionPane.OK_CANCEL_OPTION)
               != JOptionPane.OK_OPTION )
             return;
         try {
             int w = new Double(((ImageDisplay) imageDisplay)
                     .getPlaneExtents().width).intValue() + 1; //Not sure why, but needs to be corrected...
             int h = new Double(((ImageDisplay) imageDisplay)
                     .getPlaneExtents().height).intValue() + 1;
             String imgPath = slideSet.getItemText(
                     images.getColumnNum(), curImage);
             if(!(new File(imgPath)).isAbsolute())
                 imgPath = slideSet.getWorkingDirectory() + File.separator + imgPath;
             new AbstractOverlaysToSVGFileWriter()
                     .write(roiSets.get(curRoiSet)[curImage],
                     path.getAbsolutePath(),
                     w, h, imgPath);
         } catch(Exception e) {
             handleError(e);
         }
     }
     
     private void handleError(Exception e) {
         log.println(e.getLocalizedMessage());
         ij.log().debug(e);
     }
     
}
