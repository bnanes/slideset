package org.nanes.slideset.ui;

import org.nanes.slideset.SlideSet;


/**
 * An un-editable {@code TableModel} for the {@code SlideSet} class.
 * @see org.nanes.slideset.ui.SlideSetTableModel
 * @see javax.swing.JTable
 * 
 * @author Benjamin Nanes
 */
public class SlideSetLockedTableModel extends SlideSetTableModel {

    public SlideSetLockedTableModel(SlideSet data) {
        super(data);
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
     
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    
    }
    
}
