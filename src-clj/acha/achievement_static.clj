(ns acha.achievement-static)

(def ^:private -table [
 {:description
  "Delete a file that has been added in the initial commit (and at least a year has passed)",
  :key :all-things-die,
  :name "All Things Die"}
 {:description "Commit time is 1 month or more after the author time",
  :key :alzheimers,
  :name "Alzheimer's"}
 {:description "Commit on the project’s birthday",
  :key :anniversary,
  :name "Anniversary"}
 {:description "Swear in a commit message",
  :key :bad-motherfucker,
  :name "Bad Motherf*cker",
  :level-description "One level for each swear word in a message"}
 {:description "Add Basic file to the repo",
  :key :basic,
  :name "Cradle of Civilization"}
 {:description "Ask for an achievement in a commit message",
  :key :beggar,
  :name "Beggar"}
 {:description "Use someone else’s name in a commit message",
  :key :blamer,
  :name "Blamer"}
 {:description "Misspell a word in a commit message",
  :key :borat,
  :name "Borat",
  :level-description "One level for each misspelled word in a message"}
 {:description "Add C# file to the repo",
  :key :c-sharp,
  :name "It's Dangerous to Go Alone, Take LINQ"}
 {:description "Make 10+ commits with the same message",
  :key :catchphrase,
  :name "Catchphrase"}
 {:description "Change license type or edit license file",
  :key :change-of-mind,
  :name "Change of Mind"}
 {:description "Commit on Dec 25",
  :key :christmas,
  :name "Ruined Christmas"}
 {:description "StackOverflow URL in a commit body or message",
  :key :citation-needed,
  :name "Citation Needed"}
 {:description "Add Clojure file to the repo",
  :key :clojure,
  :name "Even Lispers Hate Lisp"}
 {:description "Add ClojureScript file to the repo",
  :key :clojurescript,
  :name "Even Lispers Hate Lisp (in a browser)"}
 {:description
  "Publish commit with the same N first chars of SHA-1 as existing commit",
  :key :collision,
  :name "Collision"}
 {:description "10+ commits in a row", :key :combo, :name "Combo"}
 {:description "Make a commit after someone had N commits in a row",
  :key :combo-breaker,
  :name "Combo Breaker"}
 {:description "Only add a comment",
  :key :commenter,
  :name "Commenter"}
 {:description "Add CSS file to the repo",
  :key :css,
  :name "You Designer Now?"}
 {:description "Add C++ file to the repo",
  :key :cxx,
  :name "Troubles++14"}
 {:description "Commit after 6PM friday",
  :key :dangerous-game,
  :name "Dangerous Game"}
 {:description "Add Dart file to the repo",
  :key :dart,
  :name "You Work in Google?"}
 {:description "Update master branch with force mode",
  :key :deal-with-it,
  :name "Deal with it"}
 {:description "Swap two lines", :key :easy-fix, :name "Easy Fix"}
 {:description "Use emoji in a commit message",
  :key :emoji,
  :name "C00l kid"}
 {:description "Make an empty commit",
  :key :empty-commit,
  :name "<empty title>"}
 {:description "Make a commit with no lines added, only deletions",
  :key :eraser,
  :name "Eraser"}
 {:description "Add Erlang file to the repo",
  :key :erlang,
  :name "It’s like ObjC, but for Ericsson phones"}
 {:description "Commit 2Mb file or bigger",
  :key :fat-ass,
  :name "Fat Ass"}
 {:description "Use word “fix” in a commit message",
  :key :fix,
  :name "Save the Day"}
 {:description "Two different commits within 1 minute",
  :key :flash,
  :name "Flash"}
 {:description "Commit on Apr 1", :key :fools-day, :name "Fools’ Code"}
 {:description "Add GPL license file to the repo",
  :key :for-stallman,
  :name "For Stallman!"}
 {:description "Use word “forgot” in a commit message",
  :key :forgot,
  :name "Second Thoughts"}
 {:description "Make commit #1000, or #1111, or #1234",
  :key :get,
  :name "Get"}
 {:description "Add .gitignore", :key :gitignore, :name "Gitignore"}
 {:description "Add Go file to the repo",
  :key :go,
  :name "In Google we trust"}
 {:description
  "Create 'test' or 'doc' directory (not in the first commit)",
  :key :good-boy,
  :name "Good Boy"}
 {:description "Use word “google” in a commit message",
  :key :google,
  :name "I Can Sort It out Myself"}
 {:description "Use word “hack” in a commit message",
  :key :hack,
  :name "Real Hacker"}
 {:description "Commit on Oct 31",
  :key :halloween,
  :name "This Code Looks Scary"}
 {:description "Add Haskell file to the repo",
  :key :haskell,
  :name "Ivory Tower"}
 {:description "5+ swear words in a commit message",
  :key :hello-linus,
  :name "Hello, Linus",
  :level-description "One level for each 5 swear words in a message"}
 {:description "Change tabs to spaces or vice versa",
  :key :holy-war,
  :name "Holy War"}
 {:description "Make a commit with 3+ parents",
  :key :hydra,
  :name "Hydra"}
 {:description "Use word “impossible” in a commit message",
  :key :impossible,
  :name "Mission Impossible"}
 {:description "Add Java file to the repo",
  :key :java,
  :name "Write Once. Run. Anywhere"}
 {:description "Add JS file to the repo",
  :key :javascript,
  :name "Happily Never After"}
 {:description "Commit on Feb 29",
  :key :leap-day,
  :name "Rare Occasion"}
 {:description "More than 10 lines in a commit message",
  :key :leo-tolstoy,
  :name "Leo Tolstoy"}
 {:description "You are the only committer for a month",
  :key :loneliness,
  :name "Loneliness"}
 {:description "Consecutive 777 in SHA-1", :key :lucky, :name "Lucky"}
 {:description "Use word “magic” in a commit message",
  :key :magic,
  :name "The Colour of Magic"}
 {:description "Commit message with 3 letters or less",
  :key :man-of-few-words,
  :name "A Man of Few Words"}
 {:description "Consecutive 666 in SHA-1",
  :key :mark-of-the-beast,
  :name "Mark of the Beast"}
 {:description "Add more than 1000 lines in a single commit",
  :key :massive,
  :name "Massive"}
 {:description
  "Move a file from one place to another without changing it",
  :key :mover,
  :name "Mover"}
 {:description
  "Add/edit files in 3+ different languages in a single commit",
  :key :multilingual,
  :name "Multilingual"}
 {:description "Get 5 achievements with 1 commit",
  :key :munchkin,
  :name "Munchkin"}
 {:description "Use your own name in a commit message",
  :key :narcissist,
  :name "Narcissist"}
 {:description
  "Make a commit to a repo that hasn’t been touched for 1 month or more",
  :key :necromancer,
  :name "Necromancer"}
 {:description "Use word “later” in a commit message",
  :key :never-probably,
  :name "Never, Probably"}
 {:description "Commit on Jan 1",
  :key :new-year,
  :name "New Year, New Bugs"}
 {:description "Write a commit message without any letters",
  :key :no-more-letters,
  :name "No More Letters"}
 {:description "Commit id_rsa file",
  :key :nothing-to-hide,
  :name "Nothing to Hide"}
 {:description "Add Objective-C file to the repo",
  :key :objective-c,
  :name "NSVeryDescriptiveAchievementNameWithParame..."}
 {:description "Commit with just trailing spaces removed",
  :key :ocd,
  :name "OCD"}
 {:description "Commit and revert commit within 1 minute",
  :key :ooops,
  :name "Ooops"}
 {:description "Commit between 4am and 7am local time",
  :key :owl,
  :name "Owl"}
 {:description "Add Pascal file to the repo",
  :key :pascal,
  :name "Really?"}
 {:description "Resolve 100 conflicts",
  :key :peacemaker,
  :name "Peacemaker"}
 {:description "Add Perl file to the repo",
  :key :perl,
  :name "Chmod 200"}
 {:description "Add PHP file to the repo",
  :key :php,
  :name "New Facebook is Born"}
 {:description "Commit on Programmers' Day",
  :key :programmers-day,
  :name "Professional Pride"}
 {:description "Add Python file to the repo",
  :key :python,
  :name "Why not Ruby?"}
 {:description "Get all achievements",
  :key :quest-complete,
  :name "Quest Complete"}
 {:description "Add Ruby file to the repo",
  :key :ruby,
  :name "Back on the Rails"}
 {:description "Commit on Russia Day",
  :key :russia-day,
  :name "From Russia with Love"}
 {:description "Add Scala file to the repo",
  :key :scala,
  :name "Well Typed, Bro"}
 {:description "Create a README", :key :scribbler, :name "Scribbler"}
 {:description "Use word “secure” in a commit message",
  :key :secure,
  :name "We’re Safe Now"}
 {:description "Add Bash file to the repo",
  :key :shell,
  :name "We’ll Rewrite that Later"}
 {:description "Use word “sorry” in a commit message",
  :key :sorry,
  :name "Salvation"}
 {:description "Add SQL file to the repo",
  :key :sql,
  :name "Not a Web Scale"}
 {:description "Add Swift file to the repo",
  :key :swift,
  :name "I Need to Sort Complex Objects Fast!"}
 {:description "Commit on Thanksgiving",
  :key :thanksgiving,
  :name "Turkey Code"}
 {:description "Commit exactly at 00:00", :key :time-get, :name "Get"}
 {:description "Zero achievments after 100 your own commits",
  :key :unpretending,
  :name "Unpretending"}
 {:description "Commit on Feb 14",
  :key :valentine,
  :name "In Love with Work"}
 {:description "Your commit was reverted completely by someone else",
  :key :waste,
  :name "Waste"}
 {:description "Edit a file that hasn’t been touched for a year",
  :key :what-happened-here,
  :name "What Happened Here?"}
 {:description "Add Windows Shell file to the repo",
  :key :windows-language,
  :name "You Can't Program on Windows, Can You?"}
 {:description "Make 100+ non-merge commits",
  :key :worker-bee,
  :name "Worker Bee"}
 {:description "Number of lines added == number of lines deleted",
  :key :world-balance,
  :name "World Balance"}
 {:description "Use word “wow” in a commit message",
  :key :wow,
  :name "Wow"}
 {:description "Change more than 100 files in one commit",
  :key :wrecking-ball,
  :name "Wrecking Ball"}
 {:description "Add XML file to the repo",
  :key :xml,
  :name "Zed’s Dead, Baby"}
])

(def table (map (fn [a i] (assoc a :id (+ i 100))) -table (range)))
(def table-map (into {} (map #(vector (:key %) %) table)))
