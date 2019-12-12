package seg.jUCMNav.importexport;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import fm.Feature;
import fm.FeatureDiagram;
import grl.Actor;
import grl.ElementLink;
import grl.GRLGraph;
import grl.IntentionalElement;
import grl.IntentionalElementRef;
import grl.IntentionalElementType;
import seg.jUCMNav.extensionpoints.IURNExport;
import seg.jUCMNav.model.util.MetadataHelper;
import seg.jUCMNav.views.wizards.importexport.ExportWizard;
import urn.URNspec;
import urncore.IURNDiagram;

// Base class for ExportGRLMath* implementing only default methods
public abstract class GRLMathBase implements IURNExport {

	String GRLname;
	FeatureToMathS FeatureExport = new FeatureToMathS();
	FileOutputStream fos;
	// declaring string constants
	static final String LEFT_BRACKET = "(";
	static final String RIGHT_BRACKET = ")";
	static final String COMMA = " , ";
	static final String EQUALS = " = ";
	static final String SYMBOL = "Symbol";
	static final String TIMES = "*";
	static final String DIVIDE = " / ";
	static final String PLUS = " + ";
	static final String MINUS = " - ";
	static final String SPACE = " ";
	static final String MULTI = " * ";
	static final String KPI = "KPI";
	static final String MIN = "Min";
	static final String MAX = "Max";
	static final String PIECEWISE = "Piecewise";
	static final String COLON = " : ";

	// store elements and the functions
	Map<IntentionalElement, StringBuffer> elementMap;
	// store actors and the functions
	Map<Actor, StringBuffer> actorMap;
	StringBuffer modelFormula;
	// stores the leaf element names
	Set<String> elementSet = new LinkedHashSet<String>();
	// stores the elements except indicators which are separated from the main
	// function
	Set<IntentionalElement> splitElements = new LinkedHashSet<IntentionalElement>();

	abstract StringBuffer writeLink(IntentionalElement element) throws IOException;

	abstract void writeActor(URNspec urn) throws IOException;

	abstract void writeIndependentIndicators(Iterator iterator, Set<String> list) throws IOException;

	abstract void writeSeparatedElements(Set<String> list) throws IOException;

	@Override
	public void export(URNspec urn, HashMap mapDiagrams, FileOutputStream fos) throws InvocationTargetException {
		// not used
	}

