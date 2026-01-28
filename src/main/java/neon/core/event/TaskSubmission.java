package neon.core.event;

import com.google.common.collect.ConcurrentArrayListMultimap;
import com.google.common.collect.Multimap;
import neon.util.fsm.Action;

public class TaskSubmission {
  protected final Multimap<String, Action> tasks;
  protected final Multimap<Integer, TaskQueue.RepeatEntry> repeat;

  public TaskSubmission() {
    tasks = new ConcurrentArrayListMultimap<>();
    repeat = new ConcurrentArrayListMultimap<>();
  }

  public void add(String description, Action task) {
    tasks.put(description, task);
  }

  public void add(String script, Integer start, Integer period, Integer stop) {
    TaskQueue.RepeatEntry entry = new TaskQueue.RepeatEntry(period, stop, script);
    repeat.put(start, entry);
  }

  public void add(Action task, Integer start, Integer period, Integer stop) {
    TaskQueue.RepeatEntry entry = new TaskQueue.RepeatEntry(period, stop, task);
    repeat.put(start, entry);
  }
}
