(ns com.madeye.clojure.notifymyandroid.notifymyandroid (:gen-class))

(require '[clj-http.client :as client])
(require '[com.madeye.clojure.common.common :as c])
(require '[clojure.data.xml    :as xml])
(require '[clojure.data.zip.xml :as zxml])
(require '[clojure.zip :as zip])
(require '[taoensso.timbre :as timbre])

(timbre/refer-timbre)

(declare notifymyandroid-debug notifymyandroid-live)

(defn initialise [config-file]
  (def config (c/load-props config-file))
  (if (read-string (:nma.debug config))
    (def notifyfn notifymyandroid-debug)
    (def notifyfn notifymyandroid-live)
  )
  (info "Notify function: " notifyfn)
)

(defn build-result
  [xml]
  (let [success (zxml/xml-> xml :success)
        error (zxml/xml-> xml :error)]
    (not (empty? success))
  )
)

(defn- notifymyandroid-debug
  ([apikey event description priority]
    (info "SENDING TO " apikey " @ " priority ": " event " - " description)
  )
)

(defn- notifymyandroid-live
  ([apikey event description priority]
    (let [url (:nma.base_url config)
          app-name (:nma.application_name config)
          params { :apikey apikey :application app-name :event event :description description :priority priority }
          response (client/get url { :query-params params })
          xml-string (:body response)
          reader (java.io.StringReader. xml-string)
        ]
      (build-result (zip/xml-zip (xml/parse reader)))
    )
  )
)

(defn notifymyandroid
  ([apikey event description priority]
    (notifyfn apikey event description priority)
  )
  ([event description priority]
    (notifymyandroid (:nma.apikey config) event description priority)
  )
  ([event description]
    (notifymyandroid event description (:nma.priority config))
  )
)
