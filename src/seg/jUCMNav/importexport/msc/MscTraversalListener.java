package seg.jUCMNav.importexport.msc;

import grl.EvaluationStrategy;
import grl.StrategiesGroup;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import seg.jUCMNav.editors.resourceManagement.UrnModelManager;
import seg.jUCMNav.model.ModelCreationFactory;
import seg.jUCMNav.model.commands.create.AddContainerRefCommand;
import seg.jUCMNav.model.commands.create.CreateMapCommand;
import seg.jUCMNav.model.commands.create.CreatePathCommand;
import seg.jUCMNav.model.commands.delete.DeleteComponentRefCommand;
import seg.jUCMNav.model.commands.delete.DeleteScenarioCommand;
import seg.jUCMNav.model.commands.delete.DeleteStrategiesGroupCommand;
import seg.jUCMNav.model.commands.delete.DeleteStrategyCommand;
import seg.jUCMNav.model.commands.transformations.AttachBranchCommand;
import seg.jUCMNav.model.commands.transformations.ExtendPathCommand;
import seg.jUCMNav.model.commands.transformations.MakeWellFormedCommand;
import seg.jUCMNav.model.commands.transformations.MergeStartEndCommand;
import seg.jUCMNav.model.commands.transformations.ReplaceEmptyPointCommand;
import seg.jUCMNav.model.commands.transformations.TrimEmptyNodeCommand;
import seg.jUCMNav.model.util.URNNamingHelper;
import seg.jUCMNav.model.util.URNmodelElementIDComparator;
import seg.jUCMNav.scenarios.ScenarioUtils;
import seg.jUCMNav.scenarios.algorithmInterfaces.ITraversalListener;
import seg.jUCMNav.scenarios.model.TraversalVisit;
import seg.jUCMNav.scenarios.model.UcmEnvironment;
import ucm.map.AndFork;
import ucm.map.AndJoin;
import ucm.map.ComponentRef;
import ucm.map.Connect;
import ucm.map.DirectionArrow;
import ucm.map.EmptyPoint;
import ucm.map.EndPoint;
import ucm.map.NodeConnection;
import ucm.map.OrFork;
import ucm.map.OrJoin;
import ucm.map.PathNode;
import ucm.map.RespRef;
import ucm.map.StartPoint;
import ucm.map.Stub;
import ucm.map.Timer;
import ucm.map.UCMmap;
import ucm.map.WaitingPlace;
import ucm.scenario.EnumerationType;
import ucm.scenario.Initialization;
import ucm.scenario.ScenarioDef;
import ucm.scenario.ScenarioEndPoint;
import ucm.scenario.ScenarioGroup;
import ucm.scenario.ScenarioStartPoint;
import ucm.scenario.Variable;
import urn.URNspec;
import urncore.Component;
import urncore.Condition;
import urncore.IURNContainer;
import urncore.IURNContainerRef;
import urncore.Metadata;
import urncore.NodeLabel;
import urncore.Responsibility;
import urncore.URNmodelElement;

/**
 * A traversal listener that will generate MSCs.
 * 
 * TODO: synchronous connects -> empty responsibility 
 * 
 * TODO: in/out binding traversal -> synchronous connect or responsibility 
 * 
 * TODO: filter unused definitions (resp/comp) + scenario groups.
 * 
 * TODO: timeouts
 * 
 * TODO: andfork should have stub name if concurrency is started by endpoint. 
 *  
 * @author jkealey
 * 
 */
public class MscTraversalListener implements ITraversalListener {

	protected CommandStack cs;
	protected ScenarioDef currentDestScenario;
	protected UCMmap currentMap;
	protected ScenarioDef currentSrcScenario;

	protected HashMap htComponentRefs;
	protected HashMap htComponents;
	protected HashMap htResponsibilities;
	protected HashMap htScenarioToMap;

	protected HashMap htThreadEnd;

	protected HashMap htThreadStart;
	protected TraversalVisit lastUnblocked;
	
