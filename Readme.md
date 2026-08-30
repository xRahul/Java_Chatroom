# Java IRC-like ChatRoom

Modernized multi-module Gradle chatroom — group chat, private chat, themeable Swing client.

> Ported from Eclipse `ChatRoomFinalServer` / `ChatRoomFinalClient` (Java 7 / `Hashtable` / `Thread.stop()`) to **Gradle 8.14.5 + Java 21** with `ConcurrentHashMap`, `ExecutorService`, `ObjectInputFilter` and unit tests.

## Features

* Group broadcast to all connected clients
* Private 1:1 chat (double-click user)
* Live online user list (`!` / `!user1&user2` protocol)
* Theme chooser on client (Nimbus/Metal)
* Graceful disconnect handling

## Stack

| | |
|---|---|
| Build | Gradle 8.14.5 (wrapper), `java-library` + `application` |
| Runtime | Java 21 toolchain (Temurin) |
| Test | JUnit 5.14.4, Mockito 5.23.0 |
| CI | `ci.yml` — `./gradlew build` on push/PR |
| Release | `release.yml` — auto GitHub Release on push to `master`/`main` |

## Project layout

```
shared/  -> com.chatroom.shared.MessageProtocol  (wire format: @EE@| broadcast, @user: private, ! user list)
server/  -> ChatServer (ConcurrentHashMap + bounded ExecutorService 100), ClientHandler (Runnable, filtered ObjectInputStream), ServerGUI/ServerDisplay, StartingPointServer :5555
client/  -> ChatClient, MessageListener (Runnable), ClientGUI, StartingPointClient
gradle/wrapper/  gradle-wrapper 317-tha
.github/workflows/ci.yml, release.yml
```

## Prerequisites

* JDK 21 (`java -version` → 21)
* No local Gradle needed — wrapper handles it

## Build & test

```bash
./gradlew build --no-daemon        # compile + test + jar
./gradlew test --no-daemon         # tests only (21 tests)
./gradlew :shared:test --tests "*MessageProtocolTest*"
```

## Run

```bash
# server (port 5555)
./gradlew :server:run --no-daemon

# client (connects to localhost:5555)
./gradlew :client:run --no-daemon
# → enter username → chat; double-click online user for private

# or via distributions
./gradlew :server:installDist :client:installDist --no-daemon
./server/build/install/server/bin/server
./client/build/install/client/bin/client
```

Multi-client: run `:client:run` in separate terminals.

## Protocol (shared/MessageProtocol)

* `PREFIX_BROADCAST = "@EE@|"`  broadcast
* `PREFIX_PRIVATE  = "@"`        private `@user:message`
* `PREFIX_USER_LIST = "!"`       `!alice&bob`

Helpers: `isBroadcast`, `isPrivate`, `extractPrivateTarget`, `parseUserList`, `buildBroadcast/UserList`.

Security: both sides use `ObjectInputFilter` (`String` only, max array 4096, max depth 16, reject others) + username validation `^[A-Za-z0-9_-]{1,32}$`.

## CI / Release

* **CI** (`ci.yml`): JDK 21 + `gradle/actions/setup-gradle@v3`, cache `gradle`, `./gradlew build --no-daemon` on push/PR to `master`/`main`.
* **Release** (`release.yml`): on push to `master`/`main` or `workflow_dispatch` — builds jars, runs tests, `installDist`, packages `server/build/libs/*.jar` + `client/build/libs/*.jar` into `dist/`, publishes via `softprops/action-gh-release@v2` with tag `auto-${run_number}` and `generate_release_notes: true`. Needs default `GITHUB_TOKEN` (`contents: write`).

## Notes

* Server bind is `0.0.0.0:5555` (override via `new ChatServer(port, display)`).
* No persistence — chat history is in-memory, cleared on restart.
* Previously Eclipse-only; now headless-buildable and tested.
