package seg.jUCMNav.importexport;

/**
 * This class export the Feature model into SymPy function
 * 
 * @author Amal Ahmed Anda
 *
 */
import java.io.File;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fm.Feature;
import fm.FeatureDiagram;
import fm.MandatoryFMLink;
import fm.OptionalFMLink;
import grl.Decomposition;
import grl.ElementLink;
import grl.IntentionalElement;
import seg.jUCMNav.views.wizards.importexport.ExportWizard;
import urn.URNlink;
import urn.URNspec;
import urncore.IURNDiagram;

public class FeatureToMathS {
	private FileOutputStream fos;
	private String FMname;
	private String filename;
	public static final String LEFT_BRACKET = "(";
	public static final String RIGHT_BRACKET = ")";
	public static final String COMMA = " , ";
	public static final String EQUALS = "= ";
	public static final String SYMBOLS = "symbols";
	public static final String TIMES = "*";
	public static final String DIVIDE = " / ";
	public static final String PLUS = " + ";
	public static final String MINUS = " - ";
	public static final String MULTI = " * ";
	public static final String COLON = " : ";

	//store elements and the functions
	private Map<IntentionalElement, StringBuffer> elementMap;
	private StringBuffer modelFormula;
	// stores the leaf elements
	private HashSet<String> elementSet = new HashSet<String>();
	// stores the separated elements
	private HashSet<IntentionalElement> splitElements = new HashSet<IntentionalElement>();

	// check if there are features in the feature diagram
	public void export(URNspec urn, HashMap mapDiagrams, String filename, Set<IntentionalElement> featureElements) throws InvocationTargetException {
		// adding the feature elements to split elements
		splitElements.addAll(featureElements);
		
		boolean featurFound = false;
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
			IntentionalElement element = (IntentionalElement) it.next();
			if (element.getClass().toString().contains("fm")) {
				featurFound = true;
				break;
			}
		}
		if (featurFound) {

			try {
				fos = new FileOutputStream(new File(filename), true);
				for (Iterator iter = mapDiagrams.keySet().iterator(); iter.hasNext();) {
					IURNDiagram diagram = (IURNDiagram) iter.next();
					if (diagram instanceof FeatureDiagram) {
						String diagramName = ExportWizard.getDiagramName(diagram);
						String purName = diagramName.substring(diagramName.lastIndexOf("-") + 1);
						// System.out.println("Diagram information="+diagram.toString()+"
						// name="+diagramName+" PureNam= "+purName);
						FMname = purName;
					}
				}
				
				writeFormula(urn);
				writeModel(urn);
				writeTranslation(urn);
				
			} catch (Exception e) {
				throw new InvocationTargetException(e);
			}
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
	 * create formulas for each elements
	 * 
	 * @param urn
	 * @throws IOException
	 */
	private void writeFormula(URNspec urn) throws IOException {

		elementMap = new HashMap<IntentionalElement, StringBuffer>();
		StringBuffer eleFormula;
		StringBuffer function;
		// initialize all the symbols
		write("# initalize all the variables\n");
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {

			IntentionalElement element = (IntentionalElement) it.next();
			if (element instanceof Feature || element.getType().getName().equalsIgnoreCase("Task")) {
				StringBuffer variable = new StringBuffer();
				variable.append(modifyName(element.getName()));
				variable.append(EQUALS);
				variable.append("Symbol");
				variable.append(LEFT_BRACKET);
				variable.append("'");
				variable.append(modifyName(element.getName()));
				variable.append("'");
				variable.append(RIGHT_BRACKET);
				write(variable.toString());
				write("\n");
			}
		}

		boolean leaf = false;
		// iterate all the leaf element
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
			IntentionalElement element = (IntentionalElement) it.next();
			eleFormula = new StringBuffer();
			function = new StringBuffer();
			function.append(modifyName(element.getName()));
			leaf = IsItLeaf(element);
			// feature has indicator only should consider as leaf feature
			// if the element is the leaf

			if (leaf && (element instanceof Feature || element.getType().getName().equalsIgnoreCase("Task"))
					&& element.getType().getName().compareTo("Indicator") != 0) {
				eleFormula.append(modifyName(element.getName()));

				elementSet.add("'" + modifyName(element.getName()) + "'");
				elementMap.put(element, eleFormula);
			}
		}
		
		write("# Leaf Feature functions\n");
		
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
			IntentionalElement element = (IntentionalElement) it.next();
			leaf = IsItLeaf(element);

			if (((element instanceof Feature || element.getType().getName().equalsIgnoreCase("Task"))
					&& ((element.getLinksDest().size() == 0 || leaf))
					&& (element.getToLinks().size() != 0 || element.getFromLinks().size() != 0))) {
				eleFormula = new StringBuffer();
				function = new StringBuffer();
				StringBuffer functionb = new StringBuffer();
				functionb.append(ExcludeIncludeLink(element));
				if (functionb.length() != 0) {
					function.append(modifyName(element.getName()));
					eleFormula.append(functionb);

					// System.out.println(element.getName() +"="+ eleFormula.toString() + "After
					// enclude exclude for leaf features");
					function.append(EQUALS);
					function.append(eleFormula);
					write(function.toString());
					write("\n");
					splitElements.add(element);
					elementMap.put(element, eleFormula);
				}
			}
		}
		
