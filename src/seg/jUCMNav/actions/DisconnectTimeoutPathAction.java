package seg.jUCMNav.actions;

import java.util.Vector;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPart;

import seg.jUCMNav.JUCMNavPlugin;
import seg.jUCMNav.editors.UCMNavMultiPageEditor;
import seg.jUCMNav.model.commands.delete.DeleteMultiNodeCommand;

/**
 * Given a timer with a timeout path, disconnects it.
 * 
 * @author jkealey
 *  
 */
public class DisconnectTimeoutPathAction extends UCMSelectionAction {

    public static final String DISCONNECTTIMEOUTPATH = "seg.jUCMNav.DisconnectTimeoutPath"; //$NON-NLS-1$

    /**
     * @param part
     */
    public DisconnectTimeoutPathAction(IWorkbenchPart part) {
        super(part);
        setId(DISCONNECTTIMEOUTPATH);
        setImageDescriptor(ImageDescriptor.createFromFile(JUCMNavPlugin.class, "icons/Timer16.gif")); //$NON-NLS-1$
    }

    /**
     * Can disconnect timeout path on a timer if it already has one.
     */
    protected boolean calculateEnabled() {

        SelectionHelper sel = new SelectionHelper(getSelectedObjects());
        if (sel.getTimer() != null) {
            return sel.getTimer().getSucc().size() == 2;
        } else
            return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see seg.jUCMNav.actions.UCMSelectionAction#getCommand()
     */
    protected Command getCommand() {
        Vector in = new Vector();
        Vector out = new Vector();
        SelectionHelper sel = new SelectionHelper(getSelectedObjects());
        out.add(sel.getTimer().getSucc().get(1));
        return new DeleteMultiNodeCommand(sel.getTimer(), in, out, ((UCMNavMultiPageEditor)getWorkbenchPart()).getCurrentPage().getGraphicalViewer().getEditPartRegistry());
    }

}