	protected URNspec urnspec;
	
	
	public MscTraversalListener() {

		htScenarioToMap = new HashMap();
		urnspec = (URNspec) ModelCreationFactory.getNewURNspec();
		cs = new CommandStack();

		// get rid of default scenario/scenariogroup
		Command cmd = new DeleteScenarioCommand((ScenarioDef) ((ScenarioGroup) urnspec.getUcmspec().getScenarioGroups().get(0)).getScenarios().get(0));
		cs.execute(cmd);
		cmd = new DeleteStrategiesGroupCommand((ScenarioGroup) urnspec.getUcmspec().getScenarioGroups().get(0));
		cs.execute(cmd);
		cmd = new DeleteStrategyCommand((EvaluationStrategy) urnspec.getGrlspec().getStrategies().get(0));
		cs.execute(cmd);
		cmd = new DeleteStrategiesGroupCommand((StrategiesGroup) urnspec.getGrlspec().getGroups().get(0));
		cs.execute(cmd);
	}

	private IURNContainerRef addCompRefIfAbsent(IURNContainer def) {
		IURNContainerRef comp = (IURNContainerRef) htComponentRefs.get(def);
		if (comp == null && def!=null) {
			// create a new reference
			comp = (IURNContainerRef) ModelCreationFactory.getNewObject(urnspec, ComponentRef.class, ((Component)def).getKind().getValue(), htComponents.get(def));
			// outside for autolayout
			comp.setX(-1000);
			comp.setY(-1000);
			AddContainerRefCommand cmd = new AddContainerRefCommand(currentMap, comp);
			cs.execute(cmd);

			htComponentRefs.put(def, comp);
		}
		return comp;
	}

	protected void addMetaData(URNmodelElement pn, String name, String value) {
		Metadata data = (Metadata) ModelCreationFactory.getNewObject(urnspec, Metadata.class);
		data.setName(name);
		data.setValue(value);
		pn.getMetadata().add(data);

	}

	protected void cleanupComponentRefs() {
		cs.execute(new TrimEmptyNodeCommand(currentMap));
		
		for (Iterator iter = currentMap.getNodes().iterator(); iter.hasNext();) {
			PathNode pn = (PathNode) iter.next();
			if (pn instanceof AndFork) {
				pn.setContRef(((NodeConnection)pn.getPred().get(0)).getSource().getContRef());
			}
			else if (pn instanceof AndJoin) {
				pn.setContRef(((NodeConnection)pn.getSucc().get(0)).getTarget().getContRef());
			}
		}
		
		Vector toDelete = new Vector();
		for (Iterator iter = currentMap.getContRefs().iterator(); iter.hasNext();) {
			ComponentRef container = (ComponentRef) iter.next();
			if (container.getChildren().size()==0 && container.getNodes().size()==0) {
				toDelete.add(container);
			}
		}
		for (Iterator iter = toDelete.iterator(); iter.hasNext();) {
			ComponentRef container= (ComponentRef) iter.next();
			cs.execute(new DeleteComponentRefCommand(container));
		}
	}

	private void cleanupScenarios() {
		Vector startPoints = new Vector();
		for (Iterator iter = currentMap.getNodes().iterator(); iter.hasNext();) {
			PathNode pn = (PathNode) iter.next();
			if (pn instanceof StartPoint) {
				startPoints.add(pn);
			} else if (pn instanceof EndPoint) {
				ScenarioEndPoint sep = (ScenarioEndPoint) ModelCreationFactory.getNewObject(urnspec, ScenarioEndPoint.class);
				sep.setEndPoint((EndPoint) pn);
				sep.setScenarioDef(currentDestScenario);
				sep.setEnabled(true);
			}

		}
		Collections.sort(startPoints, new URNmodelElementIDComparator());

		for (Iterator iter = startPoints.iterator(); iter.hasNext();) {
			StartPoint pn = (StartPoint) iter.next();
			ScenarioStartPoint ssp = (ScenarioStartPoint) ModelCreationFactory.getNewObject(urnspec, ScenarioStartPoint.class);
			ssp.setStartPoint(pn);
			ssp.setScenarioDef(currentDestScenario);
			ssp.setEnabled(true);
		}

		CompoundCommand compound = new CompoundCommand();
		for (Iterator iter = urnspec.getUcmspec().getScenarioGroups().iterator(); iter.hasNext();) {
			ScenarioGroup g = (ScenarioGroup) iter.next();

			// if is in use, won't delete.
			DeleteStrategiesGroupCommand cmd = new DeleteStrategiesGroupCommand(g);
			if (cmd.canExecute())
				compound.add(cmd);
		}
		cs.execute(compound);
	}

	protected void cloneComponents(ScenarioDef scenario) {
		htComponents = new HashMap();

		for (Iterator iter = scenario.getGroup().getUcmspec().getUrnspec().getUrndef().getComponents().iterator(); iter.hasNext();) {
			Component c = (Component) iter.next();
			Component clone = (Component) EcoreUtil.copy(c);
			clone.setUrndefinition(urnspec.getUrndef());
			resetCloneId(clone);

			htComponents.put(c, clone);
		}
	}

