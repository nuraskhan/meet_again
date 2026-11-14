# PLAN 001: MVP backend

The decisions the implementation follows, and why. The request is in `01-prompt-mvp-backend.txt`.

## Scope

In: registration and login, profile, friends, events with invitations, a group chat per event.
REST only.

Out, on purpose: front end, Keycloak, RabbitMQ / Redis / live delivery of chat messages, the admin
application, the emulator, images, CI. The ports for them stay reserved in `CLAUDE.md`.

## Stack

Java 25, Gradle 9, Spring Boot 4.1, PostgreSQL 17. Writes go through JPA (entities and Spring Data
repositories), reads through jOOQ, schema through Flyway migrations.

## Modules

| Module | What it is |
|--------|------------|
| `lib/model` | JPA entities, enums, repositories, `RND` |
| `lib/jooq` | jOOQ classes generated from the schema; committed |
| `lib/web` | Error format and "who is calling" for every REST module |
| `migration` | Numbered SQL files and the program that applies them |
| `applications/magain/magain-mod/auth` | Registration, login, tokens, security rules |
| `applications/magain/magain-mod/profile` | Own profile, another person's profile, search |
| `applications/magain/magain-mod/friend` | Friend requests and the friend list |
| `applications/magain/magain-mod/chat` | Chats, members, messages |
| `applications/magain/magain-mod/event` | Events, invitations, event types |
| `applications/magain/magain-launcher` | The server: assembles the modules, demo data, API tests |

Inside a feature module: `XxxController` (thin), `XxxService` (changes, JPA, one transaction per
call), `XxxQuery` (reads, jOOQ), `dto` (records).

## Decisions

1. **JPA entities have no associations**, only id columns. Joins are needed for reading, and reading
   is jOOQ. It removes lazy-loading surprises and lets the entities be plain classes with public
   fields.
2. **`RND`** is one class in `lib/model`: random values plus a factory per entity. An entity from it
   satisfies every constraint of its table and has no id; references are arguments. A test guards
   that the real schema accepts every one of them.
3. **Enum columns are `text` with a check constraint.** JPA maps them with `EnumType.STRING`; jOOQ
   reads them as the same Java enums through forced types in `lib/jooq/jooq-codegen.xml`.
4. **Moments are `timestamptz` in the database and `Instant` in Java**, in the entities and in jOOQ
   alike. In JSON they are UTC with `Z`.
5. **Nothing is deleted.** Ending a friendship, declining, leaving a chat, cancelling an event set a
   status or a moment.
6. **Auth is a signed token (JWT, HS256) issued by our own `auth` module**, checked by the standard
   resource-server support of Spring Security. The subject of the token is the user id. Keycloak
   issues the same kind of token, so moving to it later replaces the `auth` module and one decoder
   bean; the other modules only see `Principal`.
7. **"Pre-filled form" = defaults of the event type.** `EventType` carries a title, a distance range
   and a price range. `GET /event/type/list` gives them to the form; a field left out of
   `POST /event/create` takes the default of the type.
8. **Only friends can be invited.** Creating an event creates, in one transaction, the event, an
   invitation per invitee and a group chat with the creator and all invitees. Declining takes the
   invitee out of the chat.
9. **"Not yours" is answered as "not found"** (404), so ids cannot be probed.
10. **Configuration comes from `MAGAIN_*` environment variables with no defaults in
    `application.yaml`.** Local values are in the launcher's `build.gradle`, so `make start` needs
    nothing exported, and a server deployed without a secret does not start.
11. **Tests are API tests over real HTTP against `magain_test`.** Each creates its own users with
    random e-mails and depends on nothing in the database. They run in two time zones
    (`make test-tz`).

## Order of work

1. Build skeleton, compose file for PostgreSQL, migration `V001`.
2. `lib/model` with `RND`; generate `lib/jooq`.
3. `lib/web`, then the modules: auth, profile, friend, chat, event.
4. Launcher, demo data, Makefile.
5. API tests; run them in both time zones; start the server and call it by hand.
