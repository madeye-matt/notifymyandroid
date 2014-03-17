(ns com.madeye.clojure.notifymyandroid.notifymyandroid (:gen-class))

(require '[clojure.string :as str])
(require '[clj-http.client :as client])
(require '[com.madeye.clojure.common.common :as c])
(require '[clojure.data.xml    :as xml])
(require '[clojure.data.zip.xml :as zxml])
(require '[clojure.zip :as zip])
(require '[taoensso.timbre :as timbre])

(timbre/refer-timbre)

(declare notifymyandroid-debug notifymyandroid-live)

(defn- validate-config-item
  [config kw]
  (if-let [invalid (nil? (kw config))]
    (do
      (error "Config " (name kw) " is missing")
      false
    )
    true
  )
)

(defn- validate-config
  [config]
  (every? true? (map (partial validate-config-item config) [ :nma.base_url :nma.apikey :nma.application_name :nma.debug ]))
)

(defn initialise [config-file]
  (def config (c/load-props config-file))
  (if (validate-config config)
    (do 
      (if (read-string (:nma.debug config))
        (def notifyfn notifymyandroid-debug)
        (def notifyfn notifymyandroid-live)
      )
      (def nma-apikeys (str/split (:nma.apikey config) #","))
      (info "Notify function: " notifyfn)
    )
    (error "Invalid config")
  )
)

(defn build-result
  [xml]
  (let [success (zxml/xml-> xml :success)
        error-msg (zxml/xml-> xml :error)]
    (if (not (empty? success))
      true
      (do
        (error "Failed to send message: " error-msg)
        false
      )
    )
  )
)

(defn- notifymyandroid-debug
  ([event description priority apikey]
    (info "SENDING TO " apikey " @ " priority ": " event " - " description)
  )
)

(defn- notifymyandroid-live
  ([event description priority apikey]
    (let [url (:nma.base_url config)
          app-name (:nma.application_name config)
          params { :apikey apikey :application app-name :event event :description description :priority priority }
          response (client/get url { :query-params params })
          xml-string (:body response)
          reader (java.io.StringReader. xml-string)
        ]
      (debug "query-params: " params)
      (debug "response: " response)
      (info "Sending: " event "-" description "-" priority "-" apikey)
      (build-result (zip/xml-zip (xml/parse reader)))
    )
  )
)

(defn notifymyandroid
  ([event description priority apikeys]
    (debug "apikeys: "  (count apikeys))
    (doseq [this-key apikeys]
      (notifyfn event description priority this-key)
    )
  )
  ([event description priority]
    (notifymyandroid event description priority nma-apikeys)
  )
  ([event description]
    (notifymyandroid event description (:nma.priority config))
  )
)
