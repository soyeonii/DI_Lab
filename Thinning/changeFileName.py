import os

dir = 'C:\GitHub\DI_Lab\Thinning\\results\jihong'

for file_name in os.listdir(dir):
    old_file = os.path.join(dir, file_name)
    new_file = os.path.join(dir, file_name.split('.')[0])
    os.rename(old_file, new_file)