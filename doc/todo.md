# To do

* Closure#invokerForCallSite() binds to the specialized implementation only if it's
  already available. If not, it will bind to the generic invoker. If a specialization
  becomes available in the future, it will be used, but only via a specialization
  check guard of the generic invoker. Should rethink the whole scheme of call site
  management/relinking on compiled form availability change.
* Specialization check guard should only consider the arguments that may be specialized.
* Clean up compilation story as applied to a top function+nested closures bundle.

## Longer term

* Handle calls with arbitrary arity
* Pluggable call semantics (extend the constant function to be pluggable or at least
  support Smalltalk-like late bound selector-based dispatch).
* Enable concurrent compilation.
* Support for Smalltalk-like objects with flexible layout; and/or
* support for JS-like objects.