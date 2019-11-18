package seg.jUCMNav.importexport;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import fm.Feature;
import fm.FeatureDiagram;
import grl.Actor;
import grl.ActorRef;
import grl.Belief;
import grl.Contribution;
import grl.Decomposition;
import grl.Dependency;
import grl.ElementLink;
import grl.GRLGraph;
import grl.IntentionalElement;
import grl.IntentionalElementRef;
import grl.IntentionalElementType;
import grl.kpimodel.Indicator;
import seg.jUCMNav.extensionpoints.IURNExport;
import seg.jUCMNav.model.util.MetadataHelper;
import seg.jUCMNav.views.wizards.importexport.ExportWizard;
import urn.URNspec;
import urncore.IURNDiagram;
import urncore.IURNNode;

/**
 * This class export the URN model into SymPy function
 * 
 * @author Yuxuan Fan and Amal Anda
 *
 */
public class ExportGRLMathS extends GRLMathBase {

	/**
	 * 
	 * @param element
	 * @return
	 * @throws IOException
	 */
	StringBuffer writeLink(IntentionalElement element) throws IOException {

		StringBuffer formula = new StringBuffer();
		StringBuffer decomFor = new StringBuffer();
		StringBuffer conFor = new StringBuffer();
		StringBuffer depenFor = new StringBuffer();
		List<String> StrEle = new ArrayList<String>();// the element's string

		Map<String, List<IntentionalElement>> eleMap = new HashMap<String, List<IntentionalElement>>();
		List<IntentionalElement> decomList = new ArrayList<IntentionalElement>();
		List<IntentionalElement> depenList = new ArrayList<IntentionalElement>();
		List<IntentionalElement> conList = new ArrayList<IntentionalElement>();
		List<ElementLink> conLink = new ArrayList<ElementLink>();
		List<IntentionalElement> srcList = new ArrayList<IntentionalElement>();

		for (Iterator it = element.getLinksDest().iterator(); it.hasNext();) {
			ElementLink scrLink = (ElementLink) it.next();
			IntentionalElement srcElement = (IntentionalElement) (scrLink.getSrc());
			srcList.add(srcElement);
			StrEle.add(FeatureExport.modifyName(srcElement.getName()));

			if (scrLink instanceof Decomposition) {
				decomList.add(srcElement);
			}
			if (scrLink instanceof Dependency) {
				depenList.add(srcElement);
				elementSet.add("'" + FeatureExport.modifyName(element.getName()) + "'");
			}
			if (scrLink instanceof Contribution) {
				conList.add(srcElement);
				conLink.add(scrLink);
			}
			eleMap.put("Decomposition", decomList);
			eleMap.put("Dependency", depenList);
			eleMap.put("Contribution", conList);
		} // sequence- first decomposition; second contribution; third dependency
		String funcTpye = " ";
		if (!decomList.isEmpty()) {
			if (element.getDecompositionType().getName() == "And")
				funcTpye = MIN;
			if (element.getDecompositionType().getName() == "Or")
				funcTpye = MAX;
			if (element.getDecompositionType().getName() == "Xor")
				funcTpye = MAX;

			decomFor.append(writeDecomMaxMin(decomList, funcTpye));
			formula = decomFor;
		}

		if (!conList.isEmpty()) {
			conFor.append(MAX);
			conFor.append(LEFT_BRACKET);
			conFor.append("0.0");
			conFor.append(COMMA);
			conFor.append(MIN);
			conFor.append(LEFT_BRACKET);
			conFor.append("100.0");
			conFor.append(COMMA);
			conFor.append(LEFT_BRACKET);
			List<String> conTimesList = new ArrayList<String>();
			for (int i = 0; i < conLink.size(); i++) {
				String conTimes = new String();
				conTimes = Integer.toString(((Contribution) conLink.get(i)).getQuantitativeContribution()) + TIMES
						+ FeatureExport.modifyName(conList.get(i).getName());
				conTimesList.add(conTimes);
			}
			if (!decomList.isEmpty()) {
				conTimesList.add(decomFor + TIMES + "100.0");
			}

			String joined = String.join("+", conTimesList);
			conFor.append(joined);
			conFor.append(RIGHT_BRACKET);
			conFor.append(DIVIDE);
			conFor.append("100.0");
			conFor.append(RIGHT_BRACKET);
			conFor.append(RIGHT_BRACKET);

			formula = conFor;
		}
		if (!depenList.isEmpty()) {
			depenFor.append(writeDepenMaxMin(depenList, formula, element));
			formula = depenFor;
		}

		for (Iterator it = srcList.iterator(); it.hasNext();) {
			IntentionalElement subElement = (IntentionalElement) it.next();
			// if sub element is not the leaf.
			StringBuffer subFor = new StringBuffer();
			if (subElement.getLinksDest().size() != 0) {
				if (elementMap.get(subElement) == null) {
					subFor = writeLink(subElement);
				} else {
					subFor = elementMap.get(subElement);
				}
				// if the element is a feature/task and has a formula we separate
				if (subElement.getType() == IntentionalElementType.TASK_LITERAL && subFor.toString().contains(LEFT_BRACKET)) {
					continue;
				}
				if (subElement.getType().getName().compareTo("Indicator") != 0 && !FeatureExport.IsItLeaf(subElement))
					formula = new StringBuffer(formula.toString().replaceAll(FeatureExport.modifyName(subElement.getName()), subFor.toString()));
			}
			// if the element is indicator
			if (subElement.getType().getName().compareTo("Indicator") == 0) {
				StringBuffer indicatorFor = new StringBuffer();
				if (elementMap.get(subElement) == null) {
					// System.out.println(element.getName() + "Went To indicator from writeLink
					// where no formula");
					// indicatorFor = indicatorFor(subElement);
					writeIndicatorFunction(element, indicatorFor);
				} // else { // replace indicator name with formula
				// indicatorFor = eleForMap.get(subEle);
				// formula = new StringBuffer(
				// formula.toString().replaceAll(FeatureExport.modifyName(subEle.getName()),
				// indicatorFor.toString()));
				// }
			}
		}
		return formula;
	}

