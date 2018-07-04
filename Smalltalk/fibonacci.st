Object subclass: FibonacciTester instanceVariables: ()

! fibonacci: n
    n < 2 ifTrue: [^1]
    ^(self fibonacci: n - 1) + (self fibonacci: n - 2)

! main
    ^self fibonacci: 35
