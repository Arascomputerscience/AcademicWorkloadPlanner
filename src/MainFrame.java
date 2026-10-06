package gui;

import model.Task;
import util.StorageHandler;
import util.WorkloadManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class MainFrame extends JFrame {
    private WorkloadManager manager;

    // Form inputs
    private JTextField titleField;
    private JTextField courseField;
    private JTextField dueDateField;
    private JSpinner prioritySpinner;
    private JTextField effortField;

    // UI Viewers & Controls
    private JTable taskTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> filterComboBox;
    private JLabel overloadWarningLabel;
    private JProgressBar courseProgressBar;

    public MainFrame() {
        manager = new WorkloadManager();
        manager.setTasks(StorageHandler.loadTasks()); // SC6: Load on start

        setTitle("Academic Workload & Study Planner");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        refreshTaskList();

        // SC6: Save on exit
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                StorageHandler.saveTasks(manager.getAllTasks());
            }
        });
    }

    private void initComponents() {
        // --- TOP FORM PANEL (SC1 & SC2) ---
        JPanel formPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        formPanel.setBorder(BorderFactory.createTitledBorder("Add New Task"));

        formPanel.add(new JLabel("Task Title:"));
        titleField = new JTextField();
        formPanel.add(titleField);

        formPanel.add(new JLabel("Course Name:"));
        courseField = new JTextField();
        formPanel.add(courseField);

        formPanel.add(new JLabel("Due Date (YYYY-MM-DD):"));
        dueDateField = new JTextField();
        formPanel.add(dueDateField);

        formPanel.add(new JLabel("Priority (1-5):"));
        prioritySpinner = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
        formPanel.add(prioritySpinner);

        formPanel.add(new JLabel("Estimated Effort (Minutes):"));
        effortField = new JTextField();
        formPanel.add(effortField);

        JButton addButton = new JButton("Add Task");
        addButton.addActionListener(e -> handleAddTask());
        formPanel.add(addButton);

        add(formPanel, BorderLayout.NORTH);

        // --- CENTER TABLE PANEL (SC3, SC5, SC7, SC8) ---
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

        // Filter Bar (SC7)
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Filter by Course:"));
        filterComboBox = new JComboBox<>(new String[]{"All Courses"});
        filterComboBox.addActionListener(e -> refreshTaskList());
        filterPanel.add(filterComboBox);

        overloadWarningLabel = new JLabel("Status: Normal Workload");
        overloadWarningLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        filterPanel.add(overloadWarningLabel);

        centerPanel.add(filterPanel, BorderLayout.NORTH);

        // Table Model Setup
        String[] columnNames = {"Title", "Course", "Due Date", "Priority", "Effort (m)", "Score", "Done"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 6 ? Boolean.class : String.class;
            }
        };

        taskTable = new JTable(tableModel);
        tableModel.addTableModelListener(e -> {
            int row = e.getFirstRow();
            int col = e.getColumn();
            if (col == 6 && row >= 0 && row < taskTable.getRowCount()) {
                Boolean isChecked = (Boolean) tableModel.getValueAt(row, 6);
                List<Task> activeTasks = manager.getFilteredTasks((String) filterComboBox.getSelectedItem());
                if (row < activeTasks.size()) {
                    Task task = activeTasks.get(row);
                    task.setCompleted(isChecked); // SC8: Toggle state
                    SwingUtilities.invokeLater(this::refreshTaskList);
                }
            }
        });

        centerPanel.add(new JScrollPane(taskTable), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // --- BOTTOM PROGRESS BAR (SC8) ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(new JLabel("Course Progress:"));
        courseProgressBar = new JProgressBar(0, 100);
        courseProgressBar.setStringPainted(true);
        bottomPanel.add(courseProgressBar);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // SC2: Input Validation Logic
    private void handleAddTask() {
        String title = titleField.getText().trim();
        String course = courseField.getText().trim();
        String dateStr = dueDateField.getText().trim();
        String effortStr = effortField.getText().trim();

        if (title.isEmpty() || course.isEmpty() || dateStr.isEmpty() || effortStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are mandatory!", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocalDate dueDate;
        try {
            dueDate = LocalDate.parse(dateStr);
            if (dueDate.isBefore(LocalDate.now())) {
                JOptionPane.showMessageDialog(this, "Due date cannot be in the past!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format! Use YYYY-MM-DD.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int effort;
        try {
            effort = Integer.parseInt(effortStr);
            if (effort <= 0) {
                JOptionPane.showMessageDialog(this, "Effort must be a positive number!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Effort must be a numeric integer!", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int priority = (Integer) prioritySpinner.getValue();

        // Create and register new Task
        Task newTask = new Task(title, course, dueDate, priority, effort);
        manager.addTask(newTask);

        updateCourseDropdown(course);
        clearForm();
        refreshTaskList();
    }

    private void updateCourseDropdown(String course) {
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) filterComboBox.getModel();
        boolean exists = false;
        for (int i = 0; i < model.getSize(); i++) {
            if (model.getElementAt(i).equalsIgnoreCase(course)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            model.addElement(course);
        }
    }

    private void refreshTaskList() {
        tableModel.setRowCount(0);
        String selectedCourse = (String) filterComboBox.getSelectedItem();
        if (selectedCourse == null) selectedCourse = "All Courses";

        List<Task> displayedTasks = manager.getFilteredTasks(selectedCourse);

        boolean hasOverload = false;
        for (Task t : displayedTasks) {
            tableModel.addRow(new Object[]{
                    t.getTitle(),
                    t.getCourseName(),
                    t.getDueDate().toString(),
                    t.getPriorityRating(),
                    t.getEstimatedMinutes(),
                    String.format("%.1f", t.calculateCompositePriority()),
                    t.isCompleted()
            });

            // SC5: Check overload on due dates
            if (manager.isDateOverloaded(t.getDueDate())) {
                hasOverload = true;
            }
        }

        // SC5: Visual warning display
        if (hasOverload) {
            overloadWarningLabel.setText("⚠️ WARNING: Daily Capacity Exceeded (>180 mins)!");
            overloadWarningLabel.setForeground(Color.RED);
        } else {
            overloadWarningLabel.setText("Status: Workload Normal");
            overloadWarningLabel.setForeground(new Color(0, 128, 0));
        }

        // SC8: Progress Bar Update
        if (!selectedCourse.equals("All Courses")) {
            int progress = manager.getCourseCompletionPercentage(selectedCourse);
            courseProgressBar.setValue(progress);
        } else {
            courseProgressBar.setValue(0);
        }
    }

    private void clearForm() {
        titleField.setText("");
        courseField.setText("");
        dueDateField.setText("");
        effortField.setText("");
        prioritySpinner.setValue(3);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}