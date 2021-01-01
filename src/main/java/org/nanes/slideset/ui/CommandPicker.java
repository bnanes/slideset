package org.nanes.slideset.ui;

import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleInfo;
import org.scijava.ui.swing.commands.CommandFinderPanel;
import org.scijava.ui.swing.SwingDialog;
import javax.swing.JOptionPane;
import org.scijava.Context;

/**
 * Dialog box to select a recognized ImageJ command
 * 
 * @author Benjamin Nanes
 */
public class CommandPicker {
    
    private final Context context;
    private final CommandService commandService;
    private CommandFinderPanel commandFinderPanel;
    private int dialogReturnValue = JOptionPane.CLOSED_OPTION;
    
    // -- Constructor --
    
    public CommandPicker(Context c) {
        context = c;
        commandService = c.getService(CommandService.class);
    }
    
    // -- Methods --
    
    /**
     * Show the command picker dialog.
     * @return User response. See: {@link JOptionPane#OK_OPTION JOptionPane} constants
     */
    public int show() {
        final String baseDir = context.getService(org.scijava.app.AppService.class).getApp().getBaseDirectory().getAbsolutePath();
        commandFinderPanel =
            new CommandFinderPanel(commandService.getModuleService(), baseDir);
        final SwingDialog dialog =
            new SwingDialog(commandFinderPanel, JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE, false);
        dialog.setFocus(commandFinderPanel.getSearchField());
        dialog.setTitle("Select Command");
        dialogReturnValue = dialog.show();
        return dialogReturnValue;
    }
    
    /**
     * @return {@code true} iff the OK button was pressed
     */
    public boolean wasOKd() {
        if(commandFinderPanel == null)
            return false;
        return dialogReturnValue == JOptionPane.OK_OPTION;
    }
    
    /**
     * @return {@code CommandInfo} for the selected ImageJ command.
     * @throws IllegalStateException if the dialog has not been
     *         initialized or the selected item is not a valid
     *         ImageJ command.
     */
    public CommandInfo getSelectedCommand() {
        if(commandFinderPanel == null)
            throw new IllegalStateException("Dialog has not been initialized!");
        final ModuleInfo mi = commandFinderPanel.getCommand();
        if(!(mi instanceof CommandInfo))
            throw new IllegalStateException("Not a valid command! " + mi.getClass().getName());
        return (CommandInfo) mi;
    }
    
}
