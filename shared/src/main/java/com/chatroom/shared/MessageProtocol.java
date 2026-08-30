package com.chatroom.shared;

import java.util.List;
import java.util.stream.Collectors;

public final class MessageProtocol {
    public static final String PREFIX_BROADCAST = "@EE@|";
    public static final String PREFIX_PRIVATE = "@";
    public static final String PREFIX_USER_LIST = "!";

    private MessageProtocol() {}

    public static boolean isBroadcast(String message) {
        return message != null && message.startsWith(PREFIX_BROADCAST);
    }

    public static boolean isUserList(String message) {
        return message != null && message.startsWith(PREFIX_USER_LIST);
    }

    public static boolean isPrivate(String message) {
        return message != null && message.startsWith(PREFIX_PRIVATE)
                && !message.startsWith(PREFIX_BROADCAST);
    }

    public static String extractPrivateTarget(String message) {
        if (!isPrivate(message)) {
            return null;
        }
        int colonIndex = message.indexOf(':');
        if (colonIndex > 1) {
            return message.substring(1, colonIndex);
        }
        return null;
    }

    public static String extractBroadcastContent(String message) {
        if (isBroadcast(message)) {
            return message.substring(PREFIX_BROADCAST.length());
        }
        return message;
    }

    public static String extractUserList(String message) {
        if (isUserList(message)) {
            return message.substring(PREFIX_USER_LIST.length());
        }
        return message;
    }

    public static List<String> parseUserList(String message) {
        if (!isUserList(message)) {
            return List.of();
        }
        String content = extractUserList(message);
        if (content.startsWith("[") && content.endsWith("]")) {
            content = content.substring(1, content.length() - 1);
        }
        return List.of(content.split(", ")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public static String buildUserList(List<String> users) {
        return PREFIX_USER_LIST + users.toString();
    }

    public static String buildPrivateMessage(String sender, String target, String content) {
        return PREFIX_PRIVATE + target + ": " + sender + ": " + content;
    }

    public static String buildBroadcast(String sender, String message) {
        return PREFIX_BROADCAST + sender + ": " + message;
    }
}
