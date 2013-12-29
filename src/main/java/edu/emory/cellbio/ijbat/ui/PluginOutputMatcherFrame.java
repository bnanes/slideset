package edu.emory.cellbio.ijbat.ui;

import edu.emory.cellbio.ijbat.dm.DataTypeIDService;
import edu.emory.cellbio.ijbat.ex.OperationCanceledException;
import edu.emory.cellbio.ijbat.pi.PluginOutputPicker;
import imagej.ImageJ;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
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
 * GUI for selecting options for handling plugin outputs.
 * 
 * @author Benjamin Nanes
 */
public class PluginOutputMatcherFrame extends JFrame 
        implements PluginOutputPicker, SlideSetWindow, ActionListener {

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
     /** Default link directories for each field and option */
     private final ArrayList<String[]> linkDirDefaults = new ArrayList<String[]>();
     private static final String linkDirDefault = "dir";
     /** Default link file prefixes for each field and option */
     private final ArrayList<String[]> linkPreDefaults = new ArrayList<String[]>();
     private static final String linkPreDefault = "result";
     /** Default link file prefixes for each field and option */
     private final ArrayList<String[]> linkExtDefaults = new ArrayList<String[]>();
     private static final String linkExtDefault = "txt";
     
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
     
     public void addOutput(String label,
            String[] choices,
            boolean[] link,
            String[] linkDir,
            String[] linkPre,
            String[] linkExt) { 
          // Sanity checks
          if(initialized) throw new
               IllegalArgumentException("Cannot add items to an initialized PluginMatcherFrame");
          if(choices.length != link.length
                  || link.length != linkDir.length
                  || linkDir.length != linkPre.length
                  || linkPre.length != linkExt.length)
               throw new IllegalArgumentException(
                       "Different number of parameters!");
          if(choices.length == 0)
               throw new IllegalArgumentException("No choice!");
          
          // Lookup appropriate types and build index 
          final ArrayList<String> tNames = new ArrayList<String>();
          tNames.addAll(Arrays.asList(choices));
          final ArrayList<Boolean> tLink = new ArrayList<Boolean>();
          for(boolean b : link)
              tLink.add(b);
          isOptionLink.add(tLink);
          linkDirDefaults.add(linkDir);
          linkPreDefaults.add(linkPre);
          linkExtDefaults.add(linkExt);
          
          // Create components
          labels.add(new JLabel(label));
          types.add(new JComboBox(tNames.toArray()));
          dir.add(new JTextField(linkDirDefault, 2));
          base.add(new JTextField(linkPreDefault, 4));
          ext.add(new JTextField(linkExtDefault, 2));
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
      * @param labels {@link List} of human-readable 
      *               names for the input parameters
      */
     public void setParentFieldLabels(String[] labels) {
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
          for(final String i : labels) {
               final JCheckBoxMenuItem box = new JCheckBoxMenuItem(i);
               inputsMenu.add(box);
               this.inputs.add(box);
          }
     }
     
     public void getOutputChoices(
            ArrayList<Integer> outputChoices,
            ArrayList<Integer> selectedParentFields,
            ArrayList<String> linkDir,
            ArrayList<String> linkPre,
            ArrayList<String> linkExt)
            throws OperationCanceledException {
         showAndWait();
         outputSanityChecks();
         outputChoices.clear();
         selectedParentFields.clear();
         linkDir.clear();
         linkPre.clear();
         linkExt.clear();
         for(int i = 0; i< types.size(); i++) {
             outputChoices.add(types.get(i).getSelectedIndex());
             linkDir.add(dir.get(i).getText());
             linkPre.add(base.get(i).getText());
             linkExt.add(ext.get(i).getText());
         }
         for(int i = 0; i < inputs.size(); i++)
             if(inputs.get(i).isSelected())
                 selectedParentFields.add(i);
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
               setLocationRelativeTo(null);
               
               // Set listeners
               for(final JComboBox j : types) {
                    j.setActionCommand("typeChanged");
                    j.addActionListener(this);
               }
               addWindowListener( new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                         setVisible(false);
                         synchronized (e.getWindow()) {
                              e.getWindow().notifyAll();
                         }
                         dispose();
                    }
               });
          }
          active = visible;
          super.setVisible(visible);
     }
     
     // -- Helper methods --
     
     private synchronized void handleActionEvent(ActionEvent e) {
          final String ac = e.getActionCommand();
          if(ac.equals("typeChanged"))
               updateControlStates();
          else if(ac.equals("ok")) {
               okPressed = true;
               kill();
          }
          else if(ac.equals("cancel")) {
               cancelPressed = true;
               kill();
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
               if(link) {
                   String t = linkDirDefaults.get(i)[sel];
                   dir.get(i).setText(t == null ? linkDirDefault : t);
                   t = linkPreDefaults.get(i)[sel];
                   base.get(i).setText(t == null ? linkPreDefault : t);
                   t = linkExtDefaults.get(i)[sel];
                   ext.get(i).setText(t == null ? linkExtDefault : t);
               }
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
     
     /** Did the user press OK? */
     private boolean wasOKed() {
          return okPressed;
     }
     
     /** Did the user press Cancel? */
     private boolean wasCanceled() {
          return cancelPressed;
     }
     
     /** Show the dialog <b>and</b> wait for a user response */
     private synchronized void showAndWait() {
          setVisible(true);
          try { 
               while(active)
                    wait();
          } catch(java.lang.InterruptedException e) { System.out.println(e); }
     }
     
}
