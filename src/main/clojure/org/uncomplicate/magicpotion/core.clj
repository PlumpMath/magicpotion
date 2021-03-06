(ns org.uncomplicate.magicpotion.core
  (:use clojure.set))

(defn deep
  "Applies f to all the ancestors of the concept conc. 
  The hierarchy is traversed through the keyword :super.
  typical usage: (deep :properties x)" 
  [f conc]
  (mapcat f (tree-seq :super :super conc)))

(defn create-struct-deep
  [conc-def]
  (apply create-struct (set (map (comp :name :property) (deep :roles conc-def)))));;set added to solve the problem of inherited and redefined properties. This should be done more systematically later!

(defn infer-parents
  [h thing-def]
  (let [super (:super thing-def)
        name (:name thing-def)
        derive-thing (fn [h parent] (derive h name (:name parent)))] 
  (reduce derive-thing h super)))

(defn infer-hierarchy
  [h thing-def]
  (if-let [super (:super thing-def)]
    (reduce infer-parents (infer-parents h thing-def) super)
    h))
