package dev.myclient.config;

import java.util.ArrayList;
import java.util.List;

public class StaffManager {
    public static final List<String> staff = new ArrayList<>();

    public static boolean isStaff(String name) {
        for (String s : staff) if (s.equalsIgnoreCase(name)) return true;
        return false;
    }

    public static void add(String name) {
        name = name.trim();
        if (!name.isEmpty() && !isStaff(name)) staff.add(name);
    }
}
