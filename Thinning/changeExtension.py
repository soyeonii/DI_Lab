import glob
from PIL import Image
import os
from tqdm import tqdm

folder_name = 'soyeon'
extension = 'png'
src_path = f".\\Thinning\\data\\230414\\{folder_name}\\"  # jpg images path
dst_path = f".\\Thinning\\data\\230414\\{folder_name}\\"  # bmp images path

if not os.path.isdir(dst_path):  # make dst dir if it's not existed
    os.mkdir(dst_path)

for jpg_path in tqdm(list(set(glob.glob(src_path+f"*.{extension}", recursive=True)))):
    img = Image.open(jpg_path)
    jpg_name = jpg_path.replace("\\", "/").split("/")[-1]
    bmp_name = jpg_name.replace(extension, "bmp")
    img.save(dst_path+bmp_name)
    img.close()
    os.remove(jpg_path)
