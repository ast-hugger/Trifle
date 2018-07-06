Object subclass: Script instanceVariables: ()

! fibonacci: n
    ^n < 2
        ifTrue: [1]
        ifFalse: [(self fibonacci: n - 1) + (self fibonacci: n - 2)]

! doIt
    ^self fibonacci: 35