	protected void cloneResponsibilities(ScenarioDef scenario) {
		htResponsibilities = new HashMap();

		for (Iterator iter = scenario.getGroup().getUcmspec().getUrnspec().getUrndef().getResponsibilities().iterator(); iter.hasNext();) {
			Responsibility r = (Responsibility) iter.next();
			Responsibility clone = (Responsibility) EcoreUtil.copy(r);
			clone.setUrndefinition(urnspec.getUrndef());

			resetCloneId(clone);

			htResponsibilities.put(r, clone);
		}

		// Responsibility condition = (Responsibility) ModelCreationFactory.getNewObject(urnspec, Responsibility.class);
		// condition.setName("Condition");
		// condition.setUrndefinition(urnspec.getUrndef());
		// htResponsibilities.put("Condition", condition);
	}

	protected void cloneScenario(ScenarioDef src) {
		int index = src.getGroup().getUcmspec().getScenarioGroups().indexOf(src.getGroup());

		ScenarioDef dest = (ScenarioDef) EcoreUtil.copy(src);
		resetCloneId(dest);
		dest.setGroup((ScenarioGroup) urnspec.getUcmspec().getScenarioGroups().get(index));

		dest.getStartPoints().clear();
		dest.getEndPoints().clear();
		dest.getInitializations().clear();
		dest.getIncludedScenarios().clear();
		dest.getPreconditions().clear();
		dest.getPostconditions().clear();

		for (Iterator iter = ScenarioUtils.getDefinedInitializations(src).iterator(); iter.hasNext();) {
			Initialization srcinit = (Initialization) iter.next();
			Initialization destinit = (Initialization) EcoreUtil.copy(srcinit);
			index = srcinit.getVariable().getUcmspec().getVariables().indexOf(srcinit.getVariable());
			destinit.setVariable((Variable) urnspec.getUcmspec().getVariables().get(index));
			destinit.setScenarioDef(dest);
		}

		currentDestScenario = dest;
	}


	protected void cloneScenarioDataModel(ScenarioDef scenario) {

		for (Iterator iter = scenario.getGroup().getUcmspec().getScenarioGroups().iterator(); iter.hasNext();) {
			ScenarioGroup c = (ScenarioGroup) iter.next();
			ScenarioGroup clone = (ScenarioGroup) EcoreUtil.copy(c);
			clone.setUcmspec(urnspec.getUcmspec());
			clone.getScenarios().clear();
			resetCloneId(clone);
		}

		for (Iterator iter = scenario.getGroup().getUcmspec().getEnumerationTypes().iterator(); iter.hasNext();) {
			EnumerationType c = (EnumerationType) iter.next();
			EnumerationType clone = (EnumerationType) EcoreUtil.copy(c);
			clone.setUcmspec(urnspec.getUcmspec());
			resetCloneId(clone);
		}
		for (Iterator iter = scenario.getGroup().getUcmspec().getVariables().iterator(); iter.hasNext();) {
			Variable c = (Variable) iter.next();
			Variable clone = (Variable) EcoreUtil.copy(c);
			clone.setUcmspec(urnspec.getUcmspec());
			resetCloneId(clone);
		}
		for (int i = 0; i < scenario.getGroup().getUcmspec().getVariables().size(); i++) {
			Variable src = (Variable) scenario.getGroup().getUcmspec().getVariables().get(i);
			Variable dest = (Variable) urnspec.getUcmspec().getVariables().get(i);

			EnumerationType srctype = src.getEnumerationType();
			if (srctype != null) {
				int index = srctype.getUcmspec().getEnumerationTypes().indexOf(srctype);
				dest.setEnumerationType((EnumerationType) dest.getUcmspec().getEnumerationTypes().get(index));
			}
		}
	}

	public void codeExecuted(TraversalVisit visit, String code) {
		// is null in scenario initializations
//		if (visit == null)
//			System.out.println("Executed code: " + code);
//		else
//			System.out.println("Executed code: " + code + " in thread " + visit.getThreadID());

	}

