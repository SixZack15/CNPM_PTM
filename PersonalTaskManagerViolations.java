import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.util.HashSet;
import java.util.Set;

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

    // Tạo khóa duy nhất từ task
    private String buildTaskKey(String title, LocalDate dueDate) {
        return title.toLowerCase() + "|" + dueDate.format(DATE_FORMATTER);
    }

    // Kiểm tra trùng lặp dùng HashSet
    private boolean isTaskDuplicate(JSONArray tasks, String title, LocalDate dueDate) {
        Set<String> existingKeys = new HashSet<>();
        
        // Xây dựng tập hợp khóa duy nhất
        for (Object obj : tasks) {
            JSONObject task = (JSONObject) obj;
            String taskKey = buildTaskKey(
                task.get("title").toString(),
                LocalDate.parse(task.get("due_date").toString(), DATE_FORMATTER)
            );
            existingKeys.add(taskKey);
        }

        // Kiểm tra trùng lặp trong O(1)
        String newKey = buildTaskKey(title, dueDate);
        if (existingKeys.contains(newKey)) {
            System.out.println(String.format("Lỗi: Nhiệm vụ '%s' đã tồn tại với cùng ngày đến hạn.", title));
            return true;
        }
        return false;
    }

    // Tách biệt logic tạo task
    private JSONObject createNewTask(int id, String title, String description, 
                                    LocalDate dueDate, String priorityLevel) {
        JSONObject newTask = new JSONObject();
        newTask.put("id", id);
        newTask.put("title", title);
        newTask.put("description", description);
        newTask.put("due_date", dueDate.format(DATE_FORMATTER));
        newTask.put("priority", priorityLevel);
        newTask.put("status", "Chưa hoàn thành");
        newTask.put("created_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        newTask.put("last_updated_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        return newTask;
    }

    // Tạo ID mới cho task
    private int generateNextId(JSONArray tasks) {
        int nextId = 1;
        for (Object obj : tasks) {
            JSONObject task = (JSONObject) obj;
            int taskId = Integer.parseInt(task.get("id").toString());
            if (taskId >= nextId) {
                nextId = taskId + 1;
            }
        }
        return nextId;
    }

    // Lưu danh sách task vào database
    private void saveTaskList(JSONArray tasks) {
        DatabaseManager.saveTasksToDb(tasks);
    }

    // Kiểm tra dữ liệu đầu vào
    private boolean validateTaskInput(String title, String dueDateStr, String priorityLevel) {
        return validateTitle(title) && 
               validateAndParseDate(dueDateStr) != null && 
               validatePriority(priorityLevel);
    }

    /**
     * Logic chính sau khi tái cấu trúc
     */
    public JSONObject addNewTaskWithViolations(String title, String description,
                                              String dueDateStr, String priorityLevel) {

        // Kiểm tra dữ liệu đầu vào
        if (!validateTaskInput(title, dueDateStr, priorityLevel)) {
            return null;
        }
        
        LocalDate dueDate = validateAndParseDate(dueDateStr);
        JSONArray tasks = DatabaseManager.loadTasksFromDb();

        // Kiểm tra trùng lặp
        if (isTaskDuplicate(tasks, title, dueDate)) {
            return null;
        }

        // Tạo ID mới
        int nextId = generateNextId(tasks);
        
        // Tạo task mới
        JSONObject newTask = createNewTask(nextId, title, description, dueDate, priorityLevel);

        // Lưu task vào danh sách
        tasks.add(newTask);
        saveTaskList(tasks);

        System.out.println(String.format("Đã thêm nhiệm vụ mới thành công với ID: %d", nextId));
        return newTask;
    }

    public static void main(String[] args) {
        PersonalTaskManagerViolations manager = new PersonalTaskManagerViolations();
        
        // Nhóm các test case liên quan
        System.out.println("===== KIỂM THỬ CHỨC NĂNG CHÍNH =====");
        System.out.println("\n[1] Thêm nhiệm vụ hợp lệ:");
        manager.addNewTaskWithViolations(
            "Mua sách",
            "Sách Công nghệ phần mềm.",
            "2025-07-20",
            "Cao"
        );

        System.out.println("\n[2] Thử thêm trùng lặp:");
        manager.addNewTaskWithViolations(
            "Mua sách",
            "Mô tả khác",
            "2025-07-20",
            "Trung bình"
        );

        System.out.println("\n[3] Thêm với mức ưu tiên không hợp lệ:");
        manager.addNewTaskWithViolations(
            "Gọi điện",
            "Liên lạc khách hàng",
            "2025-08-01",
            "Rất cao"
        );
        
        // Nhóm kiểm thử phụ trợ
        System.out.println("\n===== KIỂM THỬ PHỤ TRỢ =====");
        System.out.println("[4] Tạo task không phụ thuộc database:");
        JSONObject testTask = manager.createNewTask(
            99, 
            "Task kiểm thử", 
            "Mô tả kiểm thử", 
            LocalDate.now(), 
            "Trung bình"
        );
        System.out.println("Kết quả tạo task mẫu:");
        System.out.println(testTask.toJSONString());
        
        // Kiểm thử hiệu năng
        System.out.println("\n[5] Kiểm thử hiệu năng với 1000 task:");
        JSONArray massTasks = new JSONArray();
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            JSONObject task = manager.createNewTask(
                i, 
                "Task " + i, 
                "Mô tả " + i, 
                LocalDate.now().plusDays(i), 
                "Trung bình"
            );
            massTasks.add(task);
        }
        
        boolean isDuplicate = manager.isTaskDuplicate(
            massTasks, 
            "Task 999", 
            LocalDate.now().plusDays(999)
        );
        
        long duration = System.currentTimeMillis() - startTime;
        System.out.println(String.format(
            "Kiểm tra 1000 task hoàn thành trong %d ms | Trùng lặp: %s", 
            duration, 
            isDuplicate
        ));
    }
}