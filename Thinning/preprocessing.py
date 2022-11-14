import cv2
import numpy as np
import os

class Preprocessing:
    def delete_background(self, img, size, padding):
        array = np.asarray(img)
        # print(array[50])
        idx = np.where(array < 1)
        max_x = np.max(idx[1])
        min_x = np.min(idx[1])
        max_y = np.max(idx[0])
        min_y = np.min(idx[0])
        img = img[min_y:max_y, min_x:max_x]
        img = cv2.resize(img, (size - padding * 2, size - padding * 2))

        tmp = np.ones((size, size))
        tmp[padding:size-padding, padding:size-padding] = img

        return tmp

    def save_img(self, img, file_name, end):
        img = img * 255
        path = './Thinning/results/' + file_name
        if not os.path.isdir(path):
            os.mkdir(path)
        cv2.imwrite(path + '/' + file_name + end, img)

    def draw_img(self, points):
        img = np.ones((64, 64))
        for x, y in points:
            img[x][y] = 0
        return img

    def simplify(self, graph):
        size = 2    # frame 크기
        space = 3   # 최소 point 간격
        # start = time.time()
        points = []
        for i in range(0, 64, size):
            for j in range(0, 64, size):
                tf = graph[i:i+size, j:j+size] == 0
                if tf.any():  # 흑백 부분이 있는지 여부
                    # frame 내의 흑백 부분 찾기
                    tmp = np.where(tf)
                    x = i + tmp[0][0]
                    y = j + tmp[1][0]
                    # 설정한 간격 이내에 다른 point가 있는지 여부
                    if not (graph[x-space:x+space, y-space:y+space] == 2).any():
                        graph[x][y] = 2  # 없다면 2로 point 표시
                        points.append((x, y))
        print('------------------------------ points ------------------------------')
        print(points,', ', len(points))
        # end = time.time()
        # print(f"{end - start:.5f} sec")
        return points

    def devide(self, points):
        n = len(points)
        size = 6
        result = []
        stack = []
        check = [[False] * 64 for _ in range(64)]
        num = 0
        def DFS(x, y):
            nonlocal num
            check[x][y] = True
            stack.append((x, y))
            surround_points = []
            for i in range(x - size, x + size):
                for j in range(y - size, y + size):
                    if (i, j) in points and not check[i][j]:
                        surround_points.append((i, j))
            for i, j in surround_points:
                if num == 0 or num == self.get_num([i - stack[-1][0], j - stack[-1][1]]):
                    if num == 0:
                        num = self.get_num([i - stack[-1][0], j - stack[-1][1]])
                    DFS(i, j)
        for i in range(n):
            x = points[i][0]
            y = points[i][1]
            if not check[x][y]:
                DFS(x, y)
                result.append(stack.copy())
                stack.clear()
                num = 0
        return result

    def get_num(self, diff):
        if diff[1] == 0:
            return 1 if diff[0] >= 0 else 2
        elif diff[0] == 0:
            return 3 if diff[1] >= 0 else 4
        else:
            gradient = diff[1] / diff[0]
            if diff[0] > 0:
                if gradient >= 1:
                    return 3
                elif -1 <= gradient < 1:
                    return 1
                else:
                    return 4
            else:
                if gradient >= 1:
                    return 4
                elif -1 <= gradient < 1:
                    return 2
                else:
                    return 3

    # def devide(self, graph):
    #     result = []
    #     stack = []
    #     max_size = 25
    #     min_size = 2

    #     dx = [-1, 0, 1, 1, 1, 0, -1, -1]
    #     dy = [1, 1, 1, 0, -1, -1, -1, 0]

    #     def DFS(x, y):
    #         if len(stack) > max_size and (3 < abs((stack[0][1] - y) / (stack[0][0] - x)) or abs((stack[0][1] - y) / (stack[0][0] - x)) < 0.33):
    #             result.append(copy.deepcopy(stack))
    #             stack.clear()
    #             return
    #         else:
    #             for i in range(8):
    #                 nx = x + dx[i]
    #                 ny = y + dy[i]
    #                 if 0 <= nx < 64 and 0 <= ny < 64 and graph[nx][ny] == 0:
    #                     graph[nx][ny] = 1
    #                     stack.append((nx, ny))
    #                     DFS(nx, ny)

    #     for i in range(64):
    #         for j in range(64):
    #             if graph[j][i] == 0:
    #                 graph[j][i] = 1
    #                 stack.append((j, i))
    #                 DFS(j, i)
    #                 if len(stack) < min_size:
    #                     stack.clear()

    #     if stack:
    #         result.append(copy.deepcopy(stack))

    #     return result
