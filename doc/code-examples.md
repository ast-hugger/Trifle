
## Benchmark function definition in Trifle
        
The actual definition using DSLish helper combinators:
        
    private static Closure fibonacci() {
        return TopLevel.define(
            fibonacci -> lambda(n ->
                if_(lessThan(n, const_(2)),
                    const_(1),
                    bind(call(fibonacci, sub(n, const_(1))), t1 ->
                        bind(call(fibonacci, sub(n, const_(2))), t2 ->
                            add(t1, t2))))));
    }

`bind` above is esentially a `let` with the variable and the initializer expression
swapped. This form makes it possible to write `let` expressions without creating
the variable to bind by hand.

Here is the above with helper combinators expanded into the underlying static factory
method calls of expression node classes.

    private static Closure altFib() {
        var n = var("n");
        var t1 = var("t1");
        var t2 = var("t2");
        return TopLevel.define(
            fibonacci -> Lambda.with(List.of(n),
                If.with(PrimitiveCall.with(LessThan.class, n, Const.value(2)),
                    Const.value(1),
                    Let.with(t1, Call.with(fibonacci, PrimitiveCall.with(Sub.class, n, Const.value(1))),
                        Let.with(t2, Call.with(fibonacci, PrimitiveCall.with(Sub.class, n, Const.value(2))),
                            PrimitiveCall.with(Add.class, t1, t2))))));
    }

## Compiled code

The final compilation scheme in which recovery code is embedded in the method,
with dead code not reachable from recovery entry points eliminated.

    public static final java.lang.Object function0(java.lang.Object);
    Code:
       0: aload_0
       1: iconst_2
       2: invokestatic  #12                 // Method com/github/vassilibykov/trifle/primitives/LessThan.lessThan:(Ljava/lang/Object;I)Z
       5: ifeq          12
       8: iconst_1
       9: goto          37
      12: aload_0
      13: iconst_1
      14: invokestatic  #18                 // Method com/github/vassilibykov/trifle/primitives/Sub.sub:(Ljava/lang/Object;I)I
      17: invokedynamic #30,  0             // InvokeDynamic #0:call1:(I)I
      22: istore_1
      23: aload_0
      24: iconst_2
      25: invokestatic  #18                 // Method com/github/vassilibykov/trifle/primitives/Sub.sub:(Ljava/lang/Object;I)I
      28: invokedynamic #30,  0             // InvokeDynamic #0:call1:(I)I
      33: istore_2
      34: iload_1
      35: iload_2
      36: iadd
      37: invokestatic  #38                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      40: areturn
      41: getfield      #42                 // Field com/github/vassilibykov/trifle/core/SquarePegException.value:Ljava/lang/Object;
      44: goto          58
      47: getfield      #42                 // Field com/github/vassilibykov/trifle/core/SquarePegException.value:Ljava/lang/Object;
      50: iload_1
      51: invokestatic  #38                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      54: astore_1
      55: goto          72
      58: astore_1
      59: aload_0
      60: iconst_2
      61: invokestatic  #18                 // Method com/github/vassilibykov/trifle/primitives/Sub.sub:(Ljava/lang/Object;I)I
      64: invokestatic  #38                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      67: invokedynamic #44,  0             // InvokeDynamic #0:call1:(Ljava/lang/Object;)Ljava/lang/Object;
      72: astore_2
      73: aload_1
      74: aload_2
      75: invokestatic  #50                 // Method com/github/vassilibykov/trifle/primitives/Add.add:(Ljava/lang/Object;Ljava/lang/Object;)I
      78: invokestatic  #38                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      81: areturn
    Exception table:
       from    to  target type
          12    22    41   Class com/github/vassilibykov/trifle/core/SquarePegException
          23    33    47   Class com/github/vassilibykov/trifle/core/SquarePegException
    
    public static final int specialized$function0(int);
    Code:
       0: iload_0
       1: iconst_2
       2: if_icmpge     9
       5: iconst_1
       6: goto          30
       9: iload_0
      10: iconst_1
      11: isub
      12: invokedynamic #30,  0             // InvokeDynamic #0:call1:(I)I
      17: istore_1
      18: iload_0
      19: iconst_2
      20: isub
      21: invokedynamic #30,  0             // InvokeDynamic #0:call1:(I)I
      26: istore_2
      27: iload_1
      28: iload_2
      29: iadd
      30: ireturn
      31: getfield      #42                 // Field com/github/vassilibykov/trifle/core/SquarePegException.value:Ljava/lang/Object;
      34: iload_0
      35: invokestatic  #38                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      38: astore_0
      39: goto          58
      42: getfield      #42                 // Field com/github/vassilibykov/trifle/core/SquarePegException.value:Ljava/lang/Object;
      45: iload_0
      46: invokestatic  #38                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      49: astore_0
      50: iload_1
      51: invokestatic  #38                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      54: astore_1
      55: goto          72
      58: astore_1
      59: aload_0
      60: iconst_2
      61: invokestatic  #18                 // Method com/github/vassilibykov/trifle/primitives/Sub.sub:(Ljava/lang/Object;I)I
      64: invokestatic  #38                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      67: invokedynamic #44,  0             // InvokeDynamic #0:call1:(Ljava/lang/Object;)Ljava/lang/Object;
      72: astore_2
      73: aload_1
      74: aload_2
      75: invokestatic  #50                 // Method com/github/vassilibykov/trifle/primitives/Add.add:(Ljava/lang/Object;Ljava/lang/Object;)I
      78: invokestatic  #38                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      81: dup
      82: instanceof    #34                 // class java/lang/Integer
      85: ifeq          97
      88: checkcast     #34                 // class java/lang/Integer
      91: invokevirtual #55                 // Method java/lang/Integer.intValue:()I
      94: goto          101
      97: invokestatic  #59                 // Method com/github/vassilibykov/trifle/core/SquarePegException.with:(Ljava/lang/Object;)Lcom/github/vassilibykov/trifle/core/SquarePegException;
     100: athrow
     101: ireturn
    Exception table:
       from    to  target type
           9    17    31   Class com/github/vassilibykov/trifle/core/SquarePegException
          18    26    42   Class com/github/vassilibykov/trifle/core/SquarePegException

