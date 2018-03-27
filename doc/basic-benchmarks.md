## Times

The times below are milliseconds to compute a recursively defined fibonacci(35).
Enfilade and Java times are under JDK 10, are after enough warmup runs (20,
though 10 might be enough) to get stable timings. Pharo/Cog doesn't seem to
benefit from warmup.

* 6080: Javasacript - Firefox Quantum (58.0.2)
* 2440: Python 2.7.12
* 1450: Lua 5.2
* 1140: Enfilade, profiling interpreter
* 1030: Ruby 2.3.1
* 670: Enfilade, plain interpreter
* 129: Smalltalk - Pharo, 64-bit Cog, March05 2018 build
* 106: Javascript - node.js
* 85: Enfilade, generic compiled form (wrapped ints)
* 66: gcc -O0
* 48: gcc -O1
* 40: Java
* 35: gcc -O2
* 33: Enfilade, adaptively specialized 
* 21: gcc -O3

There is a significant difference between JDK 9 and JDK 10 for some tests:

* Enfilade, profiling interpreter
  * 9: 1860ms
  * 10: 1140ms 
* Enfilade, adaptively specialized: no difference
* Java
  * 9: 37ms
  * 10: 40ms

Can't explain the difference between Java (fib() defined as a private static
method) and Enfilade.
