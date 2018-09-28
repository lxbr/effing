(ns com.github.lxbr.effing.specs
  (:require [clojure.spec.alpha :as s]))

(s/def ::name string?)

(s/def ::var string?)

(s/def ::kind
  #{:boolean
    :int8
    :int16
    :int32
    :int64
    :float
    :double
    :void
    :pointer})

(s/def ::pointer boolean?)

(s/def ::type
  (s/keys :req-un [::kind]
          :opt-un [::pointer]))

(s/def ::return ::type)

(s/def ::param
  (s/merge ::type (s/keys :req-un [::name])))

(s/def ::params (s/coll-of ::param :kind vector?))

(s/def ::function
  (s/keys :req-un [::return ::name ::params]
          :opt-un [::var]))

