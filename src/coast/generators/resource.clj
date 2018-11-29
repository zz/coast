(ns coast.generators.resource
  (:require [coast.db]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [coast.utils :refer [kebab snake]]
            [lighthouse.core :as db])
  (:import (java.io File)))

(def pattern #"__([\w-]+)__")

(defn replacement [match m]
  (let [s (first match)
        k (-> match last keyword)]
    (str (get m k s))))

(defn fill [m s]
  (string/replace s pattern #(replacement % m)))

(defn prompt [s]
  (print s)
  (flush)
  (read-line))

(defn overwrite? [filename force?]
  (if (and (.exists (io/file filename))
           (not (true? force?)))
    (= (prompt (str filename " already exists. Overwrite? [y/n] ")) "y")
    true))

(defn form-element [k]
  (str "[:div
     [:label {:for \"" (str (namespace k) "/" (name k)) "\"} \""(name k) "\"]
     [:input {:type \"text\" :name \"" (str (namespace k) "/" (name k)) "\" :value (-> request :params "(str k)")}]]"))

(defn cols! [table]
  (let [excluded-cols #{"id" "updated-at" "created-at"}
        cols (->> (db/cols @coast.db/conn table)
                  (keys)
                  (map name)
                  (map kebab)
                  (set))
        cols (set/difference cols excluded-cols)]
    (map #(keyword table %) cols)))

(defn spit! [f s]
  (let [dir (->> (string/split f #"/")
                 (drop-last)
                 (string/join "/"))]
    (.mkdirs (File. dir))

    (spit f s)))

(defn dl-element [k]
  (str "[:dl
       [:dt \""(name k)"\"]
       [:dd (" (str k) " " (namespace k) ")]]"))

(defn table [ks]
  (str "[:table
     [:thead
      [:tr
       " (string/join "\n       "
          (map #(str "[:th \"" (name %) "\"]" ) ks)) "]]
     [:tbody
       (for [row rows]
        [:tr
         " (string/join "\n         "
             (map #(str "[:td (" (str %) " row)]") ks)) "])]]"))

(defn write
  ([resource opts]
   (let [filename (str "src/" resource ".clj")
         template "generators/resource.clj.txt"]
     (if (overwrite? filename (:overwrite? opts))
       (let [cols (cols! resource)]
         (->> (io/resource template)
              (slurp)
              (fill {:qualified-keywords (string/join " " cols)
                     :form-elements (string/join "\n\n    "
                                     (map form-element cols))
                     :dl-elements (string/join "\n\n     "
                                    (map dl-element cols))
                     :table (table cols)
                     :resource (kebab resource)})
              (spit! filename))
         (println filename "created successfully"))
       (println resource "skipped"))))
  ([resource]
   (write resource {})))
