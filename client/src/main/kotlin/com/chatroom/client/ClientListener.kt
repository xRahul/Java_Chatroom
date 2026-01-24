package com.chatroom.client

interface ClientListener {
    fun onMessageReceived(message: String)
    fun onUserListUpdated(users: List<String>)
    fun onConnectionError(message: String)
}
