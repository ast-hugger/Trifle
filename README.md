A work in progress on an adaptive dynamic language implementation substrate for
the JVM.

## Current state

A proof of concept currently able to:

  * Interpret and collect type profiles
  * Generate compiled code in the generic form
  * Switch execution mode when a compiled form is available 
  * Detect the possibility of generating a specialized form
  * Generate a (not generally correct) specialized compiled form
  * Blend generic and specialized forms to switch to the
    specialized path when applicable.

The profiling interpreter and the generic and specialized form compilers work
for the Fibonacci example. See [early benchmarks and code samples](doc/perf-observations.md).

The specialized form compilation scheme is incorrect for the general case. The
code generator needs to track the expected profiled type of each complex
expression to detect and produce specialization failures.

Profiling needs to be extended to collect type information for all complex
expressions, not just calls.

Emergency interpreter is needed for recovery from specialization failures, as
intermediary execution state can't be mapped onto the state of the existing
profiling interpreter. Depending on its performance, emergency interpreter might
be better suited to be the profiling interpreter as well.

Specialization failure handlers should be generated in specialized code, with
execution transition to the emergency interpreter.