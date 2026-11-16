# Ports

| Внешний порт | Внутренний порт | Назначение                                      |
|--------------|-----------------|-------------------------------------------------|
| 19090        |                 | Keycloak                                        |
| 19000        |                 | UI -> hangelo-api                                |
| 19010        |                 | UI -> hangelo-admin                              |
| 29000        |                 | Rest Server hangelo                              |
| 29010        |                 | Rest Server hangelo-admin                        |
| 20000        |                 | PostgreSQL: hangelo, hangelo_test                |
| 20001        | 5672  api       | RabbitMQ                                        |
| 20002        | 15672 admin     | RabbitMQ                                        |
| 20003        | 61613 stomp     | RabbitMQ                                        |
| 20004        | 6379            | Redis                                           |
| 20005        |                 | Rest Server emulator                            |
| 20006        |                 | UI -> emulator                                  |


# Strategy: the project base

A reusable playbook. Each item is a strategy and the reason it exists.

---

## 1. One entry point: the Makefile is the menu

**Strategy.** Everything a developer or a pipeline does is one `make <target>`. The Makefile holds no
logic: a target is one line that calls Gradle or npm. Every target carries a comment saying what it
does and why it is separate.

**Why.** Nobody has to remember command sequences; the pipeline and the developer run the same thing.

| Family | Targets | What it gives |
|--------|---------|---------------|
| Whole stack on the host | `start`, `stop` | One command up, one command down |
| Infra only | `dockerUp`, `up`, `recreate`, `purge` | Infra + migrations + demo data without servers; rebuild from zero; full wipe |
| Database | `upgradeDbAll`, `data`, `jooq` | Migrate local and test DB, load demo data, regenerate typed DB classes |
| Checks | `compile`, `test`, `test-tz`, `ui-test-tz` | Compile everything, unit tests, unit tests in several time zones |
| End-to-end | `e2e`, `e2e-run` | With stack start, and against a running stack |
| Images | `image-<app>-server`, `image-<app>-ui`, `i`, `i2` | One image, all production images, production plus dev-only tooling |
| Release | `vinc`, `toMavenLocal` | Version bump, artifact publishing |

Rules:

- **Dependencies between targets express prerequisites** (`test: dockerUp upgradeDbAll`), so a target
  is always runnable from a cold machine.
- **A "run against what is already up" twin** for every expensive target (`e2e` starts the stack,
  `e2e-run` does not). Otherwise composite targets start the stack twice.
- **Multi-step loops never stop at the first failure** but still fail the target
  (`rc=0; … || rc=1; exit $rc`): one failure must not hide the others.
- **`--rerun` on tests, not `--rerun-tasks`**: DB state is not a Gradle input, so tests would be
  UP-TO-DATE; `--rerun` reruns only the test tasks and keeps compilation incremental.
- **Aggregates split by audience**: `i` is what ships, `i2` adds what never ships.

## 2. Ports and data: one table, one directory

- **A contiguous port block for the project, and a table of it at the top of the instructions**
  ("Ports"): external port, internal port, purpose. Every compose file uses these numbers; a freed
  port is marked free, not silently reused.
- **All persistent data is a bind directory under `${APP_DATA_ROOT}/<name>`**, never a named docker
  volume. Data is visible, copyable, measurable; `make recreate` wipes exactly that directory and the
  stack truly starts from zero. A named volume survives the wipe and the "recreated" stack comes up
  with old data.
- **Ownership is fixed by an init service inside compose** (root `mkdir` + `chown`, consumer waits
  with `service_completed_successfully`): `compose up` is called from many places.
- **Wiping is done from a container as root** (`docker run --rm --user root -v "$ROOT:/target"
  alpine rm -rf …`): database files belong to the container user, no sudo needed.
- **Files that must exist before `compose up` are built before it**: otherwise docker creates a
  directory in place of the missing file.

## 3. Several stacks, one system

| Stack | From what | For what |
|-------|-----------|----------|
| dev (`make start`) | Gradle run tasks + dev servers of the clients | Daily work, hot reload |
| dev-prod (`make dev-prod-up`) | The built images | Checking exactly what ships |

Both use the **same infra, same ports, same end-to-end tests without a single edit** — that is the
point of the check.

## 4. Images

