(ns lobster-writer.views
  (:require
    [re-frame.core :as re-frame]
    [lobster-writer.subs :as subs]
    [lobster-writer.events :as events]
    [lobster-writer.components.editable-list :refer [editable-list]]
    [lobster-writer.utils :as utils]
    [lobster-writer.constants :as constants]
    [re-com.core :refer [button title p v-box h-box gap label line hyperlink-href input-text h-split input-textarea]]
    [clojure.string :as s]
    [reagent.core :as r]
    ;[cljsjs.react-quill]
    ))


;(def quill (r/adapt-react-class js/ReactQuill))


;; home

(defn home []
  (let [*all-essays (re-frame/subscribe [::subs/all-essays])]
    [v-box

     :children [[p "Welcome to Lobster Writer, an application to help you write essays based on the advice of Jordan Peterson. "
                 "To start, either start a new essay or select an existing one."]
                [gap :size "25px"]
                [:ul.list-group {:style {:max-width "500px"}}
                 (->> @*all-essays
                      (map val)
                      (map #(-> [:a.list-group-item.list-group-item-active
                                 {:href "#"
                                  :on-click (partial re-frame/dispatch [::events/essay-selected (:id %)])}
                                 (:title %)])))]
                [gap :size "10px"]
                [button :class "btn-primary" :label "Start a new essay" :on-click #(re-frame/dispatch [::events/start-new-essay])]]]))


(defn about []
  [v-box
   :children [[p "Lobster Writer is an application to help you write essays. It is based on the advice of Dr. Jordan Peterson in "
               [hyperlink-href :label "this essay writing guide" :href "https://jordanbpeterson.com/docs/430_docs/Template.docx"]
               ". According to Peterson, this method will help you \"to write an excellent essay from beginning to end\". "
               "Lobster Writer is free (gratis and libre) software - you can find the souce code "
               [hyperlink-href :label "here" :href "https://github.com/DaveWM/lobster-writer"]
               "."]
              [p {:style {:font-weight "bold"}} "Lobster Writer is not associated with Dr. Peterson in any way."]]])


(defn candidate-topics [current-essay]
  [v-box
   :children [[p "This is the first step in writing your essay. List around "
               [:b "10"]
               " topics that you would like to write about, or questions that you would like to answer."]
              [editable-list {:items (:candidate-topics current-essay)
                              :on-item-added #(re-frame/dispatch [::events/candidate-topic-added %])
                              :on-item-removed #(re-frame/dispatch [::events/candidate-topic-removed %])}]
              [gap :size "15px"]
              [button :disabled? (empty? (:candidate-topics current-essay)) :class "btn-primary" :label "Next Step"
               :on-click #(re-frame/dispatch [::events/next-step])]]])


(defn reading-list [current-essay]
  [v-box
   :children [[p
               "Great, you've thought of some potential topics to write about. Now you need to find some books or articles to read. "
               "You should read around " [:b "5 to 10 books per 1000 words of essay."] " List them here."]
              [editable-list {:items (:reading-list current-essay)
                              :on-item-added #(re-frame/dispatch [::events/reading-list-item-added %])
                              :on-item-removed #(re-frame/dispatch [::events/reading-list-item-removed %])}]
              [gap :size "15px"]
              [button
               :disabled? (empty? (:reading-list current-essay)) :class "btn-primary" :label "Next Step"
               :on-click #(re-frame/dispatch [::events/next-step])]]])


(defn topic-choice [current-essay]
  [v-box
   :children [[p "You now need to choose your topic, and the length your essay will be."]
              [:ul.list-group {:style {:max-width "500px"}}
               (->> (:candidate-topics current-essay)
                    (map #(-> [:a.list-group-item.list-group-item-active
                               {:href "#"
                                :class (when (= % (:title current-essay)) "active")
                                :on-click (partial re-frame/dispatch [::events/topic-selected %])}
                               %])))]
              [gap :size "15px"]
              [label :label "Target Essay Length"]
              [input-text
               :model (str (:target-length current-essay))
               :on-change #(re-frame/dispatch [::events/essay-target-length-changed (utils/parse-int %)])]
              [gap :size "15px"]
              [button
               :disabled? (not (and (contains? (:candidate-topics current-essay) (:title current-essay))
                                    (:target-length current-essay)))
               :class "btn-primary"
               :label "Next Step"
               :on-click #(re-frame/dispatch [::events/next-step])]]])


(defn outline [current-essay]
  (let [min-sentences (min 15 (int (/ (:target-length current-essay) 100)))]
    [v-box
     :children [[p "It's now time to write the outline of your essay. You need to write "
                 [:b min-sentences] " headings, one per 100 words of your essay (up to 15 headings)."]
                [gap :size "15px"]
                [editable-list {:items (map key (:outline current-essay))
                                :on-item-added #(re-frame/dispatch [::events/outline-heading-added %])
                                :on-item-removed #(re-frame/dispatch [::events/outline-heading-removed %])}]
                [gap :size "5px"]
                [p "You have written " [:b (count (:outline current-essay))] " headings out of " [:b min-sentences] "."]
                [gap :size "15px"]
                [button
                 :disabled? (> min-sentences (count (:outline current-essay)))
                 :class "btn-primary"
                 :label "Next Step"
                 :on-click #(re-frame/dispatch [::events/next-step])]]]))


(defn outline-paragraphs [current-essay]
  [v-box
   :children (concat [[p "Aim to write about " [:b "10 to 15"] " sentences per outline heading."
                       "You can write more or less if you want."]]
                     (->> (:outline current-essay)
                          (mapcat (fn [[heading section]]
                                    [[label :label heading]
                                     [input-textarea
                                      :model (:v1 (:paragraph section))
                                      :change-on-blur? false
                                      :on-change #(re-frame/dispatch [::events/outline-paragraph-updated heading %])
                                      :rows 8
                                      :width "450px"]
                                     [p "You have written " [:b (count (get-in section [:sentences :v1]))] " sentences."]])))
                     [[button
                       :disabled? (not-every? (comp #(not (s/blank? (:v1 (:paragraph %)))) val) (:outline current-essay))
                       :class "btn-primary"
                       :label "Next Step"
                       :on-click #(re-frame/dispatch [::events/next-step])]])])


(defn rewrite-sentences [current-essay]
  [v-box
   :children (concat [[p
                       "Re-write all the sentences you wrote in the previous step. "
                       "If you can't improve on a sentence, just copy and paste it into the input box."
                       "If you want to completely remove a sentence, leave the input box blank."]]
                     (->> (:outline current-essay)
                          (map (fn [[heading section]]
                                 [v-box
                                  :children [[title :level :level3 :label heading]
                                             (->> (get-in section [:sentences :v1])
                                                  (map-indexed (fn [idx v1]
                                                                 [idx v1 (get-in section [:sentences :v2 idx])]))
                                                  (mapcat (fn [[idx v1 v2]]
                                                            [[label :label v1]
                                                             [input-textarea
                                                              :rows 2
                                                              :width "400px"
                                                              :model v2
                                                              :on-change #(re-frame/dispatch [::events/sentence-rewritten heading idx %])]
                                                             [gap :size "5px"]])))]])))
                     [[gap :size "15px"]
                      [button
                       :disabled? (->> (get-in current-essay [:outline])
                                       (mapcat (comp :v2 :sentences val))
                                       (every? s/blank?))
                       :class "btn-primary"
                       :label "Next Step"
                       :on-click #(re-frame/dispatch [::events/next-step])]])])


(defn reorder-sentences [current-essay]
  [v-box
   :children (concat [[p "Re-order the sentences within each paragraph."]]
                     (->> (:outline current-essay)
                          (map (fn [[heading section]]
                                 [v-box
                                  :children [[title :level :level3 :label heading]
                                             [p (get-in section [:paragraph :v2])]
                                             [editable-list {:items (get-in section [:sentences :v2])
                                                             :on-item-moved-up #(re-frame/dispatch [::events/sentence-moved-up heading %])
                                                             :on-item-moved-down #(re-frame/dispatch [::events/sentence-moved-down heading %])}]]])))
                     [[gap :size "15px"]
                      [button
                       :class "btn-primary"
                       :label "Next Step"
                       :on-click #(re-frame/dispatch [::events/next-step])]])])


(defn reorder-paragraphs [current-essay]
  (let [ordered-sections (utils/ordered-by (:outline current-essay) (:paragraph-order current-essay))]
    [v-box
     :children (concat [[p "Now re-order the paragraphs."]
                        (map #(-> [p %]) (:paragraph-order current-essay))
                        [editable-list {:items ordered-sections
                                        :label-fn #(get-in % [:paragraph :v2])
                                        :on-item-moved-up #(re-frame/dispatch [::events/paragraph-moved-up %])
                                        :on-item-moved-down #(re-frame/dispatch [::events/paragraph-moved-down %])}]
                        [gap :size "15px"]]
                       (->> ordered-sections
                            (map (comp :v2 :paragraph))
                            (map #(-> [p {:style {:text-indent "20px"}} %])))
                       [[gap :size "15px"]
                        [button
                         :class "btn-primary"
                         :label "Next Step"
                         :on-click #(re-frame/dispatch [::events/next-step])]])]))


(defn read-draft [current-essay]
  [v-box
   :children (concat
               [[p "Read your draft essay. You don't need to memorize it, just read it as you would someone else's essay."]]
               (->> (utils/ordered-by (:outline current-essay) (:paragraph-order current-essay))
                    (map (comp :v2 :paragraph))
                    (map #(-> [p {:style {:text-indent "20px"}} %])))
               [[gap :size "15px"]
                [button
                 :class "btn-primary"
                 :label "Next Step"
                 :on-click #(re-frame/dispatch [::events/next-step])]])])


(defn essay-step [current-essay page page-component]
  [v-box
   :children [[title :level :level2 :underline? true :label (:title current-essay)]
              [gap :size "12px"]
              [h-split
               :initial-split 10
               :splitter-size "12px"
               :panel-1 [v-box
                         :size "1"
                         :style {:background-color "#e1e8f0"
                                 :padding-left "20px"
                                 :padding-right "20px"
                                 :border-radius "8px"}
                         :children [[title :level :level3 :label "Essay Steps"]
                                    [gap :size "5px"]
                                    [:ul.nav.nav-pills.nav-stacked
                                     (->> constants/steps
                                          (map #(let [enabled (utils/step-before-or-equal? % (:highest-step current-essay))]
                                                  [:li.nav-item {:class (str (when (= (:current-step current-essay) %)
                                                                               "active ")
                                                                             (when-not enabled
                                                                               "disabled "))}
                                                   [:a {:href (when enabled (utils/step-url (:id current-essay) %))}
                                                    (utils/displayable-step-name %)]])))]]]
               :panel-2 [v-box
                         :children [[title :level :level3 :underline? true :label (utils/displayable-step-name page)]
                                    [gap :size "10px"]
                                    [page-component current-essay]]]]]])

(defn not-found []
  [p "Route not found!"])

(defn pages [page]
  (let [page-component   (case page
                           :home home
                           :about about
                           :candidate-topics candidate-topics
                           :reading-list reading-list
                           :topic-choice topic-choice
                           :outline outline
                           :outline-paragraphs outline-paragraphs
                           :rewrite-sentences rewrite-sentences
                           :reorder-sentences reorder-sentences
                           :reorder-paragraphs reorder-paragraphs
                           :read-draft read-draft
                           not-found)
        for-single-essay (not (contains? #{home about not-found} page-component))]
    (if for-single-essay
      (let [*current-essay (re-frame/subscribe [::subs/current-essay])]
        (if @*current-essay
          [essay-step @*current-essay page page-component]
          [p "Essay not found!"]))
      [page-component])))


(defn main-panel []
  (let [*active-page (re-frame/subscribe [::subs/active-page])]
    [v-box
     :padding "25px"
     :children [[h-box
                 :children [[title :level :level1 :margin-top "2px" :margin-bottom "2px" :label "Lobster Writer"]
                            [gap :size "55px"]
                            [hyperlink-href
                             :label "Home"
                             :href "/"]
                            [gap :size "20px"]
                            [hyperlink-href
                             :label "About"
                             :href "/about"]]
                 :align :center]
                [line]
                [gap :size "15px"]
                [pages @*active-page]]]))
