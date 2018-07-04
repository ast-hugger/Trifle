An implementation of (a core subset of) Scheme based on Trifle.

This is written as an extended test use case for Trifle. It implements only the
subset of Scheme that serves that purpose, and/or the pieces I find entertaining
to implement. Beyond that, I have no ambition to dominate the world with a yet
another Scheme implementation.
 
Additionally, variadic functions are not (yet?) supported because they are not
(yet?) supported by Trifle. The behavior of top-level bindings (function names
vs variable names) deviates from proper Scheme because of the way this prototype
organically evolved.
