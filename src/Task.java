package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Task implements Comparable<Task>, Serializable {
    private static final long serialVersionUID = 1L;

    private String title;
    private String courseName;
    private LocalDate dueDate;
    private int priorityRating; // 1 to 5
    private int estimatedMinutes;
    private boolean isCompleted;

    public Task(String title, String courseName, LocalDate dueDate, int priorityRating, int estimatedMinutes) {
        this.title = title;
        this.courseName = courseName;
        this.dueDate = dueDate;
        this.priorityRating = priorityRating;
        this.estimatedMinutes = estimatedMinutes;
        this.isCompleted = false;
    }

    // Algorithmic composite priority score calculation
    public double calculateCompositePriority() {
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        if (daysRemaining <= 0) daysRemaining = 1; // Prevent division by zero
        
        // Formula weighting urgency, rating, and estimated effort
        return (priorityRating * 20.0) + (estimatedMinutes / 10.0) - (daysRemaining * 5.0);
    }

    @Override
    public int compareTo(Task other) {
        // Sort descending by calculated composite score
        return Double.compare(other.calculateCompositePriority(), this.calculateCompositePriority());
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public String getCourseName() { return courseName; }
    public LocalDate getDueDate() { return dueDate; }
    public int getPriorityRating() { return priorityRating; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}