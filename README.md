# ChatRoom

Modernized Java/Kotlin Chat Application.

## Features
* Group Chat
* Private Chat
* Theme Changer Client GUI
* Modern Kotlin Implementation
* Gradle Build System

## Prerequisites
* Java 21

## How to run

### Server
```bash
./gradlew :server:run
```
The server will start on port 5555.

### Client
```bash
./gradlew :client:run
```
Enter your username in the dialog to connect.

## Development

### Build
```bash
./gradlew build
```

### Test
```bash
./gradlew test
```

## Architecture
The project is a Gradle multi-module project:
* `client`: Kotlin Swing Client
* `server`: Kotlin Server

Code has been refactored to separate Logic from UI.
