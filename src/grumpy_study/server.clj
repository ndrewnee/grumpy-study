(ns grumpy-study.server
  (:require
   [rum.core :as rum]
   [immutant.web :as web]
   [compojure.core :as cj]
   [compojure.route :as cjr]))

(def posts
  [{:id "1"
    :created #inst "2017-08-30"
    :author "nikitonskiy"
    :body "some body"}
   {:id "2"
    :created #inst "2017-08-29"
    :author "freetonik"
    :body "some body 2"}])

(rum/defc post [post]
  [:.post
   [:.post_sidebar
    [:img.avatar {:src (str "/i/" (:author post) ".jpg")}]]
   [:div
    [:p [:span.author (:author post) ": "] (:body post)]
    [:p.meta (:created post) "//" [:a {:href (str "/post/" (:id post))} "Link"]]]])

(rum/defc index [posts]
  [:html
   [:body
    (for [p posts]
      (post p))]])

(cj/defroutes routes
  (cj/GET "/" [:as req]
    {:body (rum/render-static-markup (index posts))})
  (cj/GET "/write" [:as req]
    {:body "WRITE"})
  (cj/POST "/write" [:as req]
    {:body "POST"}))

(def app routes)

(defn -main [& args]
  (let [args-map (apply array-map args)
        port-str (or (get args-map "-p")
                     (get args-map "--port")
                     "8080")]
    (web/run #'app {:port (Integer/parseInt port-str)})))

(comment
  (def server (-main "--port" "8000"))
  (web/stop server))
