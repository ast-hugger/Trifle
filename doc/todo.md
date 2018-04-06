# To do

* Implement the passing of locals from a regular to recovery method
  using call arguments instead of an array.
* Need a story for built-in functions. Probably:
  * Implement something like an environment where they can be defined
    and references to which can be compiled into FunctionConstantNoded.
  * Provide a mechanism for function implementation-like object whose
    implementation is built-in in the Java layer.
  * Might need a common interface for this and FunctionImplementations.
  * Might need a similar thing for "closures" of such objects.

## Longer term

* Handle calls of arbitrary arity.
* Pluggable call semantics (extend the constant function to be pluggable or at least
  support Smalltalk-like late bound selector-based dispatch).
* Support for Smalltalk-like objects with flexible layout; and/or
* support for JS-like objects.
* Enable concurrent compilation.
