(ns coast.dev.server
  (:require [coast.db]
            [coast.env :as env]
            [coast.repl :as repl]
            [coast.utils :as utils]
            [lighthouse.core :as db]
            [org.httpkit.server :as httpkit]
            [ring.middleware.reload :as reload]))

(def server (atom nil))

(defn start
  ([app]
   (start app nil))
  ([app opts]
   (let [port (-> (or (:port opts) (env/env :port) 1337)
                  (utils/parse-int))]
     (reset! coast.db/conn (db/connect (env/env :database-url)))
     (def app app)
     (reset! server (httpkit/run-server (reload/wrap-reload #'app)
                                        (merge opts {:port port})))
     (println "Server is listening on port" port))))

(defn stop []
  (when (not (nil? @server))
    (@server :timeout 100)
    (reset! server nil)
    (db/disconnect @coast.db/conn)
    (reset! coast.db/conn nil)
    (println "Resetting dev server")))

(defn restart
  "Here's the magic that allows you to restart the server at will from the repl. It uses a custom version of repl/refresh that takes arguments"
  [app opts]
  (stop)
  (repl/refresh :after `start :after-args [app opts]))
