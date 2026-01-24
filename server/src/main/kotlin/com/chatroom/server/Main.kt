package com.chatroom.server

fun main() {
    val gui = ServerGUI()
    val server = ChatServer(5555, gui)
    server.start()
}
