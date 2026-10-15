package util;

import model.Task;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WorkloadManager {
    private List<Task> tasks;
    public static final int DAILY_MAX_MINUTES = 180; // Daily threshold

    public WorkloadManager() {
        this.tasks = new ArrayList<>();
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public void removeTask(Task task) {
        tasks.remove(task);
    }

    public List<Task> getAllTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    // Filter tasks by course name and keep them sorted by composite priority score
    public List<Task> getFilteredTasks(String courseName) {
        List<Task> all = new ArrayList<>(tasks);
        Collections.sort(all);

        if (courseName.equals("All Courses")) {
            return all;
        }
        return all.stream()
                .filter(t -> t.getCourseName().equalsIgnoreCase(courseName))
                .collect(Collectors.toList());
    }

    // Check total scheduled time for a date (only uncompleted tasks count towards workload)
    public int getScheduledMinutesForDate(LocalDate date) {
        return tasks.stream()
                .filter(t -> !t.isCompleted() && t.getDueDate().equals(date))
                .mapToInt(Task::getEstimatedMinutes)
                .sum();
    }

    public boolean isDateOverloaded(LocalDate date) {
        return getScheduledMinutesForDate(date) > DAILY_MAX_MINUTES;
    }

    // Calculate completion percentage for progress bar
    public int getCourseCompletionPercentage(String courseName) {
        List<Task> courseTasks = tasks.stream()
                .filter(t -> t.getCourseName().equalsIgnoreCase(courseName))
                .collect(Collectors.toList());
        if (courseTasks.isEmpty()) return 0;

        long completedCount = courseTasks.stream().filter(Task::isCompleted).count();
        return (int) ((completedCount * 100.0) / courseTasks.size());
    }
}