(ns com.github.lxbr.effing
  (:require [com.github.lxbr.effing.util :as util]))

(defn create-implementation
  ([function] (create-implementation nil function))
  ([lib function] (create-implementation lib function nil))
  ([^com.kenai.jffi.Library lib function opts]
   (let [{:keys [precompile-functions]
          :or {precompile-functions true}} opts
         {:keys [var name return params]} function
         param-count (if (some? lib)
                       (count params)
                       (inc (count params)))
         return-tag (case (util/type-to-tag return)
                      (byte short int long)
                      (if (< 4 param-count)
                        'Long
                        'long)
                      
                      (float double)
                      (if (< 4 param-count)
                        'Double
                        'double)
                      
                      (booleans bytes shorts ints longs floats doubles)
                      (util/type-to-tag return)

                      nil)
         addr-sym (gensym "addr")
         ret-sym (gensym "ret")
         params-sym (gensym "params")
         func-sym (gensym "func")
         lib-sym (gensym "lib")
         invoker-sym (gensym "invoker")
         buffer-sym (gensym "buffer")
         function-bindings
         (vector
          addr-sym   (if (some? lib)
                       (let [addr (.getSymbolAddress lib name)]
                         (assert (pos-int? addr) (str "Symbol '" name "' not found."))
                         addr)
                       (list '.getSymbolAddress lib-sym name))
          ret-sym    (get util/jffi-type-map (:kind return))
          params-sym (list 'into-array
                        'com.kenai.jffi.Type
                        (mapv (fn [{:keys [kind pointer]}]
                                (if (true? pointer)
                                  (get util/jffi-type-map :pointer)
                                  (get util/jffi-type-map kind)))
                              params))
          func-sym  (list 'com.kenai.jffi.Function. addr-sym ret-sym params-sym))]
     (list
      'let (if (and (some? lib) (true? precompile-functions))
             function-bindings
             [])
      (list
       'defn (symbol (or var name))
       (-> (if (some? lib)
             []
             [(with-meta lib-sym {:tag 'com.kenai.jffi.Library})])
           (into (map (comp symbol :name)) params)
           (with-meta {:tag return-tag}))
       (list
        'let (cond->> [invoker-sym '(com.kenai.jffi.Invoker/getInstance)
                       buffer-sym (->> (map util/param-to-put-method-call params)
                                    (apply list 'doto (list 'com.kenai.jffi.HeapInvocationBuffer. func-sym)))]
               (or (nil? lib) (not precompile-functions))
               (into function-bindings))
        (let [{:keys [method transforms]} (get util/invoke-method-map (:kind return))]
          (apply list '-> (list '. invoker-sym method func-sym buffer-sym)
                 transforms))))))))

(defn create-closure-builder
  [spec]
  (let [{:keys [name return params function]} spec]
    (list
     'defn (symbol name)
     (with-meta '[closure] {:tag 'com.kenai.jffi.Closure$Handle})
     (list
      'let (vector 'ret (get util/jffi-type-map (:kind return))
                   'ps  (list 'into-array
                              'com.kenai.jffi.Type
                              (mapv (fn [{:keys [kind pointer]}]
                                      (if (true? pointer)
                                        (get util/jffi-type-map :pointer)
                                        (get util/jffi-type-map kind)))
                                    params))
                   'cm  '(com.kenai.jffi.ClosureManager/getInstance)
                   'cc  'com.kenai.jffi.CallingConvention/DEFAULT)
      '(.newClosure cm closure ret ps cc)))))
