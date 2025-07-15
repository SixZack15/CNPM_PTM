import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class PersonalTaskManagerViolations {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Thêm nhiệm vụ mới với ID tự động tăng
     */
    public JSONObject addNewTaskWithViolations(String title, String description,
                                              String dueDateStr, String priorityLevel) {

        // Validation giữ nguyên
        if (title == null || title.trim().isEmpty()) {
            System.out.println("Lỗi: Tiêu đề không được để trống.");
            return null;
        }
        
        if (dueDateStr == null || dueDateStr.trim().isEmpty()) {
            System.out.println("Lỗi: Ngày đến hạn không được để trống.");
            return null;
        }
        
        LocalDate dueDate;
        try {
            dueDate = LocalDate.parse(dueDateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            System.out.println("Lỗi: Ngày đến hạn không hợp lệ. Vui lòng sử dụng định dạng YYYY-MM-DD.");
            return null;
        }
        
        String[] validPriorities = {"Thấp", "Trung bình", "Cao"};
        boolean isValidPriority = false;
        for (String validP : validPriorities) {
            if (validP.equals(priorityLevel)) {
                isValidPriority = true;
                break;
            }
        }
        
        if (!isValidPriority) {
            System.out.println("Lỗi: Mức độ ưu tiên không hợp lệ. Vui lòng chọn từ: Thấp, Trung bình, Cao.");
            return null;
        }

        JSONArray tasks = DatabaseManager.loadTasksFromDb();

        // Kiểm tra trùng lặp
        for (Object obj : tasks) {
            JSONObject existingTask = (JSONObject) obj;
            if (existingTask.get("title").toString().equalsIgnoreCase(title) &&
                existingTask.get("due_date").toString().equals(dueDate.format(DATE_FORMATTER))) {
                System.out.println(String.format("Lỗi: Nhiệm vụ '%s' đã tồn tại với cùng ngày đến hạn.", title));
                return null;
            }
        }

        // Tính toán ID mới
        int nextId = 1;
        for (Object obj : tasks) {
            JSONObject task = (JSONObject) obj;
            int taskId = Integer.parseInt(task.get("id").toString());
            if (taskId >= nextId) {
                nextId = taskId + 1;
            }
        }

        // Tạo task mới với ID số
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

        System.out.println("\nThêm nhiệm vụ trùng lặp:");
        manager.addNewTaskWithViolations(
            "Mua sách",
            "Sách Công nghệ phần mềm.",
            "2025-07-20",
            "Cao"
        );

        System.out.println("\nThêm nhiệm vụ với tiêu đề rỗng:");
        manager.addNewTaskWithViolations(
            "",
            "Nhiệm vụ không có tiêu đề.",
            "2025-07-22",
            "Thấp"
        );
    }
}