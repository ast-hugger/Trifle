# To do

* Write up design overview of specialized code compilation and recovery.
* Untangle `let` and `letrec` in evaluator nodes. Make `letrec` multivariate.

## Longer term

* Handle calls of arbitrary arity.
* Pluggable call semantics (extend the constant function to be pluggable or at least
  support Smalltalk-like late bound selector-based dispatch).
* Support for Smalltalk-like objects with flexible layout; and/or
* support for JS-like objects.
* Enable concurrent compilation.
