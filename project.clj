(defproject buddy-auth-examples "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring "1.5.0"]
                 [compojure "1.5.1"]
                 [buddy/buddy-auth "1.3.0"]
                 [ring/ring-json "0.4.0"]
                 [hiccup "1.0.5"]]

  :plugins [[lein-ring "0.10.0"]]

  ;:ring {:handler httpbasic.core/handler}
  ;:ring {:handler session.core/handler}
  ;:ring {:handler token.core/handler}
  ;:ring {:handler jwe.core/handler}
  :ring {:handler jws.core/handler}
  )
