package seg.jUCMNav.importexport;

import java.io.File;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.draw2d.IFigure;

import fm.Feature;
import fm.FeatureDiagram;
import fm.MandatoryFMLink;
import fm.OptionalFMLink;
import grl.Decomposition;
import grl.ElementLink;
import grl.GRLGraph;
import grl.IntentionalElement;
import seg.jUCMNav.extensionpoints.IURNExport;
import seg.jUCMNav.views.wizards.importexport.ExportWizard;
import urn.URNlink;
import urn.URNspec;
import urncore.IURNDiagram;

public class FeatureToMathS   {
	private FileOutputStream fos;
	private String FMname;
	private String filename;
	public static final String LeftBracker = "(";
	public static final String RightBracker = ")";
	public static final String Comma = ",";
	public static final String Equal = "=";
	public static final String Sym = "symbols";
	public static final String Times = "*";
	public static final String Divide = "/";
	public static final String Plus = "+";
	public static final String Minus = "-";
	public static final String Multi = "*";
	

	private Map<IntentionalElement, StringBuffer> eleForMap;// store elements and the functions .
	private StringBuffer modelFormula;
	private HashSet<String> elementSet = new HashSet<String>();

	// check if there are features in the feature diagram
	public void export(URNspec urn, HashMap mapDiagrams, String filename) throws InvocationTargetException {
		boolean featurFound=false;
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) { 
			IntentionalElement element = (IntentionalElement) it.next();
    	  if (element.getClass().toString().contains("fm")) {
    		  featurFound=true;
    		  break;
    	  }
		}
		if (featurFound) {
    				
		try {
			fos = new FileOutputStream(new File(filename),true);
			for (Iterator iter = mapDiagrams.keySet().iterator(); iter.hasNext();) {
	    		IURNDiagram diagram = (IURNDiagram) iter.next();
	    		//if (diagram instanceof FeatureDiagram)
	    		String diagramName = ExportWizard.getDiagramName(diagram);
	    		String purName =  diagramName.substring(diagramName.lastIndexOf("-") + 1);
	    		//System.out.println("Diagram information="+diagram.toString()+" name="+diagramName+" PureNam= "+purName);
	    		FMname=	purName;
			}
			System.out.println(filename+urn.getName());
			writeFormula(urn);
			System.out.println("After formula");
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
	 * @param s
	 *            the string to write
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

		eleForMap = new HashMap<IntentionalElement, StringBuffer>();
		StringBuffer eleFormula;
		StringBuffer function;
		// initial all the symbols
		write("#inital all the variable\n");
	for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) { 
		
			IntentionalElement element = (IntentionalElement) it.next();
			 System.out.println("after declaration");
			 System.out.println(element.getName().toString());
			if (element instanceof Feature || element.getType().getName().equalsIgnoreCase("Task")) {
			StringBuffer variable = new StringBuffer();
			variable.append(modifyName(element.getName()));
			variable.append(Equal);
			variable.append("Symbol");
			variable.append(LeftBracker);
			variable.append("'");
			variable.append(modifyName(element.getName()));
			variable.append("'");
			variable.append(RightBracker);
			write(variable.toString());
			write("\n"); 
			}
		}
		
	boolean leaf=false;
		// iterate all the leaf element
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
			IntentionalElement element = (IntentionalElement) it.next();
			eleFormula = new StringBuffer();
			function = new StringBuffer();
			function.append(modifyName(element.getName()));
			 leaf=IsItLeaf(element);
			// feature has indicator only should consider as leaf feature
		
			
			// if the element is the leaf
			
				 
				if (  leaf &&(element instanceof Feature || element.getType().getName().equalsIgnoreCase("Task")) && element.getType().getName().compareTo("Indicator") != 0 ) {
					eleFormula.append(modifyName(element.getName()));
				
				elementSet.add("'" + modifyName(element.getName()) + "'");
				 System.out.println(eleFormula.toString() + "----leaf formula");
				eleForMap.put(element, eleFormula);
			}
		}
       for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
    	   IntentionalElement element = (IntentionalElement) it.next();
    	   leaf=IsItLeaf(element);
			// feature has indicator only should consider as leaf feature
		   //if (element.getLinksDest().size() != 0){
			// Iterator it2 = element.getLinksDest().iterator(); it2.hasNext(); 
			//	ElementLink scrLink = (ElementLink) it2.next();
			//	IntentionalElement srcElement = (IntentionalElement) (scrLink.getSrc());
			 //   if (srcElement.getType().getName().compareTo("Indicator") == 0) {
			 //   	leaf=true;
			//    }
			//}
			
