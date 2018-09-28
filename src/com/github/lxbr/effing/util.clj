(ns com.github.lxbr.effing.util)

(def type-hint-map
  {:boolean 'boolean
   :int8    'byte
   :int16   'short
   :int32   'int
   :int64   'long
   :float   'float
   :double  'double
   :void    'void
   :pointer 'long})

(def pointer-type-hint-map
  {:boolean 'booleans
   :int8    'bytes
   :int16   'shorts
   :int32   'ints
   :int64   'longs
   :float   'floats
   :double  'doubles
   :void    'bytes
   :pointer 'longs})

(defn type-to-tag
  [type]
  (let [{:keys [kind pointer]} type]
    (-> (if (true? pointer)
          pointer-type-hint-map
          type-hint-map)
        (get kind))))

(def put-method-map
  {:boolean {:method    'putInt
             :transform (fn [sym] (list 'if sym 1 0))}
   :int8    {:method 'putByte}
   :int16   {:method 'putShort}
   :int32   {:method 'putInt}
   :int64   {:method 'putLong}
   :float   {:method 'putFloat}
   :double  {:method 'putDouble}
   :pointer {:method 'putAddress}})

(def put-array-method-map
  {:boolean {:transform
             (fn [arr-sym]
               `(areduce ~arr-sym i# ret# (int-array (alength ~arr-sym))
                         (aset ret# i# (if (aget ~arr-sym i#) 1 0))))}})

(defn param-to-put-method-call
  [param]
  (let [{:keys [name kind pointer]} param
        sym (symbol name)]
    (if (true? pointer)
      (let [{:keys [transform]
             :or {transform identity}} (get put-array-method-map kind)
            tag (type-to-tag param)
            arr-sym (with-meta sym {:tag tag})]
        `(.putArray ~(transform arr-sym) 0 (alength ~arr-sym) 0))
      (let [{:keys [method transform]
             :or {transform identity}} (get put-method-map kind)]
        (list '. method (transform sym))))))

(def invoke-method-map
  {:int8    {:method 'invokeInt}
   :int16   {:method 'invokeInt}
   :int32   {:method 'invokeInt}
   :int64   {:method 'invokeLong}
   :float   {:method 'invokeFloat}
   :double  {:method 'invokeDouble}
   :boolean {:method 'invokeInt
             :transforms '[(zero?) (not)]}
   :void    {:method     'invokeAddress
             :transforms '[(do nil)]}
   :pointer {:method 'invokeAddress}})

(def jffi-type-map
  {:pointer 'com.kenai.jffi.Type/POINTER
   :void    'com.kenai.jffi.Type/VOID
   :boolean 'com.kenai.jffi.Type/SINT32
   :int8    'com.kenai.jffi.Type/SINT8
   :int16   'com.kenai.jffi.Type/SINT16
   :int32   'com.kenai.jffi.Type/SINT32
   :int64   'com.kenai.jffi.Type/SINT64
   :float   'com.kenai.jffi.Type/FLOAT
   :double  'com.kenai.jffi.Type/DOUBLE})
