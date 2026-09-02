import math

def get_factorial_built_in(n):
    if n < 0:
        raise ValueError("Factorial is Not defined for negative numbers.")
    return math.factorial(n)

print(get_factorial_built_in(5)) 