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
import grl.kpimodel.Indicator;
import seg.jUCMNav.extensionpoints.IURNExport;
import seg.jUCMNav.importexport.FeatureToMath;
import seg.jUCMNav.views.wizards.importexport.ExportWizard;
import urn.URNspec;
import urncore.IURNDiagram;
import urncore.IURNNode;

/**
 * this class export the URN model into sympy function
 * 
 * @author Yuxuan Fan and Amal Anda
 *
 */
public class ExportGRLMathS implements IURNExport {
	private String GRLname;
	FeatureToMath FeatureExport = new FeatureToMath( );
	private FileOutputStream fos;
	public static final String LeftBracker = "(";
	public static final String RightBracker = ")";
	public static final String Comma = " , ";
	public static final String Equal = " = ";
	public static final String Sym = "symbols";
	public static final String Times = "*";
	public static final String Divide = " / ";
	public static final String Plus = " + ";
	public static final String Minus = " - ";
	public static final String Multi = " * ";
	

	private Map<IntentionalElement, StringBuffer> eleForMap;// store elements and the functions .

	private Map<Actor, StringBuffer> actorForMap;
	private StringBuffer modelFormula;
	private HashSet<String> elementSet = new HashSet<String>();

	@Override
	public void export(URNspec urn, HashMap mapDiagrams, FileOutputStream fos) throws InvocationTargetException {
		// TODO Auto-generated method stub
		// not used
	}