	public void conditionEvaluated(TraversalVisit visit, Condition original_condition, boolean result) {
		String condition = original_condition==null || original_condition.getExpression()==null ?Boolean.toString(result):original_condition.getExpression();
		
		Condition cond = (Condition) ModelCreationFactory.getNewObject(urnspec, Condition.class);
		cond.setExpression(condition);

		// is null in scenario pre/post conditions
		if (visit == null) {
			//System.out.println("Condition (" + condition + ") evaluated to " + result);

			if (currentMap.getNodes().size() == 0)
				currentDestScenario.getPreconditions().add(cond);
			else
				currentDestScenario.getPostconditions().add(cond);
		} else {
			//System.out.println("Condition (" + condition + ") evaluated to " + result + " in thread " + visit.getThreadID());

			// don't show tautologies or ignored conditions (paths that were not taken)
			if (!ScenarioUtils.isEmptyCondition(condition) && result) {
				WaitingPlace wait = createWaitingPlace(visit);
				
				
				if (original_condition!=null && original_condition.getLabel()!=null && original_condition.getLabel().length()>0)
					wait.setName(original_condition.getLabel());
				else
					wait.setName((visit.getVisitedElement()).getName());
				
				wait.setContRef(addCompRefIfAbsent(visit.getParentComponentDef()));
				NodeConnection nc = (NodeConnection) wait.getSucc().get(0);
				nc.setCondition(cond);

				// RespRef respref = (RespRef) ModelCreationFactory.getNewObject(urnspec, RespRef.class);
				// respref.setLabel(null);
				//				
				// addMetaData(respref.getRespDef(), "condition", condition);
				// addMetaData(respref.getRespDef(), "value", Boolean.valueOf(result).toString());
				//					
				// extendPathAndInsert(visit, respref, true);

			}
		}
	}

	private RespRef createRespRef(TraversalVisit visit) {
		Responsibility def = null;
		if (visit.getVisitedElement() instanceof RespRef)
			def = (Responsibility) htResponsibilities.get(((RespRef) visit.getVisitedElement()).getRespDef());
		else {
			def = (Responsibility) htResponsibilities.get(visit.getVisitedElement().getClass());

			// we haven't seen an element of this type before. create a responsibility for it!
			if (def == null) {
				def = (Responsibility) ModelCreationFactory.getNewObject(urnspec, Responsibility.class);
				def.setName(visit.getVisitedElement().getClass().getInterfaces()[0].getSimpleName());
				def.setUrndefinition(urnspec.getUrndef());
				htResponsibilities.put(visit.getVisitedElement().getClass(), def);
			}
		}
		RespRef respref = (RespRef) ModelCreationFactory.getNewObject(urnspec, RespRef.class, 0, def);
		extendPathAndInsert(visit, respref, true);

		setComponentRef(respref, visit);
		return respref;
	}

	private Timer createTimer(TraversalVisit visit) {
		Timer timer = (Timer) ModelCreationFactory.getNewObject(urnspec, Timer.class);

		extendPathAndInsert(visit, timer, true);

		return timer;
	}
	private WaitingPlace createWaitingPlace(TraversalVisit visit) {
		WaitingPlace wait = (WaitingPlace) ModelCreationFactory.getNewObject(urnspec, WaitingPlace.class);

		extendPathAndInsert(visit, wait, true);

		return wait;
	}

	protected PathNode extendPath(int threadID, boolean fromEnd) {
		PathNode pn;

		if (fromEnd) {
			EndPoint end = (EndPoint) htThreadEnd.get(new Integer(threadID));
			ExtendPathCommand cmd = new ExtendPathCommand(currentMap, end, end.getX() + 100, end.getY());
			cs.execute(cmd);
			return end;
		} else {
			StartPoint start = (StartPoint) htThreadStart.get(new Integer(threadID));
			ExtendPathCommand cmd = new ExtendPathCommand(currentMap, start, start.getX() - 100, start.getY());
			cs.execute(cmd);
			return start;
		}
	}

	protected PathNode extendPath(TraversalVisit visit, boolean fromEnd) {
		return extendPath(visit.getThreadID(), fromEnd);
	}

	protected PathNode extendPathAndInsert(int threadID, PathNode pn, boolean fromEnd) {
		PathNode extremity = extendPath(threadID, fromEnd);
		EmptyPoint empty;
		if (fromEnd)
			empty = (EmptyPoint) ((NodeConnection) extremity.getPred().get(0)).getSource();
		else
			empty = (EmptyPoint) ((NodeConnection) extremity.getSucc().get(0)).getTarget();

		ReplaceEmptyPointCommand cmd2 = new ReplaceEmptyPointCommand(empty, pn);
		cs.execute(cmd2);
		
		return extremity;
	}

