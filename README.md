## What is this?

Coast is a full stack web framework written in Clojure for small teams or solo developers. It uses a relational database and renders html on the server without javascript which allows you to ship your web applications faster

```clojure
(ns my-project
  (:require [coast]))

(def routes [[:get "/" :home]])

(defn home [req]
  [:h1 "You're coasting on clojure!"])

(def app (coast/app {:routes routes}))

(coast/server app {:port 1337})
```

## Getting Started

### Installation on Mac

1. Make sure clojure is installed first

```bash
brew install clojure
```

2. Install the coast cli script

```bash
curl -o /usr/local/bin/coast https://raw.githubusercontent.com/coast-framework/coast/master/coast && chmod a+x /usr/local/bin/coast
```

3. Create a new coast project

```bash
coast new myapp && cd myapp
```

### Installation on Linux (Debian/Ubuntu)

1. Make sure you have bash, curl, rlwrap, and Java installed

```bash
curl -O https://download.clojure.org/install/linux-install-1.9.0.391.sh
chmod +x linux-install-1.9.0.391.sh
sudo ./linux-install-1.9.0.391.sh
```

2. Install the coast cli script

```bash
sudo curl -o /usr/local/bin/coast https://raw.githubusercontent.com/coast-framework/coast/master/coast && sudo chmod a+x /usr/local/bin/coast
```

3. Create a new coast project

```bash
coast new myapp && cd myapp
```

You should be greeted with the text "You're coasting on clojure!"
when you visit `http://localhost:1337`

## Quickstart

This doc will take you from a fresh coast installation to a working todo list.

### New Project

The first thing you do when you start a coast project? `coast new` in your terminal:

```bash
coast new todos
```

This will make a new folder in the current directory named "todos". Let's get in there and see what's up:

```bash
cd todos
tree .
```

This will show you the layout of a default coast project:

```bash
.
├── Makefile
├── README.md
├── bin
│   └── repl.clj
├── deps.edn
├── resources
│   ├── assets.edn
│   ├── migrations.edn
│   └── public
│       ├── css
│       │   └── app.css
│       └── js
│           └── app.js
├── src
│   ├── components.clj
│   ├── home.clj
│   ├── routes.clj
│   └── server.clj
└── test
    └── server_test.clj

7 directories, 13 files
```

### Databases

For the sake of this tutorial, we want to show a list of todos as the first thing people see when they come to our site. In coast, that means making a place for these todos to live, in this case (and in every case): the database. Coast by default uses sqlite, so making a database is as easy as

```bash
touch todos_dev.db
```

### Migrations

Now that the database is created, let's generate a migration, first get connected to your nREPL client of choice, I use atom with proto-repl, here's how I connect. From the terminal in the todos folder:

```bash
make repl
```

Then in atom open any clj file, like server.clj then press Ctrl + Command + Y and after the dialog opens hit Enter. Now that you're in, you can either type this directly in the newly opened nREPL client tab, or in your source file, I usually just add a comment form and then type stuff in under that, like so:

```clojure
; server.clj
(comment
  (coast/gen "migration" "todo/name" "todo/completed-at"))
```

Then make sure your cursor is within the `(coast/gen ...)` form and press Command + Shift + B. That will send the form over to the running program (the nREPL server) and evaluate it and create a new migration line in `migrations.edn`.

Change `migrations.edn` to look like this

```clojure
[{:db/col :todo/name
  :db/type "text"}

 {:db/col :todo/completed-at
  :db/type "timestamp"}]
```

This is edn, not sql, although sql migrations would work, in coast it's cooler if you use edn migrations for the sweet query power you'll have later. The left side of the `/` is the name of the table, and the right side is the name of the column. Or in coast terms: `:<resource>/<prop>`  Let's apply this migration to the database:

```clojure
(comment
  (coast/migrate))
```

This updates the database schema with a `todo` table, `name` and `completed_at` columns.

### Generators

Now that the database has been migrated and we have a place to store the todos we want to show them too. This is where coast generators come in. Rather than you having to type everything out, generators are a way to get you started and you can customize from there.

This will create a file in the `src` directory with the name of an `action`. Coast is a pretty humble web framework, there's no FRP or graph query languages or anything. There are just seven actions: `build`, `create`, `edit`, `change`, `delete`, `view` and `index`.

```clojure
(comment
  (coast/gen "action" "todo"))
```

There should be a new file in `src` named `todo.clj`:

```clojure
(ns todo
  (:require [coast]))

(defn index [request]
  (let [rows (coast/q '[:pull [:todo/name :todo/completed-at]])]
    [:table
     [:thead
      [:tr
       [:th "name"]
       [:th "completed-at"]]]
     [:tbody
       (for [row rows]
        [:tr
         [:td (:todo/name row)]
         [:td (:todo/completed-at row)]])]]))

; the rest omitted for brevity
```

### Routes

One thing coast doesn't do yet is update the routes file, let's do that now:

```clojure
(ns routes)

(def routes [[:get "/"      :home/index]
             [:get "/404"   :home/not-found :404]
             [:get "/500"   :home/server-error :500]
             [:get "/todos" :todo/index]])
```

Now we can check it out in the browser, there's no styling or anything so it's not going to look amazing, but:

```clojure
; server.clj
(comment
  (-main))
```

Go to `http://localhost:1337/todos` to check out your handiwork. Looks blank, let's make a few todos from the REPL:

```clojure
; server.clj

(comment
  (coast/insert [{:todo/name "todo #1"}
                 {:todo/name "todo #2"}
                 {:todo/name "todo #3" :todo/completed-at (coast/instant)}]))
```

Now you can refresh and check out the todos.

## Read The Docs

The docs are still under construction, but there should be enough there
to get a production-ready website off the ground

[Read the docs](docs/README.md)

## Contributing

Any contribution is welcome! Submit a PR and let's get it merged in!

## License

Coast on Clojure is released under the [MIT License](https://opensource.org/licenses/MIT).