		write("# Non-leaf Feature functions\n");
		
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {

			IntentionalElement element = (IntentionalElement) it.next();
			leaf = IsItLeaf(element);
			// feature has indicator only should consider as leaf feature

			if ((element instanceof Feature || element.getType().getName().equalsIgnoreCase("Task"))
					&& (element.getLinksDest().size() != 0) && !leaf) {
				eleFormula = new StringBuffer();
				function = new StringBuffer();
				function.append(modifyName(element.getName()));
				// ||
				// if ((element.getLinksDest().size() != 0) ) {
				eleFormula.append(writeLink(element));
				// System.out.println(element.getName() +"="+ eleFormula.toString() + "Next
				// iteration");
				function.append(EQUALS);
				function.append(eleFormula);
				write(function.toString());
				write("\n");
				elementMap.put(element, eleFormula);

			}
		}
	}


	// check leaf features
	public boolean IsItLeaf(IntentionalElement element) throws IOException {
		// feature has indicator only should consider as leaf feature

		if (element.getLinksDest().size() != 0) {
			for (Iterator it2 = element.getLinksDest().iterator(); it2.hasNext();) {
				ElementLink scrLink = (ElementLink) it2.next();
				if (scrLink.getClass().getTypeName().contains("pendency") == false) {
					IntentionalElement srcElement = (IntentionalElement) (scrLink.getSrc());
					if ((srcElement.getType().getName().toString().contains("ndicator") == false)) {
						return false;
					}
				}
			}
			return true;
		} else {
			return true;
		}
	}

	// for exclude and include
	private StringBuffer ExcludeIncludeLink(IntentionalElement element) throws IOException {
		StringBuffer formulaex = new StringBuffer();
		StringBuffer ExcludeFor = new StringBuffer();
		StringBuffer includeFor = new StringBuffer();
		List<IntentionalElement> ExcludelList = new ArrayList<IntentionalElement>();
		List<IntentionalElement> IncludelList = new ArrayList<IntentionalElement>();

		// ToLink include and exclude
		/*
		 * List urnLinks = element.getToLinks(); for (int i = 0; i < urnLinks.size();
		 * i++) { if (( (URNlink) urnLinks.get(i)).getToElem() instanceof
		 * IntentionalElement) { //IntentionalElement intElem = (IntentionalElement) (
		 * (URNlink) urnLinks.get(i)).getToElem(); IntentionalElement SourceElem =
		 * (IntentionalElement) ( (URNlink) urnLinks.get(i)).getFromElem();
		 * 
		 * if (( (URNlink) urnLinks.get(i)).getType().compareToIgnoreCase("exclude") ==
		 * 0) ExcludelList.add(SourceElem) ;
		 * 
		 * 
		 * } }
		 */
		// From element Link
		List urnLinkf = element.getFromLinks();
		for (int i = 0; i < urnLinkf.size(); i++) {
			if (((URNlink) urnLinkf.get(i)).getFromElem() instanceof IntentionalElement) {
				IntentionalElement intElem = (IntentionalElement) ((URNlink) urnLinkf.get(i)).getToElem();
				// IntentionalElement SourceElem = (IntentionalElement) ( (URNlink)
				// urnLinkf.get(i)).getFromElem();

				if (((URNlink) urnLinkf.get(i)).getType().compareToIgnoreCase("exclude") == 0)
					ExcludelList.add(intElem);

				if (((URNlink) urnLinkf.get(i)).getType().compareToIgnoreCase("include") == 0)
					IncludelList.add(intElem);

			}
		}
		// after calculate exclude and include features
		if (!ExcludelList.isEmpty()) {
			// ExcludeFor.append(writeSMax(element,"Max"));
			ExcludeFor.append("Max(0,");
			ExcludeFor.append(writeDecomMaxMin(S(element), "Max", 0));
			ExcludeFor.append(MINUS);
			ExcludeFor.append(writeSMax(ExcludelList, "Max"));
			ExcludeFor.append(")");
		}

		if (!IncludelList.isEmpty()) {
			// includeFor.append(writeSMax(IncludelList,"Min"));
			includeFor.append("(((");
			includeFor.append(writeDecomMaxMin(S(element), "Max", 0));
			includeFor.append(PLUS);
			includeFor.append(writeSMax(IncludelList, "Min"));
			includeFor.append(")");
			includeFor.append(DIVIDE + "200.0 )");
			includeFor.append(TIMES + " 100.0 )");
			if (!ExcludelList.isEmpty()) {
				formulaex.append("Min(");
				formulaex.append(ExcludeFor);
				formulaex.append(COMMA);
				formulaex.append(includeFor);
				formulaex.append(")");
			} else {
				formulaex = includeFor;
			}
		} else {
			if (!ExcludelList.isEmpty())
				formulaex = ExcludeFor;
		}

		return formulaex;
	}

	/**
	 * 
	 * @param element
	 * @return
	 * @throws IOException
	 */
	private StringBuffer writeLink(IntentionalElement element) throws IOException {

		StringBuffer formula = new StringBuffer();
		StringBuffer formulaex = new StringBuffer();
		StringBuffer minix = new StringBuffer();
		StringBuffer minix2 = new StringBuffer();
		StringBuffer minix3 = new StringBuffer();

		StringBuffer decomFor = new StringBuffer();
		StringBuffer opFor = new StringBuffer();
		StringBuffer mandatoryFor = new StringBuffer();
		List<String> StrEle = new ArrayList<String>();// the elements' str

		Map<String, List<IntentionalElement>> eleMap = new HashMap<String, List<IntentionalElement>>();
		List<IntentionalElement> decomList = new ArrayList<IntentionalElement>();
		List<IntentionalElement> mandatoryList = new ArrayList<IntentionalElement>();
		List<IntentionalElement> optionalList = new ArrayList<IntentionalElement>();

		List<ElementLink> opLink = new ArrayList<ElementLink>();
		List<IntentionalElement> srcList = new ArrayList<IntentionalElement>();
		// get the exclude and include formula
		formulaex = ExcludeIncludeLink(element);

		for (Iterator it = element.getLinksDest().iterator(); it.hasNext();) {
			ElementLink scrLink = (ElementLink) it.next();
			IntentionalElement srcElement = (IntentionalElement) (scrLink.getSrc());
			srcList.add(srcElement);
			StrEle.add(modifyName(srcElement.getName()));

			if (scrLink instanceof Decomposition) {
				decomList.add(srcElement);
			}

			if (scrLink instanceof MandatoryFMLink) {
				mandatoryList.add(srcElement);

			}
			if (scrLink instanceof OptionalFMLink) {
				optionalList.add(srcElement);
				opLink.add(scrLink);
			}

			eleMap.put("Decomposition", decomList);
			eleMap.put("MandatoryFMLink", mandatoryList);
			eleMap.put("OptionalFMLink", optionalList);
		} // for
			// first decomposition;
		String funcTpye = " ";
		if (!decomList.isEmpty()) {

			if (element.getDecompositionType().getName() == "And") {
				funcTpye = "Min";
				decomFor.append(writeDecomMaxMin(decomList, funcTpye, 1));

			}

			if (element.getDecompositionType().getName() == "Or") {
				decomFor.append(writeORSum(decomList));

			}

			if (element.getDecompositionType().getName() == "Xor") {

				decomFor.append(writeXORSum(decomList));
			}
			minix = decomFor;

		}

		if (!mandatoryList.isEmpty()) {

			funcTpye = "Min";
			mandatoryFor.append(writeDecomMaxMin(mandatoryList, funcTpye, 1));
			if (!decomList.isEmpty()) {
				minix2.append("Min(");
				minix2.append(minix);
				minix2.append(COMMA);
				minix2.append(mandatoryFor);
				minix2.append(")");
			} else
				minix2.append(mandatoryFor); // minix2= mandatoryFor;
		}

		if (!optionalList.isEmpty()) {

			opFor.append(writeOptionalSum(optionalList));
			if (minix2.length() > 0) {
				minix3.append("Min(");
				minix3.append(minix2);
				minix3.append(COMMA);
				minix3.append(opFor);
				minix3.append(")");
			} else {
				if (!decomList.isEmpty()) {
					minix3.append("Min(");
					minix3.append(opFor);
					minix3.append(COMMA);
					minix3.append(minix);
					minix3.append(")");
				} else
					minix3.append(opFor);// minix3=opFor;

			}
		}

		if (minix3.length() > 0)
			formula = minix3;
		else if (minix2.length() > 0)
			formula = minix2;
		else
			formula = minix;

		if (formulaex.length() > 0) {
			formula.insert(0, "Min(" + formulaex + COMMA);
			formula.append(")");

		}

		for (Iterator<IntentionalElement> it = srcList.iterator(); it.hasNext();) {
			IntentionalElement subEle = it.next();
			// if sub element is not the leaf.
			// System.out.println(subEle.getName()+ " "+subEle.getType().getName()+"
			// ++++++fe for kabel if");
			StringBuffer subFor = new StringBuffer();
			if ((subEle instanceof Feature || subEle.getType().getName().equalsIgnoreCase("Task"))
					&& subEle.getType().getName().compareTo("Indicator") != 0) {
				// System.out.println("Deslink="+subEle.getLinksDest().size()+"
				// sourceLink="+subEle.getLinksSrc().size()+" "+subEle.getName()+ "afer first
				// if");
				if (!IsItLeaf(subEle)) // (subEle.getLinksDest().size() != 0 && ( (subEle instanceof Feature) &&
																// subEle.getType().getName().equalsIgnoreCase("Task")))
				{

					if (elementMap.get(subEle) == null) {
						subFor = writeLink(subEle);
					} else {
						subFor = elementMap.get(subEle);
					}

					formula = new StringBuffer(formula.toString().replaceAll(modifyName(subEle.getName()), subFor.toString()));

				}

			}

		}
		// formula = new StringBuffer(
		// formula.toString().replaceAll(modifyName(subEle.getName()),
		// subFor.toString()));
		// System.out.println(formula+ " formula after replace");
		return formula;
	}

	public StringBuffer MaxmaxFormat(Stack<StringBuffer> subst, String func) throws IOException {
		Stack<StringBuffer> stMax = new Stack<StringBuffer>();
		int stSize = subst.size();
		if (stSize == 1) {
			StringBuffer result = new StringBuffer(subst.pop());
			return result;
		}
		for (int i = 1; i < stSize; i += 2) {
			StringBuffer subfor = new StringBuffer();
			subfor.append(func);
			subfor.append(LEFT_BRACKET);
			subfor.append(subst.pop().toString());
			subfor.append(COMMA);
			subfor.append(subst.pop().toString());
			subfor.append(RIGHT_BRACKET);
			stMax.push(subfor);
		}
		if (stSize % 2 == 1) {
			stMax.push(subst.pop());
		}
		return MaxmaxFormat(stMax, func);
	}

	// return v(f) function for leaf features only
	private String v(IntentionalElement element) throws IOException {

		String formula;
		// && element.getLinksDest().size() == 0 &&
		if (elementMap.get(element) != null && IsItLeaf(element) && !splitElements.contains(element)) {
			formula = elementMap.get(element).toString();

		} else {
			formula = modifyName(element.getName());
		}

		return formula;
	}

	private StringBuffer writeDecomMaxMin(List<IntentionalElement> list, String func, int v) throws IOException {
		StringBuffer formula = new StringBuffer();
		Stack<StringBuffer> st = new Stack<StringBuffer>();
		if (v == 1) { // for v(f) calculate include and exclude links
			if (list.size() == 1) {
				formula.append(v(list.get(0)));
			} else if (list.size() == 2) {
				formula.append(func);
				formula.append(LEFT_BRACKET);
				formula.append(v(list.get(0)));
				formula.append(COMMA);
				formula.append(v(list.get(1)));
				formula.append(RIGHT_BRACKET);
			} else if (list.size() > 2) {

				for (int i = 0; i < list.size(); i++) {
					StringBuffer subfo = new StringBuffer(modifyName(list.get(i).getName()));
					st.add(subfo);
				}
				formula.append(MaxmaxFormat(st, func));
				for (int i = 0; i < list.size(); i++) {
					formula = new StringBuffer(formula.toString().replaceAll(modifyName(list.get(i).getName()), v(list.get(i))));
				}
			}
		} // not for v(f)
		else {
			if (list.size() == 1) {
				formula.append(modifyName(list.get(0).getName()));
			} else if (list.size() == 2) {
				formula.append(func);
				formula.append(LEFT_BRACKET);
				formula.append(modifyName(list.get(0).getName()));
				formula.append(COMMA);
				formula.append(modifyName(list.get(1).getName()));
				formula.append(RIGHT_BRACKET);
			} else if (list.size() > 2) {

				for (int i = 0; i < list.size(); i++) {
					StringBuffer subfo = new StringBuffer(modifyName(list.get(i).getName()));
					st.add(subfo);
				}
				formula.append(MaxmaxFormat(st, func));

			}
		}
		return formula;
	}

	// for function sum
	private StringBuffer writeSum(List<IntentionalElement> list) throws IOException {
		StringBuffer formula = new StringBuffer();
		if (list.size() == 1) {
			formula.append(v(list.get(0)));
		} else {
			formula.append(LEFT_BRACKET);
			for (int i = 0; i < list.size(); i++) {
				formula.append(v(list.get(i)));

				if (i + 1 < list.size()) {
					formula.append(PLUS);
				}
			}
			formula.append(RIGHT_BRACKET);
		}

		return formula;
	}

	// for s() children
	private List<IntentionalElement> S(IntentionalElement element) throws IOException {
		List<IntentionalElement> formula = new ArrayList<IntentionalElement>();
		Iterator it2 = element.getLinksDest().iterator();
		if (!it2.hasNext()) {
			if ((element.getType().getName().compareTo("Indicator") != 0)
					&& (element instanceof Feature || element.getType().getName().equalsIgnoreCase("Task"))) {

				formula.add(element);
			}
		} else {
			for (Iterator it = element.getLinksDest().iterator(); it.hasNext();) {
				ElementLink scrLink = (ElementLink) it.next();
				IntentionalElement srcElement = (IntentionalElement) (scrLink.getSrc());
				if ((srcElement.getType().getName().compareTo("Indicator") != 0)
						&& (srcElement instanceof Feature || srcElement.getType().getName().equalsIgnoreCase("Task"))) {
					formula.add((IntentionalElement) S(srcElement).get(0));
				} else {
					if ((element.getType().getName().compareTo("Indicator") != 0)
							&& (element instanceof Feature || element.getType().getName().equalsIgnoreCase("Task"))) {
						formula.add(element);
						break;
					}
				}

			}
		}
		return formula;
	}

	// for s() sum
	private StringBuffer writeSSum(List<IntentionalElement> list) throws IOException {
		StringBuffer formula = new StringBuffer();
		// Stack<StringBuffer> st = new Stack<StringBuffer>();

		if (list.size() == 1) {
			formula.append(writeDecomMaxMin(S(list.get(0)), "Max", 0));
		} else {

			for (int i = 0; i < list.size(); i++) {

				formula.append(writeDecomMaxMin(S(list.get(i)), "Max", 0));

				if (i + 1 < list.size()) {

					formula.append(PLUS);
				}
			}

		}

		return formula;
	}

	// Return the s() function without plus used in optional links in Piecewise
	private StringBuffer writeSMax(List<IntentionalElement> list, String func) throws IOException {
		StringBuffer formula = new StringBuffer();
		List<IntentionalElement> Alllist = new ArrayList<IntentionalElement>();
		// Stack<StringBuffer> st = new Stack<StringBuffer>();

		if (list.size() == 1) {
			Alllist.addAll(S(list.get(0)));
		} else {

			for (int i = 0; i < list.size(); i++) {

				Alllist.addAll(S(list.get(i)));

			}

		}
		formula.append(writeDecomMaxMin(Alllist, func, 0));
		return formula;
	}

	// for OR function
	private StringBuffer writeORSum(List<IntentionalElement> list) throws IOException {
		StringBuffer formula = new StringBuffer();

		formula.append(writeSum(list));

		formula.append(DIVIDE);
		formula.append("Max");
		formula.append(LEFT_BRACKET + "1 " + COMMA);
		formula.append(writeSSum(list));
		formula.append(RIGHT_BRACKET);
		formula.append(TIMES);
		formula.append("100.0");
		return formula;
	}

	private StringBuffer writeXORSum(List<IntentionalElement> list) throws IOException {
		StringBuffer formula = new StringBuffer();

		formula.append(LEFT_BRACKET);
		formula.append(writeDecomMaxMin(list, "Max", 1));
		formula.append(RIGHT_BRACKET);
		formula.append(DIVIDE);
		formula.append("Max");
		formula.append(LEFT_BRACKET + "1" + COMMA);
		formula.append(writeSSum(list));
		formula.append(RIGHT_BRACKET);
		formula.append(TIMES);
		formula.append("100.0");
		return formula;
	}

	private StringBuffer writeOptionalSum(List<IntentionalElement> list) throws IOException {
		StringBuffer formula = new StringBuffer();
		StringBuffer Mxv = new StringBuffer();
		formula.append("Piecewise");
		formula.append(LEFT_BRACKET);
		formula.append(LEFT_BRACKET);
		formula.append("100");
		formula.append(COMMA);
		// formula.append(LeftBracker);
		Mxv = writeSMax(list, "Max");
		formula.append(Mxv);
		formula.append("<=");
		formula.append("0");
		// formula.append(RightBracker);
		formula.append(RIGHT_BRACKET);
		formula.append(COMMA);
		formula.append(LEFT_BRACKET);
		// formula.append(LeftBracker);
		formula.append(writeORSum(list));
		// formula.append(RightBracker);
		formula.append(COMMA);
		// formula.append(LeftBracker);
		// formula.append(Mxv);
		// formula.append(">");
		// formula.append("0");
		formula.append("True");
		// formula.append(RightBracker);
		formula.append(RIGHT_BRACKET);
		formula.append(RIGHT_BRACKET);

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
	private void writeModel(URNspec urn) throws IOException {
		modelFormula = new StringBuffer();
		StringBuffer function = new StringBuffer();

		function.append(modifyName(FMname));
		function.append(EQUALS);
		modelFormula = ModelFromRoot(urn);

		function.append(modelFormula);
		write("# The function of Model\n");
		write(function.toString());
		write("\n");
	}

	private StringBuffer ModelFromRoot(URNspec urn) throws IOException {
		StringBuffer formula = new StringBuffer();

		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
			IntentionalElement ele = (IntentionalElement) it.next();
			// Get formula of Root feature
			if ((ele instanceof Feature || ele.getType().getName().equalsIgnoreCase("Task")) && ele.getLinksSrc().size() <= 0
					&& ele.getLinksDest().size() > 0) {
				formula = elementMap.get(ele);
			}
		}
		return formula;
	}
	
	
	//add the elements in the list[]
	Set<String> elementList() throws IOException {
		Set<String> elementListSet = new HashSet<String>();
		for (Map.Entry<IntentionalElement, StringBuffer> entry : elementMap.entrySet()) {
			String name = modifyName(entry.getKey().getName().toString());
			if (modelFormula.toString().contains(name)) {
				elementListSet.add("'" + name + "'");
			}
		}
		if (!splitElements.isEmpty()) {
			for (IntentionalElement e : splitElements) {
				elementListSet.add("'" + modifyName(e.getName()) + "'");
			}
		}
		return elementListSet;
	}
	

	private void writeTranslation(URNspec urn) throws IOException {
		// indicator
		Set<String> dictElements = new LinkedHashSet<String>();
		StringBuffer varList = new StringBuffer();
		StringBuffer tranScript = new StringBuffer();
		StringBuffer allprint = new StringBuffer();
		
		//String modelName = modifyName(FMname);
		write("FMDiagramName " + EQUALS + " '" + modifyName(FMname) + "' " + "\n");
		
		varList.append("List ");
		// varList.append(urn.getName());
		varList.append(EQUALS);
		varList.append("[");
		
		tranScript.append("Translate");
		tranScript.append(LEFT_BRACKET);
		tranScript.append("'");
		tranScript.append(modelFormula);
		tranScript.append("'");
		tranScript.append(COMMA);
		tranScript.append("FMDiagramName");// model's name
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
		
		writeSeparatedFeatures(dictElements);
		
		varList.append(String.join(",", elementList()));
		varList.append("]");
		write("\n# Variable list");
		write("\n");
		write(varList.toString());
		write("\nLANG = []\n" + "langList = ['python','c','c++','java',\"javascript\",'matlab','r']\n");
		write("def allPrint():\n");
		// defining a dictionary for separated elements
		write("\tdict = {\n");
		// allprint.append("\tfor j in langList:\n");
		// allprint.append("\t\tLANG = 'All'\n");
		
		
		String joinFunctions = String.join(",\n", dictElements);
		write(joinFunctions);
		write("\n\t}\n");
		write("\t# Model Function\n");
		
		write("\t" + varList.toString() + "\n");
		allprint.append("\t" + tranScript + "\n");
		write(allprint.toString());
		StringBuffer scriptLang = new StringBuffer("if(len(sys.argv) == 1):\n\tLANG = langList\n" + "\tallPrint()\n"
				+ "else:\n" + "\tfor i in sys.argv:\n" + "\t\tif(sys.argv.index(i) == 0):continue\n"
				+ "\t\tif  (i.lower() not in langList):\n" + "\t\t\t" + "LANG = langList\n" + "\t\t\tbreak" + "\n"
				+ "\t\telse:\n" + "\t\t\tLANG.append(str(i.lower()))\n\tallPrint()\n");
		// StringBuffer scriptLang = new StringBuffer("if(len(sys.argv)==1):\n"+"\tLANG
		// = 'All'\n" + "\tallPrint()\n" + "else:\n"
		// + "\tfor i in sys.argv:\n" + "\t\tif(sys.argv.index(i)==0):continue\n"
		// + "\t\tif (i.lower() not in langList):\n" + "\t\t\tfor j in
		// langList:\n"+"\t\t\t\t"
		// + "LANG = str(j)\n" + "\t\t\t\t" + "allPrint()"
		// + "\n" + "\t\telse:\n" + "\t\t\tprint 'in'\n" + "\t\t\tLANG =
		// str(i.lower())\n\t\t\tallPrint()");
		// +"\t\t\tprint LANG\n"
		// + "\t\t\t" + tranScript + "\n");
		write(scriptLang.toString());

	}

	private void writeSeparatedFeatures(Set<String> list) throws IOException {
		if (!splitElements.isEmpty()) {
			String formula;
			for (IntentionalElement e : splitElements) {
				formula = new String(elementMap.get(e));
				list.add("\t\t'" + modifyName(e.getName()) + "'" + COLON + "'" + formula + "'");
			}
		}
	}
	
	public String modifyName(String name) throws IOException {

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
