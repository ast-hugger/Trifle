With 20 reps of warmup, this computes fibonacci(35) in about 880 ms.
Pharo (64 bit) on the same machine computes it in 120 ms.

To estimate the cost of operating on wrapped integers, made a version of
the expression language and the interpreter operating on 'int' values only.
It computes fibonacci(35) in 780 ms. 

With Frame replaced with just an Object[] the runtime is 800ms. 

Dispensing with object allocation altogether by creating a single 'stack'
Object[] with a current frame base index is no different from allocating
individual Object[] arrays.