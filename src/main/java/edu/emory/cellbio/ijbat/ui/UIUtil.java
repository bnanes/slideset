package edu.emory.cellbio.ijbat.ui;

import java.util.Arrays;
import java.awt.Component;
import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Utility functions related to the UI
 * 
 * @author Benjamin Nanes
 */
public class UIUtil {
     
     /**
      * Add a menu item to a menu tree, along with sub-menu nodes if needed
      * 
      * @param labels An array of menu labels where the last String represents
      *               the menu item and all other Strings represent
      *               the sub-menu tree in which the item should be placed
      * @param command The command string to send to the menu listener
      *                when this item is selected
      * @param m The top menu tree to start with
      * @param l The {@code ActionListener} to add to the menu item
      */
     public static void parseRecursiveMenuAdd(String[] labels, String command,
                                              JMenu m, ActionListener l) {
          if(labels.length == 1) {
               JMenuItem i = new JMenuItem(labels[0]);
               i.setActionCommand(command);
               i.addActionListener(l);
               m.add(i);
               return;
          }
          Component[] items = m.getMenuComponents();
          if(items != null && items.length > 0) {
               for(int u=0; u<items.length; u++) {
                    if(JMenu.class.isInstance(items[u])
                         && ((JMenu)items[u]).getText().equals(labels[0])) {
                         String[] remainingLabels =
                              Arrays.copyOfRange(labels, 1, labels.length);
                         parseRecursiveMenuAdd(remainingLabels, command, (JMenu)items[u], l);
                         return;
                    }
               }
          }
          JMenu sm = new JMenu(labels[0]);
          sm.setName(labels[0]);
          m.add(sm);
          String[] remainingLabels = Arrays.copyOfRange(labels, 1, labels.length);
          parseRecursiveMenuAdd(remainingLabels, command, sm, l);
     }
     
}