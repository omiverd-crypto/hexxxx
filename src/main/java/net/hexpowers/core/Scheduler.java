package net.hexpowers.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/** Tiny tick-based delayed-task scheduler. Tasks may schedule further tasks. */
public final class Scheduler {
    private static final class Task {
        final long due;
        final Runnable action;
        Task(long due, Runnable action) { this.due = due; this.action = action; }
    }

    private final List<Task> tasks = new ArrayList<>();
    private final Consumer<RuntimeException> onError;
    private long now;

    public Scheduler(Consumer<RuntimeException> onError) {
        this.onError = onError;
    }

    public void later(int delayTicks, Runnable action) {
        tasks.add(new Task(now + Math.max(1, delayTicks), action));
    }

    public void tick() {
        now++;
        List<Task> due = null;
        for (Iterator<Task> it = tasks.iterator(); it.hasNext(); ) {
            Task t = it.next();
            if (t.due <= now) {
                if (due == null) due = new ArrayList<>();
                due.add(t);
                it.remove();
            }
        }
        if (due == null) return;
        for (Task t : due) {
            try {
                t.action.run();
            } catch (RuntimeException e) {
                onError.accept(e);
            }
        }
    }

    public int pending() {
        return tasks.size();
    }

    public void clear() {
        tasks.clear();
    }
}
