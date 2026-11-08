package dev.myclient.config;

import java.util.ArrayList;
import java.util.List;

public class FriendManager {
    public static final List<String> friends = new ArrayList<>();

    public static boolean isFriend(String name) {
        for (String f : friends) if (f.equalsIgnoreCase(name)) return true;
        return false;
    }

    public static void add(String name) {
        name = name.trim();
        if (!name.isEmpty() && !isFriend(name)) friends.add(name);
    }
}
