package util;

import model.Task;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WorkloadManager {
    private List<Task> tasks;
    public static final int DAILY_MAX_MINUTES = 180; // SC4: Daily threshold

    public WorkloadManager() {
        this.tasks = new ArrayList<>();
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public List<Task> getAllTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    // SC3: Return active tasks sorted by dynamic composite priority
    public List<Task> getPrioritizedActiveTasks() {
        List<Task> activeTasks = tasks.stream()
                .filter(t -> !t.isCompleted())
                .collect(Collectors.toList());
        Collections.sort(activeTasks);
        return activeTasks;
    }

    // SC7: Filter active tasks by selected course name
    public List<Task> getFilteredTasks(String courseName) {
        List<Task> active = getPrioritizedActiveTasks();
        if (courseName.equals("All Courses")) {
            return active;
        }
        return active.stream()
                .filter(t -> t.getCourseName().equalsIgnoreCase(courseName))
                .collect(Collectors.toList());
    }

    // SC4 & SC5: Check total scheduled time for a date
    public int getScheduledMinutesForDate(LocalDate date) {
        return tasks.stream()
                .filter(t -> !t.isCompleted() && t.getDueDate().equals(date))
                .mapToInt(Task::getEstimatedMinutes)
                .sum();
    }

    public boolean isDateOverloaded(LocalDate date) {
        return getScheduledMinutesForDate(date) > DAILY_MAX_MINUTES;
    }

    // SC8: Calculate completion percentage for progress bar
    public int getCourseCompletionPercentage(String courseName) {
        List<Task> courseTasks = tasks.stream()
                .filter(t -> t.getCourseName().equalsIgnoreCase(courseName))
                .collect(Collectors.toList());
        if (courseTasks.isEmpty()) return 0;

        long completedCount = courseTasks.stream().filter(Task::isCompleted).count();
        return (int) ((completedCount * 100.0) / courseTasks.size());
    }
}