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
               JMenuItem i = new JMenuItem(labels[0].trim());
               i.setActionCommand(command);
               i.addActionListener(l);
               addAlphabetized(i, m, false);
               return;
          }
          Component[] items = m.getMenuComponents();
          if(items != null && items.length > 0) {
               for(int u=0; u<items.length; u++) {
                    if(JMenu.class.isInstance(items[u])
                         && ((JMenu)items[u]).getText().equals(labels[0].trim())) {
                         String[] remainingLabels =
                              Arrays.copyOfRange(labels, 1, labels.length);
                         parseRecursiveMenuAdd(remainingLabels, command, (JMenu)items[u], l);
                         return;
                    }
               }
          }
          JMenu sm = new JMenu(labels[0].trim());
          sm.setName(labels[0].trim());
          addAlphabetized(sm, m, false);
          String[] remainingLabels = Arrays.copyOfRange(labels, 1, labels.length);
          parseRecursiveMenuAdd(remainingLabels, command, sm, l);
     }
     
     /**
      * Add a menu item to it's proper alphabetized position
      * in the menu. Assumes any existing menu items are
      * already alphabetized.
      * 
      * @param jmi The menu item to add
      * @param jm The menu to which the item should be added
      * @param foldersFirst If {@code true}, add folders before
      *   other items; if {@code false}, add folders after other items.
      */
     private static void addAlphabetized(
             JMenuItem jmi,
             JMenu jm,
             boolean foldersFirst) {
         if(jm == null || jmi == null)
             throw new IllegalArgumentException();
         final int n = jm.getItemCount();
         final boolean folder = jmi instanceof JMenu;
         if(n == 0) {
             jm.add(jmi);
             return;
         }
         for(int i = 0; i < n; i++) {
             JMenuItem cur = jm.getItem(i);
             if(cur == null)
                 continue;
             boolean curFolder = cur instanceof JMenu;
             if((foldersFirst && folder && !curFolder)
                   || (!foldersFirst && !folder && curFolder)) {
                 jm.insert(jmi, i);
                 break;
             }
             if(((folder && curFolder) || (!folder && !curFolder))
                     && jmi.getText().compareToIgnoreCase(cur.getText()) <= 0) {
                 jm.insert(jmi, i);
                 break;
             }
             if(i == n - 1)
                 jm.add(jmi);
         }
     }
     
}