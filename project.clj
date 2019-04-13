(defproject grumpy-study "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520" :scope "provided"]
                 [ring/ring-core "1.7.1"]
                 [org.immutant/web "2.1.10"]
                 [compojure "1.6.1"]
                 [rum "0.11.3"]]
  :repl-options {:init-ns grumpy-study.server}
  :main grumpy-study.server
  :profiles {
    :uberjar {
      :aot [grumpy-study.server]
      :uberjar-name "grumpy-study.jar"
    }
  })
