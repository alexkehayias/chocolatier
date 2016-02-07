(ns chocolatier.utils.devcards
  "Utility functions for devcards")


(defn str->markdown-code-block
  "Wraps the str in a markdown code block. Returns a new string."
  [s]
  (str "```\n" s "\n```"))