	/**
	 * If none of the top-level intentional elements has a weight, then these
	 * top-level intentional elements should be weighted equally. we assume only the
	 * top-level elements have weight
	 * 
	 * @param urn
	 * @throws IOException
	 */

	void writeActor(URNspec urn) throws IOException {
		actorMap = new HashMap<Actor, StringBuffer>();
		StringBuffer formula;
		StringBuffer function;
		int quantSum = 0;
		int dNum = 100;
		for (Iterator it = urn.getGrlspec().getActors().iterator(); it.hasNext();) {
			Actor actor = (Actor) it.next();
			function = new StringBuffer();
			function.append(FeatureExport.modifyName(actor.getName()));
			formula = new StringBuffer(); // the part after =
			quantSum = 0;
			dNum = 100;
			boolean hasElementInActor = true;
			List<IntentionalElement> elementList = new ArrayList<IntentionalElement>(); // the elements in the actor
			List<Integer> quantList = new ArrayList<Integer>();
			List<String> actorTimesWeight = new ArrayList<String>();
			// add all the elements inside the actor to a list
			for (Iterator itAct = actor.getContRefs().iterator(); itAct.hasNext();) {
				ActorRef actorRef = (ActorRef) itAct.next();
				Iterator itIEref = actorRef.getNodes().iterator();
				if (!itIEref.hasNext()) {
					hasElementInActor = false;
				} else {
					for (; itIEref.hasNext();) {
						IURNNode node = (IURNNode) itIEref.next();
						// skip for Belief
						if (node instanceof Belief) {
							continue;
						}

						IntentionalElement element = (IntentionalElement) ((IntentionalElementRef) node).getDef();
						elementList.add(element);
						int elementImportance = element.getImportanceQuantitative();
						quantList.add(elementImportance);
						quantSum += elementImportance;
					}
				}
			}

			// there are no weighted elements in actor
			if (quantSum == 0 && hasElementInActor == true) {
				for (int i = 0; i < elementList.size(); i++) {
					IntentionalElement element = (IntentionalElement) (elementList.get(i));
					StringBuffer elementFormula = new StringBuffer();
					elementFormula.append(LEFT_BRACKET);
					elementFormula.append(elementMap.get(element));
					elementFormula.append(RIGHT_BRACKET);
					if (element.getLinksSrc().size() == 0) {
						actorTimesWeight.add(elementFormula + TIMES + "100.0");
						quantSum += 100;
					} else {
						// give the weight to top-level elements
						IntentionalElement srcElement = (IntentionalElement) (((ElementLink) (element.getLinksSrc().get(0)))
								.getDest());
						if (!elementList.contains(srcElement)) {
							actorTimesWeight.add(elementFormula + TIMES + "100.0");
							quantSum += 100;
						}
					}
				} // for
			} // if(sumQua==0)
			if (quantSum > 0) {
				// there are some elements weighted
				for (int i = 0; i < elementList.size(); i++) {
					IntentionalElement element = (IntentionalElement) (elementList.get(i));
					if (element.getImportanceQuantitative() == 0) {
						continue;
					}
					actorTimesWeight.add(elementMap.get(element) + TIMES + Integer.toString(element.getImportanceQuantitative()));
				}
			}
			if (!hasElementInActor)
				formula.append("0");
			else {
				formula.append(LEFT_BRACKET);
				String joined = String.join("+", actorTimesWeight);
				formula.append(joined);
				formula.append(RIGHT_BRACKET);
				formula.append(DIVIDE);
				formula.append(Integer.toString(Math.max(quantSum, dNum)));
			}
			function.append(EQUALS);
			function.append(formula);

			write("# " + FeatureExport.modifyName(actor.getName()) + " Actor function\n");
			write(function.toString());
			write("\n");
			actorMap.put(actor, formula);
		}
	}

	/**
	 * Writes the separated indicators to SymPy
	 *
	 * @throws IOException
	 */
	void writeIndependentIndicators(Iterator iterator, Set<String> list) throws IOException {
		String formula = new String();
		while (iterator.hasNext()) {
			IntentionalElement IndicatorV = (IntentionalElement) iterator.next();
			if (IndicatorV.getType().getName().compareTo("Indicator") == 0) {
				list.add("\t\t'" + FeatureExport.modifyName(IndicatorV.getName()) + "'" + COLON + "'"
						+ elementMap.get(IndicatorV).toString() + "'");
			}
		}
	}

	/**
	 * Writes the separated function to SymPy
	 *
	 * @throws IOException
	 */
	void writeSeparatedElements(Set<String> list) throws IOException {

		if (!splitElements.isEmpty()) {
			String formula;
			for (IntentionalElement e : splitElements) {
				formula = new String(elementMap.get(e));
				list.add("\t\t'" + FeatureExport.modifyName(e.getName()) + "'" + COLON + "'" + formula + "'");
			}
		}
	}
}
