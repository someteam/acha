(ns acha.achievement-static)

(def table
  {:eraser
       {:name "Eraser"
        :description "make a commit with no lines added, only deletions"
        :picture "..."}
    :massive
       {:name "Massive"
        :description "added more than a 1000 lines in a single commit"
        :picture "..."}
    :scribbler
       {:name "Scribbler"
        :description "created a README"
        :picture "..."}
    :owl
       {:name "Owl"
        :description "commit at 4am local time"
        :picture "..."}
    :flash
       {:name "Flash"
        :description "two different commits within 1 minute"
        :picture "..."}
    :waste
       {:name "Waste"
        :description "Your commit was reverted completely by someone else"
        :picture "..."}
    :loneliness
       {:name "Loneliness"
        :description "Being the only commiter for a week"
        :picture "..."}
    :easy-fix
       {:name "Easy fix"
        :description "swap two lines"
        :picture "..."}
    :multilingua
       {:name "Multilingua"
        :description "edit files in 5 different languages in a single commit"
        :picture "..."}
    :necromancer
       {:name "Necromancer"
        :description "make a commit to a repo that wasn’t touched for 1 month or more"
        :picture "..."}
    :mover
       {:name "Mover"
        :description "move file from one place to another without changing it"
        :picture "..."}
    :world-balance
       {:name "World balance"
        :description "Number of lines added == number of lines deleted"
        :picture "..."}
    :get
       {:name "Get"
        :description "do commit # 1000, or 1111, or 1234 (counting from beginning, chronologically sorted)"
        :picture "..."}
    :narcissist
       {:name "Narcissist"
        :description "use your own name in a commit message"
        :picture "..."}
    :blamer
       {:name "Blamer"
        :description "use someone else’s name in a commit message"
        :picture "..."}
    :collision
       {:name "Collision"
        :description "publish the first commit with the same N first chars of SHA-1 as existing commit (Collision lvl 7 is pretty rare)"
        :picture "..."}
    :lucky
       {:name "Lucky"
        :description "Consecutive 777 in SHA-1"
        :picture "..."}
    :mark-of-the-beast
       {:name "Mark of the Beast"
        :description "Consecutive 666 in SHA-1"
        :picture "..."}
    :hydra
       {:name "Hydra"
        :description "Make a commit with 3+ parents"
        :picture "..."}
    :commenter
       {:name "Commenter"
        :description "only add a comment"
        :picture "..."}
    :peacemaker
       {:name "Peacemaker"
        :description "resolve 100 conflicts"
        :picture "..."}
    :ocd
       {:name "OCD"
        :description "commit with trailing spaces removed"
        :picture "..."}
    :holy-war
       {:name "Holy war"
        :description "changed tabs to spaces or vice versa"
        :picture "..."}
    :combo
       {:name "Combo"
        :description "N commits in a row, N > 10"
        :picture "..."}
    :combo-breaker
       {:name "Combo breaker"
        :description "make a commit after someone has N commits in a row"
        :picture "..."}
    :worker-bee
       {:name "Worker bee"
        :description "make 100 non-merge commits"
        :picture "..."}
    :fat-ass
       {:name "Fat Ass"
        :description "commit 2 Mb file or bigger"
        :picture "..."}
    :ooops
       {:name "Ooops"
        :description "commit and revert commit within 1 minute"
        :picture "..."}
    :deal-with-it
       {:name "Deal with it"
        :description "update master branch with force mode"
        :picture "..."}
    :dangerous-game
       {:name "Dangerous game"
        :description "commit after 6PM friday"
        :picture "..."}
    :empty-commit
       {:name ""
        :description "do an empty commit"
        :picture "..."}
    :time-get
       {:name "Get"
        :description "commit exactly at 00:00"
        :picture "..."}
    :what-happened-here
       {:name "What happened here?"
        :description "edit a file that hasn’t been touched for a year"
        :picture "..."}
    :all-things-die
       {:name "All things die"
        :description "delete a file that has been added in initial commit (and at least a year has passed)"
        :picture "..."}
    :for-stallman
       {:name "For Stallman!"
        :description "add GPL license file to the repo"
        :picture "..."}
    :change-of-mind
       {:name "Change of mind"
        :description "change license type / edit license file"
        :picture "..."}
    :munchkin
       {:name "Munchkin"
        :description "Get 5 achivements with 1 commit"
        :picture "..."}
    :wrecking-ball
       {:name "Wrecking ball"
        :description "Change more than 100 files in one commit"
        :picture "..."}
    :alzheimers
       {:name "Alzheimer's"
        :description "Commit time overdue author time for 1 month or more"
        :picture "..."}
    :unpretending
       {:name "Unpretending"
        :description "zero achivments after 100 your own commits"
        :picture "..."}
    :good-boy
       {:name "Good boy"
        :description "Create 'test[s]' or 'doc[s]' directory (not on first commit)"
        :picture "..."}
    :gitignore
       {:name "Gitignore"
        :description ".gitignore"
        :picture "..."}
    :nothing-to-hide
       {:name "Nothing to hide"
        :description "commit id_rsa"
        :picture "..."}
    :quest-complete
       {:name "Quest complete"
        :description "get all achievements"
        :picture "..."}
    :ivory-tower
       {:name "Ivory tower"
        :description "Being first to commit Haskell file to a repo"
        :picture "..."}
    :chmod-200
       {:name "Chmod 200"
        :description "same for Perl"
        :picture "..."}
    :back-on-the-rails
       {:name "Back on the rails"
        :description "same for Ruby"
        :picture "..."}
    :even-lispers-hate-lisp
       {:name "Even lispers hate Lisp"
        :description "Clojure"
        :picture "..."}
    :even-lispers-hate-lisp-in-a-browser
       {:name "Even lispers hate Lisp (in a browser)"
        :description "ClojureScript"
        :picture "..."}
    :happily-never-after
       {:name "Happily never after"
        :description "JavaScript"
        :picture "..."}
    :why-not-ruby
       {:name "Why not Ruby?"
        :description "Python"
        :picture "..."}
    :write-once-run-anywhere
       {:name "Write once. Run. Anywhere"
        :description "Java"
        :picture "..."}
    :troubles-14
       {:name "Troubles++14"
        :description "C++"
        :picture "..."}
    :its-dangerous-to-go-alone-take-linq
       {:name "It's dangerous to go alone, take LINQ"
        :description "C#"
        :picture "..."}
    :nsverydescriptiveachievementnamewithparame
       {:name "NSVeryDescriptiveAchievementNameWithParame..."
        :description "Objective C"
        :picture "..."}
    :i-need-to-sort-complex-objects-fast
       {:name "I need to sort complex objects fast!"
        :description "Swift"
        :picture "..."}
    :not-a-web-scale
       {:name "Not a Web Scale"
        :description "SQL"
        :picture "..."}
    :its-like-objc-but-for-ericsson-phones
       {:name "It’s like ObjC, but for Ericsson phones"
        :description "Erlang"
        :picture "..."}
    :well-rewrite-that-later
       {:name "We’ll rewrite that later"
        :description "Bash"
        :picture "..."}
    :new-facebook-is-born
       {:name "New Facebook is born"
        :description "PHP"
        :picture "..."}
    :really
       {:name "Really?"
        :description "Pascal"
        :picture "..."}
    :well-typed-bro
       {:name "Well typed, bro"
        :description "Scala"
        :picture "..."}
    :zeds-dead-baby
       {:name "Zed’s dead, baby"
        :description "XML"
        :picture "..."}
    :you-designer-now
       {:name "You designer now?"
        :description "CSS"
        :picture "..."}
    :you-work-in-google
       {:name "You work in Google?"
        :description "Dart"
        :picture "..."}
    :you-cant-program-on-windows-can-you
       {:name "You can't program on Windows, can you?"
        :description "Windows shell"
        :picture "..."}
    :cradle-of-civilization
       {:name "Cradle of civilization"
        :description "Basic"
        :picture "..."}
    :professional-pride
       {:name "Professional pride"
        :description "make a commit at Programmer’s day"
        :picture "..."}
    :ruined-christmas
       {:name "Ruined christmas"
        :description "commit something at Dec 25"
        :picture "..."}
    :this-code-looks-scary
       {:name "This code looks scary"
        :description "Halloween"
        :picture "..."}
    :new-year-new-bugs
       {:name "New year, new bugs"
        :description "New Year"
        :picture "..."}
    :anniversary
       {:name "Anniversary"
        :description "Commit at project’s birthday"
        :picture "..."}
    :in-love-with-work
       {:name "In love with work"
        :description "Feb 14"
        :picture "..."}
    :rare-occasion
       {:name "Rare occasion"
        :description "Feb 29"
        :picture "..."}
    :from-russia-with-love
       {:name "From Russia with love"
        :description "Russia Day"
        :picture "..."}
    :turkey-code
       {:name "Turkey code"
        :description "Thanksgiving day"
        :picture "..."}
    :mission-impossible
       {:name "Mission impossible"
        :description "use word “impossible” in a message"
        :picture "..."}
    :the-colour-of-magic
       {:name "The Colour of Magic"
        :description "use word “magic” in a message"
        :picture "..."}
    :salvation
       {:name "Salvation"
        :description "use word “sorry” in a message"
        :picture "..."}
    :i-can-sort-it-out-myself
       {:name "I can sort it out myself"
        :description "use word “google” in a message"
        :picture "..."}
    :second-thoughts
       {:name "Second thoughts"
        :description "forget something (use word “forgot”)"
        :picture "..."}
    :save-the-day
       {:name "Save the day"
        :description "use word “fix”"
        :picture "..."}
    :were-safe-now
       {:name "We’re safe now"
        :description "use word “secure”"
        :picture "..."}
    :catchphrase
       {:name "Catchphrase"
        :description "10 commits with the same message"
        :picture "..."}
    :bad-motherfucker
       {:name "Bad motherfucker"
        :description "swear in a commit message"
        :picture "..."}
    :hello-linus
       {:name "Hello, Linus"
        :description "10+ swear words in a message"
        :picture "..."}
    :a-man-of-few-words
       {:name "A man of few words"
        :description "commit with 3-letter message or less"
        :picture "..."}
    :leo-tolstoy
       {:name "Leo Tolstoy"
        :description "more than 10 lines in a commit message"
        :picture "..."}
    :citation-needed
       {:name "Citation needed"
        :description "StackOverflow url in commit body or message"
        :picture "..."}
    :no-more-letters
       {:name "No more letters"
        :description "commit message without letters"
        :picture "..."}
    :c00l-kid
       {:name "C00l kid"
        :description "use emoji in commit message"
        :picture "..."}
    :beggar
       {:name "Beggar"
        :description "ask for an achievment in commit message"
        :picture "..."}
    :borat
       {:name "Borat"
        :description "Misspell word"
        :picture "..."}
    :real-hacker
       {:name "Real Hacker"
        :description "use word “hack”"
        :picture "..."}
    :wow
       {:name "Wow"
        :description "use word “wow”"
        :picture "..."}
    :never-probably
       {:name "Never, probably"
        :description "use word “later”"
        :picture "..."}})
