package edu.emory.cellbio.ijbat.ui;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.dm.ColumnBoundReader;
import edu.emory.cellbio.ijbat.dm.ColumnBoundWriter;
import edu.emory.cellbio.ijbat.dm.DataElement;
import edu.emory.cellbio.ijbat.dm.DataTypeIDService;
import edu.emory.cellbio.ijbat.dm.FileLinkElement;
import edu.emory.cellbio.ijbat.dm.MIME;
import edu.emory.cellbio.ijbat.dm.read.SVGFileToIJ1ROIReader;
import edu.emory.cellbio.ijbat.dm.write.IJ1ROIsToSVGFileWriter;
import edu.emory.cellbio.ijbat.ex.ImgLinkException;
import edu.emory.cellbio.ijbat.ex.LinkNotFoundException;
import edu.emory.cellbio.ijbat.ex.OperationCanceledException;
import edu.emory.cellbio.ijbat.ex.RoiLinkException;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.RoiListener;
import ij.plugin.frame.Channels;
import ij.plugin.frame.ContrastAdjuster;
import ij.plugin.frame.RoiManager;
import java.awt.Point;
import java.awt.Window;
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
import net.imagej.ImageJ;

/**
 *
 * @author Benjamin Nanes
 */
public class RoiEditorIJ1 
        extends JFrame 
        implements ActionListener, RoiListener, SlideSetWindow {
    
    // -- Fields --
    
    private SlideSet slideSet;
    private DataTypeIDService dtid;
    private ImageJ ij;

    private ArrayList<ColumnBoundReader<? extends DataElement, ImageWindow>> imageReaders;
    private ArrayList<ColumnBoundReader<? extends DataElement, Roi[]>> roiReaders;
    private ArrayList<ColumnBoundWriter<? extends DataElement, Roi[]>> roiWriters;
    
    /** Names of the image sets */
    private ArrayList<String> imageSetNames;
    /** Names of the ROI sets */
    private ArrayList<String> roiSetNames;
    /** ROI sets {@code AbstractOverlay[image#][Roi#]} */
    private ArrayList<Roi[][]> roiSets;
    /** Current image set index */
    private int curImageSet = 0;
    /** Current ROI set index */
    private int curRoiSet = -1;
    /** Current image index */
    private int curImage = 0;

    private JComboBox imageSetList;
    private JComboBox roiSetList;
    private JButton addRoiSet;
    // private JButton deleteRoiSet;
    private JButton displayMode;
    private JButton changeLevels;
    private JButton exportSVG;
    private JComboBox imageList;
    private JButton goImageNext;
    private JButton goImageBack;
    private JButton saveChanges;
    private JButton undoChanges;
    
    /** The image display */
    private ImageWindow imageWindow;
    /** The ROI manager */
    private RoiManager roiManager;

    /**  Active flag */
    private boolean active = false;
    /** Busy loading an image flag */
    private boolean loadingImage = false;
    /** The log */
    private SlideSetLog log;
    /** Read-only mode flag */
    private boolean locked = false;
    /** Changed flag */
    private boolean changed = false;

    // -- Constructors --
    
    public RoiEditorIJ1(SlideSet slideSet, DataTypeIDService dtid,
            ImageJ ij, SlideSetLog log) {
        if (slideSet == null || dtid == null || ij == null || log == null) {
            throw new IllegalArgumentException("Can't initiate with null elements");
        }
        this.slideSet = slideSet;
        this.dtid = dtid;
        this.ij = ij;
        this.log = log;
        roiSetNames = new ArrayList<String>();
        roiSets = new ArrayList<Roi[][]>();
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        buildLayout();
        setActionListeners();
    }
    
    // -- Methods --
    
    /** Run the ROI editor. Returns when finished. Use separate thread. */
    public void showAndWait() {
        ij.legacy().toggleLegacyMode(true);
        synchronized (this) {
            active = true;
            try {
                loadImageData();
                loadRois();
            } catch (SlideSetException ex) {
                handleError(ex);
                active = false;
                return;
            }
            if (!active) {
                return;
            }
            updateControls();
            setVisible(true);
        }
        Roi.addRoiListener(this);
        loadImage(curImage);
        if (imageWindow != null && imageWindow.isVisible()) {
            Point p = imageWindow.getLocationOnScreen();
            setLocation(Math.max(p.x - getWidth(), 0), Math.max(p.y, 0));
        }
        openROIManager();
        synchronized (this) {
            while (active) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
            setVisible(false);
        }
    }
    
    /** Action handler */
    @Override
    public void actionPerformed(ActionEvent e) {
        handleActionEvent(e);
    }
    
    /** Activate read-only mode and prevent changes to ROIs */
    public void lock() {
        locked = true;
    }
    
    /** Clean up and close the editor */
    @Override
    public void kill() {
        ij.log().debug("Closing ROI editor");
        if (active && (!locked) && changed
                && JOptionPane.showConfirmDialog(this, "Save changes?",
                        "ROI Editor", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION) {
            saveRois();
            writeRois();
        }
        Roi.removeRoiListener(this);
        synchronized (this) {
            active = false;
            setVisible(false);
            if (imageWindow != null && imageWindow.isVisible()) {
                imageWindow.close();
                imageWindow.dispose();
            }
            roiManager.close();
            dispose();
            notifyAll();
        }
    }
    
    /**
     * Automatically add ROIs to the ROI manager.
     * 
     * This appears to work only for some ROI types due to
     * inconsistencies in how ROI events are dispatched.
     * In particular, the following ROI types do not dispatch a
     * {@code COMPLETED} event:
     * <li>Roi</li>
     * <li>Line</li>
     * <li>OvalRoi</li>
     * <p>I might find a way to fix this at some point.
     */
    public void roiModified(final ImagePlus imp, final int code) {
        final RoiManager rman = roiManager();
        if(imageWindow == null || imageWindow.isClosed())
            return;
        if(!imageWindow.getImagePlus().equals(imp))
            return;
        changed = true;
        if(code != RoiListener.COMPLETED)
            return;
        final Roi r = imp.getRoi();
        if(rman.getRoiIndex(r) > 0)
            return;
        rman.addRoi(r);
        rman.select(rman.getCount() - 1);
    }
    
    // -- Helper methods --
    
    /** Build the window */
    private void buildLayout() {
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        add(Box.createVerticalStrut(5));

        imageSetList = new JComboBox();
        add(imageSetList);
        add(Box.createVerticalStrut(5));
        roiSetList = new JComboBox();
        add(roiSetList);
        add(Box.createVerticalStrut(5));
        
        JPanel rsetButtons = new JPanel();
        rsetButtons.setLayout(new BoxLayout(rsetButtons, BoxLayout.Y_AXIS));
        addRoiSet = new JButton("Add ROI Set");
        // deleteRoiSet = new JButton("Delete");
        exportSVG = new JButton("Export SVG");
        Box addRoiSetBox = Box.createHorizontalBox();
        addRoiSetBox.add(Box.createHorizontalGlue());
        addRoiSetBox.add(addRoiSet);
        addRoiSetBox.add(Box.createHorizontalGlue());
        rsetButtons.add(addRoiSetBox);
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
        displayMode = new JButton("Channels");
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
            public void windowClosing(WindowEvent e) {
                kill();
            }
        });
        goImageBack.setActionCommand("imageBack");
        goImageBack.addActionListener(this);
        goImageNext.setActionCommand("imageNext");
        goImageNext.addActionListener(this);
        imageList.setActionCommand("imageListSelection");
        imageList.addActionListener(this);
        addRoiSet.setActionCommand("roiSetNew");
        addRoiSet.addActionListener(this);
        exportSVG.setActionCommand("exportSVG");
        exportSVG.addActionListener(this);
        imageSetList.setActionCommand("imageSetListSelection");
        imageSetList.addActionListener(this);
        roiSetList.setActionCommand("roiSetListSelection");
        roiSetList.addActionListener(this);
        saveChanges.setActionCommand("writeRoiSets");
        saveChanges.addActionListener(this);
        undoChanges.setActionCommand("revertRoiSets");
        undoChanges.addActionListener(this);
        changeLevels.setActionCommand("changeLevels");
        changeLevels.addActionListener(this);
        displayMode.setActionCommand("changeColorMode");
        displayMode.addActionListener(this);
    }
    
    /** Handle an {@code ActionEvent} */
    private void handleActionEvent(final ActionEvent e) {
        (new Thread() {
            @Override
            public void run() {
                if (loadingImage) {
                    return;
                }
                String ac = e.getActionCommand();
                ij.log().debug("Action command: " + ac);
                if (ac.equals("imageBack")) {
                    setImage(curImage - 1);
                } else if (ac.equals("imageNext")) {
                    setImage(curImage + 1);
                } else if (ac.equals("imageListSelection")) {
                    setImage(imageList.getSelectedIndex());
                } else if (ac.equals("roiSetNew")) {
                    createRoiSet();
                } else if (ac.equals("imageSetListSelection")) {
                    setImageSet(imageSetList.getSelectedIndex());
                } else if (ac.equals("roiSetListSelection")) {
                    setRoiSet(roiSetList.getSelectedIndex());
                } else if (ac.equals("writeRoiSets")) {
                    writeRois();
                } else if (ac.equals("revertRoiSets")) {
                    revertRois();
                } else if (ac.equals("openROIManager")) {
                    openROIManager();
                } else if (ac.equals("exportSVG")) {
                    exportSVG();
                } else if (ac.equals("changeLevels")) {
                    changeLevels();
                } else if (ac.equals("changeColorMode")) {
                    changeColorMode();
                }
            }
        }).start();
    }
    
    /** Load the index of images */
    private void loadImageData() throws SlideSetException {
        final List<ColumnBoundReader> iCbrs;
        iCbrs = dtid.getCompatableColumnReaders(ImageWindow.class, slideSet);
        if (iCbrs == null || iCbrs.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "This table does not contain any images. "
                    + "Cannot create ROIs.",
                    "SlideSet - ROI Editor",
                    JOptionPane.ERROR_MESSAGE);
            active = false;
            throw new OperationCanceledException("No images in table.");
        }
        imageReaders = new ArrayList<ColumnBoundReader<? extends DataElement, ImageWindow>>();
        imageSetNames = new ArrayList<String>();
        for(ColumnBoundReader r : iCbrs) {
            imageReaders.add(r);
            imageSetNames.add(r.getColumnName());
        }
    }
    
    /** Load the ROI data */
    private void loadRois() throws SlideSetException {
        roiReaders = new ArrayList<ColumnBoundReader<? extends DataElement, Roi[]>>();
        roiWriters = new ArrayList<ColumnBoundWriter<? extends DataElement, Roi[]>>();
        roiSetNames = new ArrayList<String>();
        ArrayList<ColumnBoundReader> reads = new ArrayList<ColumnBoundReader>();
        ArrayList<ColumnBoundWriter> writes = new ArrayList<ColumnBoundWriter>();
        dtid.getColumnReadWritePairs(
                Roi[].class, slideSet, reads, writes);
        for(int u=0; u<reads.size(); u++) {
            roiReaders.add(reads.get(u));
            roiWriters.add(writes.get(u));
            final int i = reads.get(u).getColumnNum();
            roiSetNames.add(slideSet.getColumnName(i));
            final String defp = slideSet.getColumnDefaultPath(i);
            if (defp == null || defp.isEmpty()) {
                slideSet.setColumnDefaultPath(i, "roi");
            }
            final String dlp = slideSet.getDefaultLinkPrefix(i);
            if (dlp == null || dlp.isEmpty()) {
                slideSet.setDefaultLinkPrefix(
                        i, slideSet.getColumnName(i).replaceAll("\\W", "-"));
            }
            final String dlex = slideSet.getDefaultLinkExtension(i);
            if (dlex == null || dlex.isEmpty()) {
                if (slideSet.getColumnMimeType(i).equals(MIME.SVG)) {
                    slideSet.setDefaultLinkExtension(i, "svg");
                } else {
                    slideSet.setDefaultLinkExtension(i, "roiset");
                }
            }
            Roi[][] set = new Roi[slideSet.getNumRows()][];
            for (int j = 0; j < slideSet.getNumRows(); j++) {
                try {
                    set[j] = roiReaders.get(u).read(j);
                } catch (LinkNotFoundException e) {
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
        else
            curRoiSet = -1;
    }
    
    /**
     * Load and display the selected image.
     */
    private void loadImage(int imageIndex) {
        final ColumnBoundReader<? extends DataElement, ImageWindow> images;
        Point loc = null;
        synchronized (this) {
            if (loadingImage
                    || imageIndex < 0
                    || imageIndex >= slideSet.getNumRows()) {
                return;
            }
            loadingImage = true;
            images = imageReaders.get(curImageSet);
            if(imageWindow != null)
                loc = imageWindow.getLocation();
            curImage = imageIndex;
        }
        updateControls();
        if(imageWindow != null)
            imageWindow.close();
        try {
            imageWindow = images.read(curImage);
        } catch (LinkNotFoundException e) {
            log.println("\nError: Unable to find image \""
                    + slideSet.getItemText(
                            images.getColumnNum(), imageIndex) + "\"");
            if (imageWindow != null) {
                imageWindow.close();
            }
            loadingImage = false;
            return;
        } catch (ImgLinkException e) {
            log.println("\nError: Unable to load image");
            log.println("# \""
                    + slideSet.getItemText(
                            images.getColumnNum(), imageIndex) + "\"");
            log.println("# It may not be a valid image file!");
            ij.log().debug(e);
            if (imageWindow != null) {
                imageWindow.close();
            }
            loadingImage = false;
            return;
        } catch (Throwable t) {
            log.println("\nFatal error: Unexpected problem loading image!");
            ij.log().debug(t);
            kill();
            return;
        }
        attachKillListener(imageWindow);
        
        if(loc == null)
            imageWindow.setLocationRelativeTo(null);
        else
            imageWindow.setLocation(loc);
        drawRois();
        imageWindow.setTitle("ROI Editor");
        loadingImage = false;
    }
    
    /**
     * Open the indexed image, with wrap-around correction 
     * for out of range indeces.
     * @param i The index of the image to load
     */
    private void setImage(int i) {
        final int n = imageList.getItemCount();
        if (i < 0)
            i = n - 1;
        else if (i >= n)
            i = 0;
        if(i == curImage)
            return;
        saveRois();
        loadImage(i);
    }
    
    /**
     * Open the currently-selected image from the indexed image set.
     * @param i The index of the image set from which images will be opened
     */
    private void setImageSet(int i) {
        final int n = imageSetList.getItemCount();
        if (i < 0)
            i = n - 1;
        else if (i >= n)
            i = 0;
        if(i == curImageSet)
            return;
        curImageSet = i;
        updateControls();
        loadImage(curImage);
    }
    
    /** Get the ROI manager */
    private RoiManager roiManager() {
        if(roiManager == null)
            openROIManager();
        return roiManager;
    }
    
    /**
     * Draw ROIs on the image.
     */
    private void drawRois() {
        if(imageWindow == null || imageWindow.isClosed())
            return;
        if(curRoiSet < 0 || curRoiSet >= roiSets.size())
            return;
        roiManager().reset();
        roiManager().runCommand("Show All");
        WindowManager.toFront(imageWindow);
        final Roi[] rSet = roiSets.get(curRoiSet)[curImage];
        if(rSet == null)
            return;
        for(Roi r : rSet)
            roiManager().addRoi(r);
    }
    
    /**
     * Save ROIs from the ROI manager (to memory, not to disk).
     */
    private void saveRois() {
        if (locked)
            return; // Don't save changes if read-only
        if (curRoiSet < 0 || curRoiSet >= roiSets.size())
            return;
        if (imageWindow == null)
            return;
        roiSets.get(curRoiSet)[curImage] = roiManager().getRoisAsArray();
    }
    
    /**
     * Save all the ROIs to disk.
     */
    private void writeRois() {
        if (locked) { // Don't save changes if read-only. Here we'll let the user know.
            JOptionPane.showMessageDialog(this, "This ROI set is locked. Unable to save changes.", "Slide Set", JOptionPane.ERROR_MESSAGE);
            return;
        }
        saveRois();
        if (roiSets.isEmpty()) {
            return;
        }
        for (int i = 0; i < roiSets.size(); i++) {
            for (int row = 0; row < slideSet.getNumRows(); row++) {
                final ColumnBoundWriter w = roiWriters.get(i);
                try {
                    String dest
                            = slideSet.getItemText(w.getColumnNum(), row);
                    if (dest == null || dest.isEmpty()) {
                        slideSet.makeDefaultLink(w.getColumnNum(), row);
                        dest
                                = slideSet.getItemText(w.getColumnNum(), row);
                    }
                    if (w.getWriter() instanceof IJ1ROIsToSVGFileWriter) {
                        final String imgpath
                                = slideSet.resolvePath(
                                        slideSet.getItemText(imageReaders
                                        .get(curImageSet)
                                        .getColumnNum(), row));
                        dest = slideSet.resolvePath(dest);
                        final IJ1ROIsToSVGFileWriter aosvg
                                = (IJ1ROIsToSVGFileWriter) w.getWriter();
                        aosvg.write(roiSets.get(i)[row], dest, -1, -1, imgpath);
                    } else {
                        w.write(roiSets.get(i)[row], row);
                    }
                    changed = false;
                } catch (LinkNotFoundException e) {
                    log.println("\nError: \""
                            + slideSet.getItemText(w.getColumnNum(), row)
                            + "\"");
                    log.println("# is not a valid path, so the");
                    log.println("# ROI set cannot be saved!");
                } catch (SlideSetException e) {
                    log.println("\nError: Unable to save ROI set!");
                    handleError(e);
                }
            }
        }
    }
    
    /**
     * Load the indexed ROI set into the display.
     * @param i Index of the ROI set to display
     */
    private void setRoiSet(int i) {
        final int n = roiSetList.getItemCount();
        if (i < 0)
            i = n - 1;
        else if (i >= n)
            i = 0;
        if(i == curRoiSet)
            return;
        saveRois();
        curRoiSet = i;
        updateControls();
        drawRois();
    }
    
    /**
     * Create a new set of ROIs and add it as a new column in the data table.
     */
    private void createRoiSet() {
        String name = JOptionPane.showInputDialog(this, "New ROI set name:");
        if (name == null)
            return;
        name = name.trim();
        name = name.equals("") ? "ROI" : name;
        int colI;
        try {
            colI = slideSet.addColumn(name, FileLinkElement.class, MIME.SVG);
        } catch (Exception e) {
            handleError(e);
            return;
        }
        roiSetNames.add(name);
        roiSets.add(new Roi[slideSet.getNumRows()][]);
        roiReaders.add(
                new ColumnBoundReader(slideSet, colI,
                new SVGFileToIJ1ROIReader()));
        roiWriters.add(
                new ColumnBoundWriter(slideSet, colI,
                new IJ1ROIsToSVGFileWriter()));
        slideSet.setColumnDefaultPath(colI, "roi");
        slideSet.setDefaultLinkPrefix(colI, name.replaceAll("\\W", "-"));
        slideSet.setDefaultLinkExtension(colI, "svg");
        try {
            for (int i = 0; i < slideSet.getNumRows(); i++) {
                slideSet.makeDefaultLink(colI, i);
            }
        } catch (SlideSetException e) {
            handleError(e);
        }
        curRoiSet = roiSets.size() - 1;
        updateControls();
        drawRois();
    }
    
    /**
     * With user confirmation, revert overlays to last saved version.
     */
    private void revertRois() {
        if (JOptionPane.showConfirmDialog(this,
                "Revert all regions of interest to last saved version?",
                "ROI Editor", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            loadRois();
        } catch (Exception e) {
            handleError(e);
        }
        loadImage(curImage);
    }
    
    /**
     * Export and SVG file with the current image and ROIs.
     */
    private void exportSVG() {
        final JFileChooser fc = new JFileChooser(slideSet.getWorkingDirectory());
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        fc.setDialogTitle("Save ROIs as...");
        fc.setFileFilter(new FileNameExtensionFilter("SVG file", "svg"));
        fc.setSelectedFile(new File("ROI" + ".svg"));
        final int r = fc.showDialog(this, "Save");
        if (r != JFileChooser.APPROVE_OPTION) {
            return;
        }
        final File path = fc.getSelectedFile();
        if (path == null) {
            return;
        }
        if (path.exists()
                && JOptionPane.showConfirmDialog(this,
                        "File exists. OK to overwrite?",
                        "Slide Set", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }
        saveRois();
        final int w = imageWindow.getImagePlus().getWidth();
        final int h = imageWindow.getImagePlus().getHeight();
        String imgPath = slideSet.getItemText(imageReaders.get(curImageSet).getColumnNum(), curImage);
        if (!(new File(imgPath)).isAbsolute())
            imgPath = slideSet.getWorkingDirectory() + File.separator + imgPath;
        try {
            new IJ1ROIsToSVGFileWriter()
                    .write(roiSets.get(curRoiSet)[curImage],
                            path.getAbsolutePath(),
                            w, h, imgPath);
        } catch (Exception e) {
            handleError(e);
        }
    }
    
    /**
     * Update the state of the controls Do NOT call from the event dispatch
     * thread.
     */
    private void updateControls() {
        try {
            SwingUtilities.invokeAndWait(new Thread() {
                @Override
                public void run() {
                    roiSetList.setModel(
                            new DefaultComboBoxModel(getRoiSetNames()));
                    roiSetList.setSelectedIndex(curRoiSet);
                    imageSetList.setModel
                            (new DefaultComboBoxModel(getImageSetNames()));
                    imageSetList.setSelectedIndex(curImageSet);
                    imageList.setModel(
                            new DefaultComboBoxModel(getImageNames()));
                    imageList.setSelectedIndex(curImage);
                }
            });
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    /**
     * Open the IJ1 ROI Manager and line it up below the ROI Editor pane.
     */
    private void openROIManager() {
        Point p = getLocation();
        p.y += getHeight();
        synchronized(this) {
            roiManager = RoiManager.getInstance();
            if(roiManager == null)
                roiManager = new RoiManager();
            attachKillListener(roiManager);
        }
        roiManager.setLocation(p);
    }
    
    /**
     * Open the IJ1 Contrast Adjuster.
     */
    private void changeLevels() {
        new ContrastAdjuster();
    }
    
    /**
     * Open the IJ1 channels tool.
     */
    private void changeColorMode() {
        new Channels();
    }
    
    /**
     * Get the names of available ROI sets to put in the list, prefixed by the
     * row number to avoid name duplications which cause problems with
     * {@code DefaultComboBoxModel}.
     */
    private String[] getRoiSetNames() {
        String[] names = new String[roiSetNames.size()];
        names = roiSetNames.toArray(names);
        for (int i = 0; i < names.length; i++) {
            names[i] = String.valueOf(i + 1) + ": " + names[i];
        }
        return names;
    }
    
    /**
     * Get the names of available image sets to put in the list, prefixed by the
     * row number to avoid name duplications which cause problems with
     * {@code DefaultComboBoxModel}.
     */
    private String[] getImageSetNames() {
        String[] names = new String[imageSetNames.size()];
        names = imageSetNames.toArray(names);
        for (int i = 0; i < names.length; i++) {
            names[i] = String.valueOf(i + 1) + ": " + names[i];
        }
        return names;
    }

    /**
     * Get the short names of image files to put in the list, prefixed by the
     * row number to avoid name duplications which cause problems with
     * {@code DefaultComboBoxModel}.
     */
    private String[] getImageNames() {
        String[] names = new String[slideSet.getNumRows()];
        for (int i = 0; i < slideSet.getNumRows(); i++) {
            names[i] = String.valueOf(i + 1) + ": "
                    + new File(slideSet.getItemText(imageReaders.get(curImageSet).getColumnNum(), i)).getName();
        }
        return names;
    }
    
    /**
     * Attach a listener to a {@code Window}, so that when the
     * {@code Window} closes, the ROI Editor will close as well.
     * @param w The {@code Window} to watch
     */
    private void attachKillListener(final Window w) {
        try {
            ij.thread().invoke(new Thread() {
                @Override
                public void run() {
                    w.addWindowListener(
                        new WindowAdapter() {
                            @Override
                            public void windowClosing(WindowEvent e) {
                                if (active)
                                    kill();
                            }
                        });
                }
            });
        } catch (InterruptedException e) {
            throw new IllegalArgumentException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    /** Handle an exception */
    private void handleError(Exception e) {
        log.println(e.getLocalizedMessage());
        ij.log().debug(e);
    }
    
}
