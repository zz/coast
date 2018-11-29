# Schema

Relational databases have always been kind of a pain to work with from an application development standpoint.
On the one hand, it's nice that we have relations and it saves space or something, on the other, I really just
want to query my database in a way that makes sense to me. I don't want to have to think about joins
and grouping things, just do it and preferably do it declaratively. That's where `coast.schema` comes in.

## The goal

To save you from typing out SQL DDT syntax by hand and hopefully offering you a performant and a higher level
way of thinking of data storage other than slogging through joins and thinking about indices. [Here's a good overview
of a few advantages of letting Coast handle your schema for you.](https://github.com/mozilla/mentat#data-storage-is-hard)

## What is it

There's two sides to every database driven web application. The first side is the database schema. The second side
is about reading and writing from the database. An advantage of a full stack framework is that you can start abstracting
things that only make sense when you control how schema migrations are done. Let's get more concrete.

## Migrations

Existing SQL migrations in Coast still work, which is great, know SQL? good, you can migrate your database and do whatever you
want with your database schema. So this is an option for people who don't care what the schema looks like necessarily, or people
who want to let the framework take care of that and get some benefits from it. Here's what this new schema migration looks like.

From your repl:

```clojure
(coast/gen "migration")
```

Change your db schema like this

```clojure
(def todos-users [{:db/col :person/name :db/type "text" :db/unique? true :db/nil? false}
                  {:db/rel :person/todos :db/type :many :db/ref :todo/person}
                  {:db/rel :todo/person :db/type :one :db/ref :person/id}
                  {:db/col :todo/name :db/type "text"}
                  {:db/col :todo/done :db/type "boolean" :db/default false :db/nil? false}])

(coast/migrate)
```

You can run this over and over, lighthouse keeps track in a schema_migrations table in your db

If you change the name of the def like todos-users -> todos-users-1
then this will *not* be treated like a new migration
if you change the contents of the def, then this will attempt to run a
new migration. If you're unsure of what's about to happen, run this instead it will output the sql and do nothing to the database

```clojure
(coast/migrate {:dry-run? true})
```

Made a mistake with a column name? No problem, rename columns like this

```clojure
(def rename-done [{:db/id :todo/done :db/col :todo/finished}])

(coast/migrate)
```

If you're not using sqlite, you can alter columns quite a bit more with the same syntax,
reference the existing column with `:db/id` and then give it the properties you want to change

```clojure
(def done->done-at [{:db/id :todo/done :db/col :todo/done-at :db/type "timestamptz" :db/nil? false}])

(coast/migrate)
```

## Tables

All tables are created from a vector of maps, so this

```clojure
[{:db/col :person/nick :db/type "text" :db/unique? true :db/nil? false :db/default "''"}
 {:db/col :todo/name :db/type "text"}]
```

turns into this:

```sql
create table if not exists person (
  id integer primary key,
  updated_at timestamp,
  created_at timestamp not null default current_timestamp
)

create table if not exists todo (
  id integer primary key,
  updated_at timestamp,
  created_at timestamp not null default current_timestamp
)
```

The namespaces of the `:db/col` value in this case `:person/nick` -> `person` and `:todo/name` -> `todo` are the table names. This happens for the distinct namespaces of every key in every migration.

## Columns

Columns are the names of the `:db/col` values in the migration maps, so this:

```clojure
[{:db/col :person/nick :db/type "text" :db/unique? true :db/nil? false :db/default "''"}
 {:db/col :todo/name :db/type "text"}]
```

Becomes this:

```sql
alter table person add column nick text not null default '';
create unique index idx_person_nick on table person (nick);

alter table todo add column name text;
```

SQLite has several restrictions on what alter table can and can't do, namely, altering a table to add a column must have a default value when specifying not null, this will fail if you don't specify something. This can work on postgres under certain conditions.

## Relationships (Foreign Keys)

Foreign keys are a little special and require two, yes that's right, two maps to function as keys in pull queries. For example:

```clojure
[{:db/rel :person/todos :db/type :many :ref :todo/person}
 {:db/rel :todo/person :db/type :one :ref :person/id}]
```

Turns into one alter table statement with a references clause:

```sql
alter table todo add column person integer references person(id) on cascade delete;
```

You can control the `on cascade` behavior with the optional `:db/delete` key.

You're essentially building a join clause and a way to list child rows ahead of time. The way it works is this:

```clojure
[{:db/rel :person/todos :db/type :many :db/ref :todo/person}
 {:db/rel :todo/person :db/type :one :db/ref :person/id}]
```

The `:db/rel` key for `:db/type :many` (the first line) is just name, it doesn't have to be `:person/todos`, it could be anything that you want to reference in a pull query

```clojure
(pull [{:person/todos [:todo/id]}]
      [:person/id 1])
```

So if you changed it to `:people/todo-items` or something, that is perfectly fine, the pull query would change to look like this:

```clojure
(pull [{:people/todo-items [:todo/id]}]
      [:person/id 1])
```

and it would return results like this:

```clojure
{:people/todo-items [{:todo/id 1} {:todo/id 2}]}
```

The last key in the first line `:db/ref` is a "pointer" to the next line:

```clojure
{:db/rel :todo/person :db/type :one :db/ref :person/id}
```

This line is more "convention over configuration" (i.e. hardcoded), so the `:db/rel` key here is not just a name, it and the `:db/ref` key are used to build the join statement in pull requests, here's how they come together in a join, `:todo/person` is the foreign key column name, it becomes `todo.person` in the join statement, the `:db/ref` value `:person/id` becomes `person.id` in the join statement, for a join that looks like this:

```sql
from person
left outer join todo on todo.person = person.id
```

There's more info about pull in the [queries](EDN-Queries.md) section, but that's pretty much everything you need to know to have a great pull experience!
