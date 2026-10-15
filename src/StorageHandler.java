package util;

import model.Task;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class StorageHandler {
    private static final String FILE_PATH = "tasks.dat";

    // Save tasks locally
    public static void saveTasks(List<Task> tasks) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(tasks);
        } catch (IOException e) {
            System.err.println("Error saving task data: " + e.getMessage());
        }
    }

    // Load tasks locally on application launch
    @SuppressWarnings("unchecked")
    public static List<Task> loadTasks() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (List<Task>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading task data: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}