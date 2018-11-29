# EDN Queries

__Warning: This only works if you use edn migrations, any tables created or changed with regular SQL migrations won't work with this cool stuff__

## What is it

The main advantage of putting your schema in the framework's hands is that one, you don't need to declare things twice, like writing out a rails schema and then having to say "belongs_to" or "has_many" in a model somewhere. Two, you can get away with really short query building vs SQL, which, I love-hate SQL, but I love making web apps faster more than writing a lot of SQL.

## The goal

Remove the O and the M in ORM. Data in, data out. Everything transparent and declarative from your application code.

## CRUD in Coast

Not sure if it's any better than CRUD anywhere else, but the R in there is definitely unlike anything in any other web framework. Here's how it works.

## Queries

```clojure
(ns r-in-crud
  (:require [coas :refer [q]]))

(q '[:select author/name author/email post/title post/body
     :joins  author/posts
     :where  [author/name ?author/name]]
   {:author/name "Cody Coast"})
```

The following query looks pretty basic and it is, it uses this SQL to query the database

```sql
select author.name, author.email, post.title, post.body
from author
join post on post.author = author.id
where author.name = ?
-- queries are parameterized
```

Which assuming some data, would output this in your Clojure code

```clojure
[{:author/name "Cody Coast" :author/email "cody@coastonclojure.com" :post/title "First!" :post/body "Post!"}
 {:author/name "Cody Coast" :author/email "cody@coastonclojure.com" :post/title "Second!" :post/body "Post!"}
 {:author/name "Cody Coast" :author/email "cody@coastonclojure.com" :post/title "Third!" :post/body "Post!"}]
```

This isn't bad, but you can imagine a few more joins and a few more columns and things might get out of hand.
Even if they didn't get out of hand, you want something like this anyway

```clojure
[{:author/name "Cody Coast"
  :author/email "cody@coastonclojure.com"
  :author/posts [{:post/title "First"
                  :post/body "Post!"}
                 {:post/title "Second!"
                  :post/body "Post!"}
                 {:post/title "Third!"
                  :post/body "Post!"}]}]
```

Well you're in luck, thanks to letting Coast handle your schema, you can do just that. It's called `pull` and yes
it's shamelessly stolen from datomic. This is how it looks.

```clojure
(q '[:pull [author/id
            author/email
            author/name
            {:author/posts [post/title
                            post/body]}]
     :where [author/name ?author/name]]
   {:author/name "Cody Coast"})
```

Which will output what you saw earlier. It uses the relationship names and data from the schema earlier to build the select and join parts of the query.

Here are some other examples of querying in coast

```clojure
(db/q conn '[:select todo/name todo/done
             :where [todo/done ?done]
                    [todo/name like ?name]
             :order todo/created-at desc]
           {:done true
            :name "%write%"})
; => [{:todo/name "write readme" :todo/done true}]

; or like this
(db/q conn '[:select todo/name todo/done
             :from todo
             :where [todo/done true]])
; => [{:todo/name "write readme" :todo/done true}]

; if you don't want to specify every column, you don't have to
(db/q conn '[:select todo/*
             :from todo
             :where [todo/done false]])
; => [{:todo/id 1 :todo/name ... :todo/done false :todo/created-at ...}]

; joins are supported too
(db/q conn '[:select todo/* person/*
             :from todo
             :joins person/todos])
; => [{:todo/id 1 :todo/name ... :person/id 1 :person/name "swlkr" ...}]

; you can also add a sql string in the where clause
(db/q conn '[:select todo/name todo/done
             :where [todo/done ?done]
                    ["todo.created_at > now()"]
             :order todo/created-at desc]
           {:done true})
; => [{:todo/name "write readme" :todo/done true}]
```

## The limits of pull

But wait a minute, how do you control the order they get returned in that fancy pull query? Here's how

```clojure
(q '[:pull [author/email
            author/name
            {(:author/posts :order post/id desc) [post/title
                                                  post/body]}]
     :where [author/name ?author/name]
            [author/name != nil]
     :limit 10
     :order author/id desc]
   {:author/name "Cody Coast"})
```

How do you limit how many nested rows get returned? You can do that with `:limit`

```clojure
(q '[:pull [author/email
            author/name
            {(:author/posts :order post/id desc
                            :limit 10) [post/title
                                        post/body]}]
     :where [author/name ?author/name]]
   {:author/name "Cody Coast"})
```

If you know you only want to pull nested rows from one entity, you can use the `pull` function

```clojure
(pull '[author/id
        author/email
        author/name
        {(:author/posts :order post/created-at desc
                        :limit 10) [post/title
                                    post/body]}]
      [:author/name "Cody Coast"])
```

Unfortunately, or fortunately, if you want to get *really* crazy with a pull, you can't. You'll have to drop down to SQL and manipulate things with clojure yourself. The point of pull is to handle the common case, it doesn't handle arbitrary SQL functions or crazy SQL syntax. You'll have to either call `q` for that or fall back to SQL.

## Insert

Insert data into the database like this

```clojure
; simple one row insert
(let [p (db/insert conn {:person/name "swlkr"})]

  ; insert multiple rows like this
  ; p is auto-resolved to (get p :person/id)
  (db/insert conn [{:todo/name "write readme"
                    :todo/person p
                    :todo/done true}
                   {:todo/name "write tests 😅"
                    :todo/person p
                    :todo/done false}]})

; or just manually set the foreign key integer value
(db/insert conn [{:todo/name "write readme"
                  :todo/person 1
                  :todo/done true}
                 {:todo/name "write tests 😅"
                  :todo/person 1
                  :todo/done false}]}))
```

## Update

Update data like this

```clojure
(db/update conn {:todo/id 2 :todo/name "write tests 😅😅😅"})

; you can either perform an update after a select
(let [todos (db/q conn '[:select todo/id
                         :where [todo/done false]])] ; => [{:todo/id 2} {:todo/id 3}]
  (db/update conn (map #(assoc % :todo/done true) todos)))

; or update from transact
(db/transact conn '[:update todo
                    :set [todo/done true]
                    :where [todo/id [2 3]]])
```

## Delete

Delete data like this

```clojure
(db/delete conn {:todo/id 1})

; or multiple rows like this
(db/delete conn [{:todo/id 1} {:todo/id 2} {:todo/id 3}])

; there's always transact too
(db/transact conn '[:delete
                    :from todo
                    :where [todo/id [1 2 3]]]) ; this implicitly does an in query