## Generated code with a separate recovery method

An earlier implementation phase in which recovery already became compiled rather
than interpreted, but was compiled as a separate method.

    public final class com.github.vassilibykov.trifle.core.$gen$0 {
        public static final java.lang.Object closure0(java.lang.Object);
        Code:
           0: aload_0
           1: iconst_2
           2: invokestatic  #12                 // Method com/github/vassilibykov/trifle/primitives/LessThan.lessThan:(Ljava/lang/Object;I)Z
           5: ifeq          12
           8: iconst_1
           9: goto          37
          12: aload_0
          13: iconst_1
          14: invokestatic  #18                 // Method com/github/vassilibykov/trifle/primitives/Sub.sub:(Ljava/lang/Object;I)I
          17: invokedynamic #30,  0             // InvokeDynamic #0:call1:(I)I
          22: istore_1
          23: aload_0
          24: iconst_2
          25: invokestatic  #18                 // Method com/github/vassilibykov/trifle/primitives/Sub.sub:(Ljava/lang/Object;I)I
          28: invokedynamic #30,  0             // InvokeDynamic #0:call1:(I)I
          33: istore_2
          34: iload_1
          35: iload_2
          36: iadd
          37: invokestatic  #38                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
          40: areturn
          41: iconst_0
          42: iconst_3
          43: anewarray     #4                  // class java/lang/Object
          46: goto          61
          49: iconst_1
          50: iconst_3
          51: anewarray     #4                  // class java/lang/Object
          54: dup
          55: iconst_1
          56: iload_1
          57: invokestatic  #38                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
          60: aastore
          61: dup
          62: iconst_0
          63: aload_0
          64: aastore
          65: invokedynamic #46,  0             // InvokeDynamic #1:recover:(Lcom/github/vassilibykov/trifle/core/SquarePegException;I[Ljava/lang/Object;)Ljava/lang/Object;
          70: areturn
        Exception table:
           from    to  target type
              12    22    41   Class com/github/vassilibykov/trifle/core/SquarePegException
              23    33    49   Class com/github/vassilibykov/trifle/core/SquarePegException
    
      public static final java.lang.Object recovery$closure0(java.lang.Object, int, java.lang.Object[]);
        Code:
           0: aload_2
           1: dup
           2: iconst_0
           3: aaload
           4: astore_3
           5: dup
           6: iconst_1
           7: aaload
           8: astore        4
          10: dup
          11: iconst_2
          12: aaload
          13: astore        5
          15: pop
          16: aload_0
          17: iload_1
          18: tableswitch   { // 0 to 1
                         0: 69
                         1: 84
                   default: 40
              }
          40: pop
          41: aload_3
          42: iconst_2
          43: invokestatic  #12                 // Method com/github/vassilibykov/trifle/primitives/LessThan.lessThan:(Ljava/lang/Object;I)Z
          46: ifeq          56
          49: iconst_1
          50: invokestatic  #38                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
          53: goto          96
          56: aload_3
          57: iconst_1
          58: invokestatic  #18                 // Method com/github/vassilibykov/trifle/primitives/Sub.sub:(Ljava/lang/Object;I)I
          61: invokestatic  #38                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
          64: invokedynamic #52,  0             // InvokeDynamic #0:call1:(Ljava/lang/Object;)Ljava/lang/Object;
          69: astore        4
          71: aload_3
          72: iconst_2
          73: invokestatic  #18                 // Method com/github/vassilibykov/trifle/primitives/Sub.sub:(Ljava/lang/Object;I)I
          76: invokestatic  #38                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
          79: invokedynamic #52,  0             // InvokeDynamic #0:call1:(Ljava/lang/Object;)Ljava/lang/Object;
          84: astore        5
          86: aload         4
          88: aload         5
          90: invokestatic  #58                 // Method com/github/vassilibykov/trifle/primitives/Add.add:(Ljava/lang/Object;Ljava/lang/Object;)I
          93: invokestatic  #38                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
          96: areturn
    
      public static final int specialized$closure0(int);
        Code:
           0: iload_0
           1: iconst_2
           2: if_icmpge     9
           5: iconst_1
           6: goto          30
           9: iload_0
          10: iconst_1
          11: isub
          12: invokedynamic #30,  0             // InvokeDynamic #0:call1:(I)I
          17: istore_1
          18: iload_0
          19: iconst_2
          20: isub
          21: invokedynamic #30,  0             // InvokeDynamic #0:call1:(I)I
          26: istore_2
          27: iload_1
          28: iload_2
          29: iadd
          30: ireturn
          31: iconst_0
          32: iconst_3
          33: anewarray     #4                  // class java/lang/Object
          36: goto          51
          39: iconst_1
          40: iconst_3
          41: anewarray     #4                  // class java/lang/Object
          44: dup
          45: iconst_1
          46: iload_1
          47: invokestatic  #38                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
          50: aastore
          51: dup
          52: iconst_0
          53: iload_0
          54: invokestatic  #38                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
          57: aastore
          58: invokedynamic #46,  0             // InvokeDynamic #1:recover:(Lcom/github/vassilibykov/trifle/core/SquarePegException;I[Ljava/lang/Object;)Ljava/lang/Object;
          63: dup
          64: instanceof    #34                 // class java/lang/Integer
          67: ifeq          79
          70: checkcast     #34                 // class java/lang/Integer
          73: invokevirtual #63                 // Method java/lang/Integer.intValue:()I
          76: goto          83
          79: invokestatic  #67                 // Method com/github/vassilibykov/trifle/core/SquarePegException.with:(Ljava/lang/Object;)Lcom/github/vassilibykov/trifle/core/SquarePegException;
          82: athrow
          83: ireturn
        Exception table:
           from    to  target type
               9    17    31   Class com/github/vassilibykov/trifle/core/SquarePegException
              18    26    39   Class com/github/vassilibykov/trifle/core/SquarePegException
    }



