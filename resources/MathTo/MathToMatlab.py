import os
from MathToLang import *
class convertToMatlab(convertToLang):
	"""docstring for convertToMatlab"""
	def __init__(self,func,dep,args, dict):
		convertToLang.__init__(self,func,dep,args, dict)

	'''
	function y = my_func(n)
	y = ...
	end
	'''
	def writeMath(self):
		#how to get file_name and Model name
		file_name = str(self.dep)+'/'+str(self.dep)+'.m'
		if not os.path.exists(str(self.dep)):
			os.makedirs(str(self.dep))
		with open(file_name,'w+') as file:
			writeList = []
			variables = [ str(x) for x in self.args]
			file_head = 'function '+'expr'+'='+str(self.dep)+'( '+','.join(variables)+')'+'\n'
			writeList.append(file_head)

			file_expr = '\texpr = ' + str(self.func) +'\n'
			for key, value in self.dict.items():
				temp = '\t' + str(key) + ' = ' + str(value) + ';' + '\n'
				writeList.append(temp)
			writeList.append(file_expr)
			file_return =  'end'+'\n'
			writeList.append(file_return)

			file.writelines(writeList)
