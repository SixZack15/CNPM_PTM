import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class PersonalTaskManagerViolations {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] VALID_PRIORITIES = {"Thấp", "Trung bình", "Cao"};

    // Phương thức kiểm tra tiêu đề
    private boolean validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            System.out.println("Lỗi: Tiêu đề không được để trống.");
            return false;
        }
        return true;
    }

    // Phương thức kiểm tra và phân tích ngày
    private LocalDate validateAndParseDate(String dueDateStr) {
        if (dueDateStr == null || dueDateStr.trim().isEmpty()) {
            System.out.println("Lỗi: Ngày đến hạn không được để trống.");
            return null;
        }
        
        try {
            return LocalDate.parse(dueDateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            System.out.println("Lỗi: Ngày đến hạn không hợp lệ. Vui lòng sử dụng định dạng YYYY-MM-DD.");
            return null;
        }
    }

    // Phương thức kiểm tra mức ưu tiên
    private boolean validatePriority(String priorityLevel) {
        for (String validP : VALID_PRIORITIES) {
            if (validP.equals(priorityLevel)) {
                return true;
            }
        }
        System.out.println("Lỗi: Mức độ ưu tiên không hợp lệ. Vui lòng chọn từ: Thấp, Trung bình, Cao.");
        return false;
    }

    // Phương thức kiểm tra trùng lặp
    private boolean isTaskDuplicate(JSONArray tasks, String title, LocalDate dueDate) {
        for (Object obj : tasks) {
            JSONObject task = (JSONObject) obj;
            if (task.get("title").toString().equalsIgnoreCase(title) &&
                task.get("due_date").toString().equals(dueDate.format(DATE_FORMATTER))) {
                System.out.println(String.format("Lỗi: Nhiệm vụ '%s' đã tồn tại với cùng ngày đến hạn.", title));
                return true;
            }
        }
        return false;
    }

    /**
     * Thêm nhiệm vụ mới với logic kiểm tra được tách riêng
     */
    public JSONObject addNewTaskWithViolations(String title, String description,
                                              String dueDateStr, String priorityLevel) {

        // Kiểm tra tiêu đề
        if (!validateTitle(title)) return null;
        
        // Kiểm tra và phân tích ngày
        LocalDate dueDate = validateAndParseDate(dueDateStr);
        if (dueDate == null) return null;
        
        // Kiểm tra mức ưu tiên
        if (!validatePriority(priorityLevel)) return null;

        JSONArray tasks = DatabaseManager.loadTasksFromDb();

        // Kiểm tra trùng lặp
        if (isTaskDuplicate(tasks, title, dueDate)) return null;

        // Tính toán ID mới
        int nextId = 1;
        for (Object obj : tasks) {
            JSONObject task = (JSONObject) obj;
            int taskId = Integer.parseInt(task.get("id").toString());
            if (taskId >= nextId) {
                nextId = taskId + 1;
            }
        }

        // Tạo task mới
        JSONObject newTask = new JSONObject();
        newTask.put("id", nextId);
        newTask.put("title", title);
        newTask.put("description", description);
        newTask.put("due_date", dueDate.format(DATE_FORMATTER));
        newTask.put("priority", priorityLevel);
        newTask.put("status", "Chưa hoàn thành");
        newTask.put("created_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        newTask.put("last_updated_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        tasks.add(newTask);
        DatabaseManager.saveTasksToDb(tasks);

        System.out.println(String.format("Đã thêm nhiệm vụ mới thành công với ID: %d", nextId));
        return newTask;
    }

    public static void main(String[] args) {
        PersonalTaskManagerViolations manager = new PersonalTaskManagerViolations();
        
        System.out.println("\nThêm nhiệm vụ hợp lệ:");
        manager.addNewTaskWithViolations(
            "Mua sách",
            "Sách Công nghệ phần mềm.",
            "2025-07-20",
            "Cao"
        );

        System.out.println("\nThử thêm trùng lặp:");
        manager.addNewTaskWithViolations(
            "Mua sách",
            "Mô tả khác",
            "2025-07-20",
            "Trung bình"
        );

        System.out.println("\nThêm với mức ưu tiên không hợp lệ:");
        manager.addNewTaskWithViolations(
            "Gọi điện",
            "Liên lạc khách hàng",
            "2025-08-01",
            "Rất cao"
        );
    }
}