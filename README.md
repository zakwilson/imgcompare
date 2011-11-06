Compare images for similarity. Uses a fairly naive pixel value comparison algorithm and doesn't look for larger features at all. Similarity scores under 10 pretty much guarantee it's a version of the same image with higher scores indicating a larger difference.

This should work well on images that are blurred, lowered in quality, adjusted for color and brightness or that have minor alterations to objects. It works poorly with significant cropping, and not at all with significant transformation such as rotation or mirroring.

This is *bad* Clojure style, involving imperative loops and mutation. I'll experiment with replacing the loops and adding parallelization at some point in the future.
