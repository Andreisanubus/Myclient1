package dev.myclient.util;

import org.lwjgl.glfw.GLFW;

public final class Keys {
    private Keys() {}

    public static String name(int key) {
        if (key < 0) return "None";
        String n = GLFW.glfwGetKeyName(key, 0);
        if (n != null && !n.isEmpty()) return n.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LShift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RShift";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCtrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCtrl";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LAlt";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RAlt";
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_UP -> "Up";
            case GLFW.GLFW_KEY_DOWN -> "Down";
            case GLFW.GLFW_KEY_LEFT -> "Left";
            case GLFW.GLFW_KEY_RIGHT -> "Right";
            default -> key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F12 ? "F" + (key - GLFW.GLFW_KEY_F1 + 1) : "K" + key;
        };
    }

    public static void setClipboard(String s) { GLFW.glfwSetClipboardString(GLFW.glfwGetCurrentContext(), s); }

    public static String getClipboard() {
        String s = GLFW.glfwGetClipboardString(GLFW.glfwGetCurrentContext());
        return s == null ? "" : s;
    }
}
