import cv2
import numpy as np
import os
from collections import deque


class Preprocessing:
    def __init__(self, path):
        self.path = path

    def delete_background(self, img, size, padding):
        # Find the bounding box of the object in the image
        nonzero_pixels = np.nonzero(img)
        top = np.min(nonzero_pixels[0])
        bottom = np.max(nonzero_pixels[0])
        left = np.min(nonzero_pixels[1])
        right = np.max(nonzero_pixels[1])

        # Crop the image to the bounding box
        cropped_img = img[top : bottom + 1, left : right + 1]

        # Resize the cropped image
        resized_img = cv2.resize(cropped_img, (size - padding * 2, size - padding * 2))

        # Create a new image with the desired size and padding
        padded_img = np.zeros((size, size), np.uint8)
        padded_img[padding : size - padding, padding : size - padding] = resized_img

        return padded_img

    def save_img(self, img, file_name, extension):
        folder_path = f"{self.path}/{file_name}"
        os.makedirs(folder_path, exist_ok=True)
        file_path = f"{folder_path}/{file_name}{extension}"
        cv2.imwrite(file_path, img)

    def draw_img(self, points, shape=(128, 128)):
        img = np.ones(shape)
        for x, y in points:
            img[x][y] = 0
        return img

    def erosion_dilation(self, img, kernel_size=3, iterations=1):
        kernel = np.ones((kernel_size, kernel_size), np.uint8)
        eroded = cv2.erode(img, kernel, iterations=iterations)
        dilated = cv2.dilate(eroded, kernel, iterations=iterations)
        return dilated

    def simplify(self, img):
        # Define constants
        FRAME_SIZE = 3
        MIN_DISTANCE = 4

        # Initialize list to store simplified points
        points = []

        # Iterate over the image in a grid of FRAME_SIZE x FRAME_SIZE
        for i in range(0, img.shape[0], FRAME_SIZE):
            for j in reversed(range(0, img.shape[1], FRAME_SIZE)):
                frame = img[i:i+FRAME_SIZE, j:j+FRAME_SIZE]

                # Check if there is any white pixel in the frame
                if np.any(frame == 0):
                    # Find the coordinates of the first white pixel in the frame
                    x, y = np.where(frame == 0)
                    x = i + x[0]
                    y = j + y[0]
                    
                    # Check if there is any other point within the minimum distance
                    if not np.any(img[max(0, x-MIN_DISTANCE):min(img.shape[0], x+MIN_DISTANCE),
                                      max(0, y-MIN_DISTANCE):min(img.shape[1], y+MIN_DISTANCE)] == 2):
                        # Mark the point as simplified and add it to the list
                        img[x, y] = 2
                        points.append((x, y))

        # Return the list of simplified points
        return points

    def devide(self, points):
        """
        Divides a set of points into clusters based on their proximity.
        """
        result = []
        size = 8
        visited = [[False] * 128 for _ in range(128)]

        def BFS(start):
            """
            Breadth-first search algorithm to find all points in a cluster.
            """
            nonlocal stack, recent_direction
            queue = deque()
            visited[start[0]][start[1]] = True
            stack.append(start)
            queue.append(start)
            while queue:
                x, y = queue.popleft()
                neighbor_points = []
                for j in range(y - size, y + size):
                    for i in range(x - size, x + size):
                        if (i, j) in points and not visited[i][j]:
                            current_direction = self.get_direction([i - stack[-1][0], j - stack[-1][1]])
                            if recent_direction is None or recent_direction == current_direction:
                                if recent_direction is None:
                                    recent_direction = current_direction
                                neighbor_points.append((i, j))
                if neighbor_points:
                    neighbor_points.sort(
                        key=lambda p: abs(stack[-1][(recent_direction + 1) % 2] - p[(recent_direction + 1) % 2])
                    )
                    p = neighbor_points[0]
                    visited[p[0]][p[1]] = True
                    stack.append(p)
                    queue.append(p)

        for x, y in points:
            if not visited[x][y]:
                stack = []
                recent_direction = None
                BFS((x, y))
                if recent_direction is not None:
                    result.append((stack.copy(), recent_direction % 2))

        return result

    def join_points(self, points):
        result = []
        s1, s2 = 12, 8
        check = [False] * len(points)
        
        def can_join(p1, n1, p2, n2):
            if n1 != n2:
                return False
            if n1 == 0:
                p1.sort()
                p2.sort()
                if (
                    p1[0][0] - s1 <= p2[-1][0] < p1[0][0]
                    and p1[0][1] - s2 <= p2[-1][1] <= p1[0][1] + s2
                ):
                    return True
                if (
                    p1[-1][0] < p2[0][0] <= p1[-1][0] + s1
                    and p1[-1][1] - s2 <= p2[0][1] <= p1[-1][1] + s2
                ):
                    return True
            else:
                p1.sort(key=lambda x: (x[1], x[0]))
                p2.sort(key=lambda x: (x[1], x[0]))
                if (
                    p1[0][1] - s1 <= p2[-1][1] < p1[0][1]
                    and p1[0][0] - s2 <= p2[-1][0] <= p1[0][0] + s2
                ):
                    return True
                if (
                    p1[-1][1] < p2[0][1] <= p1[-1][1] + s1
                    and p1[-1][0] - s2 <= p2[0][0] <= p1[-1][0] + s2
                ):
                    return True
            return False
        
        for i, (p, n) in enumerate(points):
            if not check[i]:
                tmp = p.copy()
                check[i] = True
                for j in range(i + 1, len(points)):
                    if not check[j]:
                        np, nn = points[j]
                        if can_join(p, n, np, nn):
                            check[j] = True
                            if n == 0:
                                tmp = np + tmp
                            else:
                                tmp += np
                result.append((tmp, n))
        return result

    def get_direction(self, diff):
        # Check for vertical and horizontal lines
        if diff[0] == 0:
            return 3 if diff[1] >= 0 else 1
        elif diff[1] == 0:
            return 4 if diff[0] >= 0 else 2
        # Compute slope for diagonal lines
        else:
            slope = diff[1] / diff[0]
            # Compute direction based on slope and quadrant
            if diff[0] > 0:
                return 3 if slope >= 1 else 4 if -1 <= slope < 1 else 1
            else:
                return 1 if slope >= 1 else 2 if -1 <= slope < 1 else 3