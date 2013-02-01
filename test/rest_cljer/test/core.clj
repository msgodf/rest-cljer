(ns rest-cljer.test.core
  (:require [rest-cljer.core :refer [rest-driven]]
            [midje.sweet :refer :all]
            [clj-http.client :as http :refer [post put]]
            [environ.core :refer [env]]
            [clojure.data.json :refer [json-str read-str]])
  (:import [com.github.restdriver.clientdriver ClientDriver ClientDriverRequest$Method]))

(fact "expected rest-driven call succeeds without exceptions"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :POST, :url resource-path}
                      {:status 204}]
                     (post url) => (contains {:status 204}))))

(fact "unexpected rest-driven call should fail with exception"
      (let [restdriver-port (ClientDriver/getFreePort)
            url (str "http://localhost:" restdriver-port)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [] (post url))) => (throws RuntimeException))

(fact "test json document matching"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :POST :url resource-path
                       :body {:ping "pong"}}
                      {:status 204}]
                     (post url {:content-type :json
                                :body (json-str {:ping "pong"})
                                :throw-exceptions false}) => (contains {:status 204}))))

(fact "test sweetening of response definitions"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :GET :url resource-path}
                      {:body {:inigo "montoya"}}]
                     (let [resp (http/get url)]
                       resp => (contains {:status 200})
                       (:headers resp) => (contains {"content-type" "application/json"})
                       (read-str (:body resp) :key-fn keyword) => {:inigo "montoya"}))))

(fact "test post-processing of request and response, replace initial values with new ones using :and function"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :GET :url resource-path :and #(.withMethod % ClientDriverRequest$Method/POST)}
                      {:status 204 :and #(.withStatus % 205)}]
                     (post url) => (contains {:status 205}))))

(fact "give repeated response any times"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :PUT :url resource-path}
                      {:status 204 :times :any}]
                     (put url) => (contains {:status 204})
                     (put url) => (contains {:status 204})
                     (put url) => (contains {:status 204}))))

(fact "give repeated response a specfic number of times"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :POST :url resource-path}
                      {:status 200 :times 2}]
                     (post url) => (contains {:status 200})
                     (post url) => (contains {:status 200})))
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :POST :url resource-path}
                      {:status 200 :times 2}]
                     (post url)
                     (post url)
                     (post url))) => (throws RuntimeException))