	@Override
	public void export(URNspec urn, HashMap mapDiagrams, String filename) throws InvocationTargetException {
		Set<IntentionalElement> featureElements = new LinkedHashSet<IntentionalElement>();

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
						GRLname = purName;
					}
				}
				
				addSeparatingElements(urn, featureElements);
				writeFormula(urn);
				writeActor(urn);
				writeModel(urn);
				writeTranslation(urn);
			}

			FeatureExport.export(urn, mapDiagrams, filename, featureElements);

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
	String[] getIndicatorValues(IntentionalElement element) throws IOException {
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
	void write(String s) throws IOException {
		if (s != null && s.length() > 0) {
			fos.write(s.getBytes());
		}
	}

	/**
	 * Writes the head of the SymPy file
	 * 
	 * @throws IOException
	 */
	void writeHead(URNspec urn) throws IOException {

		write("from MathTo import * \n" + "from sympy import *\n");
		write("import sys\n");
		write("import os");
		write("\n");
		write("\n");
		write("# Creating a folder using Model name\n");
		write("ModelName= '");
		write(FeatureExport.modifyName(urn.getName()) + "'\n");
		write("if not os.path.exists(ModelName):\n");
		write("   os.makedirs(ModelName)\n");
		write("os.chdir(ModelName)");
		write("\n");
		write("\n");
	}

	/**
	 * Check if the element is part of GRL diagrams(added by Amal Ahmed Anda)
	 * 
	 * @param element
	 */
	boolean isGRLElement(IntentionalElement element) {
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
	void writeIndicatorFunction(IntentionalElement element, StringBuffer elementFormula) throws IOException {
		String[] indicatorValues = getIndicatorValues(element);
		// if worst and target values are equal
		if (indicatorValues[0].equalsIgnoreCase("S") && indicatorValues[1].equals(indicatorValues[3])) {
			// TODO: need correct formula here
			elementFormula.append(FeatureExport.modifyName(element.getName()));
		} else if (indicatorValues[0].equalsIgnoreCase("B")) {
			// checking for '<' or '<=' values
			if (indicatorValues[1].trim().matches("<=?[a-zA-Z0-9]*")) {
				elementFormula.append(PIECEWISE);
				elementFormula.append(LEFT_BRACKET);
				elementFormula.append(LEFT_BRACKET);
				elementFormula.append("100.0");
				elementFormula.append(COMMA);
				elementFormula.append(FeatureExport.modifyName(element.getName()));
				elementFormula.append(indicatorValues[1]);
				elementFormula.append(RIGHT_BRACKET);
				elementFormula.append(COMMA);
				elementFormula.append(LEFT_BRACKET);
				elementFormula.append("0.0");
				elementFormula.append(COMMA);
				elementFormula.append("True");
				elementFormula.append(RIGHT_BRACKET);
				elementFormula.append(RIGHT_BRACKET);
			}
			// checking for '>' or '>=' values
			else if (indicatorValues[1].trim().matches(">=?[a-zA-Z0-9]*")) {
				elementFormula.append(PIECEWISE);
				elementFormula.append(LEFT_BRACKET);
				elementFormula.append(LEFT_BRACKET);
				elementFormula.append("100.0");
				elementFormula.append(COMMA);
				elementFormula.append(FeatureExport.modifyName(element.getName()));
				elementFormula.append(indicatorValues[1]);
				elementFormula.append(RIGHT_BRACKET);
				elementFormula.append(COMMA);
				elementFormula.append(LEFT_BRACKET);
				elementFormula.append("0.0");
				elementFormula.append(COMMA);
				elementFormula.append("True");
				elementFormula.append(RIGHT_BRACKET);
				elementFormula.append(RIGHT_BRACKET);
			} 
			// checking for '>' or '>=' and '<' or '<=' values
			else if (indicatorValues[1].trim().matches(">=?[0-9]*and<=?[0-9]*")) {
				String[] subValues = indicatorValues[1].split("and");
				elementFormula.append(PIECEWISE);
				elementFormula.append(LEFT_BRACKET);
				elementFormula.append(LEFT_BRACKET);
				elementFormula.append("100.0");
				elementFormula.append(COMMA);
				elementFormula.append(LEFT_BRACKET);
				elementFormula.append(FeatureExport.modifyName(element.getName()));
				elementFormula.append(subValues[0]);
				elementFormula.append(RIGHT_BRACKET);
				elementFormula.append(" & ");
				elementFormula.append(LEFT_BRACKET);
				elementFormula.append(FeatureExport.modifyName(element.getName()));
				elementFormula.append(subValues[1]);
				elementFormula.append(RIGHT_BRACKET);
				elementFormula.append(RIGHT_BRACKET);
				elementFormula.append(COMMA);
				elementFormula.append(LEFT_BRACKET);
				elementFormula.append("0.0");
				elementFormula.append(COMMA);
				elementFormula.append("True");
				elementFormula.append(RIGHT_BRACKET);
				elementFormula.append(RIGHT_BRACKET);
			} 
			else {
				elementFormula.append(PIECEWISE);
				elementFormula.append(LEFT_BRACKET);
				elementFormula.append(LEFT_BRACKET);
				elementFormula.append("100.0");
				elementFormula.append(COMMA);
				if (!indicatorValues[1].trim().equals("")) {
					elementFormula.append(indicatorValues[1].replaceAll("[a-zA-Z]+", FeatureExport.modifyName(element.getName())));
				} else {
					elementFormula.append(FeatureExport.modifyName(element.getName()));
				}
				elementFormula.append(RIGHT_BRACKET);
				elementFormula.append(COMMA);
				elementFormula.append(LEFT_BRACKET);
				elementFormula.append("0.0");
				elementFormula.append(COMMA);
				elementFormula.append("True");
				elementFormula.append(RIGHT_BRACKET);
				elementFormula.append(RIGHT_BRACKET);
			}
			
		} else if (indicatorValues[0].equalsIgnoreCase("F")) {
			indicatorValues[1] = indicatorValues[1].replaceAll("current", FeatureExport.modifyName(element.getName()));
			elementFormula.append(indicatorValues[1]);
		} else {
			StringBuffer indicatorFor = indicatorFor(indicatorValues, element.getName());
			elementFormula.append(indicatorFor);
		}
	}
	
	
	StringBuffer indicatorFor(String[] indicatorValues, String indicatorName) throws IOException {
		StringBuffer formula = new StringBuffer();
		String currentName = new String(FeatureExport.modifyName(indicatorName));
		double worst = Double.parseDouble(indicatorValues[3]);
		double target = Double.parseDouble(indicatorValues[1]);
		double threshold = Double.parseDouble(indicatorValues[2]);
		formula = new StringBuffer();
		formula.append(PIECEWISE);
		formula.append(LEFT_BRACKET);
		if ((worst == threshold) && (threshold == target)) {
			// warning-- can we throw an Exception?
			System.out.println("Warning: the three value should not be equal");
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

	
	void addSeparatingElements(URNspec urn, Set<IntentionalElement> featureElements) throws IOException {
		Set<IntentionalElement> allElements = new LinkedHashSet<IntentionalElement>();

		// separating first level elements
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
			IntentionalElement element = (IntentionalElement) it.next();
			allElements.add(element);
			boolean addFlag = true;
			if (isGRLElement(element) && element.getLinksDest().size() != 0) {
				// separating the leaf features for Feature model with one if block
				if (FeatureExport.IsItLeaf(element) && element instanceof Feature) {
					featureElements.add(element);
				}
				for (Iterator it2 = element.getLinksDest().iterator(); it2.hasNext();) {
					ElementLink destLink = (ElementLink) it2.next();
					IntentionalElement srcElement = (IntentionalElement) (destLink.getSrc());
					if ((srcElement.getType() == IntentionalElementType.GOAL_LITERAL && srcElement.getLinksDest().size() != 0)
							|| (srcElement.getType() == IntentionalElementType.TASK_LITERAL && srcElement.getLinksDest().size() != 0)
							|| (srcElement.getType() == IntentionalElementType.SOFTGOAL_LITERAL && srcElement.getLinksDest().size() != 0)
							|| (srcElement.getLinksDest().size() == 0 && srcElement.getType() != IntentionalElementType.INDICATOR_LITERAL)) {
						addFlag = false;
						continue;
					}
				}
				if (addFlag)
					splitElements.add(element);
			}
		}

		// separating higher level elements
		int total = allElements.size() - (2 * splitElements.size());
		for (int i = 1; i < total; i++) {
			for (IntentionalElement e : allElements) {
				
				if (isGRLElement(e) && e.getLinksDest().size() != 0) {

					int count = 0;
					for (Iterator it2 = e.getLinksDest().iterator(); it2.hasNext();) {
						ElementLink destLink = (ElementLink) it2.next();
						IntentionalElement srcElement = (IntentionalElement) (destLink.getSrc());
						if (splitElements.contains(srcElement) || srcElement.getType() == IntentionalElementType.INDICATOR_LITERAL) {
							count++;
						}
					}
					if (count == e.getLinksDest().size()) {
						splitElements.add(e);
					}
				}
				
			}
		}
	}
	
	/**
	 * Create formulas for each elements
	 * 
	 * @param urn
	 * @throws IOException
	 */
	void writeFormula(URNspec urn) throws IOException {

		elementMap = new HashMap<IntentionalElement, StringBuffer>();
		StringBuffer elementFormula;
		StringBuffer function;
		// initialize all the symbols

		write("# initalize all the variables\n");
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
			IntentionalElement element = (IntentionalElement) it.next();

			if (isGRLElement(element)) {
				StringBuffer variable = new StringBuffer();
				variable.append(FeatureExport.modifyName(element.getName()));
				variable.append(EQUALS);
				variable.append(SYMBOL);
				variable.append(LEFT_BRACKET);
				variable.append("'");
				variable.append(FeatureExport.modifyName(element.getName()));
				variable.append("'");
				variable.append(RIGHT_BRACKET);
				write(variable.toString());
				write("\n");
			}
		}
		
		write("# Indicator function\n");
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
						write(FeatureExport.modifyName(element.getName()) + EQUALS);
						write(elementFormula.toString());
						write("\n");
					} else {
						elementFormula.append(FeatureExport.modifyName(element.getName()));
					}
					elementSet.add("'" + FeatureExport.modifyName(element.getName()) + "'");
					elementMap.put(element, elementFormula);
				}
			}
		}

		write("# Non-leaf element functions\n");

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
					// adding the separated element to the set
					addFeature(element, elementFormula.toString());
					elementMap.put(element, elementFormula);
				}
			}
		}
		
	}


	// add the separated elements(leaf features/task with formula) except indicators
	// to the set
	void addFeature(IntentionalElement element, String formula) throws IOException {
		if (element.getType() == IntentionalElementType.TASK_LITERAL && formula.contains(LEFT_BRACKET)) {
			splitElements.add(element);
		}
	}

	
	/**
	 * If there is no actor in the model, then it would be as if there were one big
	 * actor with weight 100 that contained everything.
	 * 
	 * If there are actors but they have no weight, then these actors should be
	 * weighted equally.
	 * 
	 * @param urn
	 * @throws IOException
	 */
	void writeModel(URNspec urn) throws IOException {

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
			modelFormula = modelWithoutActor(urn);
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

	StringBuffer modelWithoutActor(URNspec urn) throws IOException {
		List<IntentionalElement> eleList = new ArrayList<IntentionalElement>(); // the elements in the actor
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

	
	StringBuffer writeDecomMaxMin(List<IntentionalElement> list, String func) throws IOException {
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

	
	StringBuffer writeDepenMaxMin(List<IntentionalElement> list, StringBuffer func, IntentionalElement element)
			throws IOException {

		StringBuffer formula = new StringBuffer();
		Stack<StringBuffer> st = new Stack<StringBuffer>();
		StringBuffer indicatorFor = new StringBuffer();
		if (element.getType().getName().compareTo("Indicator") == 0) {

			// if (eleForMap.get(element) == null) {
			// System.out.println(element.getName() + "Went To indicator from writeLink
			// where no formula");
			// indicatorFor = indicatorFor(element);
			// TODO: check this
			writeIndicatorFunction(element, indicatorFor);
		}
		if (func.length() == 0 && indicatorFor.length() == 0) {
			StringBuffer eleSt = new StringBuffer(FeatureExport.modifyName(element.getName()));
			st.add(eleSt);
		} else {
			// System.out.println(" func in else ="+func.toString());
			if (func.length() != 0)
				st.add(func);
			if (indicatorFor.length() != 0)
				st.add(indicatorFor);
		}
		for (int i = 0; i < list.size(); i++) {
			StringBuffer subfo = new StringBuffer(FeatureExport.modifyName(list.get(i).getName()));
			// System.out.println(" subfo in for ="+subfo.toString());
			st.add(subfo);
		}

		formula.append(FeatureExport.MaxmaxFormat(st, MIN));
		// System.out.println(element.getName()+" Formula ===="+formula.toString());
		return formula;
		// else { // replace indicator name with formula
		// indicatorFor = eleForMap.get(subEle);
		// formula = new StringBuffer(
		// formula.toString().replaceAll(FeatureExport.modifyName(subEle.getName()),
		// indicatorFor.toString()));
		// }
		// }
	}

	
	//add the elements in the list[]
	Set<String> elementList() throws IOException {
		Set<String> elementListSet = new HashSet<String>();
		for (Map.Entry<IntentionalElement, StringBuffer> entry : elementMap.entrySet()) {
			String name = FeatureExport.modifyName(entry.getKey().getName().toString());
			if (modelFormula.toString().contains(name)) {
				elementListSet.add("'" + name + "'");
			}
		}
		if (!splitElements.isEmpty()) {
			for (IntentionalElement e : splitElements) {
				elementListSet.add("'" + FeatureExport.modifyName(e.getName()) + "'");
			}
		}
		if (!elementSet.isEmpty()) {
			elementListSet.addAll(elementSet);
		}
		return elementListSet;
	}

	
	/**
	 * Writes the translation of the elements to SymPy
	 * 
	 * @param urn
	 * @throws IOException
	 */
	void writeTranslation(URNspec urn) throws IOException {

		write("GRLDiagramName " + EQUALS + " '" + FeatureExport.modifyName(GRLname) + "' " + "\n");
		Set<String> dictElements = new LinkedHashSet<String>();
		StringBuffer varList = new StringBuffer();
		StringBuffer tranScript = new StringBuffer();
		StringBuffer allprint = new StringBuffer();

		varList.append("List");
		// varList.append(urn.getName());
		varList.append(EQUALS);
		varList.append("[");

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
		tranScript.append(COMMA);
		tranScript.append("2");
		tranScript.append(COMMA);
		tranScript.append("dict");
		tranScript.append(RIGHT_BRACKET);

		// writing all the separated indicators in the dictionary
		writeIndependentIndicators(urn.getGrlspec().getIntElements().iterator(), dictElements);
		// writing all the separated functions in the dictionary
		writeSeparatedElements(dictElements);

		varList.append(String.join(",", elementList()));
		varList.append("]");
		// printing all write() from here
		write("\n# Variable list");
		write("\n");
		write(varList.toString());
		write("\nLANG = []\n" + "langList = ['python','c','c++','java',\"javascript\",'matlab','r']\n");
		write("def allPrint():\n");
		// initializing a python dictionary named 'dict'
		write("\tdict = {\n");

		String joinFunctions = String.join(",\n", dictElements);
		write(joinFunctions);
		write("\n\t}\n");
		write("\t# Model Function\n");
		allprint.append("\t" + varList.toString() + "\n");
		allprint.append("\t" + tranScript + "\n");
		write(allprint.toString());

		StringBuffer scriptLang = new StringBuffer("if(len(sys.argv)==1):\n\tLANG = langList\n" + "\tallPrint()\n"
				+ "else:\n" + "\tfor i in sys.argv:\n" + "\t\tif(sys.argv.index(i)==0):continue\n"
				+ "\t\tif  (i.lower() not in langList):\n" + "\t\t\t" + "LANG = langList\n" + "\t\t\tbreak" + "\n"
				+ "\t\telse:\n" + "\t\t\tLANG.append(str(i.lower()))\n\tallPrint()\n");

		// + "\t\t\t" + tranScript + "\n");
		write(scriptLang.toString() + "\n");
		// ================================
		// write("\t\t\t#Indicators \n");
		// for (Iterator it = urn.getGrlspec().getIntElements().iterator();
		// it.hasNext();) {
		// IntentionalElement IndicatorV = (IntentionalElement) it.next();

		// if (IndicatorV.getType().getName().compareTo("Indicator") == 0) {
		// write("\t\t\tList=["+FeatureExport.modifyName(IndicatorV.getName())+"]\n");
		// write("\t\t\tprint '"+FeatureExport.modifyName(IndicatorV.getName())+"'\n");
		// write("\t\t\tTranslate('"+eleForMap.get(IndicatorV)+"', List[0], List,
		// LANG)\n");
		// }
		// }

	}
}
