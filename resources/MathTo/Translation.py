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
    if tra == 0:
        func = formula

    # func = simplify(formula)
    # print "after func"
    # func=formula

    if 'java' in Type:
        for key, value in dict.items():
            jval = jcode(value)
            dict[key] = jval
        convertToJava(jcode(func), dep, args, dict).writeMath()
        print('java')

    if 'python' in Type:
        print ('python')
        from sympy.printing.pycode import PythonCodePrinter, pycode
        pr = PythonCodePrinter()
        for key, value in dict.items():
            pval = pr.doprint(value)
            dict[key] = pval
        convertToPy(pr.doprint(func), dep, args, dict).writeMath()

    if 'javascript' in Type:
        for key, value in dict.items():
            jsval = jscode(value)
            dict[key] = jsval
        convertToJS(jscode(func), dep, args, dict).writeMath()
        print ('js')

    if 'matlab' in Type:
        print ('matlab')
        for key, value in dict.items():
            mval = octave_code(value)
            dict[key] = mval
        convertToMatlab(octave_code(func), dep, args, dict).writeMath()

    if 'c' in Type:
        for key, value in dict.items():
            cval = ccode(value)
            dict[key] = cval
        convertToC(ccode(func), dep, args, dict).writeMath()
        print ('c')

    if 'c++' in Type:
        for key, value in dict.items():
            cpval = cxxcode(value)
            dict[key] = cpval
        convertToCpp(cxxcode(func), dep, args, dict).writeMath()
        print ('c++')

    if 'r' in Type:
        for key, value in dict.items():
            rval = rcode(value)
            dict[key] = rval
        convertToR(rcode(func), dep, args, dict).writeMath()
        print ('R')
