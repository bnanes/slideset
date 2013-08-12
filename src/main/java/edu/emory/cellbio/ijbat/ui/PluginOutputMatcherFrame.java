package edu.emory.cellbio.ijbat.ui;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.dm.DataTypeIDService;
import edu.emory.cellbio.ijbat.ex.OperationCanceledException;
import imagej.ImageJ;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 *
 * @author Benjamin Nanes
 */
public class PluginOutputMatcherFrame extends JFrame 
        implements SlideSetWindow, ActionListener {

     // -- Fields --
     
     private ImageJ ij;
     private DataTypeIDService dtid;
     private boolean active = false;
     private boolean initialized = false;
     private boolean okPressed = false;
     private boolean cancelPressed = false;
     
     /** Header labels */
     private JLabel headLable;
     private JLabel headType;
     private JLabel headLink;
     /** Include inputs in output menu */
     private final JPopupMenu inputsMenu = new JPopupMenu();
     /** Include inputs in output check boxes */
     private final ArrayList<JCheckBoxMenuItem> inputs = new ArrayList<JCheckBoxMenuItem>();
     /** The result labels */
     private final ArrayList<JLabel> labels = new ArrayList<JLabel>();
     /** The result type combo box */
     private final ArrayList<JComboBox> types = new ArrayList<JComboBox>();
     /** The default link directory field */
     private final ArrayList<JTextField> dir = new ArrayList<JTextField>();
     /** The default link prefix field */
     private final ArrayList<JTextField> base = new ArrayList<JTextField>();
     /** The default link extension field */
     private final ArrayList<JTextField> ext = new ArrayList<JTextField>();
     
     /** Index of {@code TypeCode} options */
     private final ArrayList<ArrayList<String>> optionIndex = new ArrayList<ArrayList<String>>();
     /** Index of which {@code TypeCode} options represent file references */
     private final ArrayList<ArrayList<Boolean>> isOptionLink = new ArrayList<ArrayList<Boolean>>();
     
     /** The layout manager */
     private BoxLayout lman;
     /** Padding */
     private final int gap = 5;
     
     // -- Constructor --
     
     public PluginOutputMatcherFrame(ImageJ context, DataTypeIDService dtid) {
          ij = context;
          this.dtid = dtid;
          setTitle("Select results to save");
          setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
          lman = new BoxLayout(getContentPane(), BoxLayout.Y_AXIS);
          getContentPane().setLayout(lman);
          layoutHeaders();
     }
     
     // -- Methods --
     
     @Override
     public void actionPerformed(ActionEvent e) {
          handleActionEvent(e);
     }

     @Override
     public synchronized void kill() {
          setVisible(false);
          dispose();
          notifyAll();
     }
     
     /**
      * Add a result field to the dialog.
      * @param type Class of the result variable
      * @param label Human-readable name of the result variable
      */
     public void addOutput(Class<?> type, String label) { 
          // Sanity checks
          if(initialized) throw new
               IllegalArgumentException("Cannot add items to an initialized PluginMatcherFrame");
          if(type == null) throw new IllegalArgumentException("Cannot add null item type");
          if(label == null) label = type.getSimpleName() + "-result";
          
          // Lookup appropriate types and build index 
          final ArrayList<String> tCodes = dtid.getAppropriateTypeCodes(type);
          final ArrayList<String> tNames = new ArrayList<String>(optionIndex.size());
          final ArrayList<Boolean> tLink = new ArrayList<Boolean>(optionIndex.size());
          for(final String tc : tCodes) {
               final String[] n = dtid.getTypeCodeName(tc).split("/");
               tNames.add(n[n.length - 1]);
               tLink.add(dtid.isTypeCodeLinkLinker(tc));
          }
          tCodes.add(null);
          tNames.add("Discard");
          tLink.add(false);
          optionIndex.add(tCodes);
          isOptionLink.add(tLink);
          
          // Create components
          labels.add(new JLabel(label));
          types.add(new JComboBox(tNames.toArray()));
          dir.add(new JTextField("dir", 2));
          base.add(new JTextField("result", 4));
          ext.add(new JTextField("txt", 2));
          final int index = labels.size() - 1;
          
          // Layout components
          add(Box.createVerticalStrut(gap));
          JPanel row = new JPanel();
          add(row);
          row.setAlignmentX(CENTER_ALIGNMENT);
          row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
          row.add(Box.createHorizontalStrut(gap));
          row.add(labels.get(index));
          row.add(Box.createGlue());
          row.add(Box.createHorizontalStrut(gap));
          row.add(types.get(index));
          row.add(Box.createHorizontalStrut(2*gap));
          dir.get(index).setMaximumSize(dir.get(index).getPreferredSize());
          row.add(dir.get(index));
          row.add(Box.createHorizontalStrut(gap));
          row.add(new JLabel("/ "));
          base.get(index).setMaximumSize(base.get(index).getPreferredSize());
          row.add(base.get(index));
          row.add(new JLabel("."));
          ext.get(index).setMaximumSize(ext.get(index).getPreferredSize());
          row.add(ext.get(index));
          row.add(Box.createHorizontalStrut(gap));
          
     }
     
     /**
      * Add a popup menu to allow selection of parent
      * fields that should be included in the
      * results table.
      * @param inputs {@link List} of human-readable 
      *               names for the input parameters
      */
     public void addParentFieldsToResults(List<String> inputs) {
          
          add(Box.createVerticalStrut(gap));
          JPanel row = new JPanel();
          add(row);
          row.setAlignmentX(CENTER_ALIGNMENT);
          row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
          row.add(Box.createHorizontalStrut(gap));
          final JButton b = new JButton("Include inputs in results");
          b.setActionCommand("pop/inputs");
          b.addActionListener(this);
          row.add(b);
          row.add(Box.createGlue());
          row.add(Box.createHorizontalStrut(gap));
          
          for(final String i : inputs) {
               final JCheckBoxMenuItem box = new JCheckBoxMenuItem(i);
               inputsMenu.add(box);
               this.inputs.add(box);
          }
          
     }
     
     /** Control dialog visibility. Consider using 
      *  {@link #showAndWait() showAndWait} for convenience. */
     @Override
     public void setVisible(boolean visible) {
            
          if(visible && !initialized) {
               
               initialized = true;
               
               // Align combo boxes
               int typeMax = 0;
               for(final JComboBox j : types)
                    typeMax = Math.max(typeMax, j.getWidth());
               for(final JComboBox j : types) {
                    j.setMaximumSize(new Dimension(typeMax, 100));
                    j.setMinimumSize(new Dimension(typeMax, 1));
               }
               updateControlStates();
               
               // Create buttons
               final JButton ok = new JButton("OK");
               ok.setActionCommand("ok");
               ok.addActionListener(this);
               final JButton cancel = new JButton("Cancel");
               cancel.setActionCommand("cancel");
               cancel.addActionListener(this);
               
               // Layout buttons
               final JPanel row = new JPanel();
               add(Box.createVerticalStrut(gap));
               add(row);
               row.add(Box.createHorizontalGlue());
               row.add(ok);
               row.add(Box.createHorizontalStrut(gap));
               row.add(cancel);
               row.add(Box.createHorizontalStrut(gap));
               add(Box.createVerticalStrut(gap));
               pack();
               
               // Set listeners
               for(final JComboBox j : types) {
                    j.setActionCommand("typeChanged");
                    j.addActionListener(this);
               }
               addWindowListener( new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                         setVisible(false);
                         synchronized (e.getWindow()) {
                              e.getWindow().notifyAll();
                         }
                    }
               });
          }
          
          active = visible;
          super.setVisible(visible);
          
     }
     
     /** Did the user press OK? */
     public boolean wasOKed() {
          return okPressed;
     }
     
     /** Did the user press Cancel? */
     public boolean wasCanceled() {
          return cancelPressed;
     }
     
     /** Show the dialog <b>and</b> wait for a user response */
     public synchronized void showAndWait() {
          setVisible(true);
          try { 
               while(active)
                    wait();
          } catch(java.lang.InterruptedException e) { System.out.println(e); }
     }
     
     /**
      * Gets a {@link SlideSet} with columns configured to receive results.
      * 
      * <p> See also: {@link #getOutputIndex() getOutputIndex()}
      * 
      * @throws OperationCanceledException The dialog was dismissed in a way
      *      other than with the "OK" button.
      * @throws IllegalStateException The dialog has not been initialized appropriately.
      */
     public SlideSet getOutputTemplate() throws OperationCanceledException {
          
          // Sanity checks
          outputSanityChecks();
          
          // Add columns 
          final SlideSet result = new SlideSet(ij, dtid);
          result.addColumn("Parent Row", "Integer"); //
          for(int i=0; i<labels.size(); i++) {
               final String cType = optionIndex.get(i).get(types.get(i).getSelectedIndex());
               if(cType == null)
                    continue;
               final String cName = labels.get(i).getText();
               String cDir = dir.get(i).getText();
               cDir = cDir.trim();
               final String cBase = base.get(i).getText();
               final String cExt = ext.get(i).getText();
               final int col = result.addColumn(cName, cType);
               result.setColumnDefaultPath(col, cDir.equals("") ? null : cDir);
               result.setDefaultLinkPrefix(col, cBase);
               result.setDefaultLinkExtension(col, cExt);
          }
          
          return result;
     }
     
     /**
      * Gets an index for matching results to the appropriate columns
      * in the {@link SlideSet} returned by {@link #getOutputTemplate() getOutputTemplate()}.
      * 
      * @return An array where the index corresponds to the result number
      *         (in the order added to the dialog with 
      *         {@link #addOutput(java.lang.Class, java.lang.String) addOutput}) 
      *         and the value corresponds to the {@link SlideSet} column index
      *         where the result should be placed.  A value of -1 indicates
      *         that the result should be discarded.
      * 
      * @throws OperationCanceledException The dialog was dismissed in a way
      *      other than with the "OK" button.
      * @throws IllegalStateException The dialog has not been initialized appropriately. 
      */
     public int[] getOutputIndex() throws OperationCanceledException {
          
          outputSanityChecks();
          final int[] index = new int[labels.size()];
          int skipped = -1; // to compensate for "parent row" column
          for(int i=0; i<index.length; i++) {
               if(optionIndex.get(i).get(types.get(i).getSelectedIndex()) == null) {
                    skipped++;
                    index[i] = -1;
               }
               else
                    index[i] = i - skipped;
          }
          
          return index;
     }
     
     /**
      * Get a list of inputs parameters (by index) which should be included
      * in the results table.
      * 
      * @return A list of {@code Integer}s corresponding to the indeces 
      *         of the input passed through {@link #addInputsToResults(java.util.List)
      *         addInputsToResults()} which should be included.
      * 
      * @throws OperationCanceledException The dialog was dismissed in a way
      *      other than with the "OK" button.
      * @throws IllegalStateException The dialog has not been initialized appropriately. 
      */
     public List<Integer> getIncludedParentFields() throws OperationCanceledException {
          outputSanityChecks();
          final ArrayList<Integer> index = new ArrayList<Integer>();
          for(int i=0; i<inputs.size(); i++)
               if(inputs.get(i).isSelected())
                    index.add(i);
          return index;
     }
     
     // -- Helper methods --
     
     private synchronized void handleActionEvent(ActionEvent e) {
          final String ac = e.getActionCommand();
          if(ac.equals("typeChanged"))
               updateControlStates();
          else if(ac.equals("ok")) {
               okPressed = true;
               setVisible(false);
               notifyAll();
          }
          else if(ac.equals("cancel")) {
               cancelPressed = true;
               setVisible(false);
               notifyAll();
          }
          else if(ac.equals("pop/inputs")) {
               inputsMenu.show(this, getMousePosition().x, getMousePosition().y);
          }
          
     }
     
     private void layoutHeaders() {
          add(Box.createVerticalStrut(gap));
          JPanel row = new JPanel();
          add(row);
          row.setAlignmentX(CENTER_ALIGNMENT);
          row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
          row.add(Box.createHorizontalStrut(gap));
          headLable = new JLabel("Result");
          row.add(headLable);
          row.add(Box.createGlue());
          row.add(Box.createHorizontalStrut(gap));
          headType = new JLabel("Type");
          row.add(headType);
          row.add(Box.createGlue());
          row.add(Box.createHorizontalStrut(gap));
          headLink = new JLabel("Save As (files only)");
          row.add(headLink);
          row.add(Box.createHorizontalStrut(gap));
     }
     
     private void updateControlStates() {
          for(int i=0; i<labels.size(); i++) {
               final int sel = types.get(i).getSelectedIndex();
               final boolean link = isOptionLink.get(i).get(sel);
               dir.get(i).setEnabled(link);
               base.get(i).setEnabled(link);
               ext.get(i).setEnabled(link);
          }
     }
     
     private void outputSanityChecks() throws OperationCanceledException {
          if(!initialized)
               throw new IllegalStateException("Dialog has not been initialized! No results available!");
          if(wasCanceled())
               throw new OperationCanceledException("Canceled by user");
          if(!wasOKed())
               throw new OperationCanceledException("OK was not pressed");
     }
     
}
