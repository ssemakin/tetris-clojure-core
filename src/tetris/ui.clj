(ns tetris.ui
  (:require
    [chime :refer [chime-at]]
    [clj-time.core :as t]
    [tetris.board :as board]
    [tetris.core :as core]
    [lanterna.screen :as term :refer :all]))

(defn move-down [state]
  (update-in state [:tetromino :coords :y] dec))

(defn move-right [state]
  (update-in state [:tetromino :coords :x] inc))

(defn move-left [state]
  (update-in state [:tetromino :coords :x] dec))

(defn move-when-no-collision [state action]
  (let [next-state (action state)]
    (if (core/collision-detected?
          (core/move-to-xy
            (:x (:coords (:tetromino next-state)))
            (:y (:coords (:tetromino next-state)))
            (first (:positions (:tetromino next-state))))
          (clojure.set/union (:heap next-state) (:wall-bricks (:boundaries next-state))))
      state
      next-state)))

(defn do-draw [screen b-updated on-done-fn]

  (let [tetro (:tetromino b-updated)
        tetro-bricks (core/move-to-xy (:x (:coords tetro)) (:y (:coords tetro)) (first (:positions tetro)))
        world (clojure.set/union (:heap b-updated) (:wall-bricks (:boundaries b-updated)))
        all (clojure.set/union tetro-bricks world)
        s screen]

    (term/clear s)
    
    (doall
      (map
        #(term/put-string s (:x %) (- (:top-y (:boundaries b-updated)) (:y %)) "@")
        all))

    
    (term/redraw s)
    
    (on-done-fn b-updated)
    )
  )

(defn board-timer [screen board]
  (chime-at [(-> 1 t/secs t/from-now)]
    (fn [time]
      (let [k (term/get-key screen)]
        (cond 
          (= k :escape) (term/stop screen)
          (= k :left) (do-draw screen (move-when-no-collision board move-left) #(board-timer screen %))
          (= k :right) (do-draw screen (move-when-no-collision board move-right) #(board-timer screen %))
          (= k :down) (do-draw screen (move-when-no-collision board move-down) #(board-timer screen %))
          :else (do-draw screen (move-when-no-collision board move-down) #(board-timer screen %))
        ))
      ))
  )

(defn event-codes []
  (let [user-action (repeat 9 :user-action)
        gravity-action [:gravity-action]
        init-codes (flatten (interleave gravity-action (seq user-action)))]
    (cycle init-codes)))

(defn draw-board []

  (let [b (board/state)
        s (term/get-screen)]
    (term/start s)

    (board-timer s b)
    
    
    ))
