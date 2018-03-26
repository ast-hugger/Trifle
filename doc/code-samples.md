## Benchmark function definition in Enfilade
        
    static Function fibonacci() {
        Variable n = var("n");
        Variable t1 = var("t1");
        Variable t2 = var("t2");
        return Function.recursive(n, fibonacci ->
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
      12: invokedynamic #45,  0             // InvokeDynamic #0:"call#0":(I)I
      17: istore_1
      18: iload_0
      19: iconst_2
      20: isub
      21: invokedynamic #45,  0             // InvokeDynamic #0:"call#0":(I)I
      26: istore_2
      27: iload_1
      28: iload_2
      29: iadd
      30: ireturn                      <--- end of normal (fast) case 
    SPE handler for call @12:    
      31: iconst_2
      32: iconst_3
      33: anewarray     #4                  // class java/lang/Object
      36: goto          51
    SPE handler for call @21:
      39: iconst_4
      40: iconst_3
      41: anewarray     #4                  // class java/lang/Object
      44: dup
      45: iconst_1
      46: iload_1
      47: invokestatic  #18                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      50: aastore
    common epilogue finishing the construction of the frame replica and invoking a recovery interpreter:
      51: dup
      52: iconst_0
      53: iload_0
      54: invokestatic  #18                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      57: aastore
      58: iconst_0
      59: invokestatic  #53                 // Method com/github/vassilibykov/enfilade/acode/Interpreter.forRecovery:(I[Ljava/lang/Object;I)Lcom/github/vassilibykov/enfilade/acode/Interpreter;
      62: swap
      63: invokevirtual #57                 // Method com/github/vassilibykov/enfilade/core/SquarePegException.value:()Ljava/lang/Object;
      66: invokevirtual #60                 // Method com/github/vassilibykov/enfilade/acode/Interpreter.interpret:(Ljava/lang/Object;)Ljava/lang/Object;
      69: dup
      70: instanceof    #14                 // class java/lang/Integer
      73: ifeq          86
      76: checkcast     #14                 // class java/lang/Integer
      79: invokevirtual #64                 // Method java/lang/Integer.intValue:()I
      82: ireturn
      83: nop
      84: nop
      85: athrow
      86: invokestatic  #68                 // Method com/github/vassilibykov/enfilade/core/SquarePegException.with:(Ljava/lang/Object;)Lcom/github/vassilibykov/enfilade/core/SquarePegException;
      89: athrow
    Exception table:
       from    to  target type
           9    17    31   Class com/github/vassilibykov/enfilade/core/SquarePegException
          18    26    39   Class com/github/vassilibykov/enfilade/core/SquarePegException
