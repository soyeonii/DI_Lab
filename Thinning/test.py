n = [0, 1, 0, 0, 1, 1, 0, 1, 0]
print(sum((n1, n2) == (0, 1) for n1, n2 in zip(n, n[1:])))
print((n1, n2) == (0, 1) for n1, n2 in zip(n, n[1:]))