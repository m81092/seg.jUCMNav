package seg.jUCMNav.importexport;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 */
public class ExportGRLMath implements IURNExport {
	private String GRLname;
	FeatureToMath FeatureExport = new FeatureToMath();
	private FileOutputStream fos;
	// declaring constants
	public static final String LEFT_BRACKET = "(";
	public static final String RIGHT_BRACKET = ")";
	public static final String COMMA = " , ";
	public static final String EQUALS = " = ";
	public static final String SYMBOLS = "symbols";
	public static final String TIMES = "*";
	public static final String DIVIDE = " / ";
	public static final String PLUS = " + ";
	public static final String MINUS = " - ";
	public static final String MULTI = " * ";
	public static final String SPACE = " ";
	public static final String KPI = "KPI";

	// store elements and the functions
	private Map<IntentionalElement, StringBuffer> elementMap;
	// store actors and the functions
	private Map<Actor, StringBuffer> actorMap;
	private StringBuffer modelFormula;
	private Set<String> elementSet = new HashSet<String>();

	@Override
	public void export(URNspec urn, HashMap mapDiagrams, FileOutputStream fos) throws InvocationTargetException {
		// not used
	}

	@Override
	public void export(URNspec urn, HashMap mapDiagrams, String filename) throws InvocationTargetException {

		try {
			fos = new FileOutputStream(filename);
			// to run the functions
			writeHead(urn);
			boolean GRLFound = false;
			for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
				IntentionalElement element = (IntentionalElement) it.next();
				if (element.getType().toString().contains("Softgoal") || element.getType().toString().contains("Goal")) {
					GRLFound = true;
					break;
				}
			}

			if (GRLFound) {
				for (Iterator iter = mapDiagrams.keySet().iterator(); iter.hasNext();) {
					IURNDiagram diagram = (IURNDiagram) iter.next();
					if ((diagram instanceof GRLGraph) && !(diagram instanceof FeatureDiagram)) {
						String diagramName = ExportWizard.getDiagramName(diagram);
						String purName = diagramName.substring(diagramName.lastIndexOf("-") + 1);
						// System.out.println("Diagram information="+diagram.toString()+"
						// name="+diagramName+" PureNam= "+purName);
						GRLname = purName;
					}
				}
				writeFormula(urn);
				writeActor(urn);
				// writeIndicator(urn);
				writeModel(urn);
				writeTranslation(urn);
			}
			FeatureExport.export(urn, mapDiagrams, filename);
		} catch (Exception e) {
			throw new InvocationTargetException(e);
		} finally {
			// close the stream
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	/**
	 * Returns the indicator values based on the metadata.
	 * 
	 * @param element the element to fetch its metadata
	 */
	private String[] getIndicatorValues(IntentionalElement element) throws IOException {
		String value = MetadataHelper.getMetaData(element, KPI);
		// check if the value contains value in the form of (T, TH, W, Unit) values
		if (value != null && value.matches("[sS]{1}(:)[0-9]{1,3}(,|;)[0-9]{1,3}(,|;)[0-9]{1,3}(,|;)[a-zA-Z]*")) {
			String indicatorValues[] = value.split("[,;:]");
			return indicatorValues;
		} else if (value != null && value.matches("[fF]{1}(:).*")) {
			String indicatorValues[] = value.split("[:]");
			return indicatorValues;
		} else if (value != null && value.matches("[bB]{1}(:).*")) {
			String indicatorValues[] = value.split("[:]");
			return indicatorValues;
		} else {
			return null;
		}
	}

	/**
	 * Write the string to the file output stream.
	 * 
	 * @param s the string to write
	 * @throws IOException
	 */
	public void write(String s) throws IOException {
		if (s != null && s.length() > 0) {
			fos.write(s.getBytes());
		}
	}

	/**
	 * Writes the head of the SymPy file
	 * 
	 * @throws IOException
	 */
	private void writeHead(URNspec urn) throws IOException {
		write("from MathTo import * \n" + "from sympy import *\n");
		write("import sys\n");
		write("import os");
		write("\n");
		write("\n");
		write("# Creating a folder using Model name\n");
		write("ModelName= '");
		write(FeatureExport.modifyName(urn.getName()) + "'\n");
		write("if not os.path.exists(ModelName):\n");
		write("\tos.makedirs(ModelName)\n");
		write("os.chdir(ModelName)");
		write("\n");
		write("\n");
	}

	/**
	 * Check if the element is part of GRL diagrams(added by Amal Ahmed Anda)
	 */
	private boolean isGRLElement(IntentionalElement element) {
		for (Iterator it = element.getRefs().iterator(); it.hasNext();) {
			IntentionalElementRef ref = (IntentionalElementRef) it.next();
			if (!(ref.getDiagram() instanceof FeatureDiagram))
				return true;
		}
		return false;
	}

	/**
	 * Writes indicator function to the file
	 * 
	 * @param element
	 * @param elementFormula
	 * @throws IOException
	 */
	private void writeIndicatorFunction(IntentionalElement element, StringBuffer elementFormula)
			throws IOException {
		write("# Indicator function\n");
		String[] indicatorValues = getIndicatorValues(element);
		// if worst and target values are equal
		if (indicatorValues[0].equalsIgnoreCase("S") && indicatorValues[1].equals(indicatorValues[3])) {
			// TODO: need correct formula here
			elementFormula.append(modifyName(element.getName()));
		} else if (indicatorValues[0].equalsIgnoreCase("B")) {
			elementFormula.append(LEFT_BRACKET);
			//elementFormula.append(SPACE);
			elementFormula.append("100");
			elementFormula.append(SPACE);
			elementFormula.append("if");
			elementFormula.append(SPACE);
			elementFormula.append(indicatorValues[1]);
			elementFormula.append(SPACE);
			elementFormula.append("else");
			elementFormula.append(SPACE);
			elementFormula.append("0");
			elementFormula.append(RIGHT_BRACKET);
		} else if (indicatorValues[0].equalsIgnoreCase("F")) {
			indicatorValues[1] = indicatorValues[1].replaceAll("current", modifyName(element.getName()));
			elementFormula.append(indicatorValues[1]);
		} else {
			StringBuffer indicatorFor = indicatorFor(indicatorValues, element.getName());
			elementFormula.append(indicatorFor);
		}
		write(FeatureExport.modifyName(element.getName()) + EQUALS);
		write(elementFormula.toString());
		write("\n");
	}

	/**
	 * Create formulas for each element
	 * 
	 * @param urn
	 * @throws IOException
	 */
	private void writeFormula(URNspec urn) throws IOException {

		elementMap = new HashMap<IntentionalElement, StringBuffer>();
		StringBuffer elementFormula;
		StringBuffer function;
		// initialize all the symbols
		write("#initalize all the variables\n");
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (isGRLElement(element)) {
				StringBuffer variable = new StringBuffer();
				variable.append(FeatureExport.modifyName(element.getName()));
				variable.append(EQUALS);
				variable.append("Symbol");
				variable.append(LEFT_BRACKET);
				variable.append("'");
				variable.append(FeatureExport.modifyName(element.getName()));
				variable.append("'");
				variable.append(RIGHT_BRACKET);
				write(variable.toString());
				write("\n");
			}
		}

		// iterate all the leaf elements (for now checking just indicators)
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (isGRLElement(element)) {
				elementFormula = new StringBuffer();
				function = new StringBuffer();
				function.append(FeatureExport.modifyName(element.getName()));
				// if the element is leaf element
				if (element.getLinksDest().size() == 0) {
					if (element.getType().getName().compareTo("Indicator") == 0) {
						if (getIndicatorValues(element) != null) {
							writeIndicatorFunction(element, elementFormula);
							function.append(EQUALS);
							function.append(elementFormula);
						} else {
							// TODO: if we don't provide metadata
						}
					} else {
						elementFormula.append(FeatureExport.modifyName(element.getName()));
					}
					elementSet.add("'" + FeatureExport.modifyName(element.getName()) + "'");
					elementMap.put(element, elementFormula);
				}
			}
		}
		write("# non-leaf element functions\n");
		// checking the non leaf elements and adding the link
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (isGRLElement(element)) {
				elementFormula = new StringBuffer();
				function = new StringBuffer();
				function.append(FeatureExport.modifyName(element.getName()));

				if (element.getLinksDest().size() != 0) {
					elementFormula.append(writeLink(element));
					function.append(EQUALS);
					function.append(elementFormula);
					write(function.toString());
					write("\n");
					elementMap.put(element, elementFormula);
				}
			}
		}
	}

	/**
	 * 
	 * 
	 * @param element
	 * @throws IOException
	 */
	private StringBuffer writeLink(IntentionalElement element) throws IOException {

		StringBuffer formula = new StringBuffer();
		StringBuffer decomFor = new StringBuffer();
		StringBuffer conFor = new StringBuffer();
		StringBuffer depenFor = new StringBuffer();
		List<String> StrEle = new ArrayList<String>();// the elements' str

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
		} // for first decomposition; second contribution; third dependency
		String funcTpye = " ";
		if (!decomList.isEmpty()) {
			if (element.getDecompositionType().getName() == "And")
				funcTpye = "Min";
			if (element.getDecompositionType().getName() == "Or")
				funcTpye = "Max";
			if (element.getDecompositionType().getName() == "Xor")
				funcTpye = "Max";

			decomFor.append(writeDecomMaxMin(decomList, funcTpye));
			formula = decomFor;
		}

		if (!conList.isEmpty()) {
			conFor.append("Max");
			conFor.append(LEFT_BRACKET);
			conFor.append("0.0");
			conFor.append(COMMA);
			conFor.append("Min");
			conFor.append(LEFT_BRACKET);
			conFor.append("100.0");
			conFor.append(COMMA);
			conFor.append(LEFT_BRACKET);
			List<String> conTimesList = new ArrayList<String>();
			for (int i = 0; i < conLink.size(); i++) {
				//// System.out.println("contributuin ele:" + conList.get(i).getName());
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
		// System.out.println(formula);
		for (Iterator it = srcList.iterator(); it.hasNext();) {
			IntentionalElement subEle = (IntentionalElement) it.next();
			// if sub element is not the leaf.
			StringBuffer subFor = new StringBuffer();
			if (subEle.getLinksDest().size() != 0) {
				if (elementMap.get(subEle) == null) {
					subFor = writeLink(subEle);
				} else {
					// System.out.println("you have subfor!");
					subFor = elementMap.get(subEle);
				}
				formula = new StringBuffer(
						formula.toString().replaceAll(FeatureExport.modifyName(subEle.getName()), subFor.toString()));
			}
			// if the element is indicator
			if (subEle.getType().getName().compareTo("Indicator") == 0) {
				StringBuffer indicatorFor = new StringBuffer();
				if (elementMap.get(subEle) == null) {
					if (getIndicatorValues(subEle) != null) {
						String[] indicatorValues = getIndicatorValues(subEle);
						indicatorFor = indicatorFor(indicatorValues, subEle.getName());
					}
				} else {
					indicatorFor = elementMap.get(subEle);
					formula = new StringBuffer(
							formula.toString().replaceAll(modifyName(subEle.getName()), indicatorFor.toString()));
				}
			}
		}
		return formula;
	}

	private StringBuffer writeDecomMaxMin(List<IntentionalElement> list, String func) throws IOException {
		StringBuffer formula = new StringBuffer();
		Stack<StringBuffer> st = new Stack<StringBuffer>();
		if (list.size() == 1) {
			formula.append(FeatureExport.modifyName(list.get(0).getName()));
		} else if (list.size() == 2) {
			formula.append(func);
			formula.append(LEFT_BRACKET);
			formula.append(FeatureExport.modifyName(FeatureExport.modifyName(list.get(0).getName())));
			formula.append(COMMA);
			formula.append(FeatureExport.modifyName(FeatureExport.modifyName(list.get(1).getName())));
			formula.append(RIGHT_BRACKET);
		} else if (list.size() > 2) {

			for (int i = 0; i < list.size(); i++) {
				StringBuffer subfo = new StringBuffer(FeatureExport.modifyName(list.get(i).getName()));
				st.add(subfo);
			}
			formula.append(FeatureExport.MaxmaxFormat(st, func));
		}
		return formula;
	}

	private StringBuffer writeDepenMaxMin(List<IntentionalElement> list, StringBuffer func, IntentionalElement element)
			throws IOException {

		StringBuffer formula = new StringBuffer();
		Stack<StringBuffer> st = new Stack<StringBuffer>();
		if (func.length() == 0) {
			StringBuffer eleSt = new StringBuffer(FeatureExport.modifyName(element.getName()));
			st.add(eleSt);
		} else {
			st.add(func);
		}
		for (int i = 0; i < list.size(); i++) {
			StringBuffer subfo = new StringBuffer(FeatureExport.modifyName(list.get(i).getName()));
			st.add(subfo);
		}
		formula.append(FeatureExport.MaxmaxFormat(st, "Min"));
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

	private void writeActor(URNspec urn) throws IOException {
		actorMap = new HashMap<Actor, StringBuffer>();
		StringBuffer formula;
		StringBuffer function;
		int sumQua = 0;
		int dNum = 100;
		for (Iterator it = urn.getGrlspec().getActors().iterator(); it.hasNext();) {
			Actor actor = (Actor) it.next();
			function = new StringBuffer();
			function.append(FeatureExport.modifyName(actor.getName()));
			formula = new StringBuffer();// the part after =
			sumQua = 0;
			dNum = 100;
			boolean hasEleInActor = true;
			List<IntentionalElement> eleList = new ArrayList<IntentionalElement>();// the elements in the actor
			List<Integer> quaList = new ArrayList<Integer>();
			List<String> actorTimesQua = new ArrayList<String>();
			for (Iterator itAct = actor.getContRefs().iterator(); itAct.hasNext();) {
				ActorRef actorRef = (ActorRef) itAct.next();
				Iterator itIEref = actorRef.getNodes().iterator();
				if (!itIEref.hasNext()) {
					hasEleInActor = false;
					System.out.println("NOOOOOOOElement");
				} else {
					hasEleInActor = true;
					for (; itIEref.hasNext();) {
						IURNNode node = (IURNNode) itIEref.next();
						if (node instanceof Belief) {
							continue;
						}

						IntentionalElement ele = (IntentionalElement) ((IntentionalElementRef) node).getDef();
						eleList.add(ele);
						int eleQua = ele.getImportanceQuantitative();
						System.out.println("Element haaaaaaaaaaas wait=" + ele.getName() + " Q= " + ele.getImportanceQuantitative());
						quaList.add(eleQua);
						sumQua += eleQua;
					}
				}
			}

			if (sumQua == 0 && hasEleInActor == true) {
				// there are no weighted elements in actor
				System.out.println("sum ================================== 0 ");
				for (int i = 0; i < eleList.size(); i++) {
					IntentionalElement ele = (IntentionalElement) (eleList.get(i));
					StringBuffer eleFormula = new StringBuffer();
					eleFormula.append(LEFT_BRACKET);
					eleFormula.append(elementMap.get(ele));
					eleFormula.append(RIGHT_BRACKET);
					if (ele.getLinksSrc().size() == 0) {

						actorTimesQua.add(eleFormula + TIMES + "100.0");
						System.out.println(
								"Element haaaaaaaaaaas wait in first sum=" + ele.getName() + " Q=" + ele.getImportanceQuantitative());
						sumQua += 100;
					} else {
						// give the weight to top-level elements;
						System.out.println("give the weight to top-level elements");
						IntentionalElement srcElement = (IntentionalElement) (((ElementLink) (ele.getLinksSrc().get(0))).getDest());

						if (eleList.contains(srcElement) == false) {
							actorTimesQua.add(eleFormula + TIMES + "100.0");
							System.out.println("Element haaaaaaaaaaas wait in thae last sum=" + ele.getName() + " Q="
									+ ele.getImportanceQuantitative());
							sumQua += 100;
						}
					}
				} // for
			} // if(sumQua==0)
			if (sumQua > 0) {
				// there are some elements weighted
				// System.out.println("sum! = 0 ");
				System.out.println("there are some elements weighted" + sumQua);
				for (int i = 0; i < eleList.size(); i++) {
					IntentionalElement ele = (IntentionalElement) (eleList.get(i));
					if (ele.getImportanceQuantitative() == 0) {
						continue;
					}
					// actorTimesQua.add(eleForMap.get(ele) + Times + "800.0");

					actorTimesQua.add(elementMap.get(ele) + TIMES + Integer.toString(ele.getImportanceQuantitative()));
				}
			}
			if (!hasEleInActor)
				formula.append("0");
			else {
				formula.append(LEFT_BRACKET);
				System.out.println(
						"Actoooooooooooooooooooor actortimequantity: " + actorTimesQua + " Sum=" + sumQua + " dNum=" + dNum);
				String joined = String.join("+", actorTimesQua);
				System.out.println(joined);
				formula.append(joined);
				formula.append(RIGHT_BRACKET);
				formula.append(DIVIDE);

				formula.append(Integer.toString(Math.max(sumQua, dNum)));

			}
			function.append(EQUALS);
			function.append(formula);
			write("# Actor function\n");
			System.out.println("Actoooooooooooooooooooor: " + function.toString());
			write(function.toString());
			write("\n");
			actorMap.put(actor, formula);
		}
	}

	/**
	 * If there is no actor in the model, then it would be as if there was one big
	 * actor with weight 100 that contained everything.
	 * 
	 * If there are actors but they have no weight, then these actors should be
	 * weighted equally.
	 * 
	 * @param urn
	 * @throws IOException
	 */
	private void writeModel(URNspec urn) throws IOException {
		modelFormula = new StringBuffer();
		StringBuffer function = new StringBuffer();
		List<Actor> actorList = new ArrayList<Actor>();
		List<Actor> actHasWeight = new ArrayList<Actor>();
		List<String> actorTimesWeight = new ArrayList<String>();

		int sumQua = 0;
		int dNum = 100;
		function.append(FeatureExport.modifyName(urn.getName()));
		function.append(EQUALS);
		for (Iterator it = urn.getGrlspec().getActors().iterator(); it.hasNext();) {
			Actor actor = (Actor) it.next();
			actorList.add(actor);
			if (actor.getImportanceQuantitative() != 0) {
				actHasWeight.add(actor);
			}

		} // for
		if (actorList.size() == 0) {
			// it's like there is one big actor weighted 100 containing anything
			modelFormula = ModelWithoutActor(urn);
		} else {
			if (actHasWeight.size() == 0) {

				for (int i = 0; i < actorList.size(); i++) {
					StringBuffer actorRe = new StringBuffer();
					actorRe.append(LEFT_BRACKET);
					actorRe.append(actorMap.get(actorList.get(i)));
					actorRe.append(RIGHT_BRACKET);
					actorTimesWeight.add(actorRe + TIMES + "100.0");
				}
				sumQua = 100 * actorList.size();
			} else {
				for (int i = 0; i < actorList.size(); i++) {
					int actorQua = actorList.get(i).getImportanceQuantitative();
					StringBuffer actorRe = new StringBuffer();
					actorRe.append(LEFT_BRACKET);
					actorRe.append(actorMap.get(actorList.get(i)));
					actorRe.append(RIGHT_BRACKET);
					actorTimesWeight.add(actorMap.get(actorList.get(i)) + TIMES + actorQua);
					sumQua += actorQua;
				}
			}
			String joined = String.join("+", actorTimesWeight);

			modelFormula.append(LEFT_BRACKET);
			modelFormula.append(joined);

			modelFormula.append(RIGHT_BRACKET);
			modelFormula.append(DIVIDE);

			modelFormula.append(Integer.toString(Math.max(sumQua, dNum)));

		}
		function.append(modelFormula);
		write("# The function of Model\n");
		write(function.toString());
		write("\n");
	}

	private StringBuffer ModelWithoutActor(URNspec urn) throws IOException {
		List<IntentionalElement> eleList = new ArrayList<IntentionalElement>();// the elements in the actor
		StringBuffer formula = new StringBuffer();
		List<Integer> quaList = new ArrayList<Integer>();
		List<String> actorTimesQua = new ArrayList<String>();
		int sumQua = 0;
		int dNum = 100;
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
			IntentionalElement ele = (IntentionalElement) it.next();
			eleList.add(ele);
			int eleQua = ele.getImportanceQuantitative();
			quaList.add(eleQua);
			sumQua += eleQua;

		}

		if (eleList.size() == 0) {
			// actorFormula.append("0");

		} else {// there are elements in the actor
			if (sumQua == 0) {// there are no weighted elements in actor
				// System.out.println("sum == 0 ");
				for (int i = 0; i < eleList.size(); i++) {
					IntentionalElement ele = (IntentionalElement) (eleList.get(i));
					StringBuffer eleFormula = new StringBuffer();
					eleFormula.append(LEFT_BRACKET);
					eleFormula.append(elementMap.get(ele));
					eleFormula.append(RIGHT_BRACKET);
					if (ele.getLinksSrc().size() == 0) {
						// actorTimesQua.add(ele.getName() + Times + "100"); Amal
						actorTimesQua.add(eleFormula + TIMES + "100.0");
						sumQua += 100;
					} else {
						// give the weight to top-level elements;
						IntentionalElement srcElement = (IntentionalElement) (((ElementLink) (ele.getLinksSrc().get(0))).getDest());

						if (eleList.contains(srcElement) == false) {
							actorTimesQua.add(eleFormula + TIMES + "100.0");
							sumQua += 100;
						}
					}
				} // for
			} // if(sumQua==0)
			else {// there are some elements weighted
				// System.out.println("sum! = 0 ");
				for (int i = 0; i < eleList.size(); i++) {
					IntentionalElement ele = (IntentionalElement) (eleList.get(i));
					if (ele.getImportanceQuantitative() == 0) {
						continue;
					}
					actorTimesQua.add(elementMap.get(ele) + TIMES + "100.0");
				}
			}
		}
		formula.append(LEFT_BRACKET);
		String joined = String.join("+ ", actorTimesQua);
		formula.append(joined);

		formula.append(RIGHT_BRACKET);
		formula.append(DIVIDE);

		formula.append(Integer.toString(Math.max(sumQua, dNum)));

		return formula;

	}

	private StringBuffer indicatorFor(String[] indicatorValues, String indicatorName) throws IOException {
		StringBuffer formula = new StringBuffer();
		String currentName = new String(modifyName(indicatorName));
		double worst = Double.parseDouble(indicatorValues[3]);
		double target = Double.parseDouble(indicatorValues[1]);
		double threshold = Double.parseDouble(indicatorValues[2]);
		formula = new StringBuffer();
		formula.append("Piecewise");
		formula.append(LEFT_BRACKET);
		if ((worst == threshold) && (threshold == target)) {
			// warning-- can we throw an Exception?
			System.out.println("Warning: the three value should not be equal");
			throw new IOException("The three KPI values cannot be equal");
		}
		if (worst < target) {
			formula.append(LEFT_BRACKET);
			formula.append("100");
			formula.append(COMMA);
			formula.append(currentName);
			formula.append(">=");
			formula.append(Double.toString(target));
			formula.append(RIGHT_BRACKET);
			formula.append(COMMA);

			formula.append(LEFT_BRACKET);//
			formula.append("abs( ");
			formula.append(LEFT_BRACKET);// (x-th)
			formula.append(currentName);
			formula.append(MINUS);

			formula.append(Double.toString(threshold));

			formula.append(RIGHT_BRACKET);
			formula.append(DIVIDE);
			double diNum = (target - threshold); // * 200; removed by Amal

			formula.append(Double.toString(diNum));

			formula.append(RIGHT_BRACKET);
			formula.append(MULTI); // added by Amal
			formula.append("50"); // added by Amal
			formula.append(PLUS);
			formula.append("50");
			formula.append(COMMA);

			formula.append("(");
			formula.append(Double.toString(threshold));
			formula.append("<="); // ("<") changed by Amal
			formula.append(currentName);
			formula.append(")");
			formula.append("&");
			formula.append("(");
			formula.append(currentName);
			formula.append("<");
			formula.append(Double.toString(target));
			formula.append(")");
			formula.append(RIGHT_BRACKET);
			formula.append(COMMA);
			formula.append(LEFT_BRACKET);
			formula.append("-abs( ");
			formula.append(LEFT_BRACKET);

			formula.append(currentName);

			formula.append(MINUS);

			formula.append(Double.toString(threshold));

			formula.append(RIGHT_BRACKET);
			formula.append(DIVIDE);
			double diNum2 = (worst - threshold); // * 200; removed by Amal

			formula.append(Double.toString(diNum2));

			formula.append(RIGHT_BRACKET);
			formula.append(MULTI); // added by Amal
			formula.append("50"); // added by Amal
			formula.append(PLUS);
			formula.append("50");

			formula.append(COMMA);
			// formula.append("True");
			formula.append("(");
			formula.append(Double.toString(worst));
			formula.append("<");
			formula.append(currentName);
			formula.append(")");
			formula.append("&");
			formula.append("(");
			formula.append(currentName);
			formula.append("<");
			formula.append(Double.toString(threshold));
			formula.append(")");
			formula.append(RIGHT_BRACKET);
			formula.append(COMMA);

			formula.append(LEFT_BRACKET);
			formula.append("0");
			formula.append(COMMA);
			formula.append("True");
			formula.append(RIGHT_BRACKET);

			formula.append(RIGHT_BRACKET);
		}
		if (worst > target) {
			formula.append(LEFT_BRACKET);
			formula.append("100");
			formula.append(COMMA);
			formula.append(currentName);
			formula.append("<=");
			formula.append(Double.toString(target));
			formula.append(RIGHT_BRACKET);
			formula.append(COMMA);

			formula.append(LEFT_BRACKET);
			formula.append("abs( ");
			formula.append(LEFT_BRACKET);
			formula.append(currentName);
			formula.append(MINUS);

			formula.append(Double.toString(threshold));

			formula.append(RIGHT_BRACKET);
			formula.append(DIVIDE);
			double diNum = (threshold - target); // (target - threshold) * 200; changed by Amal

			formula.append(Double.toString(diNum));

			formula.append(RIGHT_BRACKET);
			formula.append(MULTI); // added by Amal
			formula.append("50"); // added by Amal
			formula.append(PLUS);
			formula.append("50");

			formula.append(COMMA);
			formula.append("(");
			formula.append(Double.toString(target));
			formula.append("<");
			formula.append(currentName);
			formula.append(")");
			formula.append("&");
			formula.append("(");
			formula.append(currentName);
			formula.append("<="); // ("<") changed by Amal
			formula.append(Double.toString(threshold));
			formula.append(")");
			formula.append(RIGHT_BRACKET);
			formula.append(COMMA);

			formula.append(LEFT_BRACKET);
			formula.append("-abs( ");
			formula.append(LEFT_BRACKET);
			formula.append(currentName);
			formula.append(MINUS);

			formula.append(Double.toString(threshold));

			formula.append(RIGHT_BRACKET);
			formula.append(DIVIDE);
			double diNum2 = (threshold - worst); // (worst - threshold); * 200; changed by Amal

			formula.append(Double.toString(diNum2));

			formula.append(RIGHT_BRACKET);
			formula.append(MULTI); // added by Amal
			formula.append("50"); // added by Amal
			formula.append(PLUS);
			formula.append("50");

			formula.append(COMMA);
			// formula.append("True");
			formula.append("(");
			formula.append(Double.toString(threshold));
			formula.append("<");
			formula.append(currentName);
			formula.append(")");
			formula.append("&");
			formula.append("(");
			formula.append(currentName);
			formula.append("<");
			formula.append(Double.toString(worst));
			formula.append(")");
			formula.append(RIGHT_BRACKET);

			formula.append(COMMA);

			formula.append(LEFT_BRACKET);
			formula.append("0");
			formula.append(COMMA);
			formula.append("True");
			formula.append(RIGHT_BRACKET);

			formula.append(RIGHT_BRACKET);
		}
		return formula;
	}

	// working
	private void writeTranslation(URNspec urn) throws IOException {
		write("GRLDiagramName " + EQUALS + " '" + FeatureExport.modifyName(GRLname) + "' " + "\n");
		StringBuffer varList = new StringBuffer();
		varList.append("List");
		// varList.append(urn.getName());
		varList.append(EQUALS);
		varList.append("[");
		List<String> eleList = new ArrayList<String>();
		eleList.addAll(elementSet);
		// String message = String.join("-", list); 
		varList.append(String.join(",", eleList));
		varList.append("]");
		write("\n# variable list");
		write("\n");
		write(varList.toString());

		StringBuffer tranScript = new StringBuffer();
		tranScript.append("Translate");
		tranScript.append(LEFT_BRACKET);
		tranScript.append("'");
		tranScript.append(modelFormula);
		tranScript.append("'");
		tranScript.append(COMMA);
		tranScript.append("GRLDiagramName");// model's name
		tranScript.append(COMMA);
		tranScript.append("List");
		// tranScript.append(urn.getName());
		tranScript.append(COMMA);
		tranScript.append("LANG");
		tranScript.append(RIGHT_BRACKET);
		write("\nLANG = []\n" + "langList = ['python','c','c++','java',\"javascript\",'matlab','r','cp']\n");

		StringBuffer allprint = new StringBuffer();
		allprint.append("def allPrint():\n");
		// allprint.append("\tfor j in langList:\n");
		// allprint.append("\t\tLANG = 'All'\n");

		allprint.append("\t\t" + varList.toString() + "\n");
		allprint.append("\t\t" + tranScript + "\n");
		write(allprint.toString());
		StringBuffer scriptLang = new StringBuffer("if(len(sys.argv)==1):\n\tLANG = langList\n" + "\tallPrint()\n"
				+ "else:\n" + "\tfor i in sys.argv:\n" + "\t\tif(sys.argv.index(i)==0):continue\n"
				+ "\t\tif  (i.lower() not in langList):\n" + "\t\t\t" + "LANG = langList\n" + "\t\t\tbreak" + "\n"
				+ "\t\telse:\n" + "\t\t\tLANG.append(str(i.lower()))\n\tallPrint()\n"); // +"\t\t\tprint LANG\n"
		// + "\t\t\t" + tranScript + "\n");
		write(scriptLang.toString() + "\n");

	}

	private String modifyName(String name) throws IOException {
		name = name.toLowerCase();
		name = name.substring(0, 1).toUpperCase() + name.substring(1);
		if (name.length() > 1) {
			name = name.substring(0, name.length() - 1) + name.substring(name.length() - 1).toUpperCase();
		}

		name = name.replaceAll("[\\s]+", "_");
		name = name.replaceAll("[^a-zA-Z0-9\\_]+", "");
		Pattern pattern = Pattern.compile("^[0-9]");
		Matcher matcher = pattern.matcher(name);
		while (matcher.find()) {
			name = "_" + name;
		}
		return name;
	}

}
