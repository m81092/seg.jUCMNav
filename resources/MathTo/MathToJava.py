import os
from MathToLang import *
'''
func is the formula of the model
args is all the variables and root of the Model
'''
class convertToJava(convertToLang):
	"""docstring for ClassName"""
	def __init__(self,func,dep,args, dict):
		convertToLang.__init__(self,func,dep,args, dict)

	def writeMath(self):
		#how to get file_name and Model name
		file_name = str(self.dep)+'/'+str(self.dep)+'.java'
		if not os.path.exists(str(self.dep)):
			os.makedirs(str(self.dep))
		with open(file_name,'w+') as file:
			writeList = []
			writeList.append('public class '+str(self.dep)+' {\n')
			variables = ['double ' + str(x) for x in self.args]

			file_head = '\t'+'public ' + 'double ' + str(self.dep) +'M ('+','.join(variables)+')'+'{'+'\n'


			writeList.append(file_head)
			file_expr = '\t\tdouble expr = ' + str(self.func) +';'+'\n'
			if self.dict is not None:
				for key, value in self.dict.items():
					temp = '\t\tdouble ' + str(key) + ' = ' + str(value) + ';' + '\n'
					writeList.append(temp)
			writeList.append(file_expr)
			file_return = '\t\t'+'return '+ 'expr'+';'+'\n'
			writeList.append(file_return)
			writeList.append('\t}\n')
			writeList.append('}\n')
			#print writeList
			file.writelines(writeList)
