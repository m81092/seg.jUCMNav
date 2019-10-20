# Translation.py
# Modified by Amal Ahmed Anda

from sympy import *
from MathToJava import *
from MathToC import *
from MathToCpp import *
from MathToJS import *
from MathToPython import *
from MathToMatlab import *
from MathToR import *
from MathToLang import *

init_printing()


def Translate(
    formula,
    dep,
    args,
    Type,
    tra,
    dict
    ):

    i = 0
    for arg in args:
        arg = str(arg) + '=' + 'Symbol' + '(' + "'" + str(arg) + "'" \
            + ')'
        i = i + 1
        exec(arg)
    if tra == 2:
        func = simplify(eval(formula))
        for key, value in dict.items():
            val = simplify(eval(value))
            dict[key] = val
    if tra == 1:
        func = simplify(formula)
        for key, value in dict.items():
            val = simplify(value)
            dict[key] = val
    if tra == 0:
        func = formula

    # func = simplify(formula)
    # print "after func"
    # func=formula

    if 'java' in Type:
        jDict = dict.copy()
        for key, value in jDict.items():
            jval = jcode(value)
            jDict[key] = jval
        convertToJava(jcode(func), dep, args, jDict).writeMath()
        print('java')

    if 'python' in Type:
        pDict = dict.copy()
        from sympy.printing.pycode import PythonCodePrinter, pycode
        pr = PythonCodePrinter()
        for key, value in pDict.items():
            pval = pr.doprint(value)
            pDict[key] = pval
        convertToPy(pr.doprint(func), dep, args, pDict).writeMath()
        print ('python')

    if 'javascript' in Type:
        jsDict = dict.copy()
        for key, value in jsDict.items():
            jsval = jscode(value)
            jsDict[key] = jsval
        convertToJS(jscode(func), dep, args, jsDict).writeMath()
        print ('js')

    if 'matlab' in Type:
        mDict = dict.copy()
        for key, value in mDict.items():
            mval = octave_code(value)
            mDict[key] = mval
        convertToMatlab(octave_code(func), dep, args, mDict).writeMath()
        print ('matlab')

    if 'c' in Type:
        cDict = dict.copy()
        for key, value in cDict.items():
            cval = ccode(value)
            cDict[key] = cval
        convertToC(ccode(func), dep, args, cDict).writeMath()
        print ('c')

    if 'c++' in Type:
        cpDict = dict.copy()
        for key, value in cpDict.items():
            cpval = cxxcode(value)
            cpDict[key] = cpval
        convertToCpp(cxxcode(func), dep, args, cpDict).writeMath()
        print ('c++')

    if 'r' in Type:
        rDict = dict.copy()
        for key, value in rDict.items():
            rval = rcode(value)
            rDict[key] = rval
        convertToR(rcode(func), dep, args, rDict).writeMath()
        print ('R')
