(ns coast
  (:require [potemkin :refer [import-vars]]
            [hiccup2.core]
            [coast.db]
            [coast.eta]
            [coast.env]
            [coast.time]
            [coast.components]
            [coast.responses]
            [coast.utils]
            [coast.error]
            [coast.router]
            [coast.validation]
            [coast.middleware.site]
            [coast.generators])
  (:refer-clojure :exclude [update drop]))

(import-vars
  [coast.responses
   ok
   bad-request
   not-found
   unauthorized
   server-error
   redirect
   flash]

  [coast.error
   raise
   rescue]

  [coast.db
   migrate
   rollback
   defq
   q
   pull
   transact
   delete
   insert
   update
   cols
   create
   drop]

  [coast.validation
   validate]

  [coast.components
   form
   js
   css]

  [coast.router
   wrap-routes
   prefix-routes]

  [coast.middleware.site
   wrap-layout]

  [coast.eta
   server
   app
   url-for
   action-for]

  [coast.env
   env]

  [coast.utils
   uuid]

  [coast.time
   now]

  [hiccup2.core
   raw
   html]

  [coast.generators
   gen])
