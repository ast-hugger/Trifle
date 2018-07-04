(define (factorial n)
 (if (< n 2)
  1
  (* n (factorial (- n 1)))))

(print (factorial 1))
(print (factorial 2))
(print (factorial 3))
(print (factorial 4))
(print (factorial 5))
(print (factorial 100))
