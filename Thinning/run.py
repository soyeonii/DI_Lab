from thinning import Thinning
from multiprocessing import Process
import os

file_path = './Thinning/data/case1/seunggi/'
file_names = os.listdir(file_path)
total_number = 171
consonant_number = 14

def run(start, end):
    for file_name in file_names[start:end]:
        print('=========== ' + file_name.rjust(7, " ") + ' ===========')
        Thinning(file_path, os.path.splitext(file_name)[0]).run()

if __name__ == "__main__":
    for i in range(0, total_number, consonant_number):
        Process(target=run, args=(i, i+consonant_number)).start()