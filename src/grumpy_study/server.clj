(ns grumpy-study.server
  (:require
   [rum.core :as rum]
   [immutant.web :as web]
   [compojure.core :as cj]
   [compojure.route :as cjr])
  (:import
   [org.joda.time DateTime]
   [org.joda.time.format DateTimeFormat]))

(def posts
  [{:id "1"
    :created #inst "2017-08-30"
    :author "nikitonskiy"
    :body "some body"}
   {:id "2"
    :created #inst "2017-08-29"
    :author "freetonik"
    :body "some body 2"}])

(def date-formatter (DateTimeFormat/forPattern "dd.MM.YYYY"))

(defn render-date [inst]
  (.print date-formatter (DateTime. inst)))

(rum/defc post [post]
  [:.post
   [:.post_sidebar
    [:img.avatar {:src (str "/i/" (:author post) ".jpg")}]]
   [:div
    [:p [:span.author (:author post) ": "] (:body post)]
    [:p.meta (render-date (:created post)) " // " [:a {:href (str "/post/" (:id post))} "Ссылка"]]]])

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
    children
    [:footer
     [:a {:href "https://twitter.com/nikitonsky"} "Никита Прокопов"]
     ", "
     [:a {:href "https://twitter.com/freetonik"} "Рахим Давлеткалиев"]
     ". 2019. All rights retarded"
     [:br]
     [:a {:href "/feed" :rel "alternate" :type "application/rss+xml"} "RSS"]]
    [:script {:dangerouslySetInnerHTML {:__html
                                        "
window.onload = function() {
  reloadSubtitle();
  document.getElementById('site_subtitle').onclick = reloadSubtitle;
}

function reloadSubtitle() {
  var subtitles = [
  'Вы уверены, что хотите отменить? – Да / Нет / Отмена', 
  'Select purchase to purchase for $0.00 – PURCHASE / CANCEL', 
  'Это не текст, это ссылка. Не нажимайте на ссылку.',
  'Не обновляйте эту страницу! Не нажимайте НАЗАД',
  'Произошла ошибка OK',
  'Пароль должен содержать заглавную букву и специальный символ'
  ];
  var subtitle = subtitles[Math.floor(Math.random() * subtitles.length)];
  var div = document.getElementById('site_subtitle');
  div.innerHTML = subtitle;
}"}}]]])

(rum/defc index [posts]
  (page "Ворчание ягнят"
        (for [p posts]
          (post p))))

(defn render-html [component]
  (str "<!DOCTYPE html>\n" (rum/render-static-markup component)))

(cj/defroutes routes
  (cjr/resources "/i" {:root "public/i"})
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
