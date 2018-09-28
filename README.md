# Effing

Maybe this is a reference to *Moon Palace* or it's the feeling you have when 
interfacing with native code.

## Intended Use

`Effing` is designed to run at compile time. It transforms data descriptions of
native APIs into Clojure function definitions. The usual pattern is to define
a function that expands data into code and a macro that runs that function
at compile time.

```clojure
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

  ])

(get-pid) ;; => pid integer value

(sin (/ Math/PI 2)) ;; => 1.0
```

## Performance Characteristics

There are three ways to bind native functions on the JVM.
[JNI](https://docs.oracle.com/javase/7/docs/technotes/guides/jni/spec/jniTOC.html)
is very flexible and has the lowest performance overhead but it expects you to
write C/C++. [JNA](https://github.com/java-native-access/jna) is the most user
friendly option but is significantly less performant than JNI due to its use of
the Reflection API. [JNR](https://github.com/jnr/jnr-ffi) tries to be more
user friendly than JNI while not sacrificing as much performance as JNA.

`Effing` builds on [jffi](https://github.com/jnr/jffi) which is also the basis 
for JNR. The `jffi` library contains java bindings for 
[libffi](https://github.com/libffi/libffi). The promise of `jffi` is
to manage the communication of the JVM to and from native code for you.

The performance characteristics of `Effing` are the ones of `jffi`.
The overhead of calling into native code is significantly less than
with JNA but might not be as good as JNI.

## Data Descriptions

The format of data descriptions is defined in the `com.github.lxbr.effing.specs`
namespace. Data as the public facing API has the benefit of abtracting the 
implementation and being reusable in other contexts. It also acts as an
intermediate representation.

Data can be produced and read in all languages. It can be stored in files
and databases. It can be freely shared without knowing implementation details,
whereas a concrete implementation needs to be redone on every platform.
The latter is still the preferred approach because generating code from data
is a non-trivial task on most platforms. Clojure benefits from macros by
having a much simpler and more robust interface to code generation than
concatenating strings or transforming bytecode and ASTs.

For a programmatic API please refer to [JNR](https://github.com/jnr/jnr-ffi) 
or [jffi](https://github.com/jnr/jffi).

## License

This project is licensed under the terms of the Eclipse Public License 1.0 (EPL).


