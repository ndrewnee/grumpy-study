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
    [:p.meta (:created post) "//" [:a {:href (str "/post/" (:id post))} "Ссылка"]]]])

(rum/defc page [title & children]
  [:html
   [:head
    [:meta {:http-equiv "Content-Type" :content "text-html; charset=UTF-8"}]
    [:title title]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]]
   [:body
    [:header
     [:h1 "Ворчание ягнят"]
     [:p#site_subtitle "Это не текст, это ссылка. Не нажимайте на ссылку"]]
    children]])

(rum/defc index [posts]
  (page "Ворчание ягнят"
        (for [p posts]
          (post p))))

(defn render-html [component]
  (str "<!DOCTYPE html>\n" (rum/render-static-markup component)))

(cj/defroutes routes
  (cj/GET "/" [:as req]
    {:body (rum/render-static-markup (index posts))})
  (cj/GET "/write" [:as req]
    {:body "WRITE"})
  (cj/POST "/write" [:as req]
    {:body "POST"}))

(defn with-headers [handler headers]
  (fn [request]
    (some-> (handler request)
            (update :headers merge headers))))

(def app
  (-> routes
      (with-headers {"Content-Type" "text/html; charset=utf-8"
                     "Cache-Control" "no-cache"
                     "Expires" "-1"})))

(defn -main [& args]
  (let [args-map (apply array-map args)
        port-str (or (get args-map "-p")
                     (get args-map "--port")
                     "8080")]
    (web/run #'app {:port (Integer/parseInt port-str)})))

(comment
  (def server (-main "--port" "8000"))
  (web/stop server))
