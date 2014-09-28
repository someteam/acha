(ns acha.achievement-static)

(def table
  {:eraser
       {:name "Eraser"
        :description "Make a commit with no lines added, only deletions"}
    :massive
       {:name "Massive"
        :description "Added more than a 1000 lines in a single commit"}
    :scribbler
       {:name "Scribbler"
        :description "Created a README"}
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
        :description "Being the only commiter for a week"}
    :easy-fix
       {:name "Easy fix"
        :description "Swap two lines"}
    :multilingua
       {:name "Multilingua"
        :description "Add/edit files in 5 different languages in a single commit"}
    :necromancer
       {:name "Necromancer"
        :description "Make a commit to a repo that wasn’t touched for 1 month or more"}
    :mover
       {:name "Mover"
        :description "Move file from one place to another without changing it"}
    :world-balance
       {:name "World balance"
        :description "Number of lines added == number of lines deleted"}
    :get
       {:name "Get"
        :description "Do commit #1000, or #1111, or #1234"}
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
       {:name "Holy war"
        :description "Changed tabs to spaces or vice versa"}
    :combo
       {:name "Combo"
        :description "10+ commits in a row"}
    :combo-breaker
       {:name "Combo breaker"
        :description "Make a commit after someone had N commits in a row"}
    :worker-bee
       {:name "Worker bee"
        :description "Make 100+ non-merge commits"}
    :fat-ass
       {:name "Fat Ass"
        :description "Commit 2 Mb file or bigger"}
    :ooops
       {:name "Ooops"
        :description "Commit and revert commit within 1 minute"}
    :deal-with-it
       {:name "Deal with it"
        :description "Update master branch with force mode"}
    :dangerous-game
       {:name "Dangerous game"
        :description "Commit after 6PM friday"}
    :empty-commit
       {:name "<empty title>"
        :description "Do an empty commit"}
    :time-get
       {:name "Get"
        :description "Commit exactly at 00:00"}
    :what-happened-here
       {:name "What happened here?"
        :description "Edit a file that hasn’t been touched for a year"}
    :all-things-die
       {:name "All things die"
        :description "Delete a file that has been added in initial commit (and at least a year has passed)"}
    :for-stallman
       {:name "For Stallman!"
        :description "Add GPL license file to the repo"}
    :change-of-mind
       {:name "Change of mind"
        :description "Change license type / edit license file"}
    :munchkin
       {:name "Munchkin"
        :description "Get 5 achivements with 1 commit"}
    :wrecking-ball
       {:name "Wrecking ball"
        :description "Change more than 100 files in one commit"}
    :alzheimers
       {:name "Alzheimer's"
        :description "Commit time overdue author time for 1 month or more"}
    :unpretending
       {:name "Unpretending"
        :description "Zero achivments after 100 your own commits"}
    :good-boy
       {:name "Good boy"
        :description "Create 'test' or 'doc' directory (not on first commit)"}
    :gitignore
       {:name "Gitignore"
        :description "Add .gitignore"}
    :nothing-to-hide
       {:name "Nothing to hide"
        :description "Commit id_rsa file"}
    :quest-complete
       {:name "Quest complete"
        :description "Get all achievements"}
    :haskell
       {:name "Ivory tower"
        :description "First to commit Haskell file to a repo"}
    :perl
       {:name "Chmod 200"
        :description "First to commit Perl file to a repo"}
    :ruby
       {:name "Back on the rails"
        :description "First to commit Ruby file to a repo"}
    :clojure
       {:name "Even lispers hate Lisp"
        :description "First to commit Clojure file to a repo"}
    :clojurescript
       {:name "Even lispers hate Lisp (in a browser)"
        :description "First to commit ClojureScript file to a repo"}
    :javascript
       {:name "Happily never after"
        :description "First to commit JS file to a repo"}
    :python
       {:name "Why not Ruby?"
        :description "First to commit Python file to a repo"}
    :java
       {:name "Write once. Run. Anywhere"
        :description "First to commit Java file to a repo"}
    :cxx
       {:name "Troubles++14"
        :description "First to commit C++ file to a repo"}
    :c-sharp
       {:name "It's dangerous to go alone, take LINQ"
        :description "First to commit C# file to a repo"}
    :objective-c
       {:name "NSVeryDescriptiveAchievementNameWithParame..."
        :description "First to commit Objective-C file to a repo"}
    :swift
       {:name "I need to sort complex objects fast!"
        :description "First to commit Swift file to a repo"}
    :sql
       {:name "Not a Web Scale"
        :description "First to commit SQL file to a repo"}
    :erlang
       {:name "It’s like ObjC, but for Ericsson phones"
        :description "First to commit Erlang file to a repo"}
    :shell
       {:name "We’ll rewrite that later"
        :description "First to commit Bash file to a repo"}
    :php
       {:name "New Facebook is born"
        :description "First to commit PHP file to a repo"}
    :pascal
       {:name "Really?"
        :description "First to commit Pascal file to a repo"}
    :scala
       {:name "Well typed, bro"
        :description "First to commit Scala file to a repo"}
    :xml
       {:name "Zed’s dead, baby"
        :description "First to commit XML file to a repo"}
    :css
       {:name "You designer now?"
        :description "First to commit CSS file to a repo"}
    :dart
       {:name "You work in Google?"
        :description "First to commit Dart file to a repo"}
    :windows-language
       {:name "You can't program on Windows, can you?"
        :description "First to commit Windows Shell file to a repo"}
    :basic
       {:name "Cradle of civilization"
        :description "First to commit Basic file to a repo"}
    :programmers-day
       {:name "Professional pride"
        :description "Commit at Programmer’s day"}
    :christmas
       {:name "Ruined christmas"
        :description "Commit at Dec 25"}
    :halloween
       {:name "This code looks scary"
        :description "Commit at Oct 31"}
    :new-year
       {:name "New year, new bugs"
        :description "Commit at Jan 1"}
    :anniversary
       {:name "Anniversary"
        :description "Commit at project’s birthday"}
    :valentine
       {:name "In love with work"
        :description "Commit at Feb 14"}
    :leap-day
       {:name "Rare occasion"
        :description "Commit at Feb 29"}
    :russia-day
       {:name "From Russia with love"
        :description "Commit at Russia Day"}
    :thanksgiving
       {:name "Turkey code"
        :description "Commit at Thanksgiving"}
    :fools-day
       {:name "Fool’s code"
        :description "Commit at Apr 1"}
    :impossible
       {:name "Mission impossible"
        :description "Use word “impossible” in a commit message"}
    :magic
       {:name "The Colour of Magic"
        :description "Use word “magic” in a commit message"}
    :sorry
       {:name "Salvation"
        :description "Use word “sorry” in a commit message"}
    :google
       {:name "I can sort it out myself"
        :description "Use word “google” in a commit message"}
    :forgot
       {:name "Second thoughts"
        :description "Use word “forgot” in a commit message"}
    :fix
       {:name "Save the day"
        :description "Use word “fix” in a commit message"}
    :secure
       {:name "We’re safe now"
        :description "Use word “secure” in a commit message"}
    :catchphrase
       {:name "Catchphrase"
        :description "Make 10+ commits with the same message"}
    :bad-motherfucker
       {:name "Bad motherfucker"
        :description "Swear in a commit message"
        :level-description "One level for each swear word in a message"}
    :hello-linus
       {:name "Hello, Linus"
        :description "10+ swear words in a commit message"
        :level-description "One level for each 10 swear words in a message"}
    :man-of-few-words
       {:name "A man of few words"
        :description "Commit message with 3 letters or less"}
    :leo-tolstoy
       {:name "Leo Tolstoy"
        :description "More than 10 lines in a commit message"}
    :citation-needed
       {:name "Citation needed"
        :description "StackOverflow URL in a commit body or message"}
    :no-more-letters
       {:name "No more letters"
        :description "Write commit message without any letters"}
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
       {:name "Never, probably"
        :description "Use word “later” in a commit message"}})
