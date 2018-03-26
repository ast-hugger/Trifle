## Times

The times below are milliseconds to compute a recursively defined fibonacci(35).
Times for Enfilade and Java are after enough warmup runs (20, though 10 might be
enough) to get stable timings. Pharo/Cog doesn't seem to benefit from warmup.
None of the mainstream interpreted languages respond to warming up. For Enfilade,
compiling with @NotNull runtime assertions turned off makes a slight difference.

* 6080: Javasacript - Firefox Quantum (58.0.2)
* 2440: Python 2.7.12
* 1450: Lua 5.2
* 1260: Enfilade, profiling interpreter
* 1030: Ruby 2.3.1
* 670: Enfilade, plain interpreter
* 129: Smalltalk - Pharo, 64-bit Cog, March05 2018 build
* 106: Javascript - node.js
* 85: Enfilade, generic compiled form (wrapped ints)
* 66: gcc -O0
* 48: gcc -O1
* 37: Java
* 35: gcc -O2
* 33: Enfilade, adaptively specialized 
* 21: gcc -O3

Can't explain the difference between Java (fib() defined as a private static
final method) and Enfilade.
