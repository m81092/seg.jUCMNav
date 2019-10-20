import os
from MathToLang import *

class convertToCP(convertToLang):
	"""docstring for ClassName"""
	def __init__(self,func,dep,args):
		convertToLang.__init__(self,func,dep,args)

	def writeMath(self):
		#how to get file_name and Model name 
		file_name = str(self.dep)+'/'+str(self.dep)+'.mod'
		if not os.path.exists(str(self.dep)):
			os.makedirs(str(self.dep))
		with open(file_name,'w+') as file:
			writeList = []
			variables=''
			# writeList.append('#include <stdio.h>\n\n')
			for x in self.args :
			    variables = variables+'dvar int ' + str(x)+';\n' 
			file_head = '/**** Generated Automatically by jUCMNAv \n Author Amal Ahmed Anda \n *********************************************/ \n'+variables+'\n'
			writeList.append(file_head)
			file_expr = '\t dexpr float expr = ' + str(self.func) +';'+'\n'
			file_expr=file_expr.replace('fmax','maxl').replace('fmin','minl').replace('fabs','abs')
			writeList.append(file_expr)
			file_function = '\n\t'+'maximize '+ 'expr'+';'+'\n\n'
			writeList.append(file_function)
			variables2=''
			for x in self.args :
			    variables2 = variables2+str(x)+'==0 || ' + str(x) + '==100 ;\n' 
			file_Constraint='subject to { \n decisionVar: \n '+ variables2+'\n }\n'
			writeList.append(file_Constraint)
			file.writelines(writeList)