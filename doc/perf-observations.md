## Times

The times below are milliseconds to compute a recursively defined fibonacci(35).
Times for Enfilade and Java are after enough warmup runs (20, though 10 might be
enough) to get stable timings. Pharo/Cog doesn't seem to benefit from warmup.
None of the mainstream interpreted languages respond to warming up. For Enfilade,
compiling with @NotNull runtime assertions turned off makes a slight difference.

* 6250: Javascript - Firefox 56.0
* 6080: Javasacript - Firefox Quantum (58.0.2)
* 2510: Enfilade, interpreter + type profiler
* 2440: Python 2.7.12
* 1450: Lua 5.2
* 1030: Ruby 2.3.1
* 670: Enfilade, interpreter
* 129: Smalltalk - Pharo, 64-bit Cog, March05 2018 build
* 106: Javascript - node.js
* 85: Enfilade, generic compiled form (wrapped ints)
* 66: gcc -O0
* 48: gcc -O1
* 39: Enfilade, adaptively specialized
* 39: Java
* 35: gcc -O2
* 21: gcc -O3

## Benchmark function definition in Enfilade

    static Function fibonacci() {
        Variable n = var("n");
        Variable t1 = var("t1");
        Variable t2 = var("t2");
        return Function.recursive(n, fibonacci ->
            if_(lessThan(ref(n), const_(2)),
                const_(1),
                let(t1, call(fibonacci, sub(ref(n), const_(1))),
                    let(t2, call(fibonacci, sub(ref(n), const_(2))),
                        add(ref(t1), ref(t2))))));
    }

## Bytecode of the generic compiled method

    public static final java.lang.Object generic(java.lang.Object);
    Code:
       0: aload_0
       1: iconst_2
       2: invokestatic  #12                 // Method com/github/vassilibykov/enfilade/primitives/LessThan.lessThan:(Ljava/lang/Object;I)Z
       5: ifeq          15
       8: iconst_1
       9: invokestatic  #18                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      12: goto          51
      15: aload_0
      16: iconst_1
      17: invokestatic  #24                 // Method com/github/vassilibykov/enfilade/primitives/Sub.sub:(Ljava/lang/Object;I)I
      20: invokestatic  #18                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      23: invokedynamic #35,  0             // InvokeDynamic #0:"call#0":(Ljava/lang/Object;)Ljava/lang/Object;
      28: astore_1
      29: aload_0
      30: iconst_2
      31: invokestatic  #24                 // Method com/github/vassilibykov/enfilade/primitives/Sub.sub:(Ljava/lang/Object;I)I
      34: invokestatic  #18                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      37: invokedynamic #35,  0             // InvokeDynamic #0:"call#0":(Ljava/lang/Object;)Ljava/lang/Object;
      42: astore_2
      43: aload_1
      44: aload_2
      45: invokestatic  #41                 // Method com/github/vassilibykov/enfilade/primitives/Add.add:(Ljava/lang/Object;Ljava/lang/Object;)I
      48: invokestatic  #18                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      51: areturn

## Bytecode of the adaptively specialized compiled method

    public static final int specialized(int);
    Code:
       0: iload_0
       1: iconst_2
       2: if_icmpge     9
       5: iconst_1
       6: goto          30
       9: iload_0
      10: iconst_1
      11: isub
      12: invokedynamic #46,  0             // InvokeDynamic #0:"call1#0":(I)I
      17: istore_1
      18: iload_0
      19: iconst_2
      20: isub
      21: invokedynamic #46,  0             // InvokeDynamic #0:"call1#0":(I)I
      26: istore_2
      27: iload_1
      28: iload_2
      29: iadd
      30: ireturn
 