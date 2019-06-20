package edu.emory.cellbio.ijbat.ui;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.dm.CommandTemplate;
import edu.emory.cellbio.ijbat.dm.DataTypeIDService;
import edu.emory.cellbio.ijbat.ex.ColumnTypeException;
import edu.emory.cellbio.ijbat.ex.NoPluginInputSourceException;
import edu.emory.cellbio.ijbat.ex.OperationCanceledException;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import edu.emory.cellbio.ijbat.io.CSVService;
import edu.emory.cellbio.ijbat.io.CommandSkeletonService;
import edu.emory.cellbio.ijbat.io.XMLService;
import edu.emory.cellbio.ijbat.pi.SlideSetPluginLoader;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.xml.stream.XMLStreamException;
import net.imagej.ImageJ;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpdateSite;
import org.scijava.MenuPath;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

/**
 * Main SlideSet user interface.
 * 
 * @author Benjamin Nanes
 */
public class SlideSetLauncher extends JFrame
     implements ActionListener, MouseListener, SlideSetWindow, LogListener {
     
     // -- Fields --
     
     private final ImageJ ij;
     private final DataTypeIDService dtid;
     private final XMLService xmls;
     private final CSVService csvs;
     private final SlideSetPluginLoader sspl;
     private final SlideSetLog log;
     private final HttpHelpLoader helpLoader;
     
     private JMenuBar menuBar;
     private JTextArea info;
     private JTree tree;
     private final HashMap<Component, JPopupMenu> popupMenus 
          = new HashMap<Component, JPopupMenu>();
     
     /** Has the open file been changed? */
     private boolean changed = false;
     /** Path to the currently open file */
     private String openPath = null;
     /** List of open child windows */
     private ArrayList<SlideSetWindow> childWindows
             = new ArrayList<SlideSetWindow>(5);
     /** Data tables that are in use (locked) and the threads using them */
     private HashMap<SlideSet, String> lockedTables
             = new HashMap<SlideSet, String>(5);
     
     /** Application version */
     private String ver;
     
     // -- Constructor --
     
     @SuppressWarnings(value="LeakingThisInConstructor")
     public SlideSetLauncher(ImageJ context, DataTypeIDService dtid,
             XMLService xmls, CSVService csvs, SlideSetLog log) {
          if(context == null || dtid == null || xmls == null || csvs == null)
               throw new IllegalArgumentException("Can't initialize with null elements");
          helpLoader = new HttpHelpLoader(context);
          try { 
               ver = (
                    new BufferedReader(new InputStreamReader(
                    getClass().getClassLoader().getResourceAsStream("edu/emory/cellbio/ijbat/version.info")))
                    ).readLine();
          } catch(Exception e) { e.printStackTrace(System.err); ver = "ERR"; }
          context.log().setLevel(org.scijava.log.LogService.ERROR); // Prevents Fiji log window from poping up all the time. Not a durable fix!
          this.ij = context;
          this.dtid = dtid;
          this.xmls = xmls;
          this.csvs = csvs;
          this.sspl = new SlideSetPluginLoader(context, dtid, log, helpLoader);
          this.log = log;
          this.log.registerListener(this);
          buildLayout();
     }
     
     // -- Methods --

     /** ActionListener implementation */
     @Override
     public void actionPerformed(ActionEvent e) {
          handleActionEvent(e);
     }
     
     /** MouseListener implementation */
     @Override
     public void mouseClicked(MouseEvent e) {
          handleMouseEvent(e, "click");
     }
     
     /** MouseListener implementation */
     @Override
     public void mouseEntered(MouseEvent e) {
          handleMouseEvent(e, "enter");
     }

     /** MouseListener implementation */
     @Override
     public void mouseExited(MouseEvent e) {
          handleMouseEvent(e, "exit");
     }
     
     /** MouseListener implementation */
     @Override
     public void mousePressed(MouseEvent e) {
          handleMouseEvent(e, "press");
     }

     /** MouseListener implementation */
     @Override
     public void mouseReleased(MouseEvent e) {
          handleMouseEvent(e, "release");
     }
     
     @Override
     public void kill() {
          try{ 
               closeChildWindows();
               checkChanged();
          }
          catch(OperationCanceledException e) { 
               ij.log().debug(e);
               return;
          }
          synchronized(this) {
               setVisible(false);
               dispose();
               notifyAll();
          }
          ij.log().setLevel(org.scijava.log.LogService.INFO); // Reset default log behavior. Not a durable fix!
     }

     @Override
     public void logMessage(String message) {
          if(info == null)
               return;
          info.append(message);
          final Rectangle r = new Rectangle(0 , info.getHeight()-1, 
                  1, info.getHeight());
          info.scrollRectToVisible(r);
     }
     
     @Override
     public void setVisible(boolean b) {
          if(b && !isVisible()) {
               printLogHead();
               //newFile();
          }
          super.setVisible(b);
     }
     
     // -- Helper methods --
     
     /** Build the window */
     private void buildLayout() {
          setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));
          buildMenuBar();
          buildInfo();
          buildTree();
          buildPopupMenu();
          setTitle("Slide Set");
          setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
          addWindowListener( new WindowAdapter() {
               @Override
               public void windowClosing(WindowEvent e) { kill(); }
          });
          setLocationRelativeTo(null);
          pack();
     }
     
     /** Build the menu bar */
     private void buildMenuBar() {
          final JMenu file = new JMenu("File");
          final JMenu table = new JMenu("Table");
          final JMenu logM = new JMenu("Log");
          final JMenu help = new JMenu("Help");
          
          final JMenuItem nw = new JMenuItem("New...");
          nw.setActionCommand("new");
          nw.addActionListener(this);
          file.add(nw);
          final JMenuItem save = new JMenuItem("Save");
          save.setActionCommand("save");
          save.addActionListener(this);
          file.add(save);
          final JMenuItem saveas = new JMenuItem("Save As...");
          saveas.setActionCommand("save as");
          saveas.addActionListener(this);
          file.add(saveas);
          final JMenuItem open = new JMenuItem("Open...");
          open.setActionCommand("open");
          open.addActionListener(this);
          file.add(open);
          
          final JMenuItem tabV = new JMenuItem("View Table");
          tabV.setActionCommand("view table");
          tabV.addActionListener(this);
          table.add(tabV);
          final JMenuItem rois1 = new JMenuItem("ROI Editor");
          rois1.setActionCommand("view rois ij1");
          rois1.addActionListener(this);
          table.add(rois1);
          final JMenuItem rois = new JMenuItem("ROI Editor (IJ2)");
          rois.setActionCommand("view rois");
          rois.addActionListener(this);
          table.add(rois);
          table.addSeparator();
          table.add(buildSlideSetPluginsMenu()).setText("Run Slide Set Command");
          final JMenuItem oCom = new JMenuItem("Run Other Command (experimental)");
          oCom.setActionCommand("run command other");
          oCom.addActionListener(this);
          table.add(oCom);
          //table.add(buildOtherCommandsMenu()).setText("Run Other Command (experimental)");
          table.addSeparator();
          final JMenu cskelm = new JMenu("Command Skeleton");
          final JMenuItem saveCskel = new JMenuItem("Export...");
          saveCskel.setActionCommand("save command skeleton");
          saveCskel.addActionListener(this);
          final JMenuItem runCskel = new JMenuItem("Apply...");
          runCskel.setActionCommand("run command skeleton");
          runCskel.addActionListener(this);
          final JMenuItem docCskel = new JMenuItem("Documentation");
          docCskel.setActionCommand("doc command skeleton");
          docCskel.addActionListener(this);
          cskelm.add(saveCskel);
          cskelm.add(runCskel);
          cskelm.add(docCskel);
          table.add(cskelm);
          final JMenuItem csv = new JMenuItem("Export Data As CSV");
          csv.setActionCommand("save csv");
          csv.addActionListener(this);
          table.add(csv);
          table.addSeparator();
          final JMenuItem ulk = new JMenuItem("Unlock");
          ulk.setActionCommand("unlock table");
          ulk.addActionListener(this);
          table.add(ulk);
          final JMenuItem rnt = new JMenuItem("Rename");
          rnt.setActionCommand("rename");
          rnt.addActionListener(this);
          table.add(rnt);
          final JMenuItem tabP = new JMenuItem("Properties");
          tabP.setActionCommand("table props");
          tabP.addActionListener(this);
          table.add(tabP);
          final JMenuItem rmt = new JMenuItem("Delete");
          rmt.setActionCommand("delete table");
          rmt.addActionListener(this);
          table.add(rmt);
          
          final JMenuItem sl = new JMenuItem("Save");
          sl.setActionCommand("save log");
          sl.addActionListener(this);
          logM.add(sl);
          final JMenuItem cl = new JMenuItem("Clear");
          cl.setActionCommand("clear log");
          cl.addActionListener(this);
          logM.add(cl);
          
          final JMenuItem doc = new JMenuItem("Documentation");
          doc.setActionCommand("help doc");
          doc.addActionListener(this);
          help.add(doc);
          final JMenuItem about = new JMenuItem("About Slide Set");
          about.setActionCommand("about");
          about.addActionListener(this);
          help.add(about);
          
          menuBar = new JMenuBar();
          menuBar.add(file);
          menuBar.add(table);
          menuBar.add(logM);
          menuBar.add(help);
          setJMenuBar(menuBar);
     }
     
     /** Build the menu listing {@code SlideSetPlugin}s */
     private JMenu buildSlideSetPluginsMenu() {
          JMenu m = new JMenu("Slide Set Commands");
          List<CommandInfo> plugins = sspl.getPluginInfo();
          for(PluginInfo<Command> plugin : plugins) {
               String[] path = plugin.getMenuPath().getMenuString().split(MenuPath.PATH_SEPARATOR);
               if(path == null || path[0] == null || path[0].equals("")) {
                    path = new String[1];
                    path[0] = plugin.getTitle();
               }
               if(path.length > 3
                       && path[0].trim().equalsIgnoreCase("Plugins")
                       && path[1].trim().equalsIgnoreCase("Slide Set")
                       && path[2].trim().equalsIgnoreCase("Commands")) {
                   String[] pathShrunk
                           = Arrays.copyOfRange(path, 3, path.length);
                   path = pathShrunk;
               }
               String command = "sspl/" + plugin.getClassName();
               UIUtil.parseRecursiveMenuAdd(path, command, m, this);
          }
          return m;
     }
     
     /** Build the menu listing other ImageJ commands */
     private JMenu buildOtherCommandsMenu() {
          JMenu m = new JMenu("Other ImageJ Commands");
          m.add(new JMenu("Edit "));
          m.add(new JMenu("Image "));
          m.add(new JMenu("Process "));
          m.add(new JMenu("Analyze "));
          m.add(new JMenu("Plugins "));
          /*MenuService ijms = ij.getService(MenuService.class); // Possible alternative way to do this?
          SwingJMenuCreator ijmc = new SwingJMenuCreator();
          ShadowMenu ijMenu = ijms.getMenu();
          ijmc.createMenus(ijMenu, m);*/
          PluginService ps = ij.get(PluginService.class);
          List<PluginInfo<Command>> plugins = ps.getPluginsOfType(Command.class);
          for(PluginInfo<Command> plugin : plugins) {
               String[] path = plugin.getMenuPath().getMenuString().split(MenuPath.PATH_SEPARATOR);
               String command = "ijps/" + plugin.getClassName();
               if(path != null && !path[0].equals("") && path.length > 1
                    && !path[0].equals("File ")
                    && !path[0].equals("Window ")
                    && !path[0].equals("Help ")
                    && plugin.getAnnotation() != null
                    && plugin.getAnnotation().visible())
                    UIUtil.parseRecursiveMenuAdd(path, command, m, this);
          }
          return m;
     }
     
     /** Build the info pane */
     private void buildInfo() {
          info = new JTextArea();
          info.setEditable(false);
          info.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
          Dimension d = info.getPreferredSize();
          d.width = 275;
          d.height = 100;
          final JScrollPane p = new JScrollPane(info);
          p.setPreferredSize(d);
          add(p);
          p.setTransferHandler(new DropHandler(this));
          info.setTransferHandler(new DropHandler(this));
     }
     
     /** Build the tree pane */
     private void buildTree() {      
                   
          tree = new JTree((DefaultMutableTreeNode)null);
          tree.setCellRenderer(new SlideSetTreeRenderer());
          Dimension d = tree.getPreferredSize();
          d.width = 225;
          d.height = 400;
          final JScrollPane p = new JScrollPane(tree);
          p.setPreferredSize(d);
          add(p);
          tree.addMouseListener(this);
          tree.addTreeWillExpandListener(new TreeWillExpandListener() {
               @Override
               public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                    throw new ExpandVetoException(event);
               }
               @Override
               public void treeWillExpand(TreeExpansionEvent event) { }
          });
          p.setTransferHandler(new DropHandler(this));
     }
     
     /** Build the popup (right-click) menus */
     private void buildPopupMenu() {
          
          final JMenuItem rt = new JMenuItem("Rename Table");
          rt.setActionCommand("rename");
          rt.addActionListener(this);
          final JMenuItem vt = new JMenuItem("View Table");
          vt.setActionCommand("view table");
          vt.addActionListener(this);
          final JMenuItem vr = new JMenuItem("View ROIs");
          vr.setActionCommand("view rois ij1");
          vr.addActionListener(this);
          final JMenu run = buildSlideSetPluginsMenu();
          run.addSeparator();
          final JMenuItem roc = new JMenuItem("Other (experimental)");
          roc.setActionCommand("run command other");
          roc.addActionListener(this);
          run.add(roc);
          run.setText("Run Command");
          final JMenuItem tp = new JMenuItem("Properties");
          tp.setActionCommand("table props");
          tp.addActionListener(this);
          final JMenuItem csv = new JMenuItem("Export as CSV");
          csv.setActionCommand("save csv");
          csv.addActionListener(this);
          final JMenuItem dt = new JMenuItem("Delete Table");
          dt.setActionCommand("delete table");
          dt.addActionListener(this);
          final JMenuItem ulk = new JMenuItem("Unlock");
          ulk.setActionCommand("unlock table");
          ulk.addActionListener(this);
          final JMenu cskelm = new JMenu("Command Skeleton");
          final JMenuItem saveCskel = new JMenuItem("Export");
          saveCskel.setActionCommand("save command skeleton");
          saveCskel.addActionListener(this);
          final JMenuItem runCskel = new JMenuItem("Apply");
          runCskel.setActionCommand("run command skeleton");
          runCskel.addActionListener(this);
          cskelm.add(saveCskel);
          cskelm.add(runCskel);
          
          final JPopupMenu menuP = new JPopupMenu();
          menuP.add(vt);
          menuP.add(vr);
          menuP.addSeparator();
          menuP.add(run);
          menuP.addSeparator();
          menuP.add(cskelm);
          menuP.add(csv);
          menuP.addSeparator();
          menuP.add(ulk);
          menuP.add(rt);
          menuP.add(tp);
          menuP.add(dt);
          popupMenus.put(tree, menuP);
          
          final JMenuItem ls = new JMenuItem("Save Log");
          ls.setActionCommand("save log");
          ls.addActionListener(this);
          final JMenuItem lc = new JMenuItem("Clear Log");
          lc.setActionCommand("clear log");
          lc.addActionListener(this);
          final JPopupMenu menuPL = new JPopupMenu();
          menuPL.add(ls);
          menuPL.add(lc);
          popupMenus.put(info, menuPL);
          info.addMouseListener(this);
          
     }
     
     /** Handle an action event */
     private synchronized void handleActionEvent(final ActionEvent e) {
          (new Thread() {
               @Override
               public void run() {
                    String ac = e.getActionCommand();
                    ij.log().debug("Action command: " + ac);
                    if(ac.equals("open"))
                         { openXML(); return; }
                    if(ac.equals("save"))
                         { try{saveXML(false);} catch(Exception e){} return; }
                    if(ac.equals("save as"))
                         { try{saveXML(true);} catch(Exception e){} return; }
                    if(ac.equals("new"))
                         { newFile(); return; }
                    if(ac.equals("view table"))
                         { viewTable(); return; }
                    if(ac.equals("view rois"))
                         { viewRois(2); return; }
                    if(ac.equals("view rois ij1"))
                         { viewRois(1); return; }
                    if(ac.startsWith("sspl/"))
                         { runSspl(ac.split("/")[1]); return; }
                    if(ac.startsWith("ijps/"))
                         { runSspl(ac.split("/")[1]); return; }
                    if(ac.equals("rename"))
                         { renameTable(); return; }
                    if(ac.equals("save log"))
                         { saveLogText(); return; }
                    if(ac.equals("clear log"))
                         { resetLog(); return; }
                    if(ac.equals("save csv"))
                         { saveCSV(); return; }
                    if(ac.equals("help doc"))
                         { getHelp(null); return; }
                    if(ac.equals("delete table"))
                         { deleteTable(); return; }
                    if(ac.equals("run command other"))
                         { pickAndRunCommand(); return; }
                    if(ac.equals("refresh tree"))
                         { refreshTree(); return; }
                    if(ac.equals("table props"))
                         { viewTableProperties(); return; }
                    if(ac.equals("about"))
                         { getHelp("about"); return; }
                    if(ac.equals("unlock table"))
                         { unlockTable(); return; }
                    if(ac.equals("save command skeleton"))
                         { saveCommandSkeleton(); return; }
                    if(ac.equals("run command skeleton"))
                         { runCommandSkeleton(); return; }
                    if(ac.equals("doc command skeleton"))
                         { getHelp("plugins/index.html#cskel"); return; }
                    //<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
               }
          }).start();
     }
     
     /** Handle a mouse event */
     private synchronized void handleMouseEvent(final MouseEvent e, final String type) {
          (new Thread() {
               @Override
               public void run() {
                    final Point p = e.getPoint();
                    final Component c = e.getComponent();
                    if(e.isPopupTrigger()) {
                         final JPopupMenu m = popupMenus.get(c);
                         if(m == null)
                              return;
                         if(tree.equals(c)) { //Right-clicking on the tree should select a node before showing the popup menu
                              final TreePath target = tree.getPathForLocation(p.x, p.y);
                              if(target == null)
                                   return;
                              tree.setSelectionPath(target);
                         }
                         m.show(c, p.x, p.y);
                    }
                    else if(tree.equals(c)) { //Mouse event on the tree
                         if(type.equals("click") && e.getClickCount() == 2 //Double-click
                                 && e.getButton() == MouseEvent.BUTTON1) {
                              TreePath target = tree.getPathForLocation(p.x, p.y);
                              if(target != null) {
                                   SlideSet targetTable =(SlideSet)((DefaultMutableTreeNode)target.getLastPathComponent()).getUserObject();
                                   ij.log().debug("Double click on table " + targetTable.getName());
                                   tree.setSelectionPath(target);
                                   viewTable();
                              }
                         }
                    }
                    //<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
               }
          }).start();
     }
     
     /**
      * Run a SlideSet plugin command
      * @param className The fully-specified class name of the plugin
      */
     private void runSspl(String className) {
          List<SlideSet> selected = getSelectedSlideSets();
          if(selected.isEmpty() || selected.size() > 1) {
               JOptionPane.showMessageDialog(this,
                    "Must select one table", "Slide Set", JOptionPane.ERROR_MESSAGE);
               return;
          }
          final SlideSet input = selected.get(0);
          try { lockSlideSet(input); }
          catch(OperationCanceledException e) { return; }
          ij.log().debug("Running plugin " + className);
          ij.log().debug("on input table " + input.getName() + "...");
          final SlideSet output;
          try {
              output = sspl.runPlugin(className, input,
                      new PluginInputMatcherFrame(input, ij, dtid),
                      new PluginOutputMatcherFrame(ij, dtid));
          } catch(OperationCanceledException e) {
              log.println("----------------");
              log.println("Command canceled");
              log.println("----------------");
              return;
          } catch(Exception e) {
              log.println("\nFatal error: Unable to complete command");
              log.println("# " + e.toString());
              ij.log().debug(e);
              if(e instanceof NoPluginInputSourceException)
                  JOptionPane.showMessageDialog(this,
                          "Error: This command requires an input which "
                          + "cannot be loaded from the selected table "
                          + "or entered as a constant.",
                          "Slide Set", JOptionPane.ERROR_MESSAGE);
              return;
          } finally {
              refreshTree();
              releaseSlideSet(input);
          }
          ij.log().debug("... run complete.");
          changed = true;
     }
     
     /**
      * Run an ImageJ {@code Command}
      * @param commandInfo {@code CommandInfo} specifying the {@code Command} to run
      */
     private void runSspl(CommandInfo commandInfo) {
         runSspl(commandInfo.getDelegateClassName());
     }
     
     /**
      * Launch a {@link CommandPicker} dialog, and run the
      * selected ImageJ {@code Command}.
      */
     private void pickAndRunCommand() {
         final CommandPicker cp = new CommandPicker(ij.getContext());
         if(!(cp.show() == JOptionPane.OK_OPTION))
             return;
         final CommandInfo ci;
         try {
             ci = cp.getSelectedCommand();
         } catch(Exception e) {
             log.println("\nError: Unable to load command.");
             log.println(e.getMessage());
             ij.log().debug(e);
             return;
         }
         runSspl(ci);
     }
     
     /** Open a new file selected using a dialog  - do not run on event thread */
     private void openXML() {
          final SlideSetLauncher ssl = this;
          final Object rt = tree.getModel().getRoot();
          final SlideSet data = rt == null ? null : (SlideSet)((DefaultMutableTreeNode)rt).getUserObject();
          final String wd = data == null ? null : data.getWorkingDirectory();
          final JFileChooser fc = new JFileChooser(wd == null ? null : new File(wd));
          fc.setDialogType(JFileChooser.OPEN_DIALOG);
          fc.setFileFilter(new FileNameExtensionFilter("Slide Set data file (.xml)", "xml"));
          
          FutureTask<Integer> ftOpen = new FutureTask(new Callable<Integer>() {
              public Integer call() { return fc.showOpenDialog(ssl); }
          });
          try {
            SwingUtilities.invokeAndWait(ftOpen);
            if(ftOpen.get() != JFileChooser.APPROVE_OPTION)
               return;
          } catch (InterruptedException ex) { 
            log.println("\nError: File selection interrupted.");
            log.println("# " + ex.getMessage());
          } catch (InvocationTargetException|ExecutionException ex) {
            log.println("\nError: File selection error.");
            log.println("# " + ex.getCause().getMessage());
          }
          
          File f = fc.getSelectedFile();
          openXML(f);
     }
     
     /** Open a specified file */
     private void openXML(File f) {
          if(!f.canRead())
               return;
          try{
               closeChildWindows();
               checkChanged();
          }
          catch(OperationCanceledException e) { return; }
          SlideSet root;
          try { root = xmls.read(f); }
          catch(Throwable t) {
               JOptionPane.showMessageDialog(
                       this, "Unable to open file.", "Slide Set",
                       JOptionPane.ERROR_MESSAGE);
               log.println("\nFatal error: Unable to open file.");
               log.println("# " + f.getPath());
               log.println("# " + t.getMessage());
               ij.log().debug(t);
               return;
          }
          log.println("\nOpened table \"" + root.getName() + "\" from file: ");
          log.println(f.getPath());
          populateTree(null, root);
          expandAllTreeNodes();
          changed = false;
          openPath = f.getAbsolutePath();
     }
     
     /**
      * Iteratively populate the tree pane
      * 
      * @param node The tree node on which to add. Alternatively, {@code null} to
      *             restart the tree from the root.
      * @param table The {@code SlideSet} to add at this level
      */
     private void populateTree(DefaultMutableTreeNode node, SlideSet table) {
          if(table == null) {
               tree.setModel(null);
               return;
          }
          DefaultMutableTreeNode leaf = new DefaultMutableTreeNode(table);
          if(node == null)
               tree.setModel(new DefaultTreeModel(leaf));
          else {
               node.add(leaf);
               final TreePath path = new TreePath(leaf.getPath());
               if(!tree.isVisible(path))
                    tree.makeVisible(path);
               else
                    ((DefaultTreeModel)tree.getModel()).reload(node);
          }
          node = leaf;
          for(SlideSet child : table.getChildren())
               populateTree(node, child);
     }
     
     /** Refresh the tree in case any changes have been made to the tables */
     private void refreshTree() {
          final SlideSet root = getTreeRoot();
          if(root == null) return;
          populateTree(null, root);
          expandAllTreeNodes();
     }
     
     /** Make sure that all tree nodes are expanded */
     private void expandAllTreeNodes() {
          for(int i = 0; i < tree.getRowCount(); i++)
              tree.expandRow(i);
     }
     
     /** Get the {@link SlideSet} at the root of the {@link #tree tree} */
     private SlideSet getTreeRoot() {
          try {
               return (SlideSet)((DefaultMutableTreeNode)tree.getModel().getRoot()).getUserObject();
          } catch(ClassCastException e) {
               throw new IllegalArgumentException("The table tree has been corupted!" + e);
          } catch(NullPointerException e) {
               return null;
          }
     }
     
     /** Save a file */
     private void saveXML(boolean saveAs) throws OperationCanceledException {
        final SlideSetLauncher ssl = this;  
        SlideSet data = getTreeRoot();
        Callable<File> cSave = new Callable<File>() {
            public File call() throws OperationCanceledException {
                File f;
                if(saveAs || openPath == null) {
                    JFileChooser fc = new JFileChooser();
                    final String wd = data.getWorkingDirectory();
                    fc.setCurrentDirectory(wd == null ? null : new File(wd));
                    fc.setDialogType(JFileChooser.SAVE_DIALOG);
                    fc.setFileFilter(new FileNameExtensionFilter(
                        "Slide Set data file (.xml)", "xml"));
                    fc.setSelectedFile(new File("Data" + ".xml"));
                    if(fc.showSaveDialog(ssl) != JFileChooser.APPROVE_OPTION)
                        throw new OperationCanceledException("Canceled by user");
                    f = fc.getSelectedFile();
                }
                else f = new File(openPath);
                if(f.exists() && JOptionPane.showConfirmDialog(ssl,
                        "File exists, OK to overwrite?", "Slide Set",
                        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
                    throw new OperationCanceledException("Canceled by user");
                return f;
            }
        };
        try {
            File f;
            if(SwingUtilities.isEventDispatchThread())
                f = cSave.call();
            else {
                FutureTask<File> ftSave = new FutureTask(cSave);
                SwingUtilities.invokeAndWait(ftSave);
                f = ftSave.get();
            }
            xmls.write(data, f);
            data.setWorkingDirectory(f.getParent());
            changed = false;
            openPath = f.getAbsolutePath();
            log.println("\nFile saved:");
            log.println("# " + f.getPath());
        } catch (InvocationTargetException|ExecutionException ex) {
            log.println("\nFile not saved:");
            log.println("# " + ex.getCause().getMessage());
            throw new OperationCanceledException();
        } catch (InterruptedException|IOException|XMLStreamException ex) {
            log.println("\nFatal error: Unable to save file.");
            log.println("# " + ex.getMessage());
            throw new OperationCanceledException();
        } catch (Exception ex) {
            throw new OperationCanceledException(ex);
        }
     }
     
     /** Export table data as a CSV file */
     private void saveCSV() {
         final SlideSetLauncher ssl = this; 
         final List<SlideSet> selected = getSelectedSlideSets();
          if(selected.isEmpty() || selected.size() > 1) {
               JOptionPane.showMessageDialog(this,
                    "Must select one table", "Slide Set", JOptionPane.ERROR_MESSAGE);
               return;
          }
          final SlideSet data = selected.get(0);
          final String wd = data.getWorkingDirectory();
          final String name = data.getName();
          final JFileChooser fc = new JFileChooser(wd);
          fc.setDialogType(JFileChooser.SAVE_DIALOG);
          fc.setDialogTitle("Save table data as...");
          fc.setFileFilter(new FileNameExtensionFilter("Comma Separated Value Spreadsheet (.csv)", "csv"));
          fc.setSelectedFile(new File(name + ".csv"));
          
          Callable<File> cSave = new Callable<File>() {
              public File call() throws OperationCanceledException {
                  final int r = fc.showDialog(ssl, "Save");
                  if (r != JFileChooser.APPROVE_OPTION)
                      throw new OperationCanceledException();
                  return fc.getSelectedFile();
              }
          };
          
          try {
              final File csvFile;
              if(SwingUtilities.isEventDispatchThread())
                  csvFile = cSave.call();
              else {
                  FutureTask<File> ftSave = new FutureTask(cSave);
                  SwingUtilities.invokeAndWait(ftSave);
                  csvFile = ftSave.get();
              }
              if(csvFile == null)
                  throw new OperationCanceledException();
              if( csvFile.exists() 
                    && JOptionPane.showConfirmDialog(this, 
                    "File exists. OK to overwrite?", 
                    "Slide Set", JOptionPane.OK_CANCEL_OPTION)
                    != JOptionPane.OK_OPTION )
                  throw new OperationCanceledException();
              csvs.write(data, csvFile, false);
              log.println("\nCSV file Saved:");
              log.println("# " + data.getName());
              log.println("# " + csvFile.getPath());
          } catch(IOException ex) {
              SwingUtilities.invokeLater(new Runnable() {
                  public void run() {
                      JOptionPane.showMessageDialog(ssl, 
                        "Error writing file: " + ex.getMessage(), "Slide Set", 
                        JOptionPane.ERROR_MESSAGE); }
              });
              log.println("\nFatal Error: Unable to save file");
              log.println("# " + ex.getMessage());
          } catch(OperationCanceledException|InvocationTargetException|ExecutionException ex) {
              log.println("\nCSV file not saved:");
              log.println("# Canceled by user");
          } catch(Exception ex) {
              log.println("\nError: File not saved");
              log.println("# " + ex.getMessage());
          }
     }
     
     /** Export command skeleton data */
     private void saveCommandSkeleton() {
          final SlideSetLauncher ssl = this;
          final List<SlideSet> selected = getSelectedSlideSets();
          if(selected.isEmpty() || selected.size() > 1) {
               JOptionPane.showMessageDialog(this,
                    "Must select one table", "Slide Set", JOptionPane.ERROR_MESSAGE);
               return;
          }
          final SlideSet data = selected.get(0);
          final String wd = data.getWorkingDirectory();
          final String name = data.getName();
          final JFileChooser fc = new JFileChooser(wd);
          fc.setDialogType(JFileChooser.SAVE_DIALOG);
          fc.setDialogTitle("Save command skeleton as...");
          fc.setFileFilter(new FileNameExtensionFilter("Command Skeleton File (.cskl)", "cskl"));
          fc.setSelectedFile(new File(name + ".cskl"));
          
          Callable<File> cSave = new Callable<File>() {
              public File call() throws OperationCanceledException {
                  final int r = fc.showDialog(ssl, "Save");
                  if (r != JFileChooser.APPROVE_OPTION)
                      throw new OperationCanceledException();
                  return fc.getSelectedFile();
              }
          };
          
          try {
              final File cskFile;
              if(SwingUtilities.isEventDispatchThread())
                  cskFile = cSave.call();
              else {
                  FutureTask<File> ftSave = new FutureTask(cSave);
                  SwingUtilities.invokeAndWait(ftSave);
                  cskFile = ftSave.get();
              }
              if(cskFile == null)
                  throw new OperationCanceledException();
              if( cskFile.exists() 
                    && JOptionPane.showConfirmDialog(this, 
                    "File exists. OK to overwrite?", 
                    "Slide Set", JOptionPane.OK_CANCEL_OPTION)
                    != JOptionPane.OK_OPTION )
                  throw new OperationCanceledException();
              (new CommandSkeletonService()).write(data, cskFile);
              log.println("\nCommand Skeleton File Saved:");
              log.println("# " + cskFile.getPath());
          } catch(IOException ex) {
              SwingUtilities.invokeLater(new Runnable() {
                  public void run() {
                      JOptionPane.showMessageDialog(ssl, 
                        "Error writing file: " + ex.getMessage(), "Slide Set", 
                        JOptionPane.ERROR_MESSAGE); }
              });
              log.println("\nFatal Error: Unable to save file");
              log.println("# " + ex.getMessage());
          } catch(OperationCanceledException|InvocationTargetException|ExecutionException ex) {
              log.println("\nFile not saved:");
              log.println("# Canceled by user");
          } catch(Exception ex) {
              log.println("\nError: File not saved");
              log.println("# " + ex.getMessage());
          }
     }
     
     /** Run a command skeleton */
     private void runCommandSkeleton() {
          final List<SlideSet> selected = getSelectedSlideSets();
          if(selected.isEmpty() || selected.size() > 1) {
               JOptionPane.showMessageDialog(this,
                    "Must select one table", "Slide Set", JOptionPane.ERROR_MESSAGE);
               return;
          }
          final SlideSet data = selected.get(0);
          final String wd = data.getWorkingDirectory();
          JFileChooser fc = new JFileChooser(wd);
          fc.setDialogType(JFileChooser.OPEN_DIALOG);
          fc.setDialogTitle("Run command skeleton...");
          fc.setFileFilter(new FileNameExtensionFilter("Command Skeleton File (.cskl)", "cskl"));
          if(fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
               return;
          final File f = fc.getSelectedFile();
          final CommandSkeletonService css = new CommandSkeletonService();
          final ArrayList<CommandTemplate> cts = new ArrayList<CommandTemplate>();
          final ArrayList<String> rCols = new ArrayList<String>();
          try {
              css.read(f, cts, rCols);
          } catch(Exception e) {
              JOptionPane.showMessageDialog(this, 
                    "### Error reading file:\n" + e.getMessage(), "Slide Set", 
                    JOptionPane.ERROR_MESSAGE);
              return;
          }
          try { lockSlideSet(data); }
          catch(OperationCanceledException e) { return; }
          try {
              try {
                  css.runSkeleton(cts, rCols, data, sspl, false);
              } catch(ColumnTypeException e) {
                  final int resp = JOptionPane.showConfirmDialog(this, 
                          "Warning: Table column types do not match\n"
                                  + "column types expected by this command skeleton.\n"
                                  + "This may result in unpredictable behavior.\n"
                                  + "Continue anyway?",
                          "Slide Set", 
                          JOptionPane.YES_NO_OPTION,
                          JOptionPane.WARNING_MESSAGE);
                  if(resp == JOptionPane.YES_OPTION)
                      css.runSkeleton(cts, rCols, data, sspl, true);
              }
          } catch(Exception e) {
              log.println("\nFatal error: Unable to complete command");
              log.println("# " + e.toString());
              ij.log().debug(e);
          } finally {
              refreshTree();
              releaseSlideSet(data);
          }
          changed = true;
     }
     
     /** Start a new file */
     private void newFile() {
         SlideSet old = getTreeRoot(); 
         try {
               closeChildWindows();
               checkChanged();
         } catch(OperationCanceledException e) { return; }
         try {
               populateTree(null, new SlideSet(ij, dtid));
               openPath = null;
               saveXML(true);
          }
          catch(OperationCanceledException e) {
               populateTree(null, old);
               expandAllTreeNodes();
               return;
          }
          log.println("\nNew data file created:");
          log.println(openPath);
     }
     
     /** 
      * Check if the data set has been changed
      * and offer to save it if applicable.
      */
     private void checkChanged() 
     throws OperationCanceledException {
          if(changed) {
               final int resp = JOptionPane.showConfirmDialog(this,
                    "Save changes to this data set?", "Slide Set",
                    JOptionPane.YES_NO_CANCEL_OPTION);
               if(resp == JOptionPane.CANCEL_OPTION)
                    throw new OperationCanceledException("Canceled by user");
               if(resp == JOptionPane.YES_OPTION)
                    saveXML(false);
               }
          }
     
     /** Get a list of the {@code SlideSet}s selected in the tree */
     private List<SlideSet> getSelectedSlideSets() {
          List<SlideSet> result = new ArrayList<SlideSet>(3);
          int nsel = tree.getSelectionCount();
          if(nsel < 1)
               return result;
          TreePath[] paths = tree.getSelectionPaths();
          for(int i=0; i<paths.length; i++) {
               SlideSet data = (SlideSet)
                    ((DefaultMutableTreeNode)paths[i].getLastPathComponent()).getUserObject();
               result.add(data);
          }
          return result;
     }
     
     /** View a {@code SlideSet} */
     private void viewTable() {
          List<SlideSet> list = getSelectedSlideSets();
          if(list.isEmpty()) {
               JOptionPane.showMessageDialog(this,
                    "No data table selected", "Slide Set", JOptionPane.ERROR_MESSAGE);
               return;
          }
          for(SlideSet data : list) {
               try { lockSlideSet(data); }
               catch(OperationCanceledException e) { return; }
               SlideSetViewer v = new SlideSetViewer(data, ij, dtid, log, data.isLocked(), this);
               registerChildWindow(v);
               final SlideSet dFin = data;
               v.addWindowListener(new WindowAdapter() {
                   @Override
                    public void windowClosed(WindowEvent e) {
                       releaseSlideSet(dFin);
                   }
               });
               v.setVisible(true);
          }    
          changed = true;
     }
     
     /** View table properties */
     private void viewTableProperties() {
         List<SlideSet> selected = getSelectedSlideSets();
          if(selected.isEmpty() || selected.size() > 1) {
               JOptionPane.showMessageDialog(this,
                    "Must select one table", "Slide Set", JOptionPane.ERROR_MESSAGE);
               return;
          }
          final SlideSet data = selected.get(0);
          new SlideSetPropertiesViewer(this, data).setVisible(true);
     }
     
     /** Launch the {@code RoiEditor} */
     private void viewRois(final int ijVersion) {
          List<SlideSet> selected = getSelectedSlideSets();
          if(selected.isEmpty() || selected.size() > 1) {
               JOptionPane.showMessageDialog(this,
                    "Must select one table", "Slide Set", JOptionPane.ERROR_MESSAGE);
               return;
          }
          final SlideSet data = selected.get(0);
          try { lockSlideSet(data); }
          catch(OperationCanceledException e) { return; }
          switch(ijVersion) {
              case 1:
                  RoiEditorIJ1 r1 = new RoiEditorIJ1(data, dtid, ij, log);
                  if(data.isLocked())
                     r1.lock();
                  registerChildWindow(r1);
                  r1.showAndWait();
                  break;
              case 2:
                  RoiEditor re = new RoiEditor(data, dtid, ij, log);
                  if(data.isLocked())
                     re.lock();
                  registerChildWindow(re);
                  re.showAndWait();
          }
          releaseSlideSet(data);
     }
     
     /** Rename a table */
     private void renameTable() {
          List<SlideSet> selected = getSelectedSlideSets();
          if(selected.isEmpty() || selected.size() > 1) {
               JOptionPane.showMessageDialog(this,
                    "Must select one table", "Slide Set", JOptionPane.ERROR_MESSAGE);
               return;
          }
          final SlideSet data = selected.get(0);
          final String newName = JOptionPane.showInputDialog(this, 
               "New name for table \"" + data.getName() + "\"", 
               "Slide Set", JOptionPane.PLAIN_MESSAGE);
          if(newName != null && !newName.trim().isEmpty()) {
               data.setName(newName);
               try { 
                    SwingUtilities.invokeAndWait( 
                         new Runnable() { public void run() { refreshTree(); } }); 
               } catch(Throwable t) { throw new IllegalArgumentException(t); }
          }
     }
     
     /** Delete a table */
     private void deleteTable() {
         List<SlideSet> selected = getSelectedSlideSets();
         if(selected.isEmpty() || selected.size() > 1) {
             JOptionPane.showMessageDialog(this,
                 "Must select one table", "Slide Set", JOptionPane.ERROR_MESSAGE);
             return;
         }
         final SlideSet data = selected.get(0);
         if(!data.getChildren().isEmpty()) {
             JOptionPane.showMessageDialog(this,
                 "Cannot delete data table linked to results", "Slide Set", JOptionPane.ERROR_MESSAGE);
             return;
         }
         if(data.getParent() == null) {
             JOptionPane.showMessageDialog(this,
                 "Cannot delete top-level data table", "Slide Set", JOptionPane.ERROR_MESSAGE);
             return;
         }
         if( JOptionPane.showConfirmDialog(this, 
              "Permanantly delete the selected table?", 
              "Slide Set", JOptionPane.YES_NO_OPTION)
              != JOptionPane.YES_OPTION )
             return;
         try {
             data.getParent().removeChild(data);
             populateTree(null, getTreeRoot());
             expandAllTreeNodes();
             log.println("\nDeleted table: " + data.getName());
         } catch(SlideSetException e) {
             JOptionPane.showMessageDialog(this,
                 "Unable to delete selected table", "Slide Set", JOptionPane.ERROR_MESSAGE);
             log.println(e.toString());
         }
         
     }
     
     /** Clear a read-only flag on a {@code DataSet} table */
     private void unlockTable() {
         List<SlideSet> selected = getSelectedSlideSets();
         if(selected.isEmpty() || selected.size() > 1)
             return;
         final SlideSet data = selected.get(0);
         data.setLock(false);
         refreshTree();
     }
     
     /** Keep track of a child window */
     private synchronized void registerChildWindow(SlideSetWindow w) {
          childWindows.add(w);
          w.addWindowListener( new WindowAdapter(){
               @Override
               public void windowClosed(WindowEvent e) {
                    final SlideSetWindow ssw = 
                            (SlideSetWindow) e.getWindow();
                    childWindows.remove(ssw);
               } 
          });
     }
     
     /**
      * Close any open child windows.  Checks with the
      * user to make sure this is ok.
      * @throws OperationCanceledException 
      */
     private void closeChildWindows()
             throws OperationCanceledException {
          if(!childWindows.isEmpty()) {
               final int res = JOptionPane.showConfirmDialog(this, "There are currently open "
                       + "windows associated with this data set. \n"
                       + "Close everything and risk losing unsaved data?", 
                       "Slide Set", JOptionPane.YES_NO_OPTION);
               if(res != JOptionPane.YES_OPTION)
                    throw new OperationCanceledException("Canceled by user");
               for(SlideSetWindow w : childWindows)
                    w.kill();
          }
     }
     
     /**
      * Check-out a {@link SlideSet} for use.  Important to prevent
      * concurrency errors.
      * 
      * @param table The {@code SlideSet} to lock.
      * @throws OperationCanceledException The {@code SlideSet} is
      *    already in use. Notifies the user with a dialog box, but
      *    it is the caller's responsibility not to proceed with
      *    using the {@code SlideSet}.
      */
     private void lockSlideSet(SlideSet table)
             throws OperationCanceledException {
          if(lockedTables.containsKey(table)) {
               final String owner = lockedTables.get(table);
               final String em = table.getName() 
                       + " is currently in use by " + owner;
               ij.log().debug(em);
               JOptionPane.showMessageDialog(this, 
                       table.getName() + " is currently in use", 
                       "Slide Set", JOptionPane.ERROR_MESSAGE);
               throw new OperationCanceledException(em);
          }
          final Thread curThread = Thread.currentThread();
          lockedTables.put(table, curThread.toString());
     }
     
     /**
      * Release a previously locked {@link SlideSet} for
      * further use.
      * 
      * @param table The {@code SlideSet} to unlock.
      */
     private void releaseSlideSet(SlideSet table) {
          lockedTables.remove(table);
     }
     
     /** Write the log window text to a file */
     private void saveLogText() {
          final SlideSet root = getTreeRoot();
          final String wd = root == null ? "" : root.getWorkingDirectory();
          final JFileChooser fc = new JFileChooser(wd);
          fc.setDialogType(JFileChooser.SAVE_DIALOG);
          fc.setDialogTitle("Save log file as...");
          fc.setFileFilter(new FileNameExtensionFilter("Text file (.txt)", "txt"));
          fc.setSelectedFile(new File("log.txt"));
          final int r = fc.showDialog(this, "Save");
          if(r != JFileChooser.APPROVE_OPTION)
               return;
          final File logFile = fc.getSelectedFile();
          if(logFile == null)
               return;
          if( logFile.exists() 
                  && JOptionPane.showConfirmDialog(this, 
                  "File exists. Log will be added to end of file.", 
                  "Slide Set", JOptionPane.OK_CANCEL_OPTION)
                  != JOptionPane.OK_OPTION )
               return;
          final FileWriter fw;
          try {
               logFile.createNewFile();
               fw = new FileWriter(logFile, true);
               fw.append(info.getText().replace("\n", String.format("%n")));
               fw.close();
          } catch(IOException e) {
               ij.log().debug(e.getMessage());
               ij.log().debug(e);
               JOptionPane.showMessageDialog(this, 
                    "Error writing log file", "Slide Set", 
                    JOptionPane.ERROR_MESSAGE);
          }
     }
     
     /** Get user confirmation to clear the log */
     private void resetLog() {
          if( JOptionPane.showConfirmDialog(this, 
               "Clear the log?", "Slide Set", 
               JOptionPane.OK_CANCEL_OPTION, 
               JOptionPane.PLAIN_MESSAGE) 
               != JOptionPane.OK_OPTION )
             return;
          info.setText("");
          printLogHead();
     }
     
     /** Print a header message to the {@link #info info} window */
     private void printLogHead() {
          checkUpdateSite();
          logMessage("################################\n"
                   + "#                              #\n"
                   + "#          Slide Set           #\n"
                   + "#                              #\n"
                   + "# Batch processing with ImageJ #\n"
                   + "#                              #\n");
          for(int i=0; i<(26-ver.length()); i++)
               logMessage("#");
          logMessage("ver. " + ver + "#\n");
          logMessage(DateFormat.getDateTimeInstance().format(new Date()) + "\n");
     }
     
     /**
      * Load documentation in a web browser.
      * 
      * @param pageKey Page to load. See {@link HelpLoader#getHelp(java.lang.String)}.
      */
     private void getHelp(String pageKey) {
         try {
             helpLoader.getHelp(pageKey);
         } catch(SlideSetException e) {
             JOptionPane.showMessageDialog(this, "Unable to open documentation pages:  " + e.toString(), 
                  "Slide Set", JOptionPane.ERROR_MESSAGE);
             ij.log().debug(e);
         }
     }
     
     private void checkUpdateSite() {
         
         final FilesCollection fc = new FilesCollection(ij.app().getApp().getBaseDirectory());
         try { fc.read(); }
         catch(Exception e) { logMessage("!! Error parsing update site dir"); }
         for(UpdateSite us : fc.getUpdateSites(false)) {
             if(us.getURL().contains("cellbio.emory.edu/bnanes/slideset")) {
                 logMessage("################################\n"
                         +  "#                              #\n"
                         +  "#     Slide Set has moved!     #\n"
                         +  "#                              #\n"
                         +  "#   Please change the update   #\n"
                         +  "# site in order to receive the #\n"
                         +  "#   latest Slide Set version.  #\n"
                         +  "#       For details, see:      #\n"
                         +  "# http://b.nanes.org/slideset/ #\n"
                         +  "#                              #\n");
                 return;
             }
         }
   /*    logMessage("################################\n"
                 +  "#                              #\n"
                 +  "#      Install the update      #\n"
                 +  "# site in order to receive the #\n"
                 +  "#   latest Slide Set version.  #\n"
                 +  "#       For details, see:      #\n"
                 +  "# http://b.nanes.org/slideset/ #\n"
                 +  "#                              #\n");*/
     }
     
     // -- Classes --
     
     /** Drop handler for the main view pains */
     private class DropHandler extends TransferHandler {
        
        private final SlideSetLauncher launcher;
        
        public DropHandler(SlideSetLauncher launcher) {
            this.launcher = launcher;
        }

        @Override
        public boolean canImport(TransferSupport info) {
            if(!info.isDrop())
                return false;
            else if(!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
                return false;
            return true;
        }

        @Override
        public boolean importData(TransferSupport info) {
            if(!canImport(info))
                return false;
            final Object target = info.getComponent();
            List<File> files;
            try {
                 files =
                      (List<File>)info.getTransferable().
                      getTransferData(DataFlavor.javaFileListFlavor);
            } catch(Throwable t) { throw new IllegalArgumentException("DND error: " + t); }
            if(files.size() != 1)
                return false;
            launcher.openXML(files.get(0));
            return true;
        }
                 
     }
     
     // -- Test methods --
     
}
