import cv2
import numpy as np
import os
from PIL import ImageDraw


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

    def save_img(self, img, file_name):
        img = img * 255
        cv2.imwrite('./results/' + file_name, img)

    def draw_img(self, points):
        img = np.ones((64, 64))
        for x, y in points:
            img[x][y] = 0
        return img

    def devide(self, graph):
        points = []
        size = 4  # frame 크기
        space = 2  # point 간격
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
        print('------------------------------ point 모음 ------------------------------')
        print(points)
        print('------------------------------ point 개수 ------------------------------')
        print(len(points))
        return points

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
