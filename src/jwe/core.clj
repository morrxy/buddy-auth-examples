(ns jwe.core
  (:require [compojure.core :refer :all]
            [compojure.response :refer [render]]
            [ring.util.response :refer [response redirect content-type]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [clj-time.core :as time]
            [buddy.sign.jwt :as jwt]
            [buddy.core.nonce :as nonce]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [buddy.auth.backends.token :refer [jwe-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]))

(def secret (nonce/random-bytes 32))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Semantic response helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ok [d] {:status 200 :body d})
(defn bad-request [d] {:status 400 :body d})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controllers                                      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Home page controller (ring handler)
;; If incoming user is not authenticated it raises a not authenticated
;; exception, else it simply shows a hello world message.

(defn home
  [request]
  (if-not (authenticated? request)
    (throw-unauthorized)
    (ok {:status "Logged" :message (str "hello logged user "
                                        (:identity request))})))

;; Global var that stores valid users with their
;; respective passwords.

(def authdata {:admin "secret"
               :test  "secret"})

;; Authenticate Handler
;; Responds to post requests in same url as login and is responsible for
;; identifying the incoming credentials and setting the appropriate authenticated
;; user into session. `authdata` will be used as source of valid users.

(defn login
  [request]
  (let [username (get-in request [:body :username])
        password (get-in request [:body :password])
        valid? (some-> authdata
                       (get (keyword username))
                       (= password))]
    (if valid?
      (let [claims {:user (keyword username)
                    :exp  (time/plus (time/now) (time/seconds 3600))}
            token (jwt/encrypt claims secret {:alg :a256kw :enc :a128gcm})]
        (ok {:token token}))
      (bad-request {:message "wrong auth data"}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Routes and Middlewares                           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; User defined application routes using compojure routing library.
;; Note: there are no middleware for authorization, all authorization
;; system is totally decoupled from main routes.

(defroutes app
  (GET "/" [] home)
  (POST "/login" [] login))

;; Create an instance of auth backend.
(def auth-backend (jwe-backend {:secret  secret
                                :options {:alg :a256kw :enc :a128gcm}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Entry Point
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def handler
  (as-> app $
        (wrap-authorization $ auth-backend)
        (wrap-authentication $ auth-backend)
        (wrap-json-response $ {:pretty false})
        (wrap-json-body $ {:keywords? true :bigdecimals? true})))
