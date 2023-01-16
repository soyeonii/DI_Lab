from preprocessing import Preprocessing
import numpy as np
import cv2

class Thinning:
    def __init__(self, file_path, file_name):
        "load image data"
        self.prep = Preprocessing(file_path.split('/')[-2])
        self.file_name = file_name
        # Gray image, rgb images need pre-conversion
        self.Img_Original = cv2.imread(file_path + file_name + '.bmp', cv2.IMREAD_GRAYSCALE)

        "Convert gray images to binary images using Otsu's method"
        # must set object region as 1, background region as 0 !
        self.BW_Original = self.prep.delete_background(cv2.threshold(self.Img_Original, -1, 1, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)[1], 128, 10)
        self.prep.save_img(self.BW_Original * 255, self.file_name, '_original.bmp')

    def neighbours(self, x, y, image):
        "Return 8-neighbours of image point P1(x,y), in a clockwise order"
        img = image
        x_1, y_1, x1, y1 = x-1, y-1, x+1, y+1
        return [img[x_1][y], img[x_1][y1], img[x][y1], img[x1][y1],     # P2,P3,P4,P5
                img[x1][y], img[x1][y_1], img[x][y_1], img[x_1][y_1]]    # P6,P7,P8,P9

    def transitions(self, neighbours):
        "No. of 0,1 patterns (transitions from 0 to 1) in the ordered sequence"
        n = neighbours + neighbours[0:1]      # P2, P3, ... , P8, P9, P2
        # (P2,P3), (P3,P4), ... , (P8,P9), (P9,P2)
        return sum((n1, n2) == (0, 1) for n1, n2 in zip(n, n[1:]))

    def zhangSuen(self, image):
        "the Zhang-Suen Thinning Algorithm"
        Image_Thinned = image.copy()  # deepcopy to protect the original image
        changing1 = changing2 = 1  # the points to be removed (set as 0)
        while changing1 or changing2:  # iterates until no further changes occur in the image
            # Step 1
            changing1 = []
            rows, columns = Image_Thinned.shape               # x for rows, y for columns
            for x in range(1, rows - 1):                     # No. of  rows
                for y in range(1, columns - 1):            # No. of columns
                    P2, P3, P4, P5, P6, P7, P8, P9 = n = self.neighbours(
                        x, y, Image_Thinned)
                    if (Image_Thinned[x][y] == 1 and    # Condition 0: Point P1 in the object regions
                        2 <= sum(n) <= 6 and    # Condition 1: 2<= N(P1) <= 6
                        self.transitions(n) == 1 and    # Condition 2: S(P1)=1
                        P2 * P4 * P6 == 0 and    # Condition 3
                            P4 * P6 * P8 == 0):         # Condition 4
                        changing1.append((x, y))
            for x, y in changing1:
                Image_Thinned[x][y] = 0
            # Step 2
            changing2 = []
            for x in range(1, rows - 1):
                for y in range(1, columns - 1):
                    P2, P3, P4, P5, P6, P7, P8, P9 = n = self.neighbours(
                        x, y, Image_Thinned)
                    if (Image_Thinned[x][y] == 1 and        # Condition 0
                        2 <= sum(n) <= 6 and       # Condition 1
                        self.transitions(n) == 1 and      # Condition 2
                        P2 * P4 * P8 == 0 and       # Condition 3
                            P2 * P6 * P8 == 0):            # Condition 4
                        changing2.append((x, y))
            for x, y in changing2:
                Image_Thinned[x][y] = 0
        return Image_Thinned

    def run(self):
        "Apply the algorithm on images"
        _, BW_Skeleton = cv2.threshold(self.zhangSuen(self.BW_Original), 0, 1, cv2.THRESH_BINARY_INV)
        # print('------------------------------ 왼쪽 ------------------------------')
        # print(BW_Skeleton[5:35, 5:35])
        # print(BW_Skeleton[35:60, 5:35])
        # print('------------------------------ 오른쪽 ------------------------------')
        # print(BW_Skeleton[5:35, 35:60])
        # print(BW_Skeleton[35:60, 35:60])

        "Simplify stroke"
        points = self.prep.simplify(BW_Skeleton.copy())
        self.prep.save_img(self.prep.draw_img(points) * 255, self.file_name, '_2_3.bmp')

        "Devide stroke"
        stroke_points = self.prep.devide(points)
        # for i in range(len(stroke_points)):
        #     test_img = self.prep.draw_img(stroke_points[i])
        #     self.prep.save_img(test_img, self.file_name, '_test_' + str(i) + '.bmp')

        "point 이어서 이미지 저장"
        # for i, stroke_point in enumerate(stroke_points):
        #     img = np.ones((128, 128))
        #     for j in range(1, len(stroke_point)):
        #         img = cv2.line(img, stroke_point[j-1][::-1], stroke_point[j][::-1], (0, 0, 0))
        #     self.prep.save_img(img, self.file_name, '_consonant_' + str(i) + '.bmp')

        "획별 색상 이미지 저장"
        color = [(0, 0, 255), (0, 255, 255), (0, 255, 0), (255, 255, 0), (255, 0, 0), (255, 0, 255), (0, 0, 0)]
        img = np.full((128, 128, 3), (255, 255, 255), np.uint8)
        for i, stroke_point in enumerate(stroke_points):
            for j in range(1, len(stroke_point)):
                img = cv2.line(
                    img, stroke_point[j-1][::-1], stroke_point[j][::-1], color[i % len(color)])
        self.prep.save_img(img, self.file_name, '_sep.bmp')

        "나누어진 획마다 이미지 저장"
        # for index, stroke in enumerate(strokes):
        #     self.prep.save_img(self.prep.draw_img(stroke), '_devide_' + str(index) + '.bmp')

        "Save image"
        # self.prep.save_img(invert(self.BW_Original), '_original.bmp')
        self.prep.save_img(BW_Skeleton * 255, self.file_name, '_thinning.bmp')

        "Display the results"
        # _, ax = plt.subplots(1, 2)
        # ax1, ax2 = ax.ravel()
        # ax1.imshow(invert(self.BW_Original), cmap=plt.cm.gray)
        # ax1.set_title('Original binary image')
        # ax1.axis('off')
        # ax2.imshow(BW_Skeleton, cmap=plt.cm.gray)
        # ax2.set_title('Skeleton of the image')
        # ax2.axis('off')
        # plt.show()
