(ns meta.main
  (:require [com.github.lxbr.effing :as ffi]
            [clojure.java.io :as io])
  (:import (com.kenai.jffi Library Type MemoryIO)
           java.io.File
           (java.nio ByteBuffer ByteOrder)))

;; choose your architecture
(def native-lib
  #_"jni/x86_64-SunOS/libjffi-1.2.so"
  #_"jni/arm-Linux/libjffi-1.2.so"
  #_"jni/i386-Linux/libjffi-1.2.so"
  #_"jni/sparcv9-Linux/libjffi-1.2.so"
  #_"jni/ppc64-Linux/libjffi-1.2.so"
  #_"jni/ppc64le-Linux/libjffi-1.2.so"
  #_"jni/x86_64-Linux/libjffi-1.2.so"
  #_"jni/ppc-AIX/libjffi-1.2.a"
  #_"jni/i386-Windows/jffi-1.2.dll"
  #_"jni/x86_64-Windows/jffi-1.2.dll"
  #_"jni/x86_64-OpenBSD/libjffi-1.2.so"
  "jni/Darwin/libjffi-1.2.jnilib"
  #_"jni/i386-SunOS/libjffi-1.2.so"
  #_"jni/aarch64-Linux/libjffi-1.2.so"
  #_"jni/x86_64-FreeBSD/libjffi-1.2.so"
  #_"jni/sparcv9-SunOS/libjffi-1.2.so"
  #_"jni/ppc64-AIX/libjffi-1.2.a")

(defn create-code
  [functions]
  (let [tmp (File/createTempFile
             "libjffi-1.2"
             (subs native-lib (.lastIndexOf native-lib ".")))
        res (io/resource native-lib)]
    (.deleteOnExit tmp)
    (with-open [is (io/input-stream res)]
      (io/copy is tmp))
    (let [lib (-> (.getAbsolutePath tmp)
                  (Library/openLibrary 0))]
      (->> functions
           (map (partial ffi/create-implementation lib))
           (cons 'do)))))

(defmacro bind!
  [functions]
  (create-code functions))

(bind!
    [

     {:name "ffi_prep_cif"
      :var "prep-cif"
      :return {:kind :int32}
      :params [{:name "cif"
                :kind :pointer}
               {:name "abi"
                :kind :int32}
               {:name "nargs"
                :kind :int32}
               {:name "rkind"
                :kind :pointer}
               {:name "akinds"
                :kind :pointer}]}

     {:name "ffi_call"
      :var "call!"
      :return {:kind :void}
      :params [{:name "cif"
                :kind :pointer}
               {:name "fn"
                :kind :pointer}
               {:name "rvalue"
                :kind :pointer}
               {:name "avalue"
                :kind :pointer}]}

     {:name "dlopen"
      :return {:kind :pointer}
      :params [{:name "file"
                :kind :pointer}
               {:name "mode"
                :kind :int32}]}

     {:name "dlsym"
      :return {:kind :pointer}
      :params [{:name "handle"
                :kind :pointer}
               {:name "name"
                :kind :int8
                :pointer true}]}

     ])

(let [method (.getDeclaredMethod Type "handle" (make-array Class 0))]
  (.setAccessible method true)
  (defn get-type-handle
    [type]
    (.invoke method type (object-array 0))))

(def default-abi 2)

(comment

  ;; Using the libffi functionality directly.
  ;; This only seems to work for no argument functions.
  ;; Trying to call a function that takes arguments
  ;; results in a `SIGSEGV`.
  (let [io (MemoryIO/getInstance)
        cif (.allocateMemory io 32 true)
        ret-buf (.allocateMemory io 4 true)
        lib (dlopen 0 (bit-or Library/GLOBAL Library/LAZY))
        addr (dlsym lib (.getBytes "getpid"))]
    (prep-cif cif default-abi 0 (get-type-handle Type/UINT) 0)
    (call! cif addr ret-buf 0)
    (let [ret (.getInt io ret-buf)] 
      (.freeMemory io cif)
      (.freeMemory io ret-buf)
      ret))

  )
