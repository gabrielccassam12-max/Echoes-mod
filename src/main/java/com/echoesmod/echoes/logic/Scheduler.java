package com.echoesmod.echoes.logic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Tiny server-side delayed task queue (used for barrages, delayed explosions, etc). */
public final class Scheduler {
    private record Task(long at, Runnable run) {}

    private static final List<Task> TASKS = new ArrayList<>();
    private static long now = 0;

    private Scheduler() {}

    public static void later(int delay, Runnable r) {
        TASKS.add(new Task(now + Math.max(0, delay), r));
    }

    public static void clear() { TASKS.clear(); }

    public static void tick() {
        now++;
        if (TASKS.isEmpty()) return;
        List<Task> due = new ArrayList<>();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.at() <= now) { due.add(t); it.remove(); }
        }
        for (Task t : due) {
            try { t.run().run(); } catch (Exception ex) { ex.printStackTrace(); }
        }
    }
}
