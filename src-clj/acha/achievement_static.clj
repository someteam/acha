(ns acha.achievement-static)

(def table
  {:eraser
       {:name "Eraser"
        :description "Make a commit with no lines added, only deletions"}
    :massive
       {:name "Massive"
        :description "Add more than 1000 lines in a single commit"}
    :scribbler
       {:name "Scribbler"
        :description "Create a README"}
    :owl
       {:name "Owl"
        :description "Commit between 4am and 7am local time"}
    :flash
       {:name "Flash"
        :description "Two different commits within 1 minute"}
    :waste
       {:name "Waste"
        :description "Your commit was reverted completely by someone else"}
    :loneliness
       {:name "Loneliness"
        :description "You are the only committer for a week"}
    :easy-fix
       {:name "Easy Fix"
        :description "Swap two lines"}
    :multilingual
       {:name "Multilingual"
        :description "Add/edit files in 5 different languages in a single commit"}
    :necromancer
       {:name "Necromancer"
        :description "Make a commit to a repo that hasn’t been touched for 1 month or more"}
    :mover
       {:name "Mover"
        :description "Move a file from one place to another without changing it"}
    :world-balance
       {:name "World Balance"
        :description "Number of lines added == number of lines deleted"}
    :get
       {:name "Get"
        :description "Make commit #1000, or #1111, or #1234"}
    :narcissist
       {:name "Narcissist"
        :description "Use your own name in a commit message"}
    :blamer
       {:name "Blamer"
        :description "Use someone else’s name in a commit message"}
    :collision
       {:name "Collision"
        :description "Publish commit with the same N first chars of SHA-1 as existing commit"}
    :lucky
       {:name "Lucky"
        :description "Consecutive 777 in SHA-1"}
    :mark-of-the-beast
       {:name "Mark of the Beast"
        :description "Consecutive 666 in SHA-1"}
    :hydra
       {:name "Hydra"
        :description "Make a commit with 3+ parents"}
    :commenter
       {:name "Commenter"
        :description "Only add a comment"}
    :peacemaker
       {:name "Peacemaker"
        :description "Resolve 100 conflicts"}
    :ocd
       {:name "OCD"
        :description "Commit with just trailing spaces removed"}
    :holy-war
       {:name "Holy War"
        :description "Change tabs to spaces or vice versa"}
    :combo
       {:name "Combo"
        :description "10+ commits in a row"}
    :combo-breaker
       {:name "Combo Breaker"
        :description "Make a commit after someone had N commits in a row"}
    :worker-bee
       {:name "Worker Bee"
        :description "Make 100+ non-merge commits"}
    :fat-ass
       {:name "Fat Ass"
        :description "Commit 2Mb file or bigger"}
    :ooops
       {:name "Ooops"
        :description "Commit and revert commit within 1 minute"}
    :deal-with-it
       {:name "Deal with it"
        :description "Update master branch with force mode"}
    :dangerous-game
       {:name "Dangerous Game"
        :description "Commit after 6PM friday"}
    :empty-commit
       {:name "<empty title>"
        :description "Make an empty commit"}
    :time-get
       {:name "Get"
        :description "Commit exactly at 00:00"}
    :what-happened-here
       {:name "What Happened Here?"
        :description "Edit a file that hasn’t been touched for a year"}
    :all-things-die
       {:name "All Things Die"
        :description "Delete a file that has been added in the initial commit (and at least a year has passed)"}
    :for-stallman
       {:name "For Stallman!"
        :description "Add GPL license file to the repo"}
    :change-of-mind
       {:name "Change of Mind"
        :description "Change license type or edit license file"}
    :munchkin
       {:name "Munchkin"
        :description "Get 5 achievements with 1 commit"}
    :wrecking-ball
       {:name "Wrecking Ball"
        :description "Change more than 100 files in one commit"}
    :alzheimers
       {:name "Alzheimer's"
        :description "Commit time is 1 month or more after the author time"}
    :unpretending
       {:name "Unpretending"
        :description "Zero achievments after 100 your own commits"}
    :good-boy
       {:name "Good Boy"
        :description "Create 'test' or 'doc' directory (not in the first commit)"}
    :gitignore
       {:name "Gitignore"
        :description "Add .gitignore"}
    :nothing-to-hide
       {:name "Nothing to Hide"
        :description "Commit id_rsa file"}
    :quest-complete
       {:name "Quest Complete"
        :description "Get all achievements"}
    :haskell
       {:name "Ivory Tower"
        :description "Add Haskell file to a repo"}
    :perl
       {:name "Chmod 200"
        :description "Add Perl file to a repo"}
    :ruby
       {:name "Back on the Rails"
        :description "Add Ruby file to a repo"}
    :clojure
       {:name "Even Lispers Hate Lisp"
        :description "Add Clojure file to a repo"}
    :clojurescript
       {:name "Even Lispers Hate Lisp (in a browser)"
        :description "Add ClojureScript file to a repo"}
    :javascript
       {:name "Happily Never After"
        :description "Add JS file to a repo"}
    :python
       {:name "Why not Ruby?"
        :description "Add Python file to a repo"}
    :java
       {:name "Write Once. Run. Anywhere"
        :description "Add Java file to a repo"}
    :cxx
       {:name "Troubles++14"
        :description "Add C++ file to a repo"}
    :c-sharp
       {:name "It's Dangerous to Go Alone, Take LINQ"
        :description "Add C# file to a repo"}
    :objective-c
       {:name "NSVeryDescriptiveAchievementNameWithParame..."
        :description "Add Objective-C file to a repo"}
    :swift
       {:name "I Need to Sort Complex Objects Fast!"
        :description "Add Swift file to a repo"}
    :sql
       {:name "Not a Web Scale"
        :description "Add SQL file to a repo"}
    :erlang
       {:name "It’s like ObjC, but for Ericsson phones"
        :description "Add Erlang file to a repo"}
    :shell
       {:name "We’ll Rewrite that Later"
        :description "Add Bash file to a repo"}
    :php
       {:name "New Facebook is Born"
        :description "Add PHP file to a repo"}
    :pascal
       {:name "Really?"
        :description "Add Pascal file to a repo"}
    :scala
       {:name "Well Typed, Bro"
        :description "Add Scala file to a repo"}
    :xml
       {:name "Zed’s Dead, Baby"
        :description "Add XML file to a repo"}
    :css
       {:name "You Designer Now?"
        :description "Add CSS file to a repo"}
    :dart
       {:name "You Work in Google?"
        :description "Add Dart file to a repo"}
    :windows-language
       {:name "You Can't Program on Windows, Can You?"
        :description "Add Windows Shell file to a repo"}
    :basic
       {:name "Cradle of Civilization"
        :description "Add Basic file to a repo"}
    :programmers-day
       {:name "Professional Pride"
        :description "Commit on Programmers' Day"}
    :christmas
       {:name "Ruined Christmas"
        :description "Commit on Dec 25"}
    :halloween
       {:name "This Code Looks Scary"
        :description "Commit on Oct 31"}
    :new-year
       {:name "New Year, New Bugs"
        :description "Commit on Jan 1"}
    :anniversary
       {:name "Anniversary"
        :description "Commit on the project’s birthday"}
    :valentine
       {:name "In Love with Work"
        :description "Commit on Feb 14"}
    :leap-day
       {:name "Rare Occasion"
        :description "Commit on Feb 29"}
    :russia-day
       {:name "From Russia with Love"
        :description "Commit on Russia Day"}
    :thanksgiving
       {:name "Turkey Code"
        :description "Commit on Thanksgiving"}
    :fools-day
       {:name "Fools’ Code"
        :description "Commit on Apr 1"}
    :impossible
       {:name "Mission Impossible"
        :description "Use word “impossible” in a commit message"}
    :magic
       {:name "The Colour of Magic"
        :description "Use word “magic” in a commit message"}
    :sorry
       {:name "Salvation"
        :description "Use word “sorry” in a commit message"}
    :google
       {:name "I Can Sort It out Myself"
        :description "Use word “google” in a commit message"}
    :forgot
       {:name "Second Thoughts"
        :description "Use word “forgot” in a commit message"}
    :fix
       {:name "Save the Day"
        :description "Use word “fix” in a commit message"}
    :secure
       {:name "We’re Safe Now"
        :description "Use word “secure” in a commit message"}
    :catchphrase
       {:name "Catchphrase"
        :description "Make 10+ commits with the same message"}
    :bad-motherfucker
       {:name "Bad Motherf*cker"
        :description "Swear in a commit message"
        :level-description "One level for each swear word in a message"}
    :hello-linus
       {:name "Hello, Linus"
        :description "10+ swear words in a commit message"
        :level-description "One level for each 10 swear words in a message"}
    :man-of-few-words
       {:name "A Man of Few Words"
        :description "Commit message with 3 letters or less"}
    :leo-tolstoy
       {:name "Leo Tolstoy"
        :description "More than 10 lines in a commit message"}
    :citation-needed
       {:name "Citation Needed"
        :description "StackOverflow URL in a commit body or message"}
    :no-more-letters
       {:name "No More Letters"
        :description "Write a commit message without any letters"}
    :emoji
       {:name "C00l kid"
        :description "Use emoji in a commit message"}
    :beggar
       {:name "Beggar"
        :description "Ask for an achievement in a commit message"}
    :borat
       {:name "Borat"
        :description "Misspell a word in a commit message"
        :level-description "One level for each misspelled word in a message"}
    :hack
       {:name "Real Hacker"
        :description "Use word “hack” in a commit message"}
    :wow
       {:name "Wow"
        :description "Use word “wow” in a commit message"}
    :never-probably
       {:name "Never, Probably"
        :description "Use word “later” in a commit message"}})
