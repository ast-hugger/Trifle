A work in progress on an adaptive dynamic language implementation substrate for
the JVM.

## Current state

A proof of concept currently able to:

  * Interpret and collect type profiles
  * Generate compiled code in the generic form
  * Switch execution mode when a compiled form is available 
  * Detect the possibility of generating a specialized form
  * Generate a (not fully correct) specialized compiled form
  * Blend generic and specialized forms to switch to the
    specialized path when applicable.
  * Throw SquarePegExceptions at the right times.
  * Catch SPEs at the right places and recover using a-code. 

The profiling interpreter and the generic and specialized form compilers work
for the Fibonacci example. See [early benchmarks and code samples](doc/basic-benchmarks.md).

## TODO

  * Handle call sites with a specialized signature not matching the available specialization.
  * Closures
  * Think of a story for the top level and a better way of defining recursive functions
 

