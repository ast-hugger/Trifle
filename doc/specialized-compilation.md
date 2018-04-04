A visitor method of the specialized code generator emits a code sequence that
produces its value as a certain JvmType. Unlike with the generic code generator,
this type can be primitive. The return value of a visitor method is the type
of the value the generated code produces.

This value is then used an a code fragment produced by the code generator for a
different node. The type of the value may or may not be what the other fragment
can directly work with. For example, the first fragment may have produced a
reference value (which may or may not be an `Integer`), but the second fragment
is generated for a `let` expression which needs to store that value in a local
variable whose specialized type is `int`.

To ensure the value can be used by the second fragment, the value is *bridged*.
Bridging is a core concept of specialized code generation.

Bridging is a mechanism delivering a value produced by (the compiled code of)
one evaluation node to another evaluation node. Depending on the value and on
the receiver node's expectations, it can be as simple as a no-op or as involved
as a switch to interpreted mode. In the example above, if the producer node
produces an `int`, bridging is a no-op. If the producer node produces a reference,
and its runtime value is `Integer`, bridging unwraps the integer. If the producer
produces a reference other than `Integer`, bridging throws a
`SquarePegException` the handler of which switches to interpreted mode and runs
the recovery interpreter to execute the consumer node logic.

Bridging is an operation that can fail, and it only appears in the implementation
of complex expressions. 