- **A base image built rarely and separately** (runtime + diagnostic utilities + non-root user uid
  1000), with its own hand-raised `version.txt` and its own push target.
- **One Dockerfile for all server applications**; they differ by three values (module, port, image
  name).
- **Layered boot-jar**: extract in a build stage, one `COPY` per layer from rarely to frequently
  changing. A code change ships about a megabyte.
- **The jar name is computed, never searched**: `build/libs` keeps old versions, and "first found"
  ships a stale one.
- **Build context is the jar directory**, not the repository root.
- JVM flags: `MaxRAMPercentage` instead of `-Xmx`, `ExitOnOutOfMemoryError`.
- **Image tag is `<branch>-<version>-<who>`**, the same for servers and clients. Branch prevents
  feature builds overwriting master's; `who` prevents developers and CI overwriting each other.

## 5. Database

- **Migrations are append-only**: any change is a new numbered file; applied files are never edited.
- **One command migrates local and test DBs** (`upgradeDbAll`); demo data is a separate idempotent
  step that does not overwrite what a developer changed.
- **Typed DB access generated from the schema** (`make jooq`).
- Conventions decided once: singular table names, reference columns `<table>_id` and
  `<role>__<table>_id`, moments as `timestamptz`, polymorphic references as a composite type with an
  enum kind.

## 6. Testing strategies

- **Naming and shape**: `methodName__doesSomething`; the call under test fenced by `//` `//` comment
  pairs, no assertions inside the fence.
- **Tests must pass in any time zone**: backend in two JVM zones, clients in two machine zones, with
  explicit per-zone expected values, not values recomputed the same way as the code.
- **End-to-end tests are a separate project** (`ui/itest`, Playwright) covering the whole chain
  through real UI and real broker.
  - A test prepares its own data in its own body and depends on nothing in the database.
  - Through the UI — what our user does; through an API — what the other system does.
  - Elements are found by `data-tst-id` only.
  - Cleanup is done by the harness, also after failure, and deletes nothing physically.
  - A dedicated test user, part of the stand.
- **An emulator application stands in for every external system**: no security, never deployed to
  production, own DB schema created at startup, hand-controlled (no timers, no randomness), with a
  test CRUD API. It must match the document given to the external developers — if they differ, the
  emulator is fixed.
- **Guard tests for mistakes already made once** (a spec that fails when a forbidden pattern
  reappears), named in the rule that they protect.

## 7. Documentation and working with an AI agent

- **`CLAUDE.md` is the rulebook**: each rule states its scope ("applies to all modules"), shows right
  and wrong code, and ends with **why** — usually the incident that produced it. Rules without a
  reason get broken.
- **Every piece of work is a numbered folder** `doc/plans/NNN-<short-name>/` with
  `01-prompt-….txt`, `02-PLAN-….md`, `03-REPORT-….md`. The short name is reused in commit prefixes.
- **A document per external system, written for its developers**, changed in the same change as the
  code.
- Markdown tables never contain `|` inside a cell.
- Things the human does are named explicitly: commit, push.

## 8. Git

- **Never push without commits**: `git push` runs only when there are new local commits to send.
  No pushing an empty branch, no pushing "just in case".
- **No co-author tag in commit messages**: never add `Co-Authored-By:` (or any other AI attribution
  line) to commits or pull request descriptions.
- **The remote is GitHub over HTTPS** (`origin` = the private repository `meet_again`).
  Authentication is the token in the environment variable `MEET_AGAIN_KEY`, read at run time by the
  repository's credential helper. The token is never written into a file, a URL or a command line.

## 9. Releases

- **Every deploy is a card in `RELEASES.md` of the web repository (`meet_again_ui`)**: version,
  when it was applied (UTC), the deployed commit of each repository, and the batch of changes in the
  words of a person who uses the product. Applies to a deploy of the server alone too. The format,
  the steps and the examples of right and wrong are in that repository's `CLAUDE.md`
  ("Releases: a card for every deploy").
- **When a re-deploy is asked for: the card first, then the deploy**, and the web application is
  deployed every time, because its welcome page shows the newest card.
- **Why one list, in one repository**: the product is one thing to the people who use it. Two lists
  would each be half of the answer to "what changed and when", and the welcome page can read only
  the one that is built into it.