			if (((element instanceof Feature || element.getType().getName().equalsIgnoreCase("Task")) && ((element.getLinksDest().size() == 0 || leaf))  && (element.getToLinks().size() != 0 ||  element.getFromLinks().size() != 0))) {
				  eleFormula = new StringBuffer();
				  function = new StringBuffer();
				  function.append(modifyName(element.getName()));
				  eleFormula.append(ExcludeIncludeLink(element));
					// // // // System.out.println(element.getName() +"="+ eleFormula.toString() + "Next iteration for leaf features");
					function.append(Equal);
					function.append(eleFormula);
					write(function.toString());
					write("\n");
					eleForMap.put(element, eleFormula);
			}
        }
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
			
			IntentionalElement element = (IntentionalElement) it.next();
			leaf=IsItLeaf(element);
			// feature has indicator only should consider as leaf feature
		   
			if ((element instanceof Feature || element.getType().getName().equalsIgnoreCase("Task"))  && (element.getLinksDest().size() != 0) && !leaf) {
			  eleFormula = new StringBuffer();
			  function = new StringBuffer();
			  function.append(modifyName(element.getName()));
			  // || 
			 // if ((element.getLinksDest().size() != 0) ) {
				eleFormula.append(writeLink(element));
				// // // // System.out.println(element.getName() +"="+ eleFormula.toString() + "Next iteration");
				function.append(Equal);
				function.append(eleFormula);
				write(function.toString());
				write("\n");
				eleForMap.put(element, eleFormula);
			
		}
	 }
	}
	
	// check leaf features
		public boolean IsItLeaf(IntentionalElement element) throws IOException {
			// feature has indicator only should consider as leaf feature
			if (element.getLinksDest().size() != 0){
			Iterator it2 = element.getLinksDest().iterator(); it2.hasNext(); 
				ElementLink scrLink = (ElementLink) it2.next();
				IntentionalElement srcElement = (IntentionalElement) (scrLink.getSrc());
			    if (srcElement.getType().getName().compareTo("Indicator") == 0) {
			    	return true;
			    }
			    else 
			    	{return false;}
			}
			else
				{return true;}
		}

	// for exclude and include
