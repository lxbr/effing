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
         function-bindings
         (vector
          'addr   (if (some? lib)
                    (doto (.getSymbolAddress lib name) (or var name))
                    (list '.getSymbolAddress 'lib name))
          'ret    (get util/jffi-type-map (:kind return))
          'params (list 'into-array
                        'com.kenai.jffi.Type
                        (mapv (fn [{:keys [kind pointer]}]
                                (if (true? pointer)
                                  (get util/jffi-type-map :pointer)
                                  (get util/jffi-type-map kind)))
                              params))
          'func  '(com.kenai.jffi.Function. addr ret params))]
     (list
      'let (if (true? precompile-functions)
             function-bindings
             [])
      (list
       'defn (symbol (or var name))
       (-> (if (some? lib)
             []
             [(with-meta 'lib {:tag 'com.kenai.jffi.Library})])
           (into (map (comp symbol :name)) params)
           (with-meta {:tag return-tag}))
       (list
        'let (cond->> ['invoker '(com.kenai.jffi.Invoker/getInstance)
                       'buffer (->> (map util/param-to-put-method-call params)
                                    (apply list 'doto '(com.kenai.jffi.HeapInvocationBuffer. func)))]
               (false? precompile-functions)
               (into function-bindings))
        (let [{:keys [method transforms]} (get util/invoke-method-map (:kind return))]
          (apply list '-> (list '. 'invoker method 'func 'buffer)
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
