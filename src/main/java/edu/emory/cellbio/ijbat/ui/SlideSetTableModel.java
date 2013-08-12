package edu.emory.cellbio.ijbat.ui;

import edu.emory.cellbio.ijbat.SlideSet;
import javax.swing.table.AbstractTableModel;

/**
 * Swing TableModel for the SlideSet class.
 * @see javax.swing.JTable
 * @author Benjamin Nanes
 */
public class SlideSetTableModel extends AbstractTableModel {
     
     // -- Fields --
     
     /** The underlying data */
     private SlideSet slideSet;
     
     // -- Constructors --
     
     public SlideSetTableModel(SlideSet data) {
          slideSet = data;
     }
     
     /*public SlideSetTableModel() {
          this(new SlideSet());
     }*/
     
     // -- Methods --
     
     @Override
     public Object getValueAt(int row, int column) {
          return slideSet.getUnderlying(column, row);
     }
     
     @Override
     public int getColumnCount() {
          return slideSet.getNumCols();
     }
     
     @Override
     public int getRowCount() {
          return slideSet.getNumRows();
     }
     
     @Override
     public String getColumnName(int column) {
          String[] t = slideSet.getColumnTypeCodeName(column).split("/");
          if(t == null || t.length == 0)
               t = new String[] {"X"};
          return "<html><center>" 
               + slideSet.getColumnName(column) 
               + "<br>[" 
               + t[t.length - 1]
               + "]</center></html>";
     }
     
     @Override
     public boolean isCellEditable(int rowIndex, int columnIndex) {
          return true;
     }
     
     @Override
     public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
          slideSet.setUnderlying(columnIndex, rowIndex, aValue);
     }
     
     @Override
     public Class<?> getColumnClass(int columnIndex) {
          if(slideSet.getNumRows() == 0)
               return Object.class;
          return slideSet.getUnderlying(columnIndex, 0).getClass();
     }
     
}
