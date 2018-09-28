(ns jna.main
  (:require [com.github.lxbr.effing :as ffi]
            [net.n01se.clojure-jna :as jna])
  (:import com.kenai.jffi.Library))

(set! *warn-on-reflection* true)

(jna/to-ns libc c [Double sin])

(defn create-code
  [functions]
  (let [lib (Library/getDefault)]
    (->> (map (partial ffi/create-implementation lib) functions)
         (cons 'do))))

(defmacro bind!
  [functions]
  (create-code functions))

(bind!
 [

  {:name "sin"
   :return {:kind :double}
   :params [{:name "x"
             :kind :double}]}

  ])

(defn nearly-equal?
  [^double x ^double y]
  (< (Math/abs (- x y)) 1e-5))

(comment

  ;; comparison of
  ;; 1. baseline java
  ;; 2. dynamic jna invocation
  ;; 3. predefined jna invocation
  ;; 4. predefined namespace jna invocation
  ;; 5. effing/libjffi invocation
  (let [xs (mapv double (range 1e5))
        results
        [(do (prn :java)
             (time (mapv #(Math/sin %) xs)))

         (do (prn :jna-invoke)
             (time (mapv #(jna/invoke Double c/sin %) xs)))

         (let [sin (jna/to-fn Double c/sin)]
           (prn :jna-to-fn)
           (time (mapv sin xs)))

         (do (prn :jna-to-ns)
             (time (mapv libc/sin xs)))

         (do (prn :effing)
             (time (mapv sin xs)))]]
    ;; are all result nearly equal?
    #_
    (->> (apply map vector results)
         (every? (fn [[x & more]] (every? #(nearly-equal? x %) more)))))

)