	@Override
	public void export(URNspec urn, HashMap mapDiagrams, String filename) throws InvocationTargetException {
		 
		 
		try {
			fos = new FileOutputStream(filename);
			
			// to run the functions
			writeHead(urn);
			boolean GRLFound=false;
			for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) { 
				IntentionalElement element = (IntentionalElement) it.next();
	    	  if (element.getType().toString().contains("Goal")) {
	    		  GRLFound=true;
	    		  break;
	    	  }
			}
			
			if (GRLFound) {
				for (Iterator iter = mapDiagrams.keySet().iterator(); iter.hasNext();) {
		    		IURNDiagram diagram = (IURNDiagram) iter.next();
		    		if ((diagram instanceof GRLGraph) && !(diagram instanceof FeatureDiagram)) {
		    		String diagramName = ExportWizard.getDiagramName(diagram);
		    		String purName =  diagramName.substring(diagramName.lastIndexOf("-") + 1);
		    		//////System.out.println("Diagram information="+diagram.toString()+" name="+diagramName+" PureNam= "+purName);
		    		GRLname=	purName;
		    		}
				}
			   writeFormula(urn);
			   writeActor(urn);
			  //writeIndicator(urn);
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
	 * Write the string to the file output stream.
	 * 
	 * @param s
	 *            the string to write
	 * @throws IOException
	 */
	public void write(String s) throws IOException {
		if (s != null && s.length() > 0) {
			fos.write(s.getBytes());
		}
	}

	private void writeHead(URNspec urn) throws IOException {

		String name = FeatureExport.modifyName(urn.getName());
		write("from MathTo import * \n" + "from sympy import *\n");
		write("import sys\n");
		write("import os");
		write("\n");
		write("\n");
		write("# Creating a folder using Model name\n");
		write("ModelName= '");
		write(name + "'\n");
		write("if not os.path.exists(ModelName):\n");
		write("   os.makedirs(ModelName)\n");
		write("os.chdir(ModelName)");
		write("\n");
		write("\n");
	}
 
	/***********************************************************
	 * Check if the element is part of GRL diagrams Amal Ahmed Anda
	 */
	private Boolean GRLelement(IntentionalElement element) {
       for (Iterator re = element.getRefs().iterator(); re.hasNext();) {
		 	
		 	IntentionalElementRef ref =  (IntentionalElementRef) re.next(); 
		 	if (!(ref.getDiagram() instanceof FeatureDiagram)) 
		 		return true; 
	   }
		return false;
	}
	/**
	 * create formulas for each elements
	 * 
	 * @param urn
	 * @throws IOException
	 */
	private void writeFormula(URNspec urn) throws IOException {

		eleForMap = new HashMap<IntentionalElement, StringBuffer>();
		StringBuffer eleFormula;
		StringBuffer function;
		// initial all the symbols
		
		write("#inital all the variable\n");
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) { 
			IntentionalElement element = (IntentionalElement) it.next();
			
			if (GRLelement(element)) {
			  StringBuffer variable = new StringBuffer();
			  variable.append(FeatureExport.modifyName(element.getName()));
			  variable.append(Equal);
			  variable.append("Symbol");
			  variable.append(LeftBracker);
			  variable.append("'");
			  variable.append(FeatureExport.modifyName(element.getName()));
			  variable.append("'");
			  variable.append(RightBracker);
			  write(variable.toString());
			  write("\n");
			}
		}
		
		
		// iterate all the leaf element
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
			IntentionalElement element = (IntentionalElement) it.next();
			
		if (GRLelement(element)) { // !(element instanceof Feature)
			eleFormula = new StringBuffer();
			function = new StringBuffer();
			function.append(FeatureExport.modifyName(element.getName()));
			// if the element is the leaf
			if (element.getLinksDest().size() == 0) {
				// ////System.out.println(element.getName() + "leaf");
				if (element.getType().getName().compareTo("Indicator") == 0) {
					Indicator indicator = (Indicator) element;
//					if (indicator.getWorstValue() == indicator.getTargetValue()) {
//						eleFormula.append(FeatureExport.modifyName(element.getName()));
//					} else {
//						StringBuffer indicatorFor = indicatorFor(element);
//						eleFormula.append(indicatorFor);
//						function.append(Equal);
//						function.append(eleFormula);
//					}
				} else {
					eleFormula.append(FeatureExport.modifyName(element.getName()));
				}
				elementSet.add("'" + FeatureExport.modifyName(element.getName()) + "'");
				eleForMap.put(element, eleFormula);
			}
		}
		}
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
			IntentionalElement element = (IntentionalElement) it.next();
			
			if (GRLelement(element)) {
			  eleFormula = new StringBuffer();
			  function = new StringBuffer();
			  function.append(FeatureExport.modifyName(element.getName()));

			 if (element.getLinksDest().size() != 0) {
				 eleFormula.append(writeLink(element));
				 function.append(Equal);
				 function.append(eleFormula);
				 write(function.toString());
				 write("\n");
				 eleForMap.put(element, eleFormula);
			 }
		  }
		}
	}

	/**
	 * 
	 * @param element
	 * @return
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
		} // for
			// first decomposition; second contribution; third dependency
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
			conFor.append(LeftBracker);
			conFor.append("0.0");
			conFor.append(Comma);
			conFor.append("Min");
			conFor.append(LeftBracker);
			conFor.append("100.0");
			conFor.append(Comma);
			conFor.append(LeftBracker);
			List<String> conTimesList = new ArrayList<String>();
			for (int i = 0; i < conLink.size(); i++) {
				//////System.out.println("contributuin ele:" + conList.get(i).getName());
				String conTimes = new String();
				conTimes = Integer.toString(((Contribution) conLink.get(i)).getQuantitativeContribution()) + Times
						+ FeatureExport.modifyName(conList.get(i).getName());
				conTimesList.add(conTimes);
			}
			if (!decomList.isEmpty()) {
				conTimesList.add(decomFor + Times + "100.0");
			}

			String joined = String.join("+", conTimesList);
			conFor.append(joined);
			conFor.append(RightBracker);
			conFor.append(Divide);
			conFor.append("100.0");
			conFor.append(RightBracker);
			conFor.append(RightBracker);

			formula = conFor;
		}
		if (!depenList.isEmpty()) {
			depenFor.append(writeDepenMaxMin(depenList, formula, element));
			formula = depenFor;
		}
		////System.out.println(formula);
		for (Iterator it = srcList.iterator(); it.hasNext();) {
			IntentionalElement subEle = (IntentionalElement) it.next();
			// if sub element is not the leaf.
			StringBuffer subFor = new StringBuffer();
			if (subEle.getLinksDest().size() != 0) {
				if (eleForMap.get(subEle) == null) {
					subFor = writeLink(subEle);
				} else {
					////System.out.println("you have subfor!");
					subFor = eleForMap.get(subEle);
				}
				formula = new StringBuffer(
						formula.toString().replaceAll(FeatureExport.modifyName(subEle.getName()), subFor.toString()));
			}
			// if the element is indicator
			if (subEle.getType().getName().compareTo("Indicator") == 0) {
				StringBuffer indicatorFor = new StringBuffer();
				if (eleForMap.get(subEle) == null) {
					indicatorFor = indicatorFor(subEle);
				} //else {  // replace indicator name with formula
				//	indicatorFor = eleForMap.get(subEle);
				//	formula = new StringBuffer(
				//			formula.toString().replaceAll(FeatureExport.modifyName(subEle.getName()), indicatorFor.toString()));
				//}
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
			formula.append(LeftBracker);
			formula.append(FeatureExport.modifyName(FeatureExport.modifyName(list.get(0).getName())));
			formula.append(Comma);
			formula.append(FeatureExport.modifyName(FeatureExport.modifyName(list.get(1).getName())));
			formula.append(RightBracker);
		} else if (list.size() > 2) {

			for (int i = 0; i < list.size(); i++) {
				StringBuffer subfo = new StringBuffer(FeatureExport.modifyName(list.get(i).getName()));
				st.add(subfo);
			}
			formula.append(FeatureExport.MaxmaxFormat(st, func));
		}
		return formula;
	}

	private StringBuffer writeDepenMaxMin(List<IntentionalElement> list, StringBuffer func, IntentionalElement element) throws IOException {

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
		actorForMap = new HashMap<Actor, StringBuffer>();
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
					//System.out.println("NOOOOOOOElement");
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
						//System.out.println("Element haaaaaaaaaaas wait="+ele.getName()+" Q="+ele.getImportanceQuantitative());
						quaList.add(eleQua);
						sumQua += eleQua;
					}
				}
			}

			if (sumQua == 0 && hasEleInActor == true) {// there are no weighted elements in actor
				//System.out.println("sum ================================== 0 ");
				for (int i = 0; i < eleList.size(); i++) {
					IntentionalElement ele = (IntentionalElement) (eleList.get(i));
					StringBuffer eleFormula = new StringBuffer();
					eleFormula.append(LeftBracker);
					eleFormula.append(eleForMap.get(ele));
					eleFormula.append(RightBracker);
					if (ele.getLinksSrc().size() == 0) {
						
						actorTimesQua.add(eleFormula + Times + "100.0");
						//System.out.println("Element haaaaaaaaaaas wait in first sum="+ele.getName()+" Q="+ele.getImportanceQuantitative());
						sumQua += 100;
					} else {
						// give the weight to top-level elements;
						//System.out.println("give the weight to top-level elements");
						IntentionalElement srcElement = (IntentionalElement) (((ElementLink) (ele.getLinksSrc().get(0)))
								.getDest());

						if (eleList.contains(srcElement) == false) {
							actorTimesQua.add(eleFormula + Times + "100.0");
							//System.out.println("Element haaaaaaaaaaas wait in thae last sum="+ele.getName()+" Q="+ele.getImportanceQuantitative());
							sumQua += 100;
						}
					}
				} // for
			} // if(sumQua==0)
			if (sumQua > 0) {// there are some elements weighted
				////System.out.println("sum! = 0 ");
				//System.out.println("there are some elements weighted"+sumQua);
				for (int i = 0; i < eleList.size(); i++) {
					IntentionalElement ele = (IntentionalElement) (eleList.get(i));
					if (ele.getImportanceQuantitative() == 0) {
						continue;
					}
					//actorTimesQua.add(eleForMap.get(ele) + Times + "800.0");
					
					actorTimesQua.add(eleForMap.get(ele) + Times + Integer.toString(ele.getImportanceQuantitative()));
				}
			}
			if (!hasEleInActor)
				formula.append("0");
			else {
				formula.append(LeftBracker);
				//System.out.println("Actoooooooooooooooooooor actortimequantity: "+actorTimesQua+" Sum="+sumQua+" dNum="+dNum);
				String joined = String.join("+", actorTimesQua);
				//System.out.println(joined);
				formula.append(joined);
				formula.append(RightBracker);
				formula.append(Divide);
				
				formula.append(Integer.toString(Math.max(sumQua, dNum)));
			
			}
			function.append(Equal);
			function.append(formula);
			write("#Actor function\n");
			//System.out.println("Actoooooooooooooooooooor: "+function.toString());
			write(function.toString());
			write("\n");
			actorForMap.put(actor, formula);
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
	private void writeModel(URNspec urn) throws IOException {
		modelFormula = new StringBuffer();
		StringBuffer function = new StringBuffer();
		List<Actor> actorList = new ArrayList<Actor>();
		List<Actor> actHasWeight = new ArrayList<Actor>();
		List<String> actorTimesWeight = new ArrayList<String>();

		int sumQua = 0;
		int dNum = 100;
		function.append(FeatureExport.modifyName(urn.getName()));
		function.append(Equal);
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
					actorRe.append(LeftBracker);
					actorRe.append(actorForMap.get(actorList.get(i)));
					actorRe.append(RightBracker);
					actorTimesWeight.add(actorRe + Times + "100.0");
				}
				sumQua = 100 * actorList.size();
			} else {
				for (int i = 0; i < actorList.size(); i++) {
					int actorQua = actorList.get(i).getImportanceQuantitative();
					StringBuffer actorRe = new StringBuffer();
					actorRe.append(LeftBracker);
					actorRe.append(actorForMap.get(actorList.get(i)));
					actorRe.append(RightBracker);
					actorTimesWeight.add(actorForMap.get(actorList.get(i)) + Times +actorQua);
					sumQua += actorQua;
				}
			}
			String joined = String.join("+", actorTimesWeight); 
			
			modelFormula.append(LeftBracker);
			modelFormula.append(joined);
			
			modelFormula.append(RightBracker);
			modelFormula.append(Divide);
			
			modelFormula.append(Integer.toString(Math.max(sumQua, dNum)));
			
			
		}
		function.append(modelFormula);
		write("#The function of Model\n");
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
				////System.out.println("sum == 0 ");
				for (int i = 0; i < eleList.size(); i++) {
					IntentionalElement ele = (IntentionalElement) (eleList.get(i));
					StringBuffer eleFormula = new StringBuffer();
					eleFormula.append(LeftBracker);
					eleFormula.append(eleForMap.get(ele));
					eleFormula.append(RightBracker);
					if (ele.getLinksSrc().size() == 0) {
						// actorTimesQua.add(ele.getName() + Times + "100"); Amal
						actorTimesQua.add(eleFormula + Times + "100.0");
						sumQua += 100;
					} else {
						// give the weight to top-level elements;
						IntentionalElement srcElement = (IntentionalElement) (((ElementLink) (ele.getLinksSrc().get(0)))
								.getDest());

						if (eleList.contains(srcElement) == false) {
							actorTimesQua.add(eleFormula + Times + "100.0");
							sumQua += 100;
						}
					}
				} // for
			} // if(sumQua==0)
			else {// there are some elements weighted
				////System.out.println("sum! = 0 ");
				for (int i = 0; i < eleList.size(); i++) {
					IntentionalElement ele = (IntentionalElement) (eleList.get(i));
					if (ele.getImportanceQuantitative() == 0) {
						continue;
					}
					actorTimesQua.add(eleForMap.get(ele) + Times + "100.0");
				}
			}
		}
		formula.append(LeftBracker);
		String joined = String.join("+ ", actorTimesQua); 
		formula.append(joined);
	
		formula.append(RightBracker);
		formula.append(Divide);
		
		formula.append(Integer.toString(Math.max(sumQua, dNum)));
		
		return formula;

	}

	private StringBuffer indicatorFor(IntentionalElement intElement) throws IOException {
		StringBuffer formula = new StringBuffer();
		Indicator indicator = (Indicator) intElement;
		String currentName = new String(FeatureExport.modifyName(indicator.getName()));
		double worst = 100;
		double target = 200;
		double threshold = 50;
		formula = new StringBuffer();
		formula.append("Piecewise");
		formula.append(LeftBracker);
		if ((worst == threshold) && (threshold == target)) {// warning
			////System.out.println("Warning: the three value should not be equal");
		}
		if (worst < target) {
			formula.append(LeftBracker);
			formula.append("100");
			formula.append(Comma);
			formula.append(currentName);
			formula.append(">=");
			formula.append(Double.toString(target));
			formula.append(RightBracker);
			formula.append(Comma);

			formula.append(LeftBracker);//
			formula.append("abs( ");
			formula.append(LeftBracker);// (x-th)
			formula.append(currentName);
			formula.append(Minus);
			 
			formula.append(Double.toString(threshold));
		 
			formula.append(RightBracker);
			formula.append(Divide);
			double diNum = (target - threshold); // * 200; removed by Amal
			 
			formula.append(Double.toString(diNum));
			 
			formula.append(RightBracker);
			formula.append(Multi); // added by Amal
			formula.append("50"); // added by Amal
			formula.append(Plus);
			formula.append("50");
			formula.append(Comma);

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
			formula.append(RightBracker);
			formula.append(Comma);
			formula.append(LeftBracker);
			formula.append("-abs( ");
			formula.append(LeftBracker);
			
			formula.append(currentName);
			
			formula.append(Minus);
			 
			formula.append(Double.toString(threshold));
			 
			formula.append(RightBracker);
			formula.append(Divide);
			double diNum2 = (worst - threshold); // * 200; removed by Amal
			 
			formula.append(Double.toString(diNum2));
			 
			formula.append(RightBracker);
			formula.append(Multi); // added by Amal
			formula.append("50"); // added by Amal
			formula.append(Plus);
			formula.append("50");

			formula.append(Comma);
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
			formula.append(RightBracker);
			formula.append(Comma);

			formula.append(LeftBracker);
			formula.append("0");
			formula.append(Comma);
			formula.append("True");
			formula.append(RightBracker);

			formula.append(RightBracker);
		}
		if (worst > target) {
			formula.append(LeftBracker);
			formula.append("100");
			formula.append(Comma);
			formula.append(currentName);
			formula.append("<=");
			formula.append(Double.toString(target));
			formula.append(RightBracker);
			formula.append(Comma);

			formula.append(LeftBracker);
			formula.append("abs( ");
			formula.append(LeftBracker);
			formula.append(currentName);
			formula.append(Minus);
			 
			formula.append(Double.toString(threshold));
			 
			formula.append(RightBracker);
			formula.append(Divide);
			double diNum = (threshold - target); // (target - threshold) * 200; changed by Amal
			 
			formula.append(Double.toString(diNum));
			 
			formula.append(RightBracker);
			formula.append(Multi); // added by Amal
			formula.append("50"); // added by Amal
			formula.append(Plus);
			formula.append("50");

			formula.append(Comma);
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
			formula.append(RightBracker);
			formula.append(Comma);

			formula.append(LeftBracker);
			formula.append("-abs( ");
			formula.append(LeftBracker);
			formula.append(currentName);
			formula.append(Minus);
			 
			formula.append(Double.toString(threshold));
			 
			formula.append(RightBracker);
			formula.append(Divide);
			double diNum2 = (threshold- worst); // (worst - threshold); * 200; changed by Amal
			 
			formula.append(Double.toString(diNum2));
			 
			formula.append(RightBracker);
			formula.append(Multi); // added by Amal
			formula.append("50"); // added by Amal
			formula.append(Plus);
			formula.append("50");

			formula.append(Comma);
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
			formula.append(RightBracker);

			formula.append(Comma);

			formula.append(LeftBracker);
			formula.append("0");
			formula.append(Comma);
			formula.append("True");
			formula.append(RightBracker);

			formula.append(RightBracker);
		}
		return formula;
	}

	//  working
	private void writeTranslation(URNspec urn) throws IOException {
		write("GRLDiagramName " + Equal + " '" + FeatureExport.modifyName(GRLname) + "' " + "\n");
		StringBuffer varList = new StringBuffer();
		varList.append("List");
		//varList.append(urn.getName());
		varList.append(Equal);
		varList.append("[");
		List<String> eleList = new ArrayList<String>();
		eleList.addAll(elementSet);
		// String message = String.join("-", list); 
		varList.append(String.join(",", eleList));
		varList.append("]");
		write("\n#variable list");
		write("\n");
		write(varList.toString());
		

		StringBuffer tranScript = new StringBuffer();
		tranScript.append("Translate");
		tranScript.append(LeftBracker);
		tranScript.append("'");
		tranScript.append(modelFormula);
		tranScript.append("'");
		tranScript.append(Comma);
		tranScript.append("GRLDiagramName");// model's name
		tranScript.append(Comma);
		tranScript.append("List");
		//tranScript.append(urn.getName());
		tranScript.append(Comma);
		tranScript.append("LANG");
		tranScript.append(RightBracker);
		write("\nLANG = []\n" + "langList = ['python','c','c++','java',\"javascript\",'matlab','r']\n");

		StringBuffer allprint = new StringBuffer();
		allprint.append("def allPrint():\n");
		allprint.append("\t\t"+varList.toString()+"\n");
		allprint.append("\t\t" + tranScript + "\n");
		write(allprint.toString());
		write("\t\t#Indicators \n");
		
		
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
			IntentionalElement IndicatorV = (IntentionalElement) it.next();
			
			   if (IndicatorV.getType().getName().compareTo("Indicator") == 0) {
				   String formulaIn =  eleForMap.get(IndicatorV).toString();
				   String ind="'"+FeatureExport.modifyName(IndicatorV.getName())+"'";
				write("\t\tList=["+ind+"");
				for (Iterator ite = elementSet.iterator(); ite.hasNext();) {
					String var = (String) ite.next();
					
					if ((formulaIn.contains(var.subSequence(1, var.length()-1))) && (var.compareTo(ind)!=0)) {
						write(","+var);
					}
				}
				write("]\n");
			    write("\t\tprint '"+FeatureExport.modifyName(IndicatorV.getName())+"'\n");
				write("\t\tTranslate('"+eleForMap.get(IndicatorV)+"', List[0], List, LANG)\n");
			   }
				}

	
		StringBuffer scriptLang = new StringBuffer("if(len(sys.argv)==1):\n\tLANG = langList\n" + "\tallPrint()\n" + "else:\n"
				+ "\tfor i in sys.argv:\n" + "\t\tif(sys.argv.index(i)==0):continue\n"
				+ "\t\tif  (i.lower() not in langList):\n" + "\t\t\t"
						+ "LANG = langList\n" + "\t\t\tbreak"
				+ "\n" + "\t\telse:\n" + "\t\t\tLANG.append(str(i.lower()))\n\tallPrint()\n");
				
				//+ "\t\t\t" + tranScript + "\n");
		write(scriptLang.toString()+ "\n");
		//================================
		//write("\t\t\t#Indicators \n");
		//for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
		//	IntentionalElement IndicatorV = (IntentionalElement) it.next();
				
		//	   if (IndicatorV.getType().getName().compareTo("Indicator") == 0) {
		//		write("\t\t\tList=["+FeatureExport.modifyName(IndicatorV.getName())+"]\n");
		//	    write("\t\t\tprint '"+FeatureExport.modifyName(IndicatorV.getName())+"'\n");
		//		write("\t\t\tTranslate('"+eleForMap.get(IndicatorV)+"', List[0], List, LANG)\n");
		//	   }
		//		}

	}

	
	
}