## Generated code from the initial proof-of-concept

(preserved for its sentimental value)

The initial proof-of-concept compiled two methods per closure: a generic one
with a generic signature AND all internal operations on wrapper values, and
a specialized one. Specialization failures were only possible in the specialized
method, and were handled using an interpreter.

### Bytecode of the generic compiled method

    public static final java.lang.Object generic0(java.lang.Object);
    Code:
       0: aload_0
       1: iconst_2
       2: invokestatic  #12                 // Method com/github/vassilibykov/trifle/primitives/LessThan.lessThan:(Ljava/lang/Object;I)Z
       5: ifeq          15
       8: iconst_1
       9: invokestatic  #18                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      12: goto          51
      15: aload_0
      16: iconst_1
      17: invokestatic  #24                 // Method com/github/vassilibykov/trifle/primitives/Sub.sub:(Ljava/lang/Object;I)I
      20: invokestatic  #18                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      23: invokedynamic #35,  0             // InvokeDynamic #0:call1:(Ljava/lang/Object;)Ljava/lang/Object;
      28: astore_1
      29: aload_0
      30: iconst_2
      31: invokestatic  #24                 // Method com/github/vassilibykov/trifle/primitives/Sub.sub:(Ljava/lang/Object;I)I
      34: invokestatic  #18                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      37: invokedynamic #35,  0             // InvokeDynamic #0:call1:(Ljava/lang/Object;)Ljava/lang/Object;
      42: astore_2
      43: aload_1
      44: aload_2
      45: invokestatic  #41                 // Method com/github/vassilibykov/trifle/primitives/Add.add:(Ljava/lang/Object;Ljava/lang/Object;)I
      48: invokestatic  #18                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      51: areturn

### Bytecode of the adaptively specialized compiled method

Main (fast path) part:

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
      30: ireturn
      
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

Common epilogue finishing the construction of the frame replica and invoking a recovery interpreter:

      51: dup
      52: iconst_0
      53: iload_0
      54: invokestatic  #18                 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      57: aastore
      58: iconst_0
      59: invokestatic  #53                 // Method com/github/vassilibykov/trifle/acode/Interpreter.forRecovery:(I[Ljava/lang/Object;I)Lcom/github/vassilibykov/trifle/acode/Interpreter;
      62: swap
      63: invokevirtual #57                 // Method com/github/vassilibykov/trifle/core/SquarePegException.value:()Ljava/lang/Object;
      66: invokevirtual #60                 // Method com/github/vassilibykov/trifle/acode/Interpreter.interpret:(Ljava/lang/Object;)Ljava/lang/Object;
  
The value produced by the interpreter is on the stack. If it's an `Integer`, return it normally as an `int`.
  
      69: dup
      70: instanceof    #14                 // class java/lang/Integer
      73: ifeq          86
      76: checkcast     #14                 // class java/lang/Integer
      79: invokevirtual #64                 // Method java/lang/Integer.intValue:()I
      82: ireturn
      83: nop
      84: nop
      85: athrow
      86: invokestatic  #68                 // Method com/github/vassilibykov/trifle/core/SquarePegException.with:(Ljava/lang/Object;)Lcom/github/vassilibykov/trifle/core/SquarePegException;
      89: athrow
    Exception table:
       from    to  target type
           9    17    31   Class com/github/vassilibykov/trifle/core/SquarePegException
          18    26    39   Class com/github/vassilibykov/trifle/core/SquarePegException
