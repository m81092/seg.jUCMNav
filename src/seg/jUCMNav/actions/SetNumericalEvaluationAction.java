package seg.jUCMNav.actions;

import java.util.Iterator;
import java.util.Vector;

import grl.IntentionalElementRef;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import seg.jUCMNav.JUCMNavPlugin;
import seg.jUCMNav.editparts.IntentionalElementEditPart;
import seg.jUCMNav.model.commands.transformations.ChangeNumericalEvaluationCommand;
import seg.jUCMNav.strategies.EvaluationStrategyManager;
import seg.jUCMNav.views.wizards.IntegerInputRangeDialog;

/**
*
* @author Andrew Miga
*/

public class SetNumericalEvaluationAction extends URNSelectionAction
{
    public static final String SET_NUMERICAL_EVALUATION = "seg.jUCMNav.SET_NUMERICAL_EVALUATION"; //$NON-NLS-1$
    private Vector intElementRefs;
    private int id;
    private static String[] values = { "+100", "+75", "+50", "+25", "0", "-25", "-50", "-75", "-100", "Other...", "Increase    (Shift+H)", "Decrease   (Shift+N)" };

	public SetNumericalEvaluationAction(IWorkbenchPart part, int id )
	{
		super(part);
        setId( SET_NUMERICAL_EVALUATION + id );
        setText( values[id] );
        if ( id == ChangeNumericalEvaluationCommand.INCREASE )
           	setImageDescriptor(JUCMNavPlugin.getImageDescriptor( "icons/move_up.gif")); //$NON-NLS-1$
        else if ( id == ChangeNumericalEvaluationCommand.DECREASE )
           	setImageDescriptor(JUCMNavPlugin.getImageDescriptor( "icons/move_down.gif")); //$NON-NLS-1$
        this.id = id;
	}

    /**
     * We need to have an intentional element reference selected.
     */
    protected boolean calculateEnabled()
    {
    	if ( EvaluationStrategyManager.getInstance().getEvaluationStrategy() == null )
    		return false;
    	
    	for ( Iterator iter = getSelectedObjects().iterator(); iter.hasNext(); )
    	{
    		Object obj = iter.next();
    		if ( !(obj instanceof IntentionalElementEditPart) )
    			return false;
    		
            if ( id < ChangeNumericalEvaluationCommand.INCREASE ) // operation is not increase or decrease, skip further tests
            	continue;
            
    		IntentionalElementRef ier = (IntentionalElementRef) (((IntentionalElementEditPart) obj).getModel());
            int oldEval = EvaluationStrategyManager.getInstance().getEvaluation( ier.getDef() );
            
            if ( id == ChangeNumericalEvaluationCommand.INCREASE ) { // increase operation, verify if possible
            	if ( oldEval == 100 )
            		return false; // can't increase from 100
            } else if ( id == ChangeNumericalEvaluationCommand.DECREASE ) { // decrease operation, verify if possible
            	if ( oldEval <= -100 )
            		return false; // can't decrease from -100
            }
    	}
    	    
    	intElementRefs = new Vector(); // all tests passed, create list
    	
    	for ( Iterator iter = getSelectedObjects().iterator(); iter.hasNext(); )
    	{
    		IntentionalElementRef ier = (IntentionalElementRef) (((IntentionalElementEditPart) iter.next()).getModel());    		
            intElementRefs.add( ier );    	
    	}   	
    	
    	return true;
    }

    public void run()
    {
    	if ( id < ChangeNumericalEvaluationCommand.USER_ENTRY || id >= ChangeNumericalEvaluationCommand.INCREASE )
    		execute( new ChangeNumericalEvaluationCommand( intElementRefs, id, 0, getCommandStack() ) );
    	else if ( id == ChangeNumericalEvaluationCommand.USER_ENTRY )
    	{
    		String currentEval = ( intElementRefs.size() > 1 ) ? "" : 
    			Integer.toString( EvaluationStrategyManager.getInstance().getEvaluation( ((IntentionalElementRef) (intElementRefs.get(0))).getDef() ) );
    		Integer userEntry = enterEvaluation( currentEval );
    		if ( userEntry != null ) {
       			int enteredValue = userEntry.intValue();        		
       			execute( new ChangeNumericalEvaluationCommand( intElementRefs, id, enteredValue, getCommandStack() ) );
    		}
    	} 
    }

    private Integer enterEvaluation( String currentEval )
	{	
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	    IntegerInputRangeDialog dialog = new IntegerInputRangeDialog( shell );
	    	
	    return ( dialog.open( "Enter Numerical Evaluation   (range: [-100,+100])", "Enter the new Numerical Evaluation: ", currentEval, -100, 100 ) );
	}
	
	public static String generateId( int id )
	{
		return SET_NUMERICAL_EVALUATION + id;
	}
	
	public static String getId( String operation )
	{	
		for ( int index = 0; index < values.length; index++ ){
			if( values[index].contains( operation ) )
				return SET_NUMERICAL_EVALUATION + index;
		}
		return null;
	}

}

