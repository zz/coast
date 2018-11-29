(ns coast.generators
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [coast.generators.resource :as resource]))

(defn usage []
  (println "Usage:
  coast new <project-name>
  coast gen resource <name>                                   # Creates a new file with the common crud functions in src/<resource>.clj
  coast gen migration <table>/<col> <table>/<another-col> ... # Creates a new edn migration in resources/migrations.edn
  coast gen sql:migration                                     # Creates a new sql migration in resources/migrations.edn

Examples:
  coast new my-project

  coast gen resource todo
  coast gen migration todo/name todo/done-at
  coast gen sql:migration"))

(defn migration [cols]
  (spit (io/resource "migrations.edn")
        (str "\n" (->> (map keyword cols)
                       (map #(hash-map :db/col % :db/type "text"))
                       (vec)))
        :append true))

(defn sql-migration []
  (spit (io/resource "migrations.edn")
        (str "\n" [{:db/up "" :db/down ""}])
        :append true))

(defn resource
  ([s]
   (resource s {}))
  ([s opts]
   (resource/write s opts)))

(defn gen [args]
  (let [[kind arg] args]
    (case kind
      "migration" (migration (drop 2 args))
      "sql:migration" (sql-migration arg)
      "resource" (resource arg)
      (usage))))

(defn -main [& args]
  (let [[action] args]
    (case action
      "gen" (gen (drop 1 args))
      (usage))))
