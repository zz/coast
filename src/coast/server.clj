(ns coast.server
  (:require [clojure.tools.namespace.repl :as repl]
            [org.httpkit.server :as httpkit]
            [environ.core :as environ]
            [coast.utils :as utils]
            [coast.middleware :as middleware]))

(defonce server-atom (atom nil))

(defn start
  ([app opts]
   (let [{:keys [port]} opts
         port (-> (or port (environ/env :port)) (utils/parse-int))]
     (println (str "Server is listening on port " port))
     (httpkit/run-server app {:port port})))
  ([app]
   (start app {}))
  ([]
   (reset! server-atom (start (middleware/dev (resolve (symbol "app")))))))

(defn stop []
  (when @server-atom
    (@server-atom :timeout 100)
    (reset! server-atom nil)))

(defn restart []
  (stop)
  (repl/refresh :after 'coast.server/start))

(defn dev [app opts]
  (def app app)
  (restart))

(defn prod [app opts]
  (start app opts))

(defn start-server
  ([app opts]
   (if utils/dev?
     (dev app opts)
     (prod app opts)))
  ([app]
   (start-server app {})))