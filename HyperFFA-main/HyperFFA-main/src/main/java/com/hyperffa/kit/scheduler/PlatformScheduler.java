package com.hyperffa.kit.scheduler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

public final class PlatformScheduler {

    private static final boolean IS_FOLIA;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
            folia = false;
        }
        IS_FOLIA = folia;
    }

    private PlatformScheduler() {
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    @SuppressFBWarnings({"RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT"})
    public static TaskHandle runAsyncTimer(JavaPlugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (IS_FOLIA) {
            long delayMs = Math.max(1, delayTicks * 50L);
            long periodMs = Math.max(1, periodTicks * 50L);
            io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask =
                    Bukkit.getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), delayMs, periodMs, TimeUnit.MILLISECONDS);
            return foliaTask::cancel;
        } else {
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
            return bukkitTask::cancel;
        }
    }

    public static TaskHandle runAsync(JavaPlugin plugin, Runnable task) {
        if (IS_FOLIA) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask =
                    Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
            return foliaTask::cancel;
        } else {
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            return bukkitTask::cancel;
        }
    }

    public static TaskHandle runGlobal(JavaPlugin plugin, Runnable task) {
        if (IS_FOLIA) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask =
                    Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
            return foliaTask::cancel;
        } else {
            BukkitTask bukkitTask = Bukkit.getScheduler().runTask(plugin, task);
            return bukkitTask::cancel;
        }
    }

    public static TaskHandle runEntity(JavaPlugin plugin, Entity entity, Runnable task) {
        if (IS_FOLIA) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask =
                    entity.getScheduler().run(plugin, scheduledTask -> task.run(), null);
            return (foliaTask != null) ? foliaTask::cancel : () -> {};
        } else {
            BukkitTask bukkitTask = Bukkit.getScheduler().runTask(plugin, task);
            return bukkitTask::cancel;
        }
    }
}