	protected PathNode extendPathAndInsert(TraversalVisit visit, PathNode pn, boolean fromEnd) {
		return extendPathAndInsert(visit.getThreadID(), pn, fromEnd);
	}

	public void newThreadStarted(TraversalVisit visit) {
		//System.out.println("Started thread " + visit.getThreadID() + " at: " + visit.getVisitedElement());

		CreatePathCommand cmd = new CreatePathCommand(currentMap, 0, visit.getThreadID() * 100);
		cs.execute(cmd);
		cmd.getStart().setName(((PathNode)visit.getVisitedElement()).getName());
		setComponentRef(cmd.getStart(), visit);
		
		htThreadStart.put(new Integer(visit.getThreadID()), cmd.getStart());
		htThreadEnd.put(new Integer(visit.getThreadID()), cmd.getEnd());
	}

	public void pathNodeAborted(TraversalVisit visit) {
		//System.out.println("Aborted node in thread " + visit.getThreadID() + ": " + visit.getVisitedElement());

	}

	public void pathNodeAttempted(TraversalVisit visit) {
		//System.out.println("Node is being attempted in thread " + visit.getThreadID() + ": " + visit.getVisitedElement());

	}
	
	public void pathNodeBlocked(TraversalVisit visit) {
		//System.out.println("Node could not continue and was blocked in thread " + visit.getThreadID() + ": " + visit.getVisitedElement());

		// TODO: no, we want to add a waiting place if we block and continue because of condition instead of path.
		// PathNode n = (PathNode) visit.getVisitedElement();
		// if (n instanceof WaitingPlace) {
		//			
		// createWaitingPlace(visit);
		// }

	}

	public void pathNodeUnblocked(TraversalVisit visit) {
		//System.out.println("Node was unblocked in thread " + visit.getThreadID() + ": " + visit.getVisitedElement());

		lastUnblocked = visit;
		
	}

	public void pathNodeVisited(TraversalVisit visit) {
		//System.out.println("Node was traversed successfully in thread " + visit.getThreadID() + ": " + visit.getVisitedElement());

		PathNode n = visit.getVisitedElement();
		if (!(n instanceof EmptyPoint || n instanceof DirectionArrow || n instanceof OrJoin || n instanceof OrFork || n instanceof Connect
				|| n instanceof StartPoint || n instanceof EndPoint || n instanceof Stub || n instanceof AndFork || n instanceof AndJoin || n instanceof WaitingPlace)) {

			createRespRef(visit);
		} 
		
		if (n instanceof EndPoint)
		{
			PathNode pn = (PathNode) htThreadEnd.get(new Integer(visit.getThreadID()));
			pn.setName(n.getName());

			setComponentRef(pn, visit);

		}

	}

	private void resetCloneId(URNmodelElement clone) {
		String name = clone.getName();
		URNNamingHelper.setElementNameAndID(urnspec, clone);
		clone.setName(name);
	}

	private void saveModel() {
		try {
			UrnModelManager manager = new UrnModelManager();

			IPath path = ((FileEditorInput) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().getEditorInput()).getFile()
					.getFullPath().removeLastSegments(1).append("__msctemp.jucm");
			manager.createURNspec(path, urnspec);
			manager.save(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		cs.execute(new MakeWellFormedCommand(urnspec));
		
		try {
			UrnModelManager manager = new UrnModelManager();

			IPath path = ((FileEditorInput) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().getEditorInput()).getFile()
					.getFullPath().removeLastSegments(1).append("__msctemp2.jucm");
			manager.createURNspec(path, urnspec);
			manager.save(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void setComponentRef(PathNode pn, TraversalVisit visit) {
		if (visit.getParentComponentRef() != null) {
			IURNContainerRef comp = addCompRefIfAbsent(visit.getParentComponentDef());
			pn.setContRef(comp);
			
			// bind parents. 
			for (int i=1;i<visit.getParentComponentRefs().size();i++)
			{
				IURNContainer origparent = ((ComponentRef) visit.getParentComponentRefs().get(i)).getContDef();
				IURNContainer origchild = ((ComponentRef) visit.getParentComponentRefs().get(i-1)).getContDef();
				IURNContainerRef parent = addCompRefIfAbsent(origparent);
				IURNContainerRef child = addCompRefIfAbsent(origchild);
				if (parent!=child)
					child.setParent(parent);
			}

		}
	}

	public void threadDied(int threadId) {
		//System.out.println("Thread " + threadId + " died");

	}

	public void threadsMerged(List oldThreadIDs, int newThreadID) {
	//	System.out.println("Threads " + ArrayAndListUtils.listToString(oldThreadIDs, ",") + " were merged into thread " + newThreadID);

		AndJoin andjoin = (AndJoin) ModelCreationFactory.getNewObject(urnspec, AndJoin.class);
		StartPoint sp = (StartPoint) extendPathAndInsert(newThreadID, andjoin, false);
		
		if (lastUnblocked!=null) {
			andjoin.setName(lastUnblocked.getVisitedElement().getName());
			NodeLabel lbl = (NodeLabel) ModelCreationFactory.getNewObject(urnspec, NodeLabel.class);
			lbl.setNode(andjoin);
		}
		for (int i = 0; i < oldThreadIDs.size(); i++) {
			Integer threadID = (Integer) oldThreadIDs.get(i);
			EndPoint ep = (EndPoint) htThreadEnd.get(threadID);

			Command cmd;
			if (i == 0) {
				// reuse existing path.
				cmd = new MergeStartEndCommand(currentMap, sp, ep, sp.getX(), sp.getY());
			} else {
				cmd = new AttachBranchCommand(ep, andjoin);
			}
			cs.execute(cmd);
		}

		htThreadStart.put(new Integer(newThreadID), andjoin);

	}

	public void threadSplit(int oldThreadID, List newThreadIDs) {
		//System.out.println("Thread " + oldThreadID + " was split into threads " + ArrayAndListUtils.listToString(newThreadIDs, ","));

		AndFork andfork = (AndFork) ModelCreationFactory.getNewObject(urnspec, AndFork.class);
		EndPoint ep = (EndPoint) extendPathAndInsert(oldThreadID, andfork, true);

		for (int i = 0; i < newThreadIDs.size(); i++) {
			Integer threadID = (Integer) newThreadIDs.get(i);
			StartPoint sp = (StartPoint) htThreadStart.get(threadID);

			Command cmd;
			if (i == 0) {
				// reuse existing path.
				cmd = new MergeStartEndCommand(currentMap, sp, ep, ep.getX(), ep.getY());
			} else {
				cmd = new AttachBranchCommand(sp, andfork);
			}
			cs.execute(cmd);
		}

		htThreadEnd.put(new Integer(oldThreadID), andfork);

	}


	public void timerTimeout(TraversalVisit visit) {
		//System.out.println("Timer timeout in thread " + visit.getThreadID() + ": " + visit.getVisitedElement());
		if (visit.getVisitedElement() instanceof Timer) {
			Timer timer = createTimer(visit);
			timer.setName(visit.getVisitedElement().getName() + " Timeout");
			addMetaData(timer, "ID", visit.getVisitedElement().getId());
			Condition cond = (Condition) ModelCreationFactory.getNewObject(urnspec, Condition.class);
			cond.setExpression("true");
			((NodeConnection)timer.getSucc().get(0)).setCondition(cond);
		}

	}

	public void traversalEnded(UcmEnvironment env, ScenarioDef scenario) {
		//System.out.println("Traversal ended: " + scenario.toString());

		cleanupScenarios();

		// TODO: get rid of unused definitions.

		cleanupComponentRefs();		
		saveModel();

	}

	public void traversalStarted(UcmEnvironment env, ScenarioDef scenario) {
		//System.out.println("Traversal started: " + scenario.toString());

		htThreadEnd = new HashMap();
		htThreadStart = new HashMap();
		htComponentRefs = new HashMap();

		if (htResponsibilities == null) {
			cloneResponsibilities(scenario);
		}

		if (htComponents == null) {
			cloneComponents(scenario);
		}

		currentSrcScenario = scenario;

		// first scenario
		if (htScenarioToMap.size() == 0) {
			htScenarioToMap.put(scenario, (UCMmap) urnspec.getUrndef().getSpecDiagrams().get(0));

			cloneScenarioDataModel(scenario);

		} else if (!htScenarioToMap.containsKey(scenario)) { // new scenario
			CreateMapCommand cmd = new CreateMapCommand(urnspec);
			cs.execute(cmd);

			htScenarioToMap.put(scenario, cmd.getMap());
		}

		cloneScenario(scenario);

		currentMap = (UCMmap) htScenarioToMap.get(scenario);
	}

}