private StringBuffer ExcludeIncludeLink(IntentionalElement element) throws IOException {
		StringBuffer formulaex = new StringBuffer();
		StringBuffer ExcludeFor = new StringBuffer();
		StringBuffer includeFor = new StringBuffer();
		List<IntentionalElement> ExcludelList = new ArrayList<IntentionalElement>();
		List<IntentionalElement> IncludelList = new ArrayList<IntentionalElement>();
						
		// ToLink include and exclude
		List  urnLinks =  element.getToLinks();
        for (int i = 0; i < urnLinks.size(); i++) {
            if (( (URNlink) urnLinks.get(i)).getToElem() instanceof IntentionalElement) {
                IntentionalElement intElem = (IntentionalElement) ( (URNlink) urnLinks.get(i)).getToElem();
                IntentionalElement SourceElem = (IntentionalElement) ( (URNlink) urnLinks.get(i)).getFromElem();
                
                if (( (URNlink) urnLinks.get(i)).getType().compareToIgnoreCase("exclude") == 0)
                   ExcludelList.add(SourceElem) ;
                
                
            }
        }
     // From element Link
        List  urnLinkf =  element.getFromLinks();
        for (int i = 0; i < urnLinkf.size(); i++) {
            if (( (URNlink) urnLinkf.get(i)).getFromElem() instanceof IntentionalElement) {
                IntentionalElement intElem = (IntentionalElement) ( (URNlink) urnLinkf.get(i)).getToElem();
                //IntentionalElement SourceElem = (IntentionalElement) ( (URNlink) urnLinkf.get(i)).getFromElem();
                
                
                 if (( (URNlink) urnLinkf.get(i)).getType().compareToIgnoreCase("exclude") == 0)
                 ExcludelList.add(intElem ) ;
                 
                 if (( (URNlink) urnLinkf.get(i)).getType().compareToIgnoreCase("include") == 0)
                     IncludelList.add(intElem) ;
                
               }
        }
      // after calculate exclude and include features  
        if (!ExcludelList.isEmpty() ) {
        	//ExcludeFor.append(writeSMax(element,"Max"));
        	ExcludeFor.append("Min(0,");
        	ExcludeFor.append(writeDecomMaxMin(S(element),"Max",0));
        	ExcludeFor.append(Minus);
            ExcludeFor.append(writeSMax(ExcludelList,"Max"));
            ExcludeFor.append(")");
            // // // System.out.println("Exclude function "+ExcludeFor.toString());
        }
        
        if (!IncludelList.isEmpty() ) {
        	//includeFor.append(writeSMax(IncludelList,"Min"));
        	includeFor.append("(( Min(0,(");
        	includeFor.append(writeDecomMaxMin(S(element),"Max", 0));
        	includeFor.append(Plus);
        	includeFor.append(writeSMax(IncludelList,"Min"));
        	includeFor.append(")");
        	includeFor.append(Divide+" 200.0 )");
        	includeFor.append(" ) "+Times+" 100.0 )");
        	if (!ExcludelList.isEmpty() ) {
        	formulaex.append("Min(");
        	formulaex.append(ExcludeFor);
        	formulaex.append(Comma);
        	formulaex.append(includeFor);
        	formulaex.append(")");
        	 }
        	else 
        	 {
        		formulaex=includeFor; 
        	 }
            // // // System.out.println("Include function"+includeFor.toString());
        }
        else { if (!ExcludelList.isEmpty() )
        	formulaex=ExcludeFor;
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
        formulaex=ExcludeIncludeLink(element);
        
      // for other link types
		
		for (Iterator it = element.getLinksDest().iterator(); it.hasNext();) {
			ElementLink scrLink = (ElementLink) it.next();
			IntentionalElement srcElement = (IntentionalElement) (scrLink.getSrc());
			srcList.add(srcElement);
			StrEle.add(modifyName(srcElement.getName()));
			
			// // // System.out.println("linkName"+scrLink.getName());
			
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
		if (!decomList.isEmpty() ) {
			
			 if ( element.getDecompositionType().getName() == "And")
                {
				  funcTpye = "Min";
				  decomFor.append(writeDecomMaxMin(decomList, funcTpye,1));
				 // // // // System.out.println(decomFor.toString() + " Fee decomposition");
				  
                }
				
			
			if (element.getDecompositionType().getName() == "Or") { 
				decomFor.append(writeORSum(decomList));
				// System.out.println(decomFor.toString() + " after return  from OR in writeLink");
				 
			}
			
			if (element.getDecompositionType().getName() == "Xor") {
				
				decomFor.append(writeXORSum(decomList));
			}
			minix=decomFor;
			
		}

		
	 if (!mandatoryList.isEmpty() )
               {
		         
				  funcTpye = "Min";
				  mandatoryFor.append(writeDecomMaxMin(mandatoryList, funcTpye, 1));
				//  // // // System.out.println(mandatoryFor.toString() + " Fee mandatory");
				 if (!decomList.isEmpty()) {
                     minix2.append("Min(");
                     minix2.append(minix);
                     minix2.append(Comma);
				     minix2.append(mandatoryFor);
				     minix2.append(")");
				 }
				 else minix2= mandatoryFor;
               }
			
			 
		if (!optionalList.isEmpty()) {
			
			 opFor.append( writeOptionalSum(optionalList));
			// System.out.println( opFor.toString() + " after return");
			 if (minix2.length() > 0) {
				 minix3.append("Min(");
				 minix3.append(minix2);
				 minix3.append(Comma);
			     minix3.append(opFor);
			     minix3.append(")");
			}
			 else
			 { if (!decomList.isEmpty()) {
                 minix3.append("Min(");
                 minix3.append(opFor);
                 minix3.append(Comma);
			     minix3.append(minix);
			     minix3.append(")");
			 }
			 else minix3=opFor;
				 
			 }
			 }
			 
		  if (minix3.length() > 0)
			  formula=minix3;
			  else if (minix2.length() > 0)
				      formula=minix2;
			  else
				  formula=minix;
		
      if (formulaex.length() > 0) {
    	  // System.out.println(formula+ "before exclude insert");
    	  formula.insert(0,"Min("+formulaex+Comma);
    	  formula.append(")");
    	  // System.out.println(formula+ "baed exclude insert");
    	  
      }
			
		// System.out.println(formula+ "before subEle");
		for (Iterator<IntentionalElement> it = srcList.iterator(); it.hasNext();) {
			IntentionalElement subEle = it.next();
			// if sub element is not the leaf.
			StringBuffer subFor = new StringBuffer();
			if ((subEle instanceof Feature || subEle.getType().getName().equalsIgnoreCase("Task"))&& subEle.getType().getName().compareTo("Indicator") != 0) {
			if (subEle.getLinksDest().size() != 0 && ( (subEle instanceof Feature) && subEle.getType().getName().equalsIgnoreCase("Task"))) {
				if (eleForMap.get(subEle) == null) {
					subFor = writeLink(subEle);
				} else {
					// // // System.out.println("you have subfor!");
					subFor = eleForMap.get(subEle);
				}
				formula = new StringBuffer(
						formula.toString().replaceAll(modifyName(subEle.getName()), subFor.toString()));
			}
			}
			
		}
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
			subfor.append(LeftBracker);
			subfor.append(subst.pop().toString());
			subfor.append(Comma);
			subfor.append(subst.pop().toString());
			subfor.append(RightBracker);
			stMax.push(subfor);
		}
		if (stSize % 2 == 1) {
			stMax.push(subst.pop());
		}
		return MaxmaxFormat(stMax, func);
	}
 
	// return v(f) function for leaf features only
	private String v( IntentionalElement element) throws IOException {
		
		String formula ;
		// && element.getLinksDest().size() == 0 &&
		if (eleForMap.get(element) != null && IsItLeaf(element))
            {
		// // // System.out.println("you have subfor!");
			formula = eleForMap.get(element).toString();
		
	      }
		else {
			 formula=modifyName(element.getName());
			} 
			
		
		return formula;
		}
	
	private StringBuffer writeDecomMaxMin(List<IntentionalElement> list, String func, int v) throws IOException {
		StringBuffer formula = new StringBuffer();
		Stack<StringBuffer> st = new Stack<StringBuffer>();
		if (v==1) {    // for v(f) calculate include and exclude links 
			if (list.size() == 1) {
				formula.append(v(list.get(0)));
			} else if (list.size() == 2) {
				formula.append(func);
				formula.append(LeftBracker);
				formula.append(v(list.get(0)));
				formula.append(Comma);
				formula.append(v(list.get(1)));
				formula.append(RightBracker);
			} else if (list.size() > 2) {

				for (int i = 0; i < list.size(); i++) {
					StringBuffer subfo = new StringBuffer(modifyName(list.get(i).getName()));
					st.add(subfo);
				}
				formula.append(MaxmaxFormat(st, func));
				for (int i = 0; i < list.size(); i++) {
					formula = new StringBuffer(
							formula.toString().replaceAll(modifyName(list.get(i).getName()), v(list.get(i))));
				}
			}
		} // not for v(f)
		else {if (list.size() == 1) {
			formula.append(modifyName(list.get(0).getName()));
		} else if (list.size() == 2) {
			formula.append(func);
			formula.append(LeftBracker);
			formula.append(modifyName(list.get(0).getName()));
			formula.append(Comma);
			formula.append(modifyName(list.get(1).getName()));
			formula.append(RightBracker);
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
		} else  {
			formula.append(LeftBracker);
			for (int i = 0; i < list.size(); i++) {
				formula.append(v(list.get(i)));
				
				if (i+1 < list.size())
				{
					formula.append(Plus);	
				}
			}
			formula.append(RightBracker);
		}
		
		return formula;
	}
	// for s() children
	private List<IntentionalElement> S(IntentionalElement element) throws IOException {
		List<IntentionalElement> formula = new ArrayList<IntentionalElement>();
		Iterator it2 = element.getLinksDest().iterator();
		if (!it2.hasNext()) {
			
				formula.add(element);
					}	
		else {
			for (Iterator it = element.getLinksDest().iterator(); it.hasNext();) {
				ElementLink scrLink = (ElementLink) it.next();
			    IntentionalElement srcElement = (IntentionalElement) (scrLink.getSrc());
			    if (srcElement.getType().getName().compareTo("Indicator") != 0) {
			    formula.add((IntentionalElement) S(srcElement).get(0));
			    }
			    else {
			    	formula.add(element);
			    	break;
			    }
			    
			  	}	
		}
		return formula;	
	}
	// for s() sum
	private StringBuffer writeSSum(List<IntentionalElement> list) throws IOException {
		StringBuffer formula = new StringBuffer();
		//Stack<StringBuffer> st = new Stack<StringBuffer>();
		
		if (list.size() == 1) {
			 formula.append( writeDecomMaxMin(S(list.get(0)),"Max",0));
		} else  {
			
			for (int i = 0; i < list.size(); i++) {
							
			        formula.append(writeDecomMaxMin(S(list.get(i)),"Max",0));
			    
				    if (i+1 < list.size())
				     {
					
					    formula.append(Plus);	
				    }
			}
				
		}
		
		return formula;
	}
	
	// Return the s() function without plus used in optional links in Piecewise
	private StringBuffer writeSMax(List<IntentionalElement> list, String func) throws IOException {
		StringBuffer formula = new StringBuffer();
		List<IntentionalElement> Alllist = new ArrayList<IntentionalElement>();
		//Stack<StringBuffer> st = new Stack<StringBuffer>();
		
		if (list.size() == 1) {
			Alllist.addAll( S(list.get(0)));
		} else  {
			
			for (int i = 0; i < list.size(); i++) {
								
				Alllist.addAll( S(list.get(i)));
			    
				}
			
		}
		formula.append(writeDecomMaxMin(Alllist,func,0));
		return formula;
	}
	// for OR function
	private StringBuffer writeORSum(List<IntentionalElement> list) throws IOException {
		StringBuffer formula = new StringBuffer();
		// Stack<StringBuffer> st = new Stack<StringBuffer>();
		formula.append("Max");
		formula.append(LeftBracker+"0.0"+Comma);
		formula.append(LeftBracker);
		formula.append(writeSum(list));
		formula.append(RightBracker);
		formula.append(Divide);
		formula.append("Max");
		formula.append(LeftBracker+"1.0"+Comma);
		formula.append(writeSSum(list));
		formula.append(RightBracker);
		formula.append(RightBracker);
		formula.append(Times);
		formula.append("100.0");
		// System.out.println("writeOR="+formula.toString());
		return formula;
	}

	private StringBuffer writeXORSum(List<IntentionalElement> list) throws IOException {
		StringBuffer formula = new StringBuffer();
		// Stack<StringBuffer> st = new Stack<StringBuffer>();
		formula.append("Max");
		formula.append(LeftBracker+"0.0"+Comma);
		formula.append(LeftBracker);
		formula.append(writeDecomMaxMin(list,"Max",1));
		formula.append(RightBracker);
		formula.append(Divide);
		formula.append("Max");
		formula.append(LeftBracker+"1.0"+Comma);
		formula.append(writeSSum(list));
		formula.append(RightBracker);
		formula.append(RightBracker);
		formula.append(Times);
		formula.append("100.0");
		return formula;
	}
	private StringBuffer writeOptionalSum(List<IntentionalElement> list) throws IOException {
		StringBuffer formula = new StringBuffer();		
		formula.append("Piecewise");
		formula.append(LeftBracker);
		formula.append(LeftBracker);
		formula.append("100");
		formula.append(Comma);
		formula.append(LeftBracker);
		formula.append(writeSMax(list, "Max"));
		formula.append("==");
		formula.append("0");
		formula.append(RightBracker);
		formula.append(RightBracker);
		formula.append(Comma);
		formula.append(LeftBracker);
		formula.append(LeftBracker);
		formula.append(writeORSum(list));
		formula.append(RightBracker);
		formula.append(Comma);
		formula.append("True");
		formula.append(RightBracker);
		formula.append(RightBracker);
		
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
		function.append(Equal);
		modelFormula = ModelFromRoot(urn);
			
		function.append(modelFormula);
		write("#The function of Model\n");
		write(function.toString());
		write("\n");
	}

	private StringBuffer ModelFromRoot(URNspec urn) throws IOException {
		StringBuffer formula = new StringBuffer();
				
		for (Iterator it = urn.getGrlspec().getIntElements().iterator(); it.hasNext();) {
			IntentionalElement ele = (IntentionalElement) it.next();
			// Get formula of Root feature
			if ((ele instanceof Feature || ele.getType().getName().equalsIgnoreCase("Task")) && ele.getLinksSrc().size() <= 0 && ele.getLinksDest().size() > 0) {
			    System.out.print("Fee src=0 Root feature="+ele.getName().toString());
			   formula = eleForMap.get(ele);
			}
		}
		return formula;
	}


	// not working
	private void writeTranslation(URNspec urn) throws IOException {
		// indicator
		String modelName = modifyName(FMname);
		write("modelName " + Equal + " '" + modifyName(FMname) + "' " + "\n");
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
		write(varList.toString());
		write("#variable list");
		write("\n");

		StringBuffer tranScript = new StringBuffer();
		tranScript.append("Translate");
		tranScript.append(LeftBracker);
		tranScript.append("'");
		tranScript.append(modelFormula);
		tranScript.append("'");
		tranScript.append(Comma);
		tranScript.append("modelName");// model's name
		tranScript.append(Comma);
		tranScript.append("List");
		//tranScript.append(urn.getName());
		tranScript.append(Comma);
		tranScript.append("LANG");
		tranScript.append(RightBracker);
		write("LANG = ''\n" + "langList = ['python','c','c++','java',\"javascript\",'matlab','r']\n");

		StringBuffer allprint = new StringBuffer();
		allprint.append("def allPrint():\n");
		allprint.append("\tfor j in langList:\n");
		allprint.append("\t\tLANG = str(j)\n");
		allprint.append("\t\t" + tranScript + "\n");
		write(allprint.toString());
		StringBuffer scriptLang = new StringBuffer("if(len(sys.argv)==1):\n" + "\tallPrint()\n" + "else:\n"
				+ "\tfor i in sys.argv:\n" + "\t\tif(sys.argv.index(i)==0):continue\n"
				+ "\t\tif  (i.lower() not in langList):\n" + "\t\t\tfor j in langList:\n"+"\t\t\t\t"
						+ "LANG = str(j)\n" + "\t\t\t\t" + "allPrint()"
				+ "\n" + "\t\telse:\n" + "\t\t\tprint 'in'\n" + "\t\t\tLANG = str(i.lower())\n"
				// +"\t\t\tprint LANG\n"
				+ "\t\t\t" + tranScript + "\n");
		write(scriptLang.toString());

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
