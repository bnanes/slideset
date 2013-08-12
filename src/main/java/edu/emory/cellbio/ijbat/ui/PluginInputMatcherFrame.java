package edu.emory.cellbio.ijbat.ui;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.dm.DataElement;
import edu.emory.cellbio.ijbat.dm.DataTypeIDService;

import edu.emory.cellbio.ijbat.ex.OperationCanceledException;
import imagej.ImageJ;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

import javax.swing.JFrame;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

/**
 * GUI for assigning {@code SlideSet} columns to plugin inputs
 * 
 * @author Benjamin Nanes
 */
public class PluginInputMatcherFrame extends JFrame implements ActionListener {
     
     // -- Fields --
     
     private SlideSet data;
     private ImageJ ij;
     private DataTypeIDService dtid;
     private boolean active = false;
     private boolean initialized = false;
     private boolean okPressed = false;
     private boolean cancelPressed = false;
     
     /** The layout manager */
     private BoxLayout lman;
     /** Padding */
     private final int gap = 5;
     
     /** The combo boxes */
     private ArrayList<JComboBox> fields = new ArrayList<JComboBox>();
     /** The constant value fields */
     private ArrayList<JFormattedTextField> constantVals = new ArrayList<JFormattedTextField>();
     /** The parameter labels */
     private ArrayList<JLabel> labels = new ArrayList<JLabel>();
     /** An index to match choices in each combo box to the appropriate SlideSet column index */
     private ArrayList<ArrayList<Integer>> optionIndex = new ArrayList<ArrayList<Integer>>();
     /** List of the input names */
     private ArrayList<String> inputNames = new ArrayList<String>();
     
     // -- Constructor --
     
     public PluginInputMatcherFrame(SlideSet data, ImageJ context, DataTypeIDService dtid) {
          this.data = data;
          ij = context;
          this.dtid = dtid;
          setTitle("Select values for input fields");
          setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
          lman = new BoxLayout(getContentPane(), BoxLayout.Y_AXIS);
          getContentPane().setLayout(lman);
     }
     
     // -- Methods --
     
     /** Add an input to the dialog (before display please) */
     public void addInput(Class<?> type, String label) {
          // Sanity checks
          if(initialized) throw new
               IllegalArgumentException("Cannot add items to an initialized PluginMatcherFrame");
          if(type == null) throw new IllegalArgumentException("Cannot add null item type");
          if(label == null) label = type.getSimpleName();
          inputNames.add(label);
          String nameAndType = label + " (" + type.getSimpleName() +")";
          
          // Build index
          ArrayList<Integer> cols =
               dtid.getMatchingColumns(type, data);
          ArrayList<String> colNames = new ArrayList<String>();
          for(int i : cols)
               colNames.add(data.getColumnName(i));
          
          // Test <<<<<<<<<<<<<<<<<<<<<<
          for(int i=0; i<cols.size(); i++) {
               System.out.print(i);
               System.out.print(": " + colNames.get(i) + ", index# ");
               System.out.println(cols.get(i));
          }
          
          // Create components
          final JFormattedTextField cv;
          final boolean canTable = dtid.canStoreInTable(type);
          final String sType = dtid.suggestTypeCode(type);
          if(canTable) {
               cv = new JFormattedTextField(
                    dtid.createDataElement(sType, data).getUnderlying());
               cols.add(-1);
               colNames.add("Constant value >>");
          }
          else if (dtid.isTypeCodeLinkLinker(sType)) {
               cv = new JFormattedTextField("");
               cols.add(-1);
               colNames.add("Constant " + dtid.getTypeCodeName(sType) + " >>");
          }
          else {
               cv = new JFormattedTextField("<?>");
               cv.setEnabled(false);
          }
          cv.setColumns(8);
          cv.setMaximumSize(cv.getPreferredSize());
          final JComboBox field = new JComboBox(colNames.toArray());
          final JLabel id = new JLabel(nameAndType);
          
          // Layout compontents
          add(Box.createVerticalStrut(gap));
          JPanel row = new JPanel();
          add(row);
          row.setAlignmentX(CENTER_ALIGNMENT);
          row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
          row.add(Box.createHorizontalStrut(gap));
          row.add(id);
          row.add(Box.createGlue());
          row.add(Box.createHorizontalStrut(gap));
          row.add(field);
          row.add(Box.createHorizontalStrut(gap));
          row.add(new JLabel("- or -"));
          row.add(Box.createHorizontalStrut(gap));
          row.add(cv);
          row.add(Box.createHorizontalStrut(gap));
          
          // Save components
          fields.add(field);
          labels.add(id);
          optionIndex.add(cols);
          constantVals.add(cv);
     }
     
     /**
      * <i>After</i> the dialog has been displayed and closed
      * (i.e. a call to {@code showAndWait()} has returned
      * and {@code wasOKed()} returns {@code true}), this method will get
      * a table of input values to pass to a plugin.
      * 
      * @return A matrix of {@link DataElement}s indexed as
      * [row from the {@code SlideSet}][input number]
      */
     public DataElement[][] getValues() throws OperationCanceledException {
          final DataElement[][] values = new DataElement[data.getNumRows()][fields.size()];
          for(int i=0; i<fields.size(); i++) {
               int sel = fields.get(i).getSelectedIndex();
               if(sel < 0) {
                    JOptionPane.showMessageDialog(this, 
                         "Cannot run command without values for all input parameters", 
                         "Slide Set", JOptionPane.ERROR_MESSAGE);
                    throw new OperationCanceledException("Plugin canceled because of missing input parameters");
               }
               if(optionIndex.get(i).get(sel) == -1)
                    for(int r=0; r<data.getNumRows(); r++)
                         values[r][i] = dtid.createDataElement(constantVals.get(i).getValue(), data);
               else for(int r=0; r<data.getNumRows(); r++)
                    values[r][i] = data.getDataElement(optionIndex.get(i).get(sel), r);
          }
          return values; //[rows][inputs]
     }
     
     /**
      * <i>After</i> the dialog has been displayed and closed
      * (i.e. a call to {@code showAndWait()} has returned
      * and {@code wasOKed()} returns {@code true}), this method will get
      * a {@code Map} of input parameters and their assignments
      * (either column name or {@code String} representation
      * of the constant value).
      */
     public LinkedHashMap<String, String> getInputMap() {
          LinkedHashMap<String, String> result =
               new LinkedHashMap<String, String>(fields.size() + 1, 0.99f);
          for(int i=0; i<fields.size(); i++) {
               int sel = fields.get(i).getSelectedIndex();
               int option = optionIndex.get(i).get(sel);
               if(option == -1)
                    result.put(inputNames.get(i), constantVals.get(i).getText());
               else
                    result.put(inputNames.get(i), data.getColumnName(option));
          }
          return result;
     }
     
     /** Show the dialog <b>and</b> wait for a user response */
     public synchronized void showAndWait() {
          setVisible(true);
          try { 
               while(active)
                    wait();
          } catch(java.lang.InterruptedException e) { System.out.println(e); }
     }
     
     /** Did the user press OK? */
     public boolean wasOKed() {
          return okPressed;
     }
     
     /** Did the user press Cancel? */
     public boolean wasCanceled() {
          return cancelPressed;
     }
     
     /** Control dialog visibility. {@code showAndWait} is probably a more useful option. */
     @Override
     public void setVisible(boolean b) {
          if(b && !initialized) {
               initialized = true;
               // Align combo boxes
               int fieldMax = 0;
               for(JComboBox f : fields)
                    fieldMax = Math.max(fieldMax, f.getWidth());
               for(JComboBox f : fields) {
                    f.setMaximumSize(new Dimension(fieldMax, 100));
                    f.setMinimumSize(new Dimension(fieldMax, 1));
               }
               
               // Create buttons
               JButton ok = new JButton("OK");
               ok.setActionCommand("ok");
               ok.addActionListener(this);
               JButton cancel = new JButton("Cancel");
               cancel.setActionCommand("cancel");
               cancel.addActionListener(this);
               
               // Layout buttons
               JPanel row = new JPanel();
               add(Box.createVerticalStrut(gap));
               add(row);
               row.add(Box.createHorizontalGlue());
               row.add(ok);
               row.add(Box.createHorizontalStrut(gap));
               row.add(cancel);
               row.add(Box.createHorizontalStrut(gap));
               add(Box.createVerticalStrut(gap));
               pack();
               
               // Set window listener
               this.addWindowListener( new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                         setVisible(false);
                         synchronized (e.getWindow()) {
                              e.getWindow().notifyAll();
                         }
                    }
               });
          }
          active = b;
          super.setVisible(b);
     }
     
     /** Listener for button actions */
     @Override
     public synchronized void actionPerformed(ActionEvent e) {
          if(e.getActionCommand().equals("ok"))
               okPressed = true;
          else if(e.getActionCommand().equals("cancel"))
               cancelPressed = true;
          setVisible(false);
          notifyAll();
     }
     
     // -- Helper methods --
     
     // -- Tests --
     
}
