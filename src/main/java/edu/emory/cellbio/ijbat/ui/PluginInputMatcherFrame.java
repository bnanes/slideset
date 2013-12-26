package edu.emory.cellbio.ijbat.ui;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.dm.DataTypeIDService;
import edu.emory.cellbio.ijbat.ex.OperationCanceledException;
import edu.emory.cellbio.ijbat.pi.PluginInputPicker;
import imagej.ImageJ;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.util.Arrays;
import java.util.ArrayList;

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
public class PluginInputMatcherFrame extends JFrame
       implements PluginInputPicker, SlideSetWindow, ActionListener {
     
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
          // Set window listener
          addWindowListener( new WindowAdapter() {
              @Override
              public void windowClosing(WindowEvent e) {
                  kill();
              }
          });
     }
     
     // -- Methods --
     
     /** Add an input to the dialog (before display please) */
     public void addInput(String label, String[] choices, final Object[] constantRequest) {
          // Sanity checks
          if(initialized) throw new
               IllegalArgumentException(
                  "Cannot add items to an initialized PluginMatcherFrame");
          if(choices.length != constantRequest.length)
              throw new IllegalArgumentException(
                  "Different length of choices and constantRequests!");
          if(choices.length == 0)
              throw new IllegalArgumentException("No choice!");
          
          // Build index
          ArrayList<String> colNames = new ArrayList<String>();
          colNames.addAll(Arrays.asList(choices));
          
          // Create components
          final JFormattedTextField cv = new JFormattedTextField();
          cv.setColumns(8);
          cv.setMaximumSize(cv.getPreferredSize());
          cv.setValue(constantRequest[0]);
          final JComboBox field = new JComboBox(colNames.toArray());
          field.addItemListener(new ItemListener() {
              public void itemStateChanged(ItemEvent e) {
                  int i = field.getSelectedIndex();
                  Object val = null;
                  if(i >= 0 && i < constantRequest.length)
                      val = constantRequest[i];
                  cv.setValue(val);
                  updateControls();
              }
          });
          final JLabel id = new JLabel(label);
          
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
          inputNames.add(label);
          fields.add(field);
          labels.add(id);
          constantVals.add(cv);
     }
     
     public void getInputChoices(
             ArrayList<Integer> inputChoices,
             ArrayList<Object> constants)
             throws OperationCanceledException {
          showAndWait();
          if(!wasOKed())
              throw new OperationCanceledException("Canceled by user.");
          inputChoices.clear();
          constants.clear();
          for(JComboBox cb : fields) {
              int sel = cb.getSelectedIndex();
              inputChoices.add(sel);
              if(sel < 0) {
                    JOptionPane.showMessageDialog(this, 
                         "Cannot run command without values for all input parameters", 
                         "Slide Set", JOptionPane.ERROR_MESSAGE);
                    throw new OperationCanceledException(
                         "Plugin canceled because of missing input parameters");
               }
          }
          for(JFormattedTextField ftf : constantVals)
              constants.add(ftf.getValue());
     }
     
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
               setLocationRelativeTo(null);
          }
          active = b;
          updateControls();
          super.setVisible(b);
     }
     
     /** Listener for button actions */
     @Override
     public synchronized void actionPerformed(ActionEvent e) {
          if(e.getActionCommand().equals("ok"))
               okPressed = true;
          else if(e.getActionCommand().equals("cancel"))
               cancelPressed = true;
          kill();
     }
     
     @Override
     public void kill() {
          setVisible(false);
          dispose();
          synchronized (this) {
              notifyAll();
          }
     }
     
     // -- Helper methods --
     
     /** Show the dialog <b>and</b> wait for a user response */
     private void showAndWait() {
          setVisible(true);
          synchronized(this) {
              try { 
                  while(active)
                      wait();
              } catch(java.lang.InterruptedException e) { System.out.println(e); }
          }
     }
     
     /** Did the user press OK? */
     private boolean wasOKed() {
          return okPressed;
     }
     
     private synchronized void updateControls() {
         for(JFormattedTextField f : constantVals)
             f.setEnabled(!(f.getValue() == null));
     }
     
     // -- Tests --
     
}
