# To do

* Comprehensive type inferencer tests
* Comprehensive specialization computer tests, similar to the profiler.
* Devise a scheme of testing code generation.
* Ditto for recovery.

## Longer term

* Try the transfer of locals from the regular to the recovery method
  using call arguments instead of an array.
* Handle calls of arbitrary arity.
* Pluggable call semantics (extend the constant function to be pluggable or at least
  support Smalltalk-like late bound selector-based dispatch).
* Support for Smalltalk-like objects with flexible layout; and/or
* Support for JS-like objects.
* Enable concurrent compilation.
