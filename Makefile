# The menu of the project: everything a developer or a pipeline does is one `make <target>`.
# No logic here - a target only calls Gradle or docker. Ports are in the table at the top of CLAUDE.md.

# The recipes are single commands, so they work in both shells; only the Gradle wrapper differs.
ifeq ($(OS),Windows_NT)
SHELL := cmd.exe
GRADLE := .\gradlew.bat
else
GRADLE := ./gradlew
endif

# All data of the local stack lives in this one directory (a bind mount, never a docker volume).
export APP_DATA_ROOT ?= $(CURDIR)/.data

COMPOSE := docker compose -f docker/local/docker-compose.yaml
LAUNCHER := :applications:hangelo:hangelo-launcher

.PHONY: start stop dockerUp up recreate purge upgradeDbAll data jooq compile test test-tz

# ---------------------------------------------------------------- whole stack on the host

# Infra, migrations, demo data, then the server in the foreground (http://localhost:29000).
# Ctrl+C stops the server; the infra stays up for the next start.
start: up
	$(GRADLE) $(LAUNCHER):bootRun

# Stops the infra. The data stays.
stop:
	$(COMPOSE) stop

# ---------------------------------------------------------------- infra only

# Starts the containers and returns when they are ready to accept connections.
dockerUp:
	$(COMPOSE) up -d --wait

# Infra + migrations + demo data, without the server: enough for tests and for `make jooq`.
up: dockerUp upgradeDbAll data

# The local stack from zero: all data is lost.
recreate: purge up

# Removes the containers and ALL local data. Separate from `recreate` to wipe without starting.
# The wipe runs in a container as root: the database files belong to the container's user.
purge:
	$(COMPOSE) down
	docker run --rm --user root -v "$(APP_DATA_ROOT):/target" alpine:3.22 sh -c "rm -rf /target/*"

# ---------------------------------------------------------------- database

# Applies the migrations to the local database and to the test database.
upgradeDbAll: dockerUp
	$(GRADLE) :migration:upgradeDbAll

# Loads demo data into the local database. Does nothing if it is already there.
data: upgradeDbAll
	$(GRADLE) $(LAUNCHER):populateLocalDb

# Regenerates the jOOQ classes (lib/jooq) from the local database. Run after adding a migration,
# commit the result.
jooq: upgradeDbAll
	$(GRADLE) :lib:jooq:jooqCodegen

# ---------------------------------------------------------------- checks

# Compiles everything, tests included, without running them and without a database.
compile:
	$(GRADLE) classes testClasses

# All tests. They use the test database, so the infra and the migrations come first.
# --continue: a failure in one module does not hide the failures in the others.
test: dockerUp upgradeDbAll
	$(GRADLE) test --continue

# The same tests in two distant time zones (the list is in the root build.gradle).
# Separate from `test` because it takes twice as long.
test-tz: dockerUp upgradeDbAll
	$(GRADLE) testTz --continue
