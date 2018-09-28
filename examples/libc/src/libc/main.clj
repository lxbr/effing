(ns libc.main
  (:require [com.github.lxbr.effing :as ffi])
  (:import com.kenai.jffi.Library))

(defn create-code
  [functions]
  (let [lib (Library/getDefault)]
    (->> functions
         (map (partial ffi/create-implementation lib))
         (cons 'do))))

(defmacro bind!
  [functions]
  (create-code functions))

(bind!
 [

  {:name "getpid"
   :var  "get-pid"
   :return {:kind :int32}
   :params []}

  {:name "sin"
   :return {:kind :double}
   :params [{:name "x"
             :kind :double}]}

  {:name "toupper"
   :var  "to-upper-case"
   :return {:kind :int32}
   :params [{:name "ch"
             :kind :int32}]}
  
  {:name "uname"
   :return {:kind :int32}
   :params [{:name "buffer"
             :kind :int8
             :pointer true}]}

  ])

(comment

  (get-pid)

  (->> (range 0 21)
       (map (comp sin #(* 2 Math/PI (/ % 20))))
       (map #(+ 11 (Math/round (* 11 %))))
       (map #(str (apply str (repeat % " ")) "*"))
       (run! println))

  (->> (.getBytes "hello, world!")
       (map (comp char to-upper-case))
       (apply str))

  (let [bytes (byte-array 2048)]
    (uname bytes)
    ;; the struct encoded in `bytes` is platform dependent
    ;; splitting by `zero?` relies on the fact that macos
    ;; uses 256 bytes per struct entry and entries are far shorter
    ;; the entry order is also platform dependent
    ;; see `sys/utsname.h`
    (->> (partition-by zero? bytes)
         (remove (comp zero? first))
         (map byte-array)
         (map #(String. %))))

  )

