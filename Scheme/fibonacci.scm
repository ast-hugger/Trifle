(define (fibonacci n)
 (if (< n 2)
  1
  (+ (fibonacci (- n 1)) (fibonacci (- n 2)))))

(define (repeat n f)
 (if (< 0 n)
  (begin
   (f)
   (repeat (- n 1) f))))

(print (fibonacci 1))
(print (fibonacci 2))
(print (fibonacci 3))
(print (fibonacci 4))
(print (fibonacci 5))
(print (fibonacci 6))

(print "Warming up...")
(repeat 30 (lambda () (fibonacci 35)))
(print "done, time:")
(print (ms-to-run (lambda () (fibonacci 35))))
