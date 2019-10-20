import os
from MathToLang import *

class convertToJS(convertToLang):
	"""docstring for convertToJs"""
	def __init__(self,func,dep,args,dict):
		convertToLang.__init__(self,func,dep,args,dict)


	'''
	function myFunction(a,b){
		return a*b;
	}'''
	def writeMath(self):
		#how to get file_name and Model name
		file_name = str(self.dep)+'/'+str(self.dep)+'.js'
		if not os.path.exists(str(self.dep)):
			os.makedirs(str(self.dep))
		with open(file_name,'w+') as file:
			writeList = []
			variables = [ str(x) for x in self.args]
			file_head = 'function '+str(self.dep)+'( '+','.join(variables)+')'+'{'+'\n'
			writeList.append(file_head)

			file_expr = '\texpr = ' + str(self.func) +';'+'\n'
			for key, value in self.dict.items():
				temp = '\t' + str(key) + ' = ' + str(value) + ';' + '\n'
				writeList.append(temp)
			writeList.append(file_expr)
			file_return = '\t'+'return '+ 'expr'+';'+'\n'
			writeList.append(file_return)
			writeList.append('}\n')

			file.writelines(writeList)
