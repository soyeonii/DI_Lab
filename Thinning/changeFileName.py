import os

dir = "C:\GitHub\DI_Lab\Thinning\data\\230414\soyeon"

for i, file_name in enumerate(os.listdir(dir)):
    old_file = os.path.join(dir, file_name)
    new_file = os.path.join(dir, str(i + 150) + '.bmp')
    os.rename(old_file, new_file)

print('OK')