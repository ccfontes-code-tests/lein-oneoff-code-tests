; insert `{:user {:plugins [[lein-oneoff "0.3.1"]]}}` in `~/.lein/profiles.clj`
; usage: lein oneoff --exec toilet.clj <filename>

(defdeps [[org.clojure/clojure "1.7.0-beta3"]])

(ns toilet 
  (:require [clojure.test :refer [deftest is]]
  	        [clojure.set :refer [union]]
  	        [clojure.pprint :refer [cl-format]]))

(def i++ (fnil inc 0))

(def letters ; source: http://stackoverflow.com/a/2578825/1865008
  (set
    (map char
         (concat (range 65 91)
  	             (range 97 123)))))

(def word-sep (set " \t"))

(defn char-seq
  "Like line-seq, but for characters.
  Source: http://stackoverflow.com/a/11671362/1865008"
  [^java.io.Reader buff-reader]
  (let [c (.read buff-reader)]
    (if (>= c 0)
      (cons (char c) (lazy-seq (char-seq buff-reader))))))

(defn ->update-fn
  "Returns the correct partially applied report map update function."
  [chars]
  (let [non-punct (filter (union #{\newline} word-sep letters) chars)
  	    update-freq (fn [m] (reduce #(update-in %1 [:freq %2] i++) m non-punct))]
    (cond
      (word-sep (first chars)) identity
      (= \newline (first chars)) #(update % :lines + (count chars))
      :else (comp #(update % :words inc) update-freq))))

(defn wc
  "Returns map with :words, :lines count, and :freq (frequencies)"
  [chars]
  (let [partition-pred (some-fn #{\newline} (complement word-sep))]
    (->> (partition-by partition-pred chars)
      (reduce #((->update-fn %2) %1) {:words 0 :lines 0}))))

(defn avg-letters-per-word [{:keys [freq words]}]
  (if (= words 0)
  	0
    (float
      (/ (->> freq vals (apply +))
         words))))

(defn most-common-letter [{:keys [freq]}]
  (or (some->> freq (sort-by val) last first)
  	  "N/A"))

(defn report [{:keys [words lines freq] :as m}]
  (print "words:") (println words)
  (print "lines:") (println lines)
  (cl-format true "average letters per word:~,1f\n" (avg-letters-per-word m))
  (print "most common letter:") (println (most-common-letter m)))

(defn main
  "Read file into lazy char seq for wc.
  Will store at most unique words in memory."
  [filename]
  (with-open [buff-reader (clojure.java.io/reader filename)]
    (let [lazy-chars (char-seq buff-reader)]
      (wc lazy-chars))))

(-> *command-line-args* first main report)

(deftest wc

  (is (with-open [buff-reader (clojure.java.io/reader "wc")]
    (-> buff-reader char-seq seq)))

  (is (= (->update-fn [\space]) identity))

  (is (= (wc "hello world\nrecruit minions")
  	     {:words 4 :lines 2})))
