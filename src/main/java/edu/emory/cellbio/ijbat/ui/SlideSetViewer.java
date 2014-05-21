package edu.emory.cellbio.ijbat.ui;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.dm.DataElement;
import edu.emory.cellbio.ijbat.dm.DataTypeIDService;
import edu.emory.cellbio.ijbat.dm.FileLink;
import edu.emory.cellbio.ijbat.dm.MIME;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import edu.emory.cellbio.ijbat.io.Util;

import net.imagej.ImageJ;

import java.awt.Dimension;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.event.TableModelEvent;
import javax.swing.WindowConstants;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.JOptionPane;
import javax.swing.TransferHandler;
import javax.swing.table.JTableHeader;

/**
 * View and edit a {@code SlideSet}
 * 
 * @author Benjamin Nanes
 */
public class SlideSetViewer extends JFrame 
   implements ActionListener, MouseListener, SlideSetWindow {
     
     // -- Fields --
     
     private final SlideSet data;
     private final ImageJ ij;
     private final DataTypeIDService dtid;
     private final SlideSetLog log;
     
     private JScrollPane pane;
     private JTable table;
     private JMenuBar menuBar;
     /** Index specifying popup menus associated with various components */
     private final HashMap<Component, JPopupMenu> popupMenuHash 
          = new HashMap<Component, JPopupMenu>();
     /** Table popup menu */
     private JPopupMenu menuP;
     /** Column header popup menu */
     private JPopupMenu menuColP;
     /** Location where the last popup menu was triggered */
     private Point lastPopupPoint = null;
     
     private static final int COLWIDTH = 175;
     
     // -- Constructors --
     
     public SlideSetViewer(SlideSet data, ImageJ context, DataTypeIDService dtid, SlideSetLog log) {
          this(data, context, dtid, log, null);
     }
     
     public SlideSetViewer(SlideSet data, ImageJ context,
          DataTypeIDService dtid, SlideSetLog log, Component parent) {
          this.data = data;
          this.ij = context;
          this.dtid = dtid;
          this.log = log;
          buildLayout(parent);
     }
     
     // -- Methods --
     
     @Override
     public void kill() {
               setVisible(false);
               dispose();
          synchronized (this) {
               notifyAll();
          }
     }

     /** ActionListener implementation */
     @Override
     public void actionPerformed(ActionEvent e) {
          handleActionEvent(e);
     }

     /** MouseListener implementation */
     @Override
     public void mouseClicked(MouseEvent e) {
          handleMouseEvent(e);
     }

     @Override
     public void mouseEntered(MouseEvent e) {
          handleMouseEvent(e);
     }

     @Override
     public void mouseExited(MouseEvent e) {
          handleMouseEvent(e);
     }

     @Override
     public void mousePressed(MouseEvent e) {
          handleMouseEvent(e);
     }

     @Override
     public void mouseReleased(MouseEvent e) {
          handleMouseEvent(e);
     }
     
     // -- Helper methods --
     
     /** Build the window */
     private void buildLayout(Component parent) {
          buildTableLayout();
          buildMenuP();
          buildMenuColP();
          buildMenuBar();
          
          setTitle(data.getName());
          setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
          addWindowListener( new WindowAdapter() {
               @Override
               public void windowClosing(WindowEvent e) { kill(); }
          });
          setLocationRelativeTo(null);
          pack();
     }
     
     /** Set up the table itself */
     private void buildTableLayout() {
          table = new JTable(new SlideSetTableModel(data));
          table.setCellSelectionEnabled(true);
          table.setPreferredScrollableViewportSize(
               new Dimension(table.getColumnCount() * COLWIDTH, 15 * table.getRowHeight()));
          table.getTableHeader().setTransferHandler(new DropHandler());
          table.setTransferHandler(new DropHandler());
          pane = new JScrollPane(table);
          add(pane);
     }
     
     /** Set up the table pop-up menu */
     private void buildMenuP() {
          menuP = new JPopupMenu();
          JMenuItem aRow = new JMenuItem("Add Row");
          aRow.setActionCommand("+row");
          aRow.addActionListener(this);
          JMenu aCol = buildElementTypeMenuTree("+col");
          aCol.setText("Add Column");
          
          JMenuItem setMult = new JMenuItem("Set Selected Values");
          setMult.setActionCommand("set");
          setMult.addActionListener(this);
          
          JMenuItem setSeq = new JMenuItem("Set from Sequence");
          setSeq.setActionCommand("set-seq");
          setSeq.addActionListener(this);
          
          JMenuItem rName = new JMenuItem("Rename Column");
          rName.setActionCommand("rename/sel");
          rName.addActionListener(this);
          JMenu conv = buildElementTypeMenuTree("conv|sel");
          conv.setText("Convert Column");
          
          JMenuItem dRow = new JMenuItem("Delete Selected Rows");
          dRow.setActionCommand("-row");
          dRow.addActionListener(this);
          JMenuItem dCol = new JMenuItem("Delete Selected Columns");
          dCol.setActionCommand("-col/sel");
          dCol.addActionListener(this);
          
          menuP.add(aRow);
          menuP.add(aCol);
          menuP.addSeparator();
          menuP.add(setMult);
          menuP.add(setSeq);
          menuP.addSeparator();
          menuP.add(rName);
          menuP.add(conv);
          menuP.addSeparator();
          menuP.add(dRow);
          menuP.add(dCol);
          pane.addMouseListener(this);
          popupMenuHash.put(pane, menuP);
          table.addMouseListener(this);
          popupMenuHash.put(table, menuP);
     }
     
     /** Set up the column header popup menu */
     private void buildMenuColP() {
          
          menuColP = new JPopupMenu();
          final JTableHeader th = table.getTableHeader();
          
          final JMenuItem rName = new JMenuItem("Rename");
          rName.setActionCommand("rename/head");
          rName.addActionListener(this);
          final JMenu conv = buildElementTypeMenuTree("conv|head");
          conv.setText("Convert");
          final JMenuItem dCol = new JMenuItem("Delete");
          dCol.setActionCommand("-col/head");
          dCol.addActionListener(this);
          
          final JMenuItem aRow = new JMenuItem("Add Row");
          aRow.setActionCommand("+row");
          aRow.addActionListener(this);
          final JMenu aCol = buildElementTypeMenuTree("+col");
          aCol.setText("Add Column");
          
          menuColP.add(rName);
          menuColP.add(conv);
          menuColP.add(dCol);
          menuColP.addSeparator();
          menuColP.add(aRow);
          menuColP.add(aCol);
          th.addMouseListener(this);
          popupMenuHash.put(th, menuColP);
          
     }
     
     /** Setup the main menu */
     private void buildMenuBar() {
          menuBar = new JMenuBar();
          final JMenu r = new JMenu("Row");
          final JMenu c = new JMenu("Column");
          final JMenu t = new JMenu("Table");
          
          JMenuItem aRow = new JMenuItem("Add");
          aRow.setActionCommand("+row");
          aRow.addActionListener(this);
          JMenu aCol = buildElementTypeMenuTree("+col");
          aCol.setText("Add");

          JMenuItem rName = new JMenuItem("Rename");
          rName.setActionCommand("rename/sel");
          rName.addActionListener(this);
          JMenu conv = buildElementTypeMenuTree("conv|sel");
          conv.setText("Convert");
          
          JMenuItem dRow = new JMenuItem("Delete Selected");
          dRow.setActionCommand("-row");
          dRow.addActionListener(this);
          JMenuItem dCol = new JMenuItem("Delete Selected");
          dCol.setActionCommand("-col/sel");
          dCol.addActionListener(this);
          
          JMenuItem tRename = new JMenuItem("Rename");
          tRename.setActionCommand("table/rename");
          tRename.addActionListener(this);
          JMenuItem tProps = new JMenuItem("Properties");
          tProps.setActionCommand("table/props");
          tProps.addActionListener(this);
          
          r.add(aRow);
          r.add(dRow);
          c.add(aCol);
          c.add(dCol);
          c.addSeparator();
          c.add(rName);
          c.add(conv);
          t.add(tRename);
          t.add(tProps);
          menuBar.add(r);
          menuBar.add(c);
          menuBar.add(t);
          setJMenuBar(menuBar);
     }
     
     /**
      * Build a {@code JMenu} filled with available type codes
      * 
      * @param commandPrefix A prefix to add to the {@code ActionCommand}
      *            before the element type and mime type.  For example, if
      *            this is set to "{@code +col}", the selected element
      *            type is "{@code class}", and the selected mime type
      *            is "{@code text/plain}", the resulting
      *            {@code ActionCommand} will be
      *            "{@code +col|class|text/plain}".
      */
     private JMenu buildElementTypeMenuTree(String commandPrefix) {
          JMenu m = new JMenu();
          ArrayList<Class<? extends DataElement>> types
                  = dtid.getElementTypes(false);
          for(Class<? extends DataElement> type : types) {
              if(FileLink.class.isAssignableFrom(type)) { // Need mime type...
                  final String stem
                          = commandPrefix + "|" + type.getName() + "|";
                  for(String mime : dtid.getMimeTypes()) {
                      final String[] labels = {
                          dtid.getReadableElementType(type, "").trim(),
                          dtid.getMimeReadableName(mime) };
                      final String command = stem + mime;
                      UIUtil.parseRecursiveMenuAdd(labels, command, m, this);
                  }// ...and a 'custom' mime for good measure. 
                  final String[] labels = { 
                        dtid.getReadableElementType(type, "").trim(), "Other" };
                  final String command = stem + "$";
                  UIUtil.parseRecursiveMenuAdd(labels, command, m, this);
              } else { // No mime type
                  final String[] labels
                          = { dtid.getReadableElementType(type, "") };
                  final String command
                          = commandPrefix + "|" + type.getName();
                  UIUtil.parseRecursiveMenuAdd(labels, command, m, this);
              }
          }
          return m;
     }
     
     /**
      * @deprecated Use {@link edu.emory.cellbio.ijbat.ui.UIUtil#parseRecursiveMenuAdd} instead</p>
      * 
      * <p>Add a menu item to a menu tree, along with sub-menu nodes if needed
      * 
      * @param labels An array of menu labels where the last String
      *               the menu item and all other Strings represent
      *               the sub-menu tree in which the item should be placed
      * @param command The command string to send to the menu listener
      *                when this item is selected
      * @param m The top menu tree to start with
      */
     private void parseRecursiveMenuAdd(String[] labels, String command, JMenu m) {
          if(labels.length == 1) {
               JMenuItem i = new JMenuItem(labels[0]);
               i.setActionCommand(command);
               i.addActionListener(this);
               m.add(i);
               return;
          }
          Component[] items = m.getMenuComponents();
          if(items != null && items.length > 0) {
               for(int u=0; u<items.length; u++) {
                    if(items[u].getName() != null &&
                         items[u].getName().equals(labels[0]) &&
                         JMenu.class.isInstance(items[u])) {
                         String[] remainingLabels =
                              Arrays.copyOfRange(labels, 1, labels.length);
                         parseRecursiveMenuAdd(remainingLabels, command, (JMenu)items[u]);
                         return;
                    }
               }
          }
          JMenu sm = new JMenu(labels[0]);
          sm.setName(labels[0]);
          m.add(sm);
          String[] remainingLabels = Arrays.copyOfRange(labels, 1, labels.length);
          parseRecursiveMenuAdd(remainingLabels, command, sm);
     }
     
     /** Handle an action event */
     private void handleActionEvent(ActionEvent e) {
          String ac = e.getActionCommand();
          System.out.println("Action command: " + ac);
          
          // Add a row
          if(ac.equals("+row")) {
               try {
                   data.addRow();
               } catch(SlideSetException ex) {
                   JOptionPane.showMessageDialog(this,
                           "Error adding row. See log for details.",
                           "Slide Set", JOptionPane.ERROR_MESSAGE);
                   handleError(ex);
               }
               table.tableChanged(new TableModelEvent(table.getModel()));
          }
          
          // Add a column
          else if(ac.startsWith("+col|")) {
               final String[] acs = ac.split("\\|");
               if(acs.length < 2)
                   throw new IllegalArgumentException(
                           "Bad action command: " + ac);
               String name = JOptionPane.showInputDialog(this, "Column name:", "");
               if(name == null)
                    return;
               try {
                    final int i = data.addColumn(name, acs[1]);
                    if(acs.length == 3) {
                        if(acs[2].equals("$"))
                            acs[2] = JOptionPane
                                    .showInputDialog(this,
                                    "Enter MIME type:", MIME.TXT);
                        data.setColumnMimeType(i, acs[2]);
                    }
                    Dimension d = getSize();
                    if(d.width / table.getColumnCount() < COLWIDTH) {
                        d.width = table.getColumnCount() * COLWIDTH;
                        if(d.width < GraphicsEnvironment.getLocalGraphicsEnvironment()
                             .getMaximumWindowBounds().width)
                            setSize(d);
                    }
               } catch(SlideSetException ex) {
                    handleError(ex);
               } finally {
                    table.tableChanged(
                          new TableModelEvent(table.getModel(),
                          TableModelEvent.ALL_COLUMNS));
               }
          }
          
          // Delete a row or two
          else if(ac.equals("-row")) {
               int[] rows = table.getSelectedRows();
               if(rows == null || rows.length < 1) {
                    JOptionPane.showMessageDialog(
                         this, "No rows selected", "Slide Set", JOptionPane.INFORMATION_MESSAGE);
                    return;
               }
               else if(
                    JOptionPane.showConfirmDialog(this,
                    "Delete " + String.valueOf(rows.length) + " selected row(s)?",
                    "Slide Set", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    Arrays.sort(rows);
                    for(int i= rows.length-1; i>=0; i--)
                         data.removeRow(rows[i]);
                    table.tableChanged(
                         new TableModelEvent(table.getModel(), TableModelEvent.ALL_COLUMNS));
               }
          }
          
          // Delete a column or two
          else if(ac.startsWith("-col/")) {
               final String[] pac = ac.split("/");
               final int[] cols;
               if(pac[1].equals("sel"))
                    cols = table.getSelectedColumns();
               else if(pac[1].equals("head"))
                    cols = getColumnFromHeaderEvent(e);
               else
                    return;
               if(cols == null || cols.length < 1) {
                    JOptionPane.showMessageDialog(
                         this, "No columns selected", "Slide Set", JOptionPane.INFORMATION_MESSAGE);
                    return;
               }
               else {
                    final String s = cols.length == 1 
                         ? data.getColumnName(cols[0]) 
                         : String.valueOf(cols.length) + " selected column(s)";
                    if( JOptionPane.showConfirmDialog(this,
                            "Delete " + s + "?", "Slide Set", JOptionPane.YES_NO_OPTION) 
                            == JOptionPane.YES_OPTION) {
                         int widthToRemove = cols.length *
                              table.getWidth() / table.getColumnCount();
                         Arrays.sort(cols);
                         for(int i= cols.length-1; i>=0; i--)
                              data.removeColumn(cols[i]);
                         Dimension d = getSize();
                         int newWidth = d.width - widthToRemove;
                         if(newWidth >= COLWIDTH + 25)
                             d.width = newWidth;
                         else
                             d.width = COLWIDTH + 25;
                         setSize(d);
                         table.tableChanged(
                              new TableModelEvent(table.getModel(), TableModelEvent.ALL_COLUMNS));
                    }
               }
          }
          
          // Set values of selected cells
          else if(ac.equals("set")) {
               int col = table.getSelectedColumn();
               int[] rows = table.getSelectedRows();
               if(table.getSelectedColumnCount() > 1) {
                    JOptionPane.showMessageDialog(
                         this, "Cannot set values from multiple columns",
                         "Slide Set", JOptionPane.INFORMATION_MESSAGE);
                    return;
               }
               if(table.getSelectedColumnCount() == 0 || table.getSelectedRowCount() == 0) {
                    JOptionPane.showMessageDialog(
                         this, "No columns selected",
                         "Slide Set", JOptionPane.INFORMATION_MESSAGE);
                    return;
               }
               String val = JOptionPane.showInputDialog(this, "New value:", "");
               if(val == null)
                    return;
               try {
                    for(int i : rows)
                         data.getDataElement(col, i).setUnderlyingText(val);
               } catch(SlideSetException ex) {
                    handleError(ex);
               } finally {
               table.tableChanged(
                         new TableModelEvent(table.getModel(), TableModelEvent.ALL_COLUMNS));
               }
          }
          
          // Set values of selected cells from a sequence
          else if(ac.equals("set-seq"))
               setFromSequence();
          
          // Rename a column
          else if(ac.startsWith("rename/")) {
               final String[] pac = ac.split("/");
               final int[] cols;
               if(pac[1].equals("sel"))
                    cols = table.getSelectedColumns();
               else if(pac[1].equals("head"))
                    cols = getColumnFromHeaderEvent(e);
               else
                    return;
               if(cols == null || cols.length < 1 || cols[0] < 0) {
                    JOptionPane.showMessageDialog(
                         this, "No column selected",
                         "Slide Set", JOptionPane.INFORMATION_MESSAGE);
                    return;
               }
               if(cols.length > 1) {
                    JOptionPane.showMessageDialog(
                         this, "Cannot rename multiple columns",
                         "Slide Set", JOptionPane.INFORMATION_MESSAGE);
                    return;
               }
               String val = JOptionPane.showInputDialog(this, "New column name:", "");
               if(val == null)
                    return;
               data.setColumnName(cols[0], val);
               table.tableChanged(
                         new TableModelEvent(table.getModel(), TableModelEvent.HEADER_ROW));
          }
          
          // Cast a column (or at least try)
          else if(ac.startsWith("conv|")) {
               final String[] acs = ac.split("\\|");
               if(acs.length < 3)
                    throw new IllegalArgumentException(
                            "Bad action command: " + ac);
               final int[] cols;
               if(acs[1].equals("sel"))
                    cols = table.getSelectedColumns();
               else if(acs[1].equals("head"))
                    cols = getColumnFromHeaderEvent(e);
               else
                    return;
               final String typeS = acs[2];
               String mime = acs.length > 3 ? acs[3] : null;
               if(mime != null && mime.equals("$"))
                    mime = JOptionPane.showInputDialog(
                            this, "Enter MIME type:", MIME.TXT);
               if(cols == null || cols.length < 1 || cols[0] < 0) {
                    JOptionPane.showMessageDialog(
                         this, "No column selected",
                         "Slide Set", JOptionPane.INFORMATION_MESSAGE);
                    return;
               }
               if(cols.length > 1) {
                    JOptionPane.showMessageDialog(
                         this, "Cannot convert multiple columns",
                         "Slide Set", JOptionPane.INFORMATION_MESSAGE);
                    return;
               }
               try {
                   data.convertColumn(
                       cols[0],
                       (Class<? extends DataElement<?>>) Class.forName(typeS),
                       mime);
               }
               catch(Exception ex) {
                    JOptionPane.showMessageDialog(
                         this, "Could not convert column to the desired type",
                         "Slide Set", JOptionPane.ERROR_MESSAGE);
                    handleError(ex);
               }
               table.tableChanged(
                    new TableModelEvent(table.getModel(), TableModelEvent.UPDATE));
               table.tableChanged(
                    new TableModelEvent(table.getModel(), TableModelEvent.HEADER_ROW));
          }
          
          // Rename this table
          else if(ac.equals("table/rename"))
              renameTable();
          
          // View this table's properties
          else if(ac.equals("table/props"))
              viewTableProperties();
     }
     
     /** Find out which column header triggered a popup menu */
     private int[] getColumnFromHeaderEvent(ActionEvent e) {
          final Object source = e.getSource();
          if(!(source instanceof JMenuItem) )
               throw new IllegalArgumentException(
                    "Event was not generated by an item from the column header popup menu");
          final int[] r = new int[1];
          r[0] = table.columnAtPoint(lastPopupPoint);
          return r;
     }
     
     /** Handle a mouse event */
     private void handleMouseEvent(MouseEvent e) {
          if(e.isPopupTrigger()) {
               final Component c = e.getComponent();
               final JPopupMenu m = popupMenuHash.get(c);
               if(m != null) {
                    lastPopupPoint = e.getPoint();
                    m.show(e.getComponent(), e.getX(), e.getY());
               }
          }
     }
     
     /** Rename this table */
     private void renameTable() {
         String newName = JOptionPane.showInputDialog(
                 this, "New table name:", data.getName(),
                 JOptionPane.PLAIN_MESSAGE);
         if(newName != null && (!newName.trim().isEmpty())) {
             data.setName(newName);
             setTitle(newName);
         }
     }
     
     /** View this table's properties */
     private void viewTableProperties() {
         SlideSetPropertiesViewer sspv
                 = new SlideSetPropertiesViewer(this, data);
         sspv.setVisible(true);
     }
     
     /** Set values of selected cells using a sequence */
     private void setFromSequence() {
         int col = table.getSelectedColumn();
         int[] rows = table.getSelectedRows();
         if (table.getSelectedColumnCount() > 1) {
             JOptionPane.showMessageDialog(
                     this, "Cannot set values from multiple columns",
                     "Slide Set", JOptionPane.INFORMATION_MESSAGE);
             return;
         }
         if (table.getSelectedColumnCount() == 0 || table.getSelectedRowCount() == 0) {
             JOptionPane.showMessageDialog(
                     this, "No columns selected",
                     "Slide Set", JOptionPane.INFORMATION_MESSAGE);
             return;
         }
         String val = JOptionPane.showInputDialog(this, "New values (comma-separated):", "");
         if (val == null || val.isEmpty())
             return;
         String[] vals = val.split(",");
         try {
             for(int i = 0; i < rows.length; i++) {
                 data.getDataElement(col, rows[i]).setUnderlyingText(vals[i % vals.length]);
             }
         } catch (SlideSetException ex) {
             handleError(ex);
         } finally {
             table.tableChanged(
                     new TableModelEvent(table.getModel(), TableModelEvent.ALL_COLUMNS));
         }
     }
     
     /** Record an error */
     private void handleError(Exception e) {
         log.println(e.getLocalizedMessage());
         e.printStackTrace(System.out);
     }
     
     /** Drop handler for the table header */
     private class DropHandler extends TransferHandler {

          @Override
          public boolean canImport(TransferSupport info) {
               if(!info.isDrop())
                    return false;
               if(!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
                    return false;
               final Point dp = info.getDropLocation().getDropPoint();
               final int col = table.convertColumnIndexToModel(table.columnAtPoint(dp));
               if(col < 0 || col >= data.getNumCols())
                    return false;
               return true;
          }

          @Override
          public boolean importData(TransferSupport info) {
               if(!canImport(info))
                    return false;
               final Point dp = info.getDropLocation().getDropPoint();
               final int col = table.convertColumnIndexToModel(table.columnAtPoint(dp));
               final int row = table.convertRowIndexToModel(table.rowAtPoint(dp));
               List<File> files;
               try {
                    files =
                         (List<File>)info.getTransferable().
                         getTransferData(DataFlavor.javaFileListFlavor);
               } catch(Throwable t) { throw new IllegalArgumentException("DND error: " + t); }
               if(info.getComponent() instanceof JTableHeader)
                    for(File f :  files) {
                         final String p = Util.makePathRelative(f.getPath(), data.getWorkingDirectory());
                         int r;
                         try{ r = data.addRow(); }
                         catch(Exception e) { handleError(e); return false; }
                         try{ data.setUnderlying(col, r, p); }
                         catch(Throwable t) { data.removeRow(r); return false; }
                    }
               else if(info.getComponent() instanceof JTable) {
                    if(files.size() != 1)
                         return false;
                    if(col < 0 || row < 0)
                         return false;
                    final String p = 
                         Util.makePathRelative(files.get(0).getPath(), data.getWorkingDirectory());
                    try{ data.setUnderlying(col, row, p); }
                    catch(Throwable t) { System.out.println(t.toString()); }
               }
               table.tableChanged(new TableModelEvent(table.getModel()));
               return true;
          }
          
     }
     
}
