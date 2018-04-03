A closure call is a fundamental type of call in Enfilade, as even a top-level
function is a closure with an empty list of copied values.

## Interpreted mode calls

In interpreted mode a closure call is executed by the interpreter calling one of
the `invoke()` instance methods of the closure class. The closure in turn calls
one of the `invokeExact()` methods of the method handle stored as the value of
its `genericInvoker` field. `genericInvoker` is permanently bound to the dynamic
invoker of the core call site of the closure's implementation function,
inserting the closure's copied values to match the implementation function's
synthetic parameters. Invoking it results in executing the function in whatever
execution mode it currently supports. All argument values involved in the call
are references.

If the function has been compiled and has a specialized form, its core dynamic
invoker will have a specialization guard which will unbox the arguments and
direct execution to the specialized form if every argument mathing a specialized
form's parameter of a primitive type can be unboxed into that type. For example,
if the second parameter of the specialized form is an `int` and the second
argument of the call is an `Integer`, the specialized form will be invoked
instead of the generic one, with the argument unboxed for the call.

## Compiled mode calls

A closure call is compiled as an `invokedynamic` instruction bootstrapped and
managed by the `ClosureInvokeDynamic` class. The arguments pushed onto the stack
are the closure followed by the call arguments, left-to-right. 

In generic code the type of the call site is `Object Object{n} -> Object`,
where `n` is the number of the call arguments. The first `Object` type refers to
the closure because, of course, the subexpression of the call which must produce
a closure may produce any other object (resuling in call failure).

In compiled code the type of the call site corresponds to the *observed* types of
the call arguments and its return values. For example, a call whose first
argument was observed to be an `int` and a second argument a reference, and the
returned values were `int`s will be compiled as a call site of type 
`Object int Object -> int`. The first `Object` is, again, the closure. Because
this signature depends on the values observed for this particular call, another
call of the same function may have a different call site signature, for example
`Object int int -> int`. A yet another call site may have the generic signature
`Object Object Object -> Object`.

Call sites of types narrower than the generic may encounter values incompatible
with one of their narrow (non-Object) types. That is because narrow types are
based on values observed during profiling, and future execution of the program
may follow a path it previosly didn't, producing values of different types. Such
specialization failures (`SquarePegException`s) are caught and handled by the
recovery mechanism. Thus, they are unrelated to "normal" closure invocations,
and we will assume execution proceeds normally, with all values matching their
observed types.

### Dispatching a call to a not yet compiled function

If the target of an `invokedynamic` instruction is a closure whose
implementation function has not yet been compiled, its core call site
is configured so that invoking it invokes the interpreter. In the future
when the function is compiled, the function's call site is relinked
so that invoking it invokes either the generic compiled form or a
specialization guard which dispatches to either the specialized or the
generic form.

Because of this, if an as yet uncompiled function is invoked from compiled
code, there is no attempt to cache the call to the implementation's invoker.
Each call is processed by `ClosureInvokeDynamic`'s dispatch method until
the target is finally compiled.

### Dispatching a call to a compiled function

If the target of a call is a compiled function, `ClosureInvokeDynamic` attempts
to use an optimal invoker for the function's compiled code. The optimal invoker
is the following.

1. If the type of a call site includes no primitive types, i.e. all types are
`Object`, the optimal invoker is the method hanle of the function's generic
compiled form.

1. if the type of a call site includes primitive types and the function has a
specialized implementation, their types are compared. If the types are equal,
the invoker is the method handle of the specialized compiled form.

1. Otherwise, the invoker is the main invoker of the implementation function. It
will branch to either the specialized or generic implementation as appropriate,
based on the actual call arguments.

### Inline cache and the handling of copied values

Closure objects corresponding to the same lambda expression in the source will
share their implementation function. However, they will in general have different
sets of copied values. An exception is a closure object with no copied values,
in which case its set of copied values is the same as any other closure object
for the same lambda expression.

A call site in compiled code may be invoking closures created by multiple
lambda expressions. However, we may assume that the common case (but not a
guarantee) is when a call site invokes closures which originate from the same
lambda expresion and share their implementation function--but, as discussed
above, not the set of copied values. Unless that set is empty.

A closure may be invoked at multiple call sites. These call site may have
different signatures. Thus, an optimal invoker as discussed above is specific to
a specific pair of a closure and a call site. In practice, the optimal invoker
is computed by a closure on request from a call site, with the call site
supplying the type the invoker must have. *Very importantly, if a closure has
copied values, the optimal invoker it produces has those copied values bound to
the implementation function's synthetic parameters. The invoker is thus specific
to the closure, and must not be reused for any other closure with the same
implementation function.*

These three considerations suggest the following strategy for inline caching
of closure invokers.

* If a closure has no copied values, a call site may cache the optimal
invoker the closure produced, and reuse the same invoker for any other
closure with the same implementation function.

* If a closure has copied values, the optimal invoker it produced at call site's
request is closure-specific and must not be cached by the call site.

* However, that invoker may be reused by the same or any other call site
which requires the same signature if the closure is invoked again. Therefore,
it can be cached by the closure as part of optimal invoker computation. 
