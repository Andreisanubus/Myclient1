package dev.myclient.config;

import java.util.ArrayList;
import java.util.List;

public class WaypointManager {
    public record Waypoint(String name, double x, double y, double z, String dim) {}
    public static final List<Waypoint> list = new ArrayList<>();
}
