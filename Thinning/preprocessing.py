import cv2
import numpy as np
import os
from collections import deque

class Preprocessing:
    def __init__(self, folder_name):
        self.folder_name = folder_name

    def delete_background(self, img, size, padding):
        idx = np.where(img == 1)
        max_x = np.max(idx[1])
        min_x = np.min(idx[1])
        max_y = np.max(idx[0])
        min_y = np.min(idx[0])
        img = img[min_y:max_y, min_x:max_x]
        img = cv2.resize(img, (size - padding * 2, size - padding * 2))
        tmp = np.zeros((size, size), np.uint8)
        tmp[padding:size-padding, padding:size-padding] = img
        return tmp

    def save_img(self, img, file_name, end):
        path = './Thinning/results/230110/' + self.folder_name + '/' + file_name
        if not os.path.isdir(path):
            os.makedirs(path)
        cv2.imwrite(path + '/' + file_name + end, img)

    def draw_img(self, points):
        img = np.ones((128, 128))
        for x, y in points:
            img[x][y] = 0
        return img

    def erosion_dilation(self, img):
        kernel = np.ones((3, 3), np.uint8)
        return cv2.dilate(cv2.erode(img, kernel, iterations = 1), kernel, iterations = 1)

    def simplify(self, graph):
        size = 3    # frame 크기
        space = 4  # 최소 point 간격
        points = []
        for i in range(0, 128, size):
            for j in reversed(range(0, 128, size)):
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
        return points

    def devide(self, points):
        n = len(points)
        size = 10
        result = []
        stack = []
        check = [[False] * 128 for _ in range(128)]
        num = -1
        def BFS(start):
            nonlocal num
            queue = deque()
            check[start[0]][start[1]] = True
            stack.append(start)
            queue.append(start)
            while queue:
                x, y = queue.popleft()
                surround_points = []
                for j in range(y - size, y + size):
                    for i in range(x - size, x + size):
                        if (i, j) in points and not check[i][j]:
                            tmp = self.get_num([i - stack[-1][0], j - stack[-1][1]])
                            if num == -1 or num == tmp:
                                if num == -1:
                                    num = tmp
                                surround_points.append((i, j))
                if surround_points:
                    surround_points.sort(key=lambda x: abs(stack[-1][(num+1) % 2] - x[(num+1) % 2]))
                    check[surround_points[0][0]][surround_points[0][1]] = True
                    stack.append(surround_points[0])
                    queue.append(surround_points[0])
        for i in range(n):
            x = points[i][0]
            y = points[i][1]
            if not check[x][y]:
                BFS((x, y))
                if num != -1:
                    result.append((stack.copy(), num))
                stack.clear()
                num = -1
        return result

    def join_points(self, points):
        s1, s2 = 10, 5
        check = [False] * len(points)
        result = []
        for i in range(len(points)):
            if not check[i]:
                tmp = []
                check[i] = True
                p, n = points[i]
                tmp += p
                for j in range(i+1, len(points)):
                    if not check[j]:
                        np, nn = points[j]
                        if n % 2 == nn % 2:
                            if n % 2 == 0:
                                p.sort()
                                np.sort()
                                if p[0][0] - s1 <= np[-1][0] < p[0][0] and p[0][1] - s2 <= np[-1][1] <= p[0][1] + s2:
                                    check[j] = True
                                    tmp = np + tmp
                                if p[-1][0] < np[0][0] <= p[-1][0] + s1 and p[-1][1] - s2 <= np[0][1] <= p[-1][1] + s2:
                                    check[j] = True
                                    tmp += np
                            if n % 2 == 1:
                                p.sort(key=lambda x: (x[1], x[0]))
                                np.sort(key=lambda x: (x[1], x[0]))
                                if p[0][1] - s1 <= np[-1][1] < p[0][1] and p[0][0] - s2 <= np[-1][0] <= p[0][0] + s2:
                                    check[j] = True
                                    tmp = np + tmp
                                if p[-1][1] < np[0][1] <= p[-1][1] + s1 and p[-1][0] - s2 <= np[0][0] <= p[-1][0] + s2:
                                    check[j] = True
                                    tmp += np
                result.append(tmp)
        return result

    def get_num(self, diff):
        if diff[0] == 0:
            return 3 if diff[1] >= 0 else 1
        elif diff[1] == 0:
            return 4 if diff[0] >= 0 else 2
        else:
            slope = diff[1] / diff[0]
            if diff[0] > 0:
                if slope >= 1:
                    return 3
                elif -1 <= slope < 1:
                    return 4
                else:
                    return 1
            else:
                if slope >= 1:
                    return 1
                elif -1 <= slope < 1:
                    return 2
                else:
                    return 3

    # def get_slope(self, p1, p2):
    #     return abs((p1[0]-p2[0]) / (p1[1]-p2[1])) if p1[1] != p2[1] else 0

    # def get_quadrant(self, diff):
    #     x, y = diff
    #     if x == 0:
    #         return 3 if y >= 0 else 1
    #     if y == 0:
    #         return 4 if x >= 0 else 2
        
    #     if x > 0:
    #         if y > 0:
    #             return 1
    #         else:
    #             return 4
    #     else:
    #         if y > 0:
    #             return 2
    #         else:
    #             return 3

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
    #                 if 0 <= nx < 128 and 0 <= ny < 128 and graph[nx][ny] == 0:
    #                     graph[nx][ny] = 1
    #                     stack.append((nx, ny))
    #                     DFS(nx, ny)

    #     for i in range(128):
    #         for j in range(128):
    #             if graph[j][i] == 0:
    #                 graph[j][i] = 1
    #                 stack.append((j, i))
    #                 DFS(j, i)
    #                 if len(stack) < min_size:
    #                     stack.clear()

    #     if stack:
    #         result.append(copy.deepcopy(stack))

    #     return result
