package gui;

import model.Task;
import util.StorageHandler;
import util.WorkloadManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class Main extends JFrame {
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
    private CalendarPanel calendarPanel;
    private boolean isUpdatingTable = false;

    // Flashing Animation Controls
    private boolean flashToggle = false;
    private Timer flashTimer;

    // Modern Color Palette
    private final Color PRIMARY_ACCENT = new Color(79, 70, 229);   // Deep Indigo
    private final Color SECONDARY_ACCENT = new Color(99, 102, 241); // Soft Purple/Indigo
    private final Color BG_DARK = new Color(30, 41, 59);          // Slate Dark Navy
    private final Color BG_CARD = new Color(248, 250, 252);        // Clean Light Off-White
    private final Color DANGER_COLOR = new Color(225, 29, 72);     // Vivid Red
    private final Color SUCCESS_COLOR = new Color(16, 185, 129);   // Mint Green

    public Main() {
        manager = new WorkloadManager();
        manager.setTasks(StorageHandler.loadTasks()); // Load saved tasks on start

        // Setup timer to trigger repaint for flashing overdue due dates
        flashTimer = new Timer(500, e -> {
            flashToggle = !flashToggle;
            if (taskTable != null) {
                taskTable.repaint();
            }
        });
        flashTimer.start();

        setTitle("Academic Workload & Study Planner");
        setSize(1000, 700);
        setLocationRelativeTo(null); // Center on screen
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(15, 15));

        // Display Welcome Title Screen
        showWelcomeScreen();

        initComponents();
        populateExistingCourses();
        refreshTaskList();

        // Save tasks on exit
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                StorageHandler.saveTasks(manager.getAllTasks());
            }
        });
    }

    // --- WELCOME TITLE SCREEN (SPLASH DIALOG) ---
    private void showWelcomeScreen() {
        JDialog welcomeDialog = new JDialog(this, "Welcome", true);
        welcomeDialog.setSize(500, 320);
        welcomeDialog.setLocationRelativeTo(this);
        welcomeDialog.setUndecorated(true);

        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBackground(BG_DARK);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY_ACCENT, 3),
                new EmptyBorder(30, 30, 30, 30)
        ));

        JLabel titleLabel = new JLabel("Welcome to your personal", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        titleLabel.setForeground(new Color(203, 213, 225));

        JLabel mainTitle = new JLabel("Academic Workload Planner", SwingConstants.CENTER);
        mainTitle.setFont(new Font("SansSerif", Font.BOLD, 24));
        mainTitle.setForeground(Color.WHITE);

        JPanel textGroup = new JPanel(new GridLayout(2, 1, 5, 5));
        textGroup.setOpaque(false);
        textGroup.add(titleLabel);
        textGroup.add(mainTitle);

        JLabel subtext = new JLabel("<html><center>Organize assignments, track priority scores, and balance your daily workload effectively!</center></html>", SwingConstants.CENTER);
        subtext.setFont(new Font("SansSerif", Font.ITALIC, 13));
        subtext.setForeground(new Color(148, 163, 184));

        JButton startButton = new JButton("Get Started");
        startButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        startButton.setBackground(PRIMARY_ACCENT);
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        startButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        startButton.addActionListener(e -> welcomeDialog.dispose());

        contentPanel.add(textGroup, BorderLayout.NORTH);
        contentPanel.add(subtext, BorderLayout.CENTER);
        contentPanel.add(startButton, BorderLayout.SOUTH);

        welcomeDialog.setContentPane(contentPanel);
        welcomeDialog.setVisible(true);
    }

    private void initComponents() {
        getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));
        getContentPane().setBackground(new Color(241, 245, 249));

        // --- TOP FORM PANEL ---
        JPanel formCard = new JPanel(new BorderLayout(10, 10));
        formCard.setBackground(BG_CARD);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(12, 15, 12, 15)
        ));

        JLabel formHeader = new JLabel("➕ Add New Academic Task");
        formHeader.setFont(new Font("SansSerif", Font.BOLD, 15));
        formHeader.setForeground(PRIMARY_ACCENT);
        formCard.add(formHeader, BorderLayout.NORTH);

        JPanel formGrid = new JPanel(new GridLayout(3, 4, 10, 10));
        formGrid.setOpaque(false);

        formGrid.add(createLabel("Task Title:"));
        titleField = new JTextField();
        formGrid.add(titleField);

        formGrid.add(createLabel("Course Name:"));
        courseField = new JTextField();
        formGrid.add(courseField);

        formGrid.add(createLabel("Due Date (YYYY-MM-DD):"));
        dueDateField = new JTextField();
        formGrid.add(dueDateField);

        formGrid.add(createLabel("Priority (1-5):"));
        prioritySpinner = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
        formGrid.add(prioritySpinner);

        formGrid.add(createLabel("Estimated Effort (Mins):"));
        effortField = new JTextField();
        formGrid.add(effortField);

        JButton addButton = new JButton("Add Task");
        addButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        addButton.setBackground(PRIMARY_ACCENT);
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        addButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addButton.addActionListener(e -> handleAddTask());
        formGrid.add(addButton);

        formCard.add(formGrid, BorderLayout.CENTER);
        add(formCard, BorderLayout.NORTH);

        // --- CENTER TABBED PANE (Task Manager + Calendar View) ---
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 12));

        // Tab 1: Task Manager Panel
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);

        // Filter Bar
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        filterPanel.setBackground(BG_CARD);
        filterPanel.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));

        filterPanel.add(createLabel("Filter Course:"));
        filterComboBox = new JComboBox<>(new String[]{"All Courses"});
        filterComboBox.addActionListener(e -> refreshTaskList());
        filterPanel.add(filterComboBox);

        JButton deleteButton = new JButton("🗑️ Delete Selected");
        deleteButton.setBackground(DANGER_COLOR);
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        deleteButton.setFocusPainted(false);
        deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteButton.addActionListener(e -> handleDeleteTask());
        filterPanel.add(deleteButton);

        overloadWarningLabel = new JLabel("Status: Workload Normal");
        overloadWarningLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        filterPanel.add(overloadWarningLabel);

        centerPanel.add(filterPanel, BorderLayout.NORTH);

        // Custom Table Setup
        String[] columnNames = {"Title", "Course", "Due Date", "Priority", "Effort (m)", "Score", "Done"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 6 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Allow checking/unchecking
            }
        };

        taskTable = new JTable(tableModel);
        taskTable.setRowHeight(28);
        taskTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        taskTable.getTableHeader().setBackground(SECONDARY_ACCENT);
        taskTable.getTableHeader().setForeground(Color.WHITE);

        // Custom Renderer to flash red ONLY for the Due Date column (Index 2) when overdue
        DefaultTableCellRenderer overdueRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Center align columns 2 through 5
                if (column >= 2 && column <= 5) {
                    setHorizontalAlignment(JLabel.CENTER);
                } else {
                    setHorizontalAlignment(JLabel.LEFT);
                }

                // Check overdue status
                String selectedCourse = (String) filterComboBox.getSelectedItem();
                List<Task> currentTasks = manager.getFilteredTasks(selectedCourse != null ? selectedCourse : "All Courses");

                if (row >= 0 && row < currentTasks.size()) {
                    Task task = currentTasks.get(row);
                    boolean isOverdue = !task.isCompleted() && task.getDueDate().isBefore(LocalDate.now());

                    // Flash ONLY column 2 (Due Date) when overdue
                    if (isOverdue && column == 2) {
                        if (!isSelected) {
                            c.setBackground(flashToggle ? new Color(254, 226, 226) : new Color(239, 68, 68));
                            c.setForeground(flashToggle ? DANGER_COLOR : Color.WHITE);
                        }
                    } else {
                        if (!isSelected) {
                            c.setBackground(Color.WHITE);
                            c.setForeground(Color.BLACK);
                        }
                    }
                }

                return c;
            }
        };

        for (int i = 0; i < 6; i++) {
            taskTable.getColumnModel().getColumn(i).setCellRenderer(overdueRenderer);
        }

        tableModel.addTableModelListener(e -> {
            if (isUpdatingTable) return;

            int row = e.getFirstRow();
            int col = e.getColumn();
            if (col == 6 && row >= 0 && row < taskTable.getRowCount()) {
                Boolean isChecked = (Boolean) tableModel.getValueAt(row, 6);
                String selectedCourse = (String) filterComboBox.getSelectedItem();
                List<Task> currentTasks = manager.getFilteredTasks(selectedCourse != null ? selectedCourse : "All Courses");

                if (row < currentTasks.size()) {
                    Task task = currentTasks.get(row);
                    task.setCompleted(isChecked != null && isChecked);
                    SwingUtilities.invokeLater(this::refreshTaskList);
                }
            }
        });

        JScrollPane tablePane = new JScrollPane(taskTable);
        tablePane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        centerPanel.add(tablePane, BorderLayout.CENTER);

        // --- BOTTOM PROGRESS BAR ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        bottomPanel.setBackground(BG_CARD);
        bottomPanel.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));

        JLabel progressLabel = createLabel("Course Completion Progress:");
        progressLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        bottomPanel.add(progressLabel);

        courseProgressBar = new JProgressBar(0, 100);
        courseProgressBar.setPreferredSize(new Dimension(250, 22));
        courseProgressBar.setStringPainted(true);
        courseProgressBar.setForeground(SUCCESS_COLOR);
        bottomPanel.add(courseProgressBar);

        centerPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Add Tab 1 to TabbedPane
        tabbedPane.addTab("📋 Task Manager", centerPanel);

        // --- Tab 2: Calendar View Panel ---
        calendarPanel = new CalendarPanel();
        tabbedPane.addTab("📅 Calendar View", calendarPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(new Color(51, 65, 85));
        return label;
    }

    private void populateExistingCourses() {
        for (Task task : manager.getAllTasks()) {
            updateCourseDropdown(task.getCourseName());
        }
    }

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

        Task newTask = new Task(title, course, dueDate, priority, effort);
        manager.addTask(newTask);

        updateCourseDropdown(course);
        clearForm();
        refreshTaskList();
    }

    private void handleDeleteTask() {
        int selectedRow = taskTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a task from the table to delete.",
                    "No Task Selected",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this task?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            String selectedCourse = (String) filterComboBox.getSelectedItem();
            List<Task> displayedTasks = manager.getFilteredTasks(selectedCourse != null ? selectedCourse : "All Courses");

            if (selectedRow < displayedTasks.size()) {
                Task taskToRemove = displayedTasks.get(selectedRow);
                manager.removeTask(taskToRemove);
                refreshTaskList();
            }
        }
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
        isUpdatingTable = true;
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

            if (manager.isDateOverloaded(t.getDueDate())) {
                hasOverload = true;
            }
        }

        if (hasOverload) {
            overloadWarningLabel.setText("⚠️ WARNING: Daily Capacity Exceeded (>180 mins)!");
            overloadWarningLabel.setForeground(DANGER_COLOR);
        } else {
            overloadWarningLabel.setText("Status: Workload Normal");
            overloadWarningLabel.setForeground(SUCCESS_COLOR);
        }

        if (!selectedCourse.equals("All Courses")) {
            int progress = manager.getCourseCompletionPercentage(selectedCourse);
            courseProgressBar.setValue(progress);
        } else {
            courseProgressBar.setValue(0);
        }

        // Refresh the calendar view if initialized
        if (calendarPanel != null) {
            calendarPanel.refreshCalendar();
        }

        isUpdatingTable = false;
    }

    private void clearForm() {
        titleField.setText("");
        courseField.setText("");
        dueDateField.setText("");
        effortField.setText("");
        prioritySpinner.setValue(3);
    }

    // --- PRIVATE INNER CLASS FOR THE CALENDAR VIEW ---
    private class CalendarPanel extends JPanel {
        private YearMonth currentYearMonth;
        private JLabel monthTitleLabel;
        private JPanel daysGridPanel;

        public CalendarPanel() {
            this.currentYearMonth = YearMonth.now();

            setLayout(new BorderLayout(10, 10));
            setBackground(BG_CARD);
            setBorder(new EmptyBorder(10, 10, 10, 10));

            initComponents();
            refreshCalendar();
        }

        private void initComponents() {
            JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
            navPanel.setOpaque(false);

            JButton prevButton = new JButton("◀ Previous Month");
            styleNavButton(prevButton);
            prevButton.addActionListener(e -> {
                currentYearMonth = currentYearMonth.minusMonths(1);
                refreshCalendar();
            });

            monthTitleLabel = new JLabel("", SwingConstants.CENTER);
            monthTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            monthTitleLabel.setForeground(new Color(30, 41, 59));
            monthTitleLabel.setPreferredSize(new Dimension(220, 30));

            JButton nextButton = new JButton("Next Month ▶");
            styleNavButton(nextButton);
            nextButton.addActionListener(e -> {
                currentYearMonth = currentYearMonth.plusMonths(1);
                refreshCalendar();
            });

            navPanel.add(prevButton);
            navPanel.add(monthTitleLabel);
            navPanel.add(nextButton);
            add(navPanel, BorderLayout.NORTH);

            JPanel headerPanel = new JPanel(new GridLayout(1, 7, 5, 5));
            headerPanel.setOpaque(false);
            String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            for (String day : daysOfWeek) {
                JLabel lbl = new JLabel(day, SwingConstants.CENTER);
                lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
                lbl.setForeground(PRIMARY_ACCENT);
                headerPanel.add(lbl);
            }

            daysGridPanel = new JPanel(new GridLayout(0, 7, 5, 5));
            daysGridPanel.setOpaque(false);

            JPanel centerContainer = new JPanel(new BorderLayout(5, 5));
            centerContainer.setOpaque(false);
            centerContainer.add(headerPanel, BorderLayout.NORTH);
            centerContainer.add(daysGridPanel, BorderLayout.CENTER);

            add(centerContainer, BorderLayout.CENTER);
        }

        private void styleNavButton(JButton button) {
            button.setFont(new Font("SansSerif", Font.BOLD, 12));
            button.setBackground(PRIMARY_ACCENT);
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        public void refreshCalendar() {
            daysGridPanel.removeAll();
            monthTitleLabel.setText(currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + currentYearMonth.getYear());

            LocalDate firstOfMonth = currentYearMonth.atDay(1);
            int dayOfWeekValue = firstOfMonth.getDayOfWeek().getValue(); 
            int blankCells = dayOfWeekValue % 7; 

            for (int i = 0; i < blankCells; i++) {
                JPanel emptyCell = new JPanel();
                emptyCell.setOpaque(false);
                daysGridPanel.add(emptyCell);
            }

            int daysInMonth = currentYearMonth.lengthOfMonth();
            LocalDate today = LocalDate.now();

            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date = currentYearMonth.atDay(day);
                JPanel dayCell = createDayCell(date, today.equals(date));
                daysGridPanel.add(dayCell);
            }

            daysGridPanel.revalidate();
            daysGridPanel.repaint();
        }

        private JPanel createDayCell(LocalDate date, boolean isToday) {
            JPanel cell = new JPanel();
            cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
            cell.setBackground(isToday ? new Color(224, 231, 255) : Color.WHITE);
            cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isToday ? PRIMARY_ACCENT : new Color(226, 232, 240), isToday ? 2 : 1),
                new EmptyBorder(4, 4, 4, 4)
            ));

            JLabel dayNumLabel = new JLabel(String.valueOf(date.getDayOfMonth()));
            dayNumLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            dayNumLabel.setForeground(isToday ? PRIMARY_ACCENT : new Color(51, 65, 85));
            cell.add(dayNumLabel);

            List<Task> tasksForDate = manager.getAllTasks().stream()
                    .filter(t -> t.getDueDate().equals(date))
                    .toList();

            for (Task task : tasksForDate) {
                JLabel taskLabel = new JLabel("• " + task.getTitle());
                taskLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
                
                if (task.isCompleted()) {
                    taskLabel.setForeground(new Color(148, 163, 184));
                } else if (manager.isDateOverloaded(date)) {
                    taskLabel.setForeground(DANGER_COLOR);
                } else {
                    taskLabel.setForeground(PRIMARY_ACCENT);
                }
                
                cell.add(taskLabel);
            }

            return cell;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}