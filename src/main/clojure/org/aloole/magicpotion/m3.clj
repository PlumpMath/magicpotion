(ns org.aloole.magicpotion.m3
  (:use org.aloole.magicpotion.utils)
  (:use [org.aloole.magicpotion.validation :as validation])
  (:use org.aloole.magicpotion.core))

(def concept-struct (create-struct :name :properties :super))

(def property-struct (create-struct :name :validators :super))

;;---------------------------------------------------------------------

(defn create-concept-def 
  [& params]
  (with-meta
    (apply struct-map concept-struct params)
    {:type ::m3-concept}))
  
(defn concept-def 
  [v] 
  (::def (meta v)))

;;---------------------------------------------------------------------

(defn create-property-def 
  [& params]
  (with-meta
    (apply struct-map property-struct params)
    {:type ::m3-property}))

(defn property-def 
  [v] 
  (::def (meta v)))

(defn create-relationship-def 
  [& params]
  (with-meta
    (apply struct-map property-struct params)
    {:type ::m3-relationship}))
;;---------------------------------------------------------------------
(defmulti create-validator type)

(defmethod create-validator ::m3-property
  [property-def]
    (create-val-validator (reverse (deep :validators property-def))))

(defmethod create-validator ::m3-relatinship
  [property-def]
    (create-ref-validator (reverse (deep :validators property-def))))
;;---------------------------------------------------------------------
(defmacro validators
  [v]
  `(:validators (property-def (var ~v))))

(defn create-property [property-def]
  (let [property-name (:name property-def)]
    (ref (fn [conc]
           (property-name conc))
         :meta {:type ::property
                ::def property-def
                ::hierarchy (infer-hierarchy (make-hierarchy) property-def)
                ::validation/validator (create-validator property-def)})))

(defmacro property
  [name validators super]
  `(let [property-def# (create-property-def 
                         :name (to-keyword ~name)
                         :validators ~validators
                         :super (map property-def ~super))]
     (def ~name (create-property property-def#))))

(defn create-ref-property [property-def]
  (let [property-name (:name property-def)]
    (ref (fn [conc]
           (property-name conc))
         :meta {:type ::ref-property
                ::def property-def
                ::hierarchy (infer-hierarchy (make-hierarchy) property-def)
                ::validation/validator (create-validator property-def)})))

(defmacro ref-property
  [name validators super]
  `(let [property-def# (create-ref-property-def 
                         :name (to-keyword ~name)
                         :validators ~validators
                         :super (map property-def ~super))]
     (def ~name (create-property property-def#))))
;;--------------------------------------------------------------------------------------

(defn create-validators
  [property-defs]
  (let [validators (zipmap (map :name property-defs) 
                           (map create-validator property-defs))] 
        (reduce #(if (val %2) (assoc %1 (key %2) (val%2)) %1) {} validators)))

;;---------------------------------------------------------------------
(defn create-concept [concept-def]
  (let [concept-name (:name concept-def)
        concept-struct (create-struct-deep concept-def)
        validators (create-validators (deep :properties concept-def))
        hierarchy (infer-hierarchy (make-hierarchy) concept-def)
        instance-metadata {:type concept-name
                           ::def concept-def
                           ::hierarchy hierarchy
                           ::struct concept-struct
                           ::validation/validators validators}]
    (ref (fn [& property-entries]
           (with-meta
             (apply struct-map concept-struct (validate validators property-entries))
             instance-metadata))
       :meta {:type ::concept
              ::def concept-def
              ::hierarchy hierarchy
              ::name concept-name
              ::validation/validators validators})))

(defmacro concept
  ([name]
   (concept name nil))
  ([name properties]
   (concept name properties nil))
  ([name properties super]
   `(let [name-keyword# (to-keyword ~name)
          concept-def# (create-concept-def
                         :name (to-keyword ~name)
                         :properties (map property-def ~properties) 
                         :super (map concept-def ~super))]
          ;;validators# (create-validator (deep :validators property-def))
          ;;validators# (create-validators (deep :properties concept-def#))]
      (do
        (def ~(suf-symbol name "?") (is-instance name-keyword#))
        (def ~name (create-concept concept-def#))))))

;;  ----------------- Predicates -------------------------------------
(defn is-instance
  [t]
  (let [concept-name (if (keyword? t) t (::name (meta t)))]
    (assert concept-name)
    (fn [instance]
      (if-let [hierarchy (::hierarchy (meta instance))]
              (isa? (::hierarchy (meta instance)) (type instance) concept-name)
              false))))

