(ns reitit.spec-test
  (:require [clojure.test :refer [deftest testing is are]]
            [#?(:clj clojure.spec.test.alpha :cljs cljs.spec.test.alpha) :as stest]
            [clojure.spec.alpha :as s]
            [reitit.core :as r]
            [reitit.spec :as rs]
            [expound.alpha :as e])
  #?(:clj
     (:import (clojure.lang ExceptionInfo))))

(stest/instrument)

(deftest router-spec-test

  (testing "router"

    (testing "route-data"
      (are [data]
        (is (r/router? (r/router data)))

        ["/api" {}]

        ["api" {}]

        [["/api" {}]]

        ["/api"
         ["/ipa" ::ipa]
         ["/tea"
          ["/room"]]])

      (testing "with invalid routes"
        (are [data]
          (is (thrown-with-msg?
                ExceptionInfo
                #"Call to #'reitit.core/router did not conform to spec"
                (r/router
                  data)))

          ;; path
          [:invalid {}]

          ;; vector data
          ["/api" []
           ["/ipa"]])))

    (testing "routes conform to spec (can't spec protocol functions)"
      (is (s/valid? ::rs/routes (r/routes (r/router ["/ping"])))))

    (testing "options"

      (are [opts]
        (is (r/router? (r/router ["/api"] opts)))

        {:path "/"}
        {:data {}}
        {:expand (fn [_ _] {})}
        {:coerce (fn [route _] route)}
        {:compile (fn [_ _])}
        {:conflicts (fn [_])}
        {:router r/linear-router})

      (are [opts]
        (is (thrown-with-msg?
              ExceptionInfo
              #"Call to #'reitit.core/router did not conform to spec"
              (r/router
                ["/api"] opts)))

        {:path :api}
        {:path nil}
        {:data nil}
        {:expand nil}
        {:coerce nil}
        {:compile nil}
        {:conflicts nil}
        {:router nil}))))

(deftest route-data-validation-test
  (testing "validation is turned off by default"
    (is (r/router? (r/router
                     ["/api" {:handler "identity"}]))))

  (testing "with default spec validates :name and :handler"
    (is (thrown-with-msg?
          ExceptionInfo
          #"Invalid route data"
          (r/router
            ["/api" {:handler "identity"}]
            {:validate rs/validate-spec!})))
    (is (thrown-with-msg?
          ExceptionInfo
          #"Invalid route data"
          (r/router
            ["/api" {:name "kikka"}]
            {:validate rs/validate-spec!}))))

  (testing "spec can be overridden"
    (is (r/router? (r/router
                     ["/api" {:handler "identity"}]
                     {:spec any?
                      :validate rs/validate-spec!})))))

(deftest parameters-test
  (is (s/valid?
        ::rs/parameters
        {:parameters {:query {:a string?}
                      :body {:b string?}
                      :form {:c string?}
                      :header {:d string?}
                      :path {:e string?}}}))

  (is (not (s/valid?
             ::rs/parameters
             {:parameters {:header {"d" string?}}})))

  (is (s/valid?
        ::rs/responses
        {:responses {200 {:description "ok", :schema string?}
                     400 {:description "fail"}
                     500 {:schema string?}
                     :default {}}}))

  (is (not (s/valid?
             ::rs/responses
             {:responses {"200" {:description "ok", :schema string?}}})))

  (is (not (s/valid?
             ::rs/responses
             {:responses {200 {:description :ok, :schema string?}}}))))
