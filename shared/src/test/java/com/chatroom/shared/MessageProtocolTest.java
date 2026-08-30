package com.chatroom.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageProtocolTest {

    @Test
    void testIsBroadcast() {
        assertTrue(MessageProtocol.isBroadcast("@EE@|Hello World"));
        assertTrue(MessageProtocol.isBroadcast("@EE@|User: Hello"));
        assertFalse(MessageProtocol.isBroadcast("@User: Hello"));
        assertFalse(MessageProtocol.isBroadcast("!User1, User2"));
        assertFalse(MessageProtocol.isBroadcast(null));
    }

    @Test
    void testIsUserList() {
        assertTrue(MessageProtocol.isUserList("![User1, User2]"));
        assertTrue(MessageProtocol.isUserList("![]"));
        assertFalse(MessageProtocol.isUserList("@EE@|Hello"));
        assertFalse(MessageProtocol.isUserList("@User: Hello"));
        assertFalse(MessageProtocol.isUserList(null));
    }

    @Test
    void testIsPrivate() {
        assertTrue(MessageProtocol.isPrivate("@User: Hello"));
        assertTrue(MessageProtocol.isPrivate("@Target: message"));
        assertFalse(MessageProtocol.isPrivate("@EE@|Hello"));
        assertFalse(MessageProtocol.isPrivate("!User1, User2"));
        assertFalse(MessageProtocol.isPrivate(null));
    }

    @Test
    void testExtractPrivateTarget() {
        assertEquals("User", MessageProtocol.extractPrivateTarget("@User: message"));
        assertEquals("Target", MessageProtocol.extractPrivateTarget("@Target: Hello"));
        assertNull(MessageProtocol.extractPrivateTarget("@EE@|Hello"));
        assertNull(MessageProtocol.extractPrivateTarget("@: Hello"));
        assertNull(MessageProtocol.extractPrivateTarget(null));
    }

    @Test
    void testExtractBroadcastContent() {
        assertEquals("Hello World", MessageProtocol.extractBroadcastContent("@EE@|Hello World"));
        assertEquals("User: Message", MessageProtocol.extractBroadcastContent("@EE@|User: Message"));
        assertEquals("Something", MessageProtocol.extractBroadcastContent("Something"));
    }

    @Test
    void testParseUserList() {
        List<String> users = MessageProtocol.parseUserList("![User1, User2]");
        assertEquals(2, users.size());
        assertTrue(users.contains("User1"));
        assertTrue(users.contains("User2"));
    }

    @Test
    void testParseUserListSingleUser() {
        List<String> users = MessageProtocol.parseUserList("![User1]");
        assertEquals(1, users.size());
        assertEquals("User1", users.get(0));
    }

    @Test
    void testParseUserListEmpty() {
        List<String> users = MessageProtocol.parseUserList("![]");
        assertTrue(users.isEmpty());
    }

    @Test
    void testParseUserListInvalid() {
        List<String> users = MessageProtocol.parseUserList("Hello");
        assertTrue(users.isEmpty());
    }

    @Test
    void testBuildUserList() {
        String userList = MessageProtocol.buildUserList(List.of("User1", "User2"));
        assertTrue(userList.startsWith("!"));
        assertTrue(userList.contains("User1"));
        assertTrue(userList.contains("User2"));
    }

    @Test
    void testBuildBroadcast() {
        String broadcast = MessageProtocol.buildBroadcast("User", "Hello");
        assertEquals("@EE@|User: Hello", broadcast);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "hello"})
    void testIsBroadcastWithInvalidInput(String input) {
        assertFalse(MessageProtocol.isBroadcast(input));
    }
}
