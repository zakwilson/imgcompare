(ns imgcompare.core
  (:require [clojure.java.io :as j]
            clojure.main)
  (:import (java.io File) (javax.imageio ImageIO) (java.awt.image BufferedImage))
  (:gen-class))

(set! *warn-on-reflection* true)

(defn int-sum
  "(reduce unchecked-add ints) with type hinting"
  ([ints]
     (let [f (first ints)]
       (if f
         (int-sum f (next ints))
         0)))
  ([i ints]
     (let [f (first ints)]
       (if f
         (recur (+ (int i) (int f)) (next ints))
         i))))

(def imgtype (BufferedImage/TYPE_INT_RGB))
(def comp-dist 5)
(def max-size 50)

(defn pick-size [size ^BufferedImage img1 ^BufferedImage img2]
  (let [img1-width (.getWidth img1)
        img2-width (.getWidth img2)
        img1-height (.getHeight img1)
        img2-height (.getHeight img2)]
    (min size img1-width img2-width img1-height img2-height)))

(defn scale-image [size ^BufferedImage img]
  (let [orig-width (.getWidth img)
        orig-height (.getHeight img)
        width (min orig-width size)
        height (min orig-height
                    size
                    (* (/ width orig-width) orig-height))
        width (min width ;round and round we go!
                   (* (/ height orig-height) orig-width))
        simg (BufferedImage. width height imgtype)
        g (.createGraphics simg)]
    (.drawImage g img 0 0 width height nil)
    (.dispose g)
    simg))

(defn crop [^BufferedImage img width height]
  (let [cimg (BufferedImage. width height imgtype)
        x (.getWidth img)
        y (.getHeight img)
        g (.createGraphics cimg)
        half-width-diff (/ (Math/abs (int (- x width))) 2)
        half-height-diff (/ (Math/abs (int (- y height))) 2)
        startx (Math/floor half-width-diff)
        starty (Math/floor half-height-diff)
        endx (Math/ceil (- x half-width-diff))
        endy (Math/ceil (- y half-height-diff))]
    (.drawImage g img 0 0 width height startx starty endx endy nil)
    (.dispose g)
    cimg))

(defn ensure-same-size [^BufferedImage img1 ^BufferedImage img2]
  (let [width1 (.getWidth img1)
        height1 (.getHeight img1)
        width2 (.getWidth img2)
        height2 (.getWidth img2)
        height (min height1 height2)
        width (min width1 width2)]
    [(crop img1 height width) (crop img2 height width)]))
        

(defn px-dist [px1 px2 px1x px1y px2x px2y]
  (let [px1 (int (bit-and 0x00ffffff px1)) ; ignore alpha
        px2 (int (bit-and 0x00ffffff px2))
        r1 (bit-and (int (/ px1 (int 65536))) (int 0xff))
        g1 (bit-and (int (/ px1 (int 256))) (int 0xff))
        b1 (bit-and px1 (int 0xff))
        r2 (bit-and (int (/ px2 (int 65536))) (int 0xff))
        g2 (bit-and (int (/ px2 (int 256))) (int 0xff))
        b2 (bit-and px2 (int 0xff))
        color-dist (int-sum
                    (map (fn [x]
                           (Math/abs (int x)))
                         [(- r1 r2) (- g1 g2) (- b1 b2)]))
        geo-dist (+ (Math/pow (- px1x px2x) 2)
                    (Math/pow (- px1y px2y) 2))]

    (/ color-dist (max geo-dist 1))))

(defn px-compare [^BufferedImage img ^BufferedImage img2 x y]
  (let [startx (max 0 (- x comp-dist))
        starty (max 0 (- y comp-dist))
        endx (min (.getWidth img) (+ x comp-dist))
        endy (min (.getHeight img) (+ x comp-dist))
        px (.getRGB img x y)]
    (with-local-vars [dist 0]
      (dotimes [xi (- endx startx)]
        (dotimes [yi (- endy starty)]
          (let [curx (+ xi startx)
                cury (+ yi starty)
                px2 (.getRGB img2 curx cury)]
          (var-set dist (+ (var-get dist)
                           (px-dist px
                                    px2
                                    x
                                    y
                                    curx
                                    cury))))))
      (var-get dist))))

(defn imgcompare [^BufferedImage img1 ^BufferedImage img2]
  (with-local-vars [dist (BigDecimal. 0)]
    (dotimes [x (.getWidth img1)]
      (dotimes [y (.getHeight img1)]
        (var-set dist (+ (var-get dist)
                         (px-compare img1 img2 x y)))))
    (var-get dist)))

(defn c [^String img1 ^String img2]
  (let [img1 (ImageIO/read (j/as-file img1))
        img2 (ImageIO/read (j/as-file img2))
        img1s (scale-image max-size img1)
        img2s (scale-image max-size img2)
        [img1c img2c] (ensure-same-size img1s img2s)]
    (int (/ (- (int (imgcompare img1c img2c))
               (int (imgcompare img1c img1c)))
            10000))))

(defn -main []
  (clojure.main/repl :init #(in-ns 'imgcompare.core)))