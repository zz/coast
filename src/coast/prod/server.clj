(ns coast.prod.server
  (:require [coast.db]
            [coast.env :as env]
            [coast.utils :as utils]
            [lighthouse.core :as db]
            [org.httpkit.server :as httpkit]))

(defn start
  "The prod server doesn't handle restarts with an atom, it's built for speed"
  ([app opts]
   (let [port (or (-> (or (:port opts) (env/env :port))
                      (utils/parse-int))
                  1337)]
     (println "Server is listening on port" port)
     (reset! coast.db/conn (db/connect (env/env :database-url)))
     (httpkit/run-server app (merge opts {:port port}))))
  ([app]
   (start app nil)))
