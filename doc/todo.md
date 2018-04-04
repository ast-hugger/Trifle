# To do

* Separate primitive operations definitions from primitive calls.
* Rethink the specialization story. Generic signature method should be
compiled specialized as well. Perhaps we need at up to three forms:
pure generic as a fast fallback, specialized with a generic signature,
and specialized with a specialized signature. 

## Longer term

* Handle calls of arbitrary arity.
* Pluggable call semantics (extend the constant function to be pluggable or at least
  support Smalltalk-like late bound selector-based dispatch).
* Support for Smalltalk-like objects with flexible layout; and/or
* support for JS-like objects.
* Maube untangle `let` and `letrec` in evaluator nodes. Make `letrec` multivariate.
* Enable concurrent compilation.
