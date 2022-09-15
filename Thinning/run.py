from thinning import Thinning
import os
#from run_morphology import Morphology


# def case123_sep(img_path, case):
#    sep_case_model = Sep_Case_Model(img_path, case)
#    case_sep = sep_case_model.run()
#
#    sep_con_vow = Sep_Con_Vow(img_path, case, case_sep)
#    sep_con_vow.run()


file_path = './data/'
file_names = os.listdir(file_path)
for file_name in file_names:
    reprocessing = Thinning(os.path.splitext(file_name)[0])
    reprocessing.run()

# Thinning('./data/0.bmp').run()

#img_path += 'preprocess.bmp'
#case_model = Case_Model(img_path)
#case = case_model.run()

# if case == 'case1' or case == 'case2' or case == 'case3':
#    case123_sep(img_path, case)


#print('case: ', case)
#print('sep', case_sep)
print('세선화 완료')
