(ns grumpy-study.server
  (:require
   [clojure.string :as str]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [compojure.core :as compojure]
   [compojure.route]
   [immutant.web :as web]
   [ring.util.response]
   [ring.middleware.params]
   [ring.middleware.multipart-params]
   [rum.core :as rum])
  (:import
   [java.util UUID]
   [org.joda.time DateTime]
   [org.joda.time.format DateTimeFormat])
  (:gen-class))

(def styles (slurp (io/resource "style.css")))
(def script (slurp (io/resource "script.js")))

(def date-formatter (DateTimeFormat/forPattern "dd.MM.YYYY"))

(defn render-date [inst]
  (.print date-formatter (DateTime. inst)))

(defn zip [col1 col2]
  (map vector col1 col2))

(rum/defc post [post]
  [:.post
   [:.post_sidebar
    [:img.avatar {:src (str "/i/" (:author post) ".jpg")}]]
   [:div
    (for [name (:pictures post)]
      [:img {:src (str "post/" (:id post) "/" name)}])
    (for [[p idx] (zip (str/split (:body post) #"\n+") (range))]
      [:p
       (when (== 0 idx)
         [:span.author (:author post) ": "])
       p])
    [:p.meta (render-date (:created post)) " // " [:a {:href (str "/post/" (:id post))} "Ссылка"]]]])

(rum/defc page [opts & children]
  (let [{:keys [title index?] :or {title "Ворчание ягнят" index? false}} opts]
    [:html
     [:head
      [:meta {:http-equiv "Content-Type" :content "text-html; charset=UTF-8"}]
      [:title title]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]]
     [:style {:dangerouslySetInnerHTML {:__html styles}}]
     [:body
      [:header
       (if index?
         [:h1 title]
         [:h1 [:a {:href "/"} title]])
       [:p#site_subtitle "Это не текст, это ссылка. Не нажимайте на ссылку"]]
      children
      [:footer
       [:a {:href "https://twitter.com/nikitonsky"} "Никита Прокопов"]
       ", "
       [:a {:href "https://twitter.com/freetonik"} "Рахим Давлеткалиев"]
       ". 2019. All rights retarded"
       [:br]
       [:a {:href "/feed" :rel "alternate" :type "application/rss+xml"} "RSS"]]
      [:script {:dangerouslySetInnerHTML {:__html script}}]]]))

(defn safe-slurp [source]
  (try
    (slurp source)
    (catch Exception e
      nil)))

(defn get-post [post-id]
  (let [path (str "posts/" post-id "/post.edn")]
    (-> (io/file path)
        (safe-slurp)
        (edn/read-string))))

(def ^:const encode-table "-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz")

(defn encode [num len]
  (loop [num num
         res ()
         len len]
    (if (== 0 len)
      (str/join res)
      (recur (quot num 64) (conj res (nth encode-table (rem num 64))) (dec len)))))

(defn next-post-id []
  (str (encode (quot (System/currentTimeMillis) 1000) 6)
       (encode (rand-int (* 64 64 64)) 3)))

(defn save-post! [post pictures]
  (let [dir           (io/file (str "posts/" (:id post)))
        picture-names (for [[picture idx] (zip pictures (range))
                            :let [in-name (:filename picture)
                                  [_ ext] (re-matches #".*(\.[^\.]+)" in-name)]]
                        (str (:id post) "_" (inc idx) ext))]
    (.mkdirs dir)
    (doseq [[picture name] (zip pictures picture-names)]
      (io/copy (:tempfile picture) (io/file dir name))
      (.delete (:tempfile picture)))
    (spit (io/file dir "post.edn") (pr-str (assoc post :pictures (vec picture-names))))))

(rum/defc index-page [post-ids]
  (page {:index? true}
        (for [post-id post-ids]
          (post (get-post post-id)))))

(rum/defc post-page [post-id]
  (page {}
        (post (get-post post-id))))

(rum/defc edit-post-page [post-id]
  (let [post (get-post post-id)
        create? (nil? post)]
    (page {"title" (if create? "Создание" "Редактирование")}
          [:form {:action (str "/post/" post-id "/edit")
                  :method "POST"
                  :enctype "multipart/form-data"}
           [:.edit_post_picture
            [:input {:type "file" :name "picture"}]]
           [:.edit_post_body
            [:textarea
             {:value (:body post "")
              :name "body"
              :placeholder "Пиши сюда..."}]]
           [:..edit_post_submit
            [:button (if create? "Создать" "Сохранить")]]])))

(defn render-html [component]
  (str "<!DOCTYPE html>\n" (rum/render-static-markup component)))

(defn post-ids []
  (->>
   (for [name (seq (.list (io/file "posts")))
         :let [child (io/file "posts" name)]
         :when (.isDirectory child)]
     name)
   (sort)
   (reverse)))

(compojure/defroutes routes
  (compojure.route/resources "/i" {:root "public/i"})
  (compojure/GET "/" []
    {:body (render-html (index-page (post-ids)))})
  (compojure/GET "/post/new" []
    {:status 303
     :headers {"Location" (str "/post/" (next-post-id) "/edit")}})
  (compojure/GET "/post/:post-id" [post-id]
    {:body (render-html (post-page post-id))})
  (compojure/GET "/post/:id/:img" [id img]
    (ring.util.response/file-response (str "posts/" id "/" img)))
  (compojure/GET "/post/:post-id/edit" [post-id]
    {:body (render-html (edit-post-page post-id))})
  (ring.middleware.multipart-params/wrap-multipart-params
   (compojure/POST "/post/:post-id/edit" [post-id :as req]
     (let [params (:multipart-params req)
           body (get params "body")
           picture (get params "picture")]

       (save-post! {:id post-id
                    :body body
                    :author "nikitonsky"}
                   [picture])
       {:status 302
        :headers {"Location" (str "/post/" post-id)}})))
  (fn [req]
    {:status 404
     :body "404 Not Found"}))

(defn with-headers [handler headers]
  (fn [request]
    (some-> (handler request)
            (update :headers merge headers))))

(defn print-errors [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (.printStackTrace e)
        {:status 500
         :headers {"Content-Type" "text/plain; charset=utf-8"}
         :body (with-out-str
                 (clojure.stacktrace/print-stack-trace (clojure.stacktrace/root-cause e)))}))))

(def app
  (-> routes
      (ring.middleware.params/wrap-params)
      (with-headers {"Content-Type" "text/html; charset=utf-8"
                     "Cache-Control" "no-cache"
                     "Expires" "-1"})
      (print-errors)))

(defn -main [& args]
  (let [args-map (apply array-map args)
        port-str (or (get args-map "-p")
                     (get args-map "--port")
                     "8080")]
    (println "Starting web server on port" port-str)
    (web/run #'app {:port (Integer/parseInt port-str)})))

(comment
  (def server (-main "--port" "8000"))
  (web/stop server))
