package org.nanes.slideset.ui;

import org.nanes.slideset.SlideSet;
import org.nanes.slideset.dm.DataTypeIDService;
import org.nanes.slideset.ex.OperationCanceledException;
import org.nanes.slideset.ex.SlideSetException;
import org.nanes.slideset.pi.PluginInputPicker;
import net.imagej.ImageJ;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.util.Arrays;
import java.util.ArrayList;
import javax.swing.AbstractAction;

import javax.swing.JFrame;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.WindowConstants;

/**
 * GUI for assigning {@code SlideSet} columns to plugin inputs
 * 
 * @author Benjamin Nanes
 */
public class PluginInputMatcherFrame extends JFrame
       implements PluginInputPicker, SlideSetWindow, ActionListener {
     
     // -- Fields --
     
     private final SlideSet data;
     private final ImageJ ij;
     private final DataTypeIDService dtid;
     private boolean active = false;
     private boolean initialized = false;
     private boolean okPressed = false;
     private boolean cancelPressed = false;
     
     /** The layout manager */
     private final BoxLayout lman;
     /** Padding */
     private static final int gap = 5;
     
     /** The combo boxes */
     private final ArrayList<JComboBox> fields = new ArrayList<JComboBox>();
     /** The constant value fields */
     private final ArrayList<JFormattedTextField> constantVals = new ArrayList<JFormattedTextField>();
     /** The appropriate values menu buttons */
     private final ArrayList<JButton> constantValOptions = new ArrayList<JButton>();
     /** The parameter labels */
     private final ArrayList<JLabel> labels = new ArrayList<JLabel>();
     /** An index to match choices in each combo box to the appropriate SlideSet column index */
     private final ArrayList<ArrayList<Integer>> optionIndex = new ArrayList<ArrayList<Integer>>();
     /** List of the input names */
     private final ArrayList<String> inputNames = new ArrayList<String>();
     /** Button to view the relevant documentation, if applicable */
     private JButton getHelp = null;
     
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
     public void addInput(String label,
             String[] choices,
             final Object[] constantRequest,
             final String[] acceptableValues) {
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
          JButton av = null;
          if(acceptableValues != null && acceptableValues.length > 0)
              av = makeParameterOptionsMenu(acceptableValues, cv);
          
          // Save components
          inputNames.add(label);
          fields.add(field);
          labels.add(id);
          constantVals.add(cv);
          constantValOptions.add(av);
     }

     public void setHelpPath(final String helpPath, final HelpLoader helpLoader) {
        if(helpPath == null || helpPath.trim().isEmpty())
            return;
        getHelp = new JButton();
        getHelp.setAction(new AbstractAction("Documentation") {
            public void actionPerformed(ActionEvent ae) {
                try {
                    helpLoader.getHelp(helpPath.trim());
                } catch(SlideSetException e) {}
            }
        });
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
               doComponentLayout();
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
               row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
               add(Box.createVerticalStrut(gap));
               add(row);
               row.add(Box.createHorizontalStrut(gap));
               if(getHelp != null) {
                   row.add(getHelp);
                   row.add(Box.createHorizontalStrut(gap));
               }
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
              } catch(java.lang.InterruptedException e) { ij.log().debug(e); }
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
     
     /** Finalize the dialog before display */
     private void doComponentLayout() {
         boolean cvo = false;
         for(JButton i : constantValOptions) {
             cvo = i != null;
             if(cvo) break;
         }
         for(int i = 0; i < labels.size(); i++) {
            add(Box.createVerticalStrut(gap));
            JPanel row = new JPanel();
            add(row);
            row.setAlignmentY(CENTER_ALIGNMENT);
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.add(Box.createHorizontalStrut(gap));
            row.add(labels.get(i));
            row.add(Box.createGlue());
            row.add(Box.createHorizontalStrut(gap));
            row.add(fields.get(i));
            row.add(Box.createHorizontalStrut(gap));
            row.add(new JLabel("- or -"));
            row.add(Box.createHorizontalStrut(gap));
            row.add(constantVals.get(i));
            row.add(Box.createHorizontalStrut(gap));
            if(cvo) {
                if(constantValOptions.get(i) != null) {
                    row.add(constantValOptions.get(i));
                    row.add(Box.createHorizontalStrut(gap));
                } else {
                    JButton template = new JButton("...");
                    template.setMargin(new Insets(0,0,0,0)); 
                    row.add(Box.createHorizontalStrut(template.getPreferredSize().width + gap));
                }
            }
         }
     }
     
     /** Create a button and menu to list appropriate
      *  values for an input parameter */
     private JButton makeParameterOptionsMenu(
             String[] options,
             final JFormattedTextField field) {
         final JButton b = new JButton();
         final JPopupMenu pm = new JPopupMenu();
         for(String o : options) {
             final JMenuItem mi = new JMenuItem();
             mi.setAction(new AbstractAction(o) {
                 public void actionPerformed(ActionEvent ae) {
                     try {
                        if(field.isEnabled()) 
                            field.setValue(mi.getText());
                     } catch(Exception e) {}
                 }
             });
             pm.add(mi);
         }
         final PluginInputMatcherFrame pimf = this;
         b.setAction(new AbstractAction("...") {
             public void actionPerformed(ActionEvent ae) {
                 pm.show(pimf, pimf.getMousePosition().x, pimf.getMousePosition().y);
             }
         });
         b.setMargin(new Insets(0,0,0,0));
         b.setToolTipText("View appropriate parameter values");
         return b;
     }
     
     // -- Tests --
     
}
