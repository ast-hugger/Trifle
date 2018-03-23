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

The profiling interpreter and the generic and specialized form compilers work
for the Fibonacci example. See [early benchmarks and code samples](doc/perf-observations.md).

Specialization failures are generated in specialized code, but there 
are no handlers to transition execution to the emergency interpreter.

Started work on the emergency interpreter. Emergency interpreter is needed for
recovery from specialization failures, as intermediary execution state can't be
mapped onto the state of the existing profiling interpreter. Depending on its
performance, emergency interpreter might even be better suited to be the
profiling interpreter as well.

