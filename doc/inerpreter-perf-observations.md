With 20 reps of warmup, this computes fibonacci(35) in about 900 ms.
Pharo on the same machine computes it in 120 ms.



Interestingly, replacing Frame with just an Object[] and saving the caller frame
in a local in the call method reduces runtime by about 15%. However, dispensing
with object allocation altogether by creating a single 'stack' Object[] with a
current frame base index makes no \ difference compared to using Object[]
instead of Frame.