; insert `{:user {: plugins [[lein-oneoff "0.3.1"]]}}` in `~/.lein/profiles.clj`
; execute: $ lein oneoff ascii_graphics_editor.clj --exec

(defdeps [[org.clojure/clojure "1.6.0"] [midje "1.6.3"] [the-flood "0.1.1"]])

(ns ascii_graphics_editor
  (:require [midje.sweet :refer [against-background facts falsey]]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [the-flood.core :refer [flood-fill]]))

(defn range-inclusive [start end]
  (range start (inc end)))

(defn draw-line-segment [img [start end] coord-fn C]
  (reduce #(assoc-in %1 (coord-fn %2) C)
  	      img
  	      (->> [start end] sort (apply range-inclusive))))

(def coord-out-boundaries-msg
  "Pixel or Dimension portion must be betwen 1 and 250, inclusive.")

(defn coords-inside-limits? [& coords]
  (every? #(<= 0 % 250) coords))

(defn dec-cmd-line-coords [cmd-line]
  (if (= (first cmd-line) 'I)
    cmd-line
    (mapv #(if (integer? %) (dec %) %)
          cmd-line)))

(defn uppercase-alpha-char? [any]
  (re-matches #"[A-Z]" (str any)))

(defmulti cmd->op
  "Returns the image operation function corresponding to the command.
  Command arguments are closed over the returning function."
  (fn [& args] (first args)))

(defmethod cmd->op 'I [_ M N]
  {:pre [(every? integer? [M N])]}
  "Create a new M x N image with all pixels coloured white (O)."
  (constantly
    (let [px-line (vec (repeat M 'O))]
      (vec (repeat N px-line)))))

(defmethod cmd->op 'C [_]
  "Clears the table, setting all pixels to white (O)."
  (constantly []))

(defmethod cmd->op 'L
  [_ X Y C]
  {:pre [(every? integer? [X Y])
         (uppercase-alpha-char? C)]}
  "Colours the pixel (X,Y) with colour C." ; I know this isn't a docstring
  (fn [img] (assoc-in img [Y X] C)))

(defmethod cmd->op 'V
  [_ X Y1 Y2 C]
  {:pre [(every? integer? [X Y1 Y2])
         (uppercase-alpha-char? C)]}
  "Draw a vertical segment of colour C in column X between rows Y1 and Y2 (inclusive)"
  (fn [img]
    (draw-line-segment img [Y1 Y2] #(vector %1 X) C)))

(defmethod cmd->op 'H
  [_ X1 X2 Y C]
  {:pre [(every? integer? [X1 X2 Y])
         (uppercase-alpha-char? C)]}
  "Draw a horizontal segment of colour C in row Y between columns X1 and X2 (inclusive)."
  (fn [img]
    (draw-line-segment img [X1 X2] #(vector Y %1) C)))

(defmethod cmd->op 'F
  [_ X Y C]
  {:pre [(every? integer? [X Y])
         (uppercase-alpha-char? C)]}
  "Flood Fill: where → area R where (X,Y) is contained; color → C"
  (fn [img] (flood-fill img [X Y] C)))

(defmethod cmd->op 'S [_]
  "Show the contents of the current image"
  (fn [img]
  	(println "=>")
    (doseq [line img]
      (println (str/join line)))
    img))

(defmethod cmd->op :default [cmd & _]
  (println "Unrecognized command:" cmd)
  identity)

(defn img-task [img input]
  (try

    (let [operation (apply cmd->op (dec-cmd-line-coords input))]
      (if (->> input (filter integer?) (apply coords-inside-limits?))
        (operation img)
        (println coord-out-boundaries-msg)))

    (catch clojure.lang.ArityException e
      (println "Wrong number of arguments:" (-> input rest count))
      img)

    (catch AssertionError e
      (let [msg (.getMessage e)]
        (cond
          (re-find #"integer?" msg) (println "Argument must be an integer.")
          (re-find #"uppercase-alpha-char?" msg)
            (println "Argument must be a character from A-Z.")))
      img)

    (catch Exception e
      (println "Uncaught error:" (.getMessage e))
      img)))

(defn main []
  (reduce
    (fn [img user-input]
      (let [user-input (edn/read-string (str "[" user-input "]"))
      	    cmd (first user-input)]
        (cond
          (= user-input []) img
          (= cmd 'X) (reduced img)
          (and (nil? img) (not= cmd 'I)) (println "Please create an image first.")
          :else (img-task img user-input))))
    nil
   	(repeatedly #(do (print "> ") (flush) (read-line)))))

(if (some #{"--exec"} *command-line-args*)
  (main)
  ; run tests: $ lein oneoff --repl ccfontes_graphics_editor.clj
  ;            user=> (use 'midje.repl)
  ;            user=> (autotest :files "ccfontes_graphics_editor.clj")
  (facts

    (range-inclusive 0 2) => [0 1 2]

    (coords-inside-limits? 1 100 250) => true
    (coords-inside-limits? -1 0 251) => false

    (dec-cmd-line-coords ['I 4 5]) => ['I 4 5]
    (dec-cmd-line-coords ['L 3 5 'G]) => ['L 2 4 'G]

    'M => uppercase-alpha-char?
    (uppercase-alpha-char? 'MM) => falsey
    (uppercase-alpha-char? 4) => falsey

    (img-task nil ['I 2 2]) => [['O 'O] ['O 'O]]

    (with-out-str (img-task [['O]] ['S])) => "=>\nO\n"

    (let [img ((cmd->op 'I 3 2) nil)]

  	  img => [['O 'O 'O] ['O 'O 'O]]

      (draw-line-segment img [0 1] #(vector % 1) 'R) => [['O 'R 'O] ['O 'R 'O]]

      ((cmd->op 'C) img) => []

      (with-out-str (img-task img ['I 300 2])) => (str coord-out-boundaries-msg "\n")
      (with-out-str (img-task img ['I 'i 2 1])) => "Wrong number of arguments: 3\n"
      (with-out-str (img-task img ['I 'i 2])) => "Argument must be an integer.\n"
      (with-out-str (img-task img ['L 3 2 4])) => "Argument must be a character from A-Z.\n"

      ((cmd->op 'L 1 0 'B) img) => [['O 'B 'O] ['O 'O 'O]]

      ((cmd->op 'V 1 0 1 'R) img) => [['O 'R 'O] ['O 'R 'O]]

      ((cmd->op 'H 0 1 1 'R) img) => [['O 'O 'O] ['R 'R 'O]]

      ((cmd->op 'F 1 1 'G) img) => [['G 'G 'G] ['G 'G 'G]]

      (with-out-str ((cmd->op 'D 1 1 'G) img)) => "Unrecognized command: D\n"

      (with-out-str ((cmd->op 'S) img)) => "=>\nOOO\nOOO\n")))
