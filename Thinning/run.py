from thinning import Thinning
import threading
import os

read_path = "./Thinning/data/230414/jihong"
write_path = "./Thinning/result/230414"
file_names = os.listdir(read_path)
n = len(file_names)


def run(start, end):
    for file_name in file_names[start:end]:
        print("=========== " + file_name.rjust(7, " ") + " ===========")
        Thinning(read_path, write_path, os.path.splitext(file_name)[0]).run()


if __name__ == "__main__":
    for i in range(0, n, 10):
        threading.Thread(target=run, args=(i, i + 10)).start()