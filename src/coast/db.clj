(ns coast.db
  (:require [lighthouse.core :as db]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:refer-clojure :exclude [update drop]))

(def conn (atom nil))

(defn migrations []
  (edn/read-string (str "[" (->> "migrations.edn"
                                io/resource
                                slurp)
                        "]")))

(defn migrate
  ([]
   (migrate {}))
  ([m]
   (doseq [migration (migrations)]
     (db/migrate @conn migration m))))

(defn rollback
  ([]
   (db/rollback @conn))
  ([m]
   (db/rollback @conn m)))

(defn q
  ([v params]
   (db/q @conn v params))
  ([v]
   (db/q @conn v)))

(defn defq [filename]
  (db/defq @conn filename))

(defn pull [v where-clause]
  (db/pull @conn v where-clause))

(defn transact
  ([query params]
   (db/transact @conn query params))
  ([query]
   (db/transact @conn query)))

(defn insert [val]
  (db/insert @conn val))

(defn update [val]
  (db/update @conn val))

(defn delete [val]
  (db/delete @conn val))

(defn cols [k]
  (db/cols @conn k))

(def create db/create)
(def drop db/drop)
