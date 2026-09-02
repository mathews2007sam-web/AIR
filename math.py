<<<<<<< HEAD
import math

def get_factorial_built_in(n):
    if n < 0:
        raise ValueError("Factorial is Not defined for negative numbers.")
    return math.factorial(n)

=======
import math

def get_factorial_built_in(n):
    if n < 0:
        raise ValueError("factorial is Not defined for negative numbers.")
    return math.factorial(n)

>>>>>>> 1a2815a4ebdfaeee7b9499b1d421a0fda3923d8c
print(get_factorial_built_in(5)) 