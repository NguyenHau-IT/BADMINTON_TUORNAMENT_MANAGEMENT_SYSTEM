package com.example.btms.util.monitor;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;
import com.example.btms.util.log.Log;

/**
 * Thread Pool Monitor để log thông tin thread pool và system threads
 */
public class ThreadPoolMonitor {

    private final Log logger;
    private ScheduledExecutorService monitorService;
    private boolean monitoring = false;

    public ThreadPoolMonitor(Log logger) {
        this.logger = logger;
    }

    /**
     * Bắt đầu monitoring thread pool với interval định trước
     */
    public void startMonitoring(int intervalSeconds) {
        if (monitoring) {
            return;
        }

        monitorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ThreadPoolMonitor");
            t.setDaemon(true);
            return t;
        });

        monitorService.scheduleAtFixedRate(
                this::logThreadInfo,
                0,
                intervalSeconds,
                TimeUnit.SECONDS);

        monitoring = true;
        logger.logTs("🔄 Thread Pool Monitoring started (interval: %d seconds)", intervalSeconds);
    }

    /**
     * Dừng monitoring
     */
    public void stopMonitoring() {
        if (!monitoring || monitorService == null) {
            return;
        }

        monitorService.shutdown();
        try {
            if (!monitorService.awaitTermination(2, TimeUnit.SECONDS)) {
                monitorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            monitorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        monitoring = false;
        logger.logTs("⏹️ Thread Pool Monitoring stopped");
    }

    /**
     * Log thông tin thread hiện tại (cơ bản)
     */
    public void logThreadInfo() {
        try {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

            // Basic thread info
            int activeThreads = threadBean.getThreadCount();
            int peakThreads = threadBean.getPeakThreadCount();
            int daemonThreads = threadBean.getDaemonThreadCount();

            logger.log("═══════════════════════════════════════════════════════════════");
            logger.log("🧵 [THREAD-MONITOR] Active: %d | Peak: %d | Daemon: %d | NonDaemon: %d",
                    activeThreads, peakThreads, daemonThreads, activeThreads - daemonThreads);

            // Memory info
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory() / (1024 * 1024);
            long freeMemory = runtime.freeMemory() / (1024 * 1024);
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory() / (1024 * 1024);
            double memoryPercent = (double) usedMemory / maxMemory * 100;

            logger.log("💾 [MEMORY-MONITOR] Used: %d MB | Free: %d MB | Total: %d MB | Max: %d MB (%.1f%%)",
                    usedMemory, freeMemory, totalMemory, maxMemory, memoryPercent);

            // GC info if available
            try {
                java.lang.management.GarbageCollectorMXBean[] gcBeans = ManagementFactory.getGarbageCollectorMXBeans()
                        .toArray(new java.lang.management.GarbageCollectorMXBean[0]);

                for (java.lang.management.GarbageCollectorMXBean gcBean : gcBeans) {
                    if (gcBean.getCollectionCount() > 0) {
                        logger.log("🗑️ [GC-MONITOR] %s: Collections=%d | Time=%dms",
                                gcBean.getName(), gcBean.getCollectionCount(), gcBean.getCollectionTime());
                    }
                }
            } catch (Exception gcEx) {
                // GC info not available, skip
            }

            logger.log("═══════════════════════════════════════════════════════════════");

        } catch (Exception e) {
            logger.log("❌ [THREAD-MONITOR] Error getting thread info: %s", e.getMessage());
        }
    }

    /**
     * Log thông tin thread chi tiết với nhóm
     */
    public void logDetailedThreadInfo() {
        try {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            long[] threadIds = threadBean.getAllThreadIds();

            logger.log("");
            logger.log("╔════════════════════════════════════════════════════════════════╗");
            logger.log("║                   DETAILED THREAD ANALYSIS                     ║");
            logger.log("╚════════════════════════════════════════════════════════════════╝");
            logger.log("📊 [THREAD-DETAIL] Total threads: %d", threadIds.length);

            // Group threads by name pattern
            Map<String, Integer> threadGroups = new HashMap<>();
            Map<String, Long> threadCpuTime = new HashMap<>();

            for (long threadId : threadIds) {
                try {
                    java.lang.management.ThreadInfo info = threadBean.getThreadInfo(threadId);
                    if (info != null) {
                        String name = info.getThreadName();
                        String group = getThreadGroup(name);
                        threadGroups.merge(group, 1, Integer::sum);

                        // Get thread state
                        Thread.State state = info.getThreadState();
                    }
                } catch (Exception ignore) {
                    // Thread might have been terminated
                }
            }

            // Log grouped results (sorted by count DESC)
            logger.log("");
            logger.log("📈 Thread Groups (by count):");
            logger.log("┌────────────────────────────────────────────────────┐");

            threadGroups.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .forEach(entry -> {
                        String group = entry.getKey();
                        int count = entry.getValue();
                        int barLength = Math.max(1, count / 2);
                        String bar = "█".repeat(barLength);
                        logger.log("│ %-20s: %3d threads %s", group, count, bar);
                    });

            logger.log("└────────────────────────────────────────────────────┘");

            // Log thread state distribution
            logThreadStateDistribution(threadBean, threadIds);

            logger.log("");

        } catch (Exception e) {
            logger.log("❌ [THREAD-DETAIL] Error: %s", e.getMessage());
        }
    }

    /**
     * Log thread state distribution (RUNNABLE, WAITING, TIMED_WAITING, etc)
     */
    private void logThreadStateDistribution(ThreadMXBean threadBean, long[] threadIds) {
        Map<Thread.State, Integer> stateCount = new HashMap<>();

        for (long threadId : threadIds) {
            try {
                java.lang.management.ThreadInfo info = threadBean.getThreadInfo(threadId);
                if (info != null) {
                    Thread.State state = info.getThreadState();
                    stateCount.merge(state, 1, Integer::sum);
                }
            } catch (Exception ignore) {
            }
        }

        if (!stateCount.isEmpty()) {
            logger.log("");
            logger.log("⚙️ Thread States:");
            logger.log("┌────────────────────────────────────────────────────┐");

            stateCount.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .forEach(entry -> {
                        Thread.State state = entry.getKey();
                        int count = entry.getValue();
                        String icon = getStateIcon(state);
                        logger.log("│ %s %-15s: %3d threads", icon, state.toString(), count);
                    });

            logger.log("└────────────────────────────────────────────────────┘");
        }
    }

    /**
     * Log full thread list dengan state
     */
    public void logFullThreadList() {
        try {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            long[] threadIds = threadBean.getAllThreadIds();

            logger.log("");
            logger.log("╔════════════════════════════════════════════════════════════════╗");
            logger.log("║                      FULL THREAD LIST                          ║");
            logger.log("╚════════════════════════════════════════════════════════════════╝");

            java.util.List<java.lang.management.ThreadInfo> threadInfos = new java.util.ArrayList<>();
            for (long threadId : threadIds) {
                try {
                    java.lang.management.ThreadInfo info = threadBean.getThreadInfo(threadId);
                    if (info != null) {
                        threadInfos.add(info);
                    }
                } catch (Exception ignore) {
                }
            }

            // Sort by name
            threadInfos.sort((a, b) -> a.getThreadName().compareTo(b.getThreadName()));

            logger.log("┌─────┬────────────────────────────────────────┬──────────────────┐");
            logger.log("│ ID  │ Thread Name                            │ State            │");
            logger.log("├─────┼────────────────────────────────────────┼──────────────────┤");

            for (java.lang.management.ThreadInfo info : threadInfos) {
                String name = info.getThreadName();
                if (name.length() > 38) {
                    name = name.substring(0, 35) + "...";
                }
                String icon = getStateIcon(info.getThreadState());
                logger.log("│ %3d │ %-38s │ %s %-10s │",
                        info.getThreadId(),
                        name,
                        icon,
                        info.getThreadState().toString());
            }

            logger.log("└─────┴────────────────────────────────────────┴──────────────────┘");
            logger.log("");

        } catch (Exception e) {
            logger.log("❌ [FULL-LIST] Error: %s", e.getMessage());
        }
    }

    /**
     * Log chi tiết về một ThreadPoolExecutor cụ thể
     */
    public void logThreadPoolDetails(String name, ThreadPoolExecutor executor) {
        if (executor == null) {
            logger.log("❌ [THREAD-POOL] %s: null executor", name);
            return;
        }

        try {
            int active = executor.getActiveCount();
            int core = executor.getCorePoolSize();
            int max = executor.getMaximumPoolSize();
            int current = executor.getPoolSize();
            long completed = executor.getCompletedTaskCount();
            long total = executor.getTaskCount();
            int queued = executor.getQueue().size();

            logger.log("🏊 [THREAD-POOL] %s: Active=%d/%d | Pool=%d/%d | Completed=%d/%d | Queued=%d",
                    name, active, core, current, max, completed, total, queued);

            // Check if pool is busy
            if (active >= core * 0.8) {
                logger.log("⚠️ [THREAD-POOL] %s: High utilization (%.1f%%)", name, (double) active / core * 100);
            }

            if (queued > 10) {
                logger.log("⚠️ [THREAD-POOL] %s: High queue size: %d", name, queued);
            }

        } catch (Exception e) {
            logger.log("❌ [THREAD-POOL] Error monitoring %s: %s", name, e.getMessage());
        }
    }

    private String getThreadGroup(String threadName) {
        if (threadName == null)
            return "Unknown";

        if (threadName.startsWith("BTMS-Enhanced"))
            return "Virtual Threads";
        if (threadName.startsWith("BTMS-IO-Intensive"))
            return "I/O Pool";
        if (threadName.startsWith("BTMS-CPU-Intensive"))
            return "CPU Pool";
        if (threadName.startsWith("BTMS-Scheduled"))
            return "Scheduled";
        if (threadName.startsWith("Court-"))
            return "Court Serial";
        if (threadName.startsWith("pool-"))
            return "ThreadPool";
        if (threadName.startsWith("Timer-"))
            return "Timer";
        if (threadName.startsWith("AWT-"))
            return "AWT/Swing";
        if (threadName.startsWith("ForkJoinPool"))
            return "ForkJoinPool";
        if (threadName.startsWith("H2-"))
            return "H2 Database";
        if (threadName.startsWith("LogViewer-"))
            return "LogViewer";
        if (threadName.startsWith("ThreadPoolMonitor"))
            return "Monitoring";
        if (threadName.startsWith("sound-"))
            return "Sound";
        if (threadName.startsWith("SSE-"))
            return "SSE";
        if (threadName.equals("main"))
            return "Main";
        if (threadName.startsWith("Finalizer"))
            return "GC";
        if (threadName.startsWith("Reference Handler"))
            return "GC";
        if (threadName.startsWith("Signal Dispatcher"))
            return "System";

        return "Other";
    }

    private String getStateIcon(Thread.State state) {
        if (state == null)
            return "❓";
        switch (state) {
            case RUNNABLE:
                return "🟢"; // Running
            case WAITING:
                return "🟡"; // Waiting
            case TIMED_WAITING:
                return "🟠"; // Timed wait
            case BLOCKED:
                return "🔴"; // Blocked
            case NEW:
                return "⚪"; // New
            case TERMINATED:
                return "⚫"; // Terminated
            default:
                return "❓";
        }
    }

    public boolean isMonitoring() {
        return monitoring;
    }

    /**
     * Cleanup resources
     */
    public void shutdown() {
        stopMonitoring();
        logger.log("🔚 [THREAD-MONITOR] Monitor shutdown completed");
    }
}