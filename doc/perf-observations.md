## Timings

The times below are milliseconds to compute fibonacci(35). Times for Pharo and
Enfilade are after enough warmup runs (20 for Enfilade) to get stable timings.

* 850: Enfilade, profiling evaluator
* 120: Pharo, 64-bit Cog, March05 2018 build
* 85: Enfilade, generic compiled form (wrapped ints)
* 66: gcc -O0
* 48: gcc -O1
* 41: Enfilade, adaptively specialized
* 35: gcc -O2
* 21: gcc -O3

## Enfilade function definition

    static Function fibonacci() {
        Var n = var("n");
        Var t1 = var("t1");
        Var t2 = var("t2");
        return Function.withRecursion(new Var[]{n}, fibonacci ->
            if_(lessThan(n, const_(2)),
                const_(1),
                let(t1, call(fibonacci, sub(n, const_(1))),
                    let(t2, call(fibonacci, sub(n, const_(2))),
                        add(t1, t2)))));
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
 