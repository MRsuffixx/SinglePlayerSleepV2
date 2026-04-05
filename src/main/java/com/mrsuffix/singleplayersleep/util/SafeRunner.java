package com.mrsuffix.singleplayersleep.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SafeRunner {
    
    private SafeRunner() {
    }
    
    public static void runSync(JavaPlugin plugin, Runnable task) {
        if (plugin == null || task == null) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }
    
    public static void runLater(JavaPlugin plugin, Runnable task, long delay) {
        if (plugin == null || task == null) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, task, delay);
    }
}
