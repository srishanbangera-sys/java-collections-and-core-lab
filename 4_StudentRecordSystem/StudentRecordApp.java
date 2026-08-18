package studentrecord;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// ==========================================
// 1. MODEL CLASSES
// ==========================================

class StudentRecord implements Comparable<StudentRecord> {
    private int rollNo;
    private String name;
    private String department;
    private double mathMarks;
    private double scienceMarks;
    private double englishMarks;
    private double totalMarks;
    private double percentage;
    private String grade;

    public StudentRecord(int rollNo, String name, String department, double mathMarks, double scienceMarks, double englishMarks) {
        this.rollNo = rollNo;
        this.name = name;
        this.department = department;
        this.mathMarks = mathMarks;
        this.scienceMarks = scienceMarks;
        this.englishMarks = englishMarks;
        calculatePerformance();
    }

    public void calculatePerformance() {
        this.totalMarks = mathMarks + scienceMarks + englishMarks;
        this.percentage = Math.round((totalMarks / 300.0 * 100.0) * 100.0) / 100.0;
        if (percentage >= 90) this.grade = "A+";
        else if (percentage >= 80) this.grade = "A";
        else if (percentage >= 70) this.grade = "B";
        else if (percentage >= 60) this.grade = "C";
        else if (percentage >= 50) this.grade = "D";
        else this.grade = "F";
    }

    public int getRollNo() { return rollNo; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public double getMathMarks() { return mathMarks; }
    public double getScienceMarks() { return scienceMarks; }
    public double getEnglishMarks() { return englishMarks; }
    public double getTotalMarks() { return totalMarks; }
    public double getPercentage() { return percentage; }
    public String getGrade() { return grade; }

    public void setName(String name) { this.name = name; }
    public void setDepartment(String department) { this.department = department; }
    public void setMarks(double math, double science, double english) {
        this.mathMarks = math;
        this.scienceMarks = science;
        this.englishMarks = english;
        calculatePerformance();
    }

    // TreeSet Comparator: Descending order by totalMarks, then Roll No
    @Override
    public int compareTo(StudentRecord o) {
        int markComp = Double.compare(o.totalMarks, this.totalMarks);
        if (markComp != 0) return markComp;
        return Integer.compare(this.rollNo, o.rollNo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentRecord)) return false;
        StudentRecord record = (StudentRecord) o;
        return rollNo == record.rollNo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rollNo);
    }
}

// Action Log for Undo Stack (ArrayDeque)
class ActionLog {
    public enum ActionType { ADD, DELETE, UPDATE }

    private ActionType type;
    private StudentRecord record;
    private StudentRecord previousState; // For updates
    private String description;

    public ActionLog(ActionType type, StudentRecord record, String description) {
        this.type = type;
        this.record = record;
        this.description = description;
    }

    public ActionLog(ActionType type, StudentRecord record, StudentRecord previousState, String description) {
        this.type = type;
        this.record = record;
        this.previousState = previousState;
        this.description = description;
    }

    public ActionType getType() { return type; }
    public StudentRecord getRecord() { return record; }
    public StudentRecord getPreviousState() { return previousState; }
    public String getDescription() { return description; }
}

// ==========================================
// 2. RECORD SYSTEM SERVICE (COLLECTIONS)
// ==========================================

class StudentRecordService {

    // 1. ArrayList: Dynamic list of active student records
    private final List<StudentRecord> recordList = new CopyOnWriteArrayList<>();

    // 2. HashMap: Instant O(1) indexing by Roll Number
    private final Map<Integer, StudentRecord> rollMap = new ConcurrentHashMap<>();

    // 3. ArrayDeque: LIFO Stack for Operation Undo History & Audit Trail
    private final Deque<ActionLog> undoStack = new ArrayDeque<>();

    // 4. TreeSet: Auto-sorted Leaderboard (Top Performers by Total Marks)
    private final SortedSet<StudentRecord> leaderboardTree = Collections.synchronizedSortedSet(new TreeSet<>());

    public StudentRecordService() {
        // Seed Initial Data
        addRecord(new StudentRecord(101, "Alexander Pierce", "Computer Science", 95, 92, 88), false);
        addRecord(new StudentRecord(102, "Brianna Scott", "Data Science", 88, 85, 90), false);
        addRecord(new StudentRecord(103, "Christopher Lee", "Cybersecurity", 74, 68, 79), false);
        addRecord(new StudentRecord(104, "Deborah Vance", "AI & Robotics", 98, 96, 94), false);
        addRecord(new StudentRecord(105, "Edward Norton", "Information Tech", 62, 58, 65), false);
    }

    public synchronized boolean addRecord(StudentRecord record, boolean trackUndo) {
        if (rollMap.containsKey(record.getRollNo())) {
            return false;
        }

        recordList.add(record);                // 1. Add to ArrayList
        rollMap.put(record.getRollNo(), record); // 2. Put in HashMap
        leaderboardTree.add(record);           // 3. Add to TreeSet

        if (trackUndo) {
            // 4. Push to ArrayDeque Undo Stack
            undoStack.push(new ActionLog(ActionLog.ActionType.ADD, record, "Added student: " + record.getName() + " (Roll " + record.getRollNo() + ")"));
        }
        return true;
    }

    public synchronized boolean deleteRecord(int rollNo, boolean trackUndo) {
        StudentRecord record = rollMap.get(rollNo);
        if (record == null) return false;

        recordList.remove(record);         // 1. Remove from ArrayList
        rollMap.remove(rollNo);            // 2. Remove from HashMap
        leaderboardTree.remove(record);    // 3. Remove from TreeSet

        if (trackUndo) {
            // 4. Push to ArrayDeque Undo Stack
            undoStack.push(new ActionLog(ActionLog.ActionType.DELETE, record, "Deleted student: " + record.getName() + " (Roll " + rollNo + ")"));
        }
        return true;
    }

    /**
     * Demonstrates ArrayDeque LIFO Stack POP operation to Undo last user action.
     */
    public synchronized Map<String, Object> undoLastAction() {
        Map<String, Object> res = new HashMap<>();

        if (undoStack.isEmpty()) { // ArrayDeque isEmpty check
            res.put("success", false);
            res.put("message", "No actions available to undo in stack.");
            return res;
        }

        ActionLog lastAction = undoStack.pop(); // ArrayDeque pop (LIFO)

        if (lastAction.getType() == ActionLog.ActionType.ADD) {
            // Undo an ADD action -> DELETE record without tracking undo
            deleteRecord(lastAction.getRecord().getRollNo(), false);
            res.put("message", "Undo successful: Reverted creation of " + lastAction.getRecord().getName());
        } else if (lastAction.getType() == ActionLog.ActionType.DELETE) {
            // Undo a DELETE action -> RE-ADD record without tracking undo
            addRecord(lastAction.getRecord(), false);
            res.put("message", "Undo successful: Restored deleted student " + lastAction.getRecord().getName());
        }

        res.put("success", true);
        return res;
    }

    public StudentRecord getByRollNo(int rollNo) {
        return rollMap.get(rollNo); // HashMap lookup
    }

    public List<StudentRecord> getAllRecords() { return recordList; }
    public SortedSet<StudentRecord> getLeaderboard() { return leaderboardTree; }
    public Deque<ActionLog> getUndoStack() { return undoStack; }

    public Map<String, Object> getCollectionsStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("ArrayList (Total Records)", recordList.size());
        stats.put("HashMap (Indexed Roll Keys)", rollMap.size());
        stats.put("TreeSet (Leaderboard Rank)", leaderboardTree.size());
        stats.put("ArrayDeque (Undo Stack Count)", undoStack.size());
        return stats;
    }
}

// ==========================================
// 3. HTTP SERVER & API
// ==========================================

public class StudentRecordApp {
    private static final int PORT = 8084;
    private static final StudentRecordService service = new StudentRecordService();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/", new StaticUIHandler());
        server.createContext("/api/records", new RecordsApiHandler());
        server.createContext("/api/undo", new UndoApiHandler());
        server.createContext("/api/stats", new StatsApiHandler());

        server.setExecutor(null);
        System.out.println("==================================================================");
        System.out.println("  STUDENT RECORD SYSTEM MINI PROJECT (Java Collections Framework)");
        System.out.println("  Server started on http://localhost:" + PORT);
        System.out.println("==================================================================");
        server.start();
    }

    // ---------------------------------------------------------
    // HANDLERS
    // ---------------------------------------------------------

    static class StaticUIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            String html = getFrontendHTML();
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class RecordsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if (method.equalsIgnoreCase("GET")) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("records", service.getAllRecords());
                resp.put("leaderboard", service.getLeaderboard());
                resp.put("stats", service.getCollectionsStats());
                
                List<String> undoList = new ArrayList<>();
                for (ActionLog log : service.getUndoStack()) {
                    undoList.add(log.getDescription());
                }
                resp.put("undoLogs", undoList);

                sendJsonResponse(exchange, 200, toJson(resp));
            } 
            else if (method.equalsIgnoreCase("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> p = parseJsonOrForm(body);

                int roll = Integer.parseInt(p.get("rollNo"));
                String name = p.get("name");
                String dept = p.get("department");
                double math = Double.parseDouble(p.get("mathMarks"));
                double sci = Double.parseDouble(p.get("scienceMarks"));
                double eng = Double.parseDouble(p.get("englishMarks"));

                StudentRecord rec = new StudentRecord(roll, name, dept, math, sci, eng);
                boolean added = service.addRecord(rec, true);

                Map<String, Object> resp = new HashMap<>();
                resp.put("success", added);
                resp.put("message", added ? "Record added & pushed to ArrayDeque Undo Stack!" : "Roll number already exists in HashMap!");

                sendJsonResponse(exchange, added ? 201 : 400, toJson(resp));
            } 
            else if (method.equalsIgnoreCase("DELETE")) {
                String query = exchange.getRequestURI().getQuery();
                String rollStr = getQueryParam(query, "rollNo");

                if (rollStr != null) {
                    int roll = Integer.parseInt(rollStr);
                    boolean deleted = service.deleteRecord(roll, true);
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("success", deleted);
                    resp.put("message", deleted ? "Record deleted & pushed to ArrayDeque Undo Stack!" : "Record not found!");
                    sendJsonResponse(exchange, 200, toJson(resp));
                } else {
                    sendResponse(exchange, 400, "Missing rollNo parameter");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class UndoApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                Map<String, Object> res = service.undoLastAction();
                sendJsonResponse(exchange, 200, toJson(res));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class StatsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJsonResponse(exchange, 200, toJson(service.getCollectionsStats()));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    // ---------------------------------------------------------
    // UTILITY METHODS
    // ---------------------------------------------------------

    private static String getQueryParam(String queryStr, String key) {
        if (queryStr == null) return null;
        for (String pair : queryStr.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equals(key)) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
        byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        sendResponse(exchange, statusCode, json);
    }

    private static Map<String, String> parseJsonOrForm(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.trim().isEmpty()) return map;

        if (body.startsWith("{")) {
            String clean = body.replaceAll("[{}\"]", "");
            for (String pair : clean.split(",")) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    map.put(kv[0].trim(), kv[1].trim());
                }
            }
        } else {
            for (String pair : body.split("&")) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    map.put(kv[0].trim(), java.net.URLDecoder.decode(kv[1].trim(), StandardCharsets.UTF_8));
                }
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + escapeJson((String) obj) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();

        if (obj instanceof List || obj instanceof Set || obj instanceof Queue) {
            Collection<?> col = (Collection<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            int i = 0;
            for (Object item : col) {
                sb.append(toJson(item));
                if (++i < col.size()) sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        }

        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            int count = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append("\"").append(escapeJson(entry.getKey().toString())).append("\":");
                sb.append(toJson(entry.getValue()));
                if (++count < map.size()) sb.append(",");
            }
            sb.append("}");
            return sb.toString();
        }

        if (obj instanceof StudentRecord) {
            StudentRecord r = (StudentRecord) obj;
            return String.format("{\"rollNo\":%d,\"name\":\"%s\",\"department\":\"%s\",\"mathMarks\":%.1f,\"scienceMarks\":%.1f,\"englishMarks\":%.1f,\"totalMarks\":%.1f,\"percentage\":%.2f,\"grade\":\"%s\"}",
                    r.getRollNo(), escapeJson(r.getName()), escapeJson(r.getDepartment()),
                    r.getMathMarks(), r.getScienceMarks(), r.getEnglishMarks(),
                    r.getTotalMarks(), r.getPercentage(), escapeJson(r.getGrade()));
        }

        return "\"" + escapeJson(obj.toString()) + "\"";
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    // ---------------------------------------------------------
    // FRONTEND HTML UI
    // ---------------------------------------------------------

    private static String getFrontendHTML() {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Student Record System Mini Project - Java Collections</title>
            <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap" rel="stylesheet">
            <style>
                :root {
                    --bg-dark: #0b0f19;
                    --card-bg: #151d30;
                    --accent-violet: #8b5cf6;
                    --accent-indigo: #6366f1;
                    --text-light: #f8fafc;
                    --text-muted: #94a3b8;
                    --border-color: #1e293b;
                }
                * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Outfit', sans-serif; }
                body { background: var(--bg-dark); color: var(--text-light); padding: 2rem; min-height: 100vh; }
                .container { max-width: 1350px; margin: 0 auto; }
                
                header {
                    text-align: center;
                    margin-bottom: 2rem;
                    padding-bottom: 1.5rem;
                    border-bottom: 1px solid var(--border-color);
                }
                header h1 {
                    font-size: 2.6rem;
                    background: linear-gradient(135deg, var(--accent-violet), var(--accent-indigo));
                    -webkit-background-clip: text;
                    -webkit-text-fill-color: transparent;
                    margin-bottom: 0.5rem;
                }
                header p { color: var(--text-muted); font-size: 1.1rem; }

                .stats-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                    gap: 1.2rem;
                    margin-bottom: 2rem;
                }
                .stat-card {
                    background: var(--card-bg);
                    border: 1px solid var(--border-color);
                    border-radius: 12px;
                    padding: 1.25rem;
                }
                .stat-card .label { font-size: 0.8rem; color: var(--accent-violet); text-transform: uppercase; font-weight: 700; }
                .stat-card .value { font-size: 1.8rem; font-weight: 700; margin-top: 0.3rem; }

                .main-layout {
                    display: grid;
                    grid-template-columns: 1fr 2.6fr;
                    gap: 2rem;
                }
                @media(max-width: 1000px) { .main-layout { grid-template-columns: 1fr; } }

                .card {
                    background: var(--card-bg);
                    border: 1px solid var(--border-color);
                    border-radius: 16px;
                    padding: 1.5rem;
                    box-shadow: 0 10px 25px -5px rgba(0,0,0,0.4);
                }
                .card-title {
                    font-size: 1.2rem;
                    font-weight: 600;
                    margin-bottom: 1.2rem;
                    color: var(--text-light);
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }

                .form-group { margin-bottom: 0.9rem; }
                .form-group label { display: block; font-size: 0.85rem; color: var(--text-muted); margin-bottom: 0.3rem; }
                .form-group input, .form-group select {
                    width: 100%;
                    padding: 0.65rem 0.9rem;
                    background: #0b0f19;
                    border: 1px solid var(--border-color);
                    border-radius: 8px;
                    color: white;
                }

                .marks-row { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 0.5rem; }

                button.btn {
                    width: 100%;
                    padding: 0.75rem;
                    background: linear-gradient(135deg, var(--accent-violet), var(--accent-indigo));
                    border: none;
                    border-radius: 8px;
                    color: white;
                    font-weight: 700;
                    font-size: 0.95rem;
                    cursor: pointer;
                    transition: opacity 0.2s;
                }
                button.btn:hover { opacity: 0.9; }

                button.btn-undo {
                    background: #334155;
                    color: #f8fafc;
                    padding: 0.5rem 1rem;
                    font-size: 0.85rem;
                    border-radius: 6px;
                    border: 1px solid var(--border-color);
                    cursor: pointer;
                    width: auto;
                }

                table { width: 100%; border-collapse: collapse; margin-top: 0.5rem; }
                th, td { padding: 0.8rem 0.9rem; text-align: left; border-bottom: 1px solid var(--border-color); font-size: 0.9rem; }
                th { color: var(--text-muted); background: rgba(0,0,0,0.3); font-weight: 500; }

                .grade-badge {
                    padding: 0.2rem 0.5rem; border-radius: 6px; font-weight: 700; font-size: 0.8rem;
                }
                .grade-A_PLUS, .grade-A { background: rgba(139, 92, 246, 0.2); color: #a78bfa; border: 1px solid #8b5cf6; }
                .grade-B, .grade-C { background: rgba(59, 130, 246, 0.2); color: #60a5fa; border: 1px solid #3b82f6; }
                .grade-D, .grade-F { background: rgba(239, 68, 68, 0.2); color: #f87171; border: 1px solid #ef4444; }

                .undo-log-item {
                    background: #0b0f19; padding: 0.6rem 0.8rem; border-left: 3px solid var(--accent-violet);
                    border-radius: 4px; font-size: 0.85rem; margin-bottom: 0.5rem; color: var(--text-muted);
                }

                .tab-header { display: flex; gap: 1rem; margin-bottom: 1rem; border-bottom: 1px solid var(--border-color); }
                .tab-header button {
                    background: none; border: none; color: var(--text-muted); padding: 0.6rem 1rem;
                    font-weight: 600; cursor: pointer; font-size: 0.95rem;
                }
                .tab-header button.active { color: var(--accent-violet); border-bottom: 2px solid var(--accent-violet); }
            </style>
        </head>
        <body>
            <div class="container">
                <header>
                    <h1>🎓 Student Record System Mini Project</h1>
                    <p>Java Collections Framework Integration (ArrayList, HashMap, ArrayDeque Undo Stack & TreeSet Leaderboard)</p>
                </header>

                <div class="stats-grid" id="statsGrid">
                    <!-- Dynamic Collections Stats -->
                </div>

                <div class="main-layout">
                    <!-- Left: Form & Undo History -->
                    <div>
                        <div class="card" style="margin-bottom: 1.5rem;">
                            <div class="card-title">📝 Add Student Record</div>
                            <form id="recordForm">
                                <div class="form-group">
                                    <label>Roll Number (HashMap Key)</label>
                                    <input type="number" id="rollNo" placeholder="e.g. 106" required>
                                </div>
                                <div class="form-group">
                                    <label>Student Name</label>
                                    <input type="text" id="name" placeholder="e.g. Jane Doe" required>
                                </div>
                                <div class="form-group">
                                    <label>Department / Major</label>
                                    <input type="text" id="department" placeholder="Computer Science" required>
                                </div>
                                <div class="form-group">
                                    <label>Subject Marks (Out of 100)</label>
                                    <div class="marks-row">
                                        <input type="number" id="mathMarks" placeholder="Math" min="0" max="100" required>
                                        <input type="number" id="scienceMarks" placeholder="Science" min="0" max="100" required>
                                        <input type="number" id="englishMarks" placeholder="English" min="0" max="100" required>
                                    </div>
                                </div>
                                <button type="submit" class="btn">Save Record</button>
                            </form>
                        </div>

                        <div class="card">
                            <div class="card-title">
                                <span>↩️ Audit & Undo (ArrayDeque)</span>
                                <button class="btn-undo" onclick="undoLast()">Undo Last Action</button>
                            </div>
                            <div id="undoLogList">
                                <!-- Dynamic Undo stack -->
                            </div>
                        </div>
                    </div>

                    <!-- Right: Records Table & TreeSet Leaderboard -->
                    <div class="card">
                        <div class="tab-header">
                            <button class="active" onclick="switchTab('recordsTab', this)">📋 Records (ArrayList + HashMap Index)</button>
                            <button onclick="switchTab('leaderboardTab', this)">🏆 Top Performers (TreeSet Sorted)</button>
                        </div>

                        <!-- Records Tab -->
                        <div id="recordsTab">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Roll & Name</th>
                                        <th>Department</th>
                                        <th>Marks (M/S/E)</th>
                                        <th>Total & %</th>
                                        <th>Grade</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody id="recordsTable">
                                    <!-- Dynamic rows -->
                                </tbody>
                            </table>
                        </div>

                        <!-- Leaderboard Tab -->
                        <div id="leaderboardTab" style="display:none;">
                            <p style="font-size:0.85rem; color:var(--text-muted); margin-bottom:1rem;">
                                Automatically sorted by Total Marks descending using <strong>TreeSet & Comparable Interface</strong>.
                            </p>
                            <table>
                                <thead>
                                    <tr>
                                        <th>Rank</th>
                                        <th>Roll & Name</th>
                                        <th>Department</th>
                                        <th>Total Marks</th>
                                        <th>Percentage</th>
                                    </tr>
                                </thead>
                                <tbody id="leaderboardTable">
                                    <!-- Dynamic rows -->
                                </tbody>
                            </table>
                        </div>

                    </div>
                </div>
            </div>

            <script>
                async function loadData() {
                    const res = await fetch('/api/records');
                    const data = await res.json();

                    // Stats Grid
                    const stats = data.stats;
                    const statsGrid = document.getElementById('statsGrid');
                    statsGrid.innerHTML = Object.entries(stats).map(([k, v]) => `
                        <div class="stat-card">
                            <div class="label">${k.split(' ')[0]}</div>
                            <div class="value">${v}</div>
                            <div style="font-size:0.8rem; color:var(--text-muted);">${k.substring(k.indexOf(' ')+1)}</div>
                        </div>
                    `).join('');

                    // Records Table
                    const tbody = document.getElementById('recordsTable');
                    tbody.innerHTML = data.records.map(r => `
                        <tr>
                            <td>
                                <strong>${r.name}</strong><br>
                                <span style="font-size:0.75rem; color:var(--text-muted);">Roll #${r.rollNo}</span>
                            </td>
                            <td>${r.department}</td>
                            <td>${r.mathMarks} / ${r.scienceMarks} / ${r.englishMarks}</td>
                            <td><strong>${r.totalMarks}</strong> (${r.percentage}%)</td>
                            <td>
                                <span class="grade-badge grade-${r.grade.replace('+', '_PLUS')}">${r.grade}</span>
                            </td>
                            <td>
                                <button style="background:#ef4444; color:white; border:none; padding:0.3rem 0.6rem; border-radius:4px; cursor:pointer;"
                                        onclick="deleteRecord(${r.rollNo})">Delete</button>
                            </td>
                        </tr>
                    `).join('');

                    // Leaderboard Table
                    const lbody = document.getElementById('leaderboardTable');
                    lbody.innerHTML = data.leaderboard.map((r, idx) => `
                        <tr>
                            <td><strong>#${idx + 1}</strong></td>
                            <td><strong>${r.name}</strong> (Roll ${r.rollNo})</td>
                            <td>${r.department}</td>
                            <td><strong>${r.totalMarks}</strong> / 300</td>
                            <td><span style="color:var(--accent-violet); font-weight:700;">${r.percentage}%</span></td>
                        </tr>
                    `).join('');

                    // Undo Logs Stack (ArrayDeque LIFO order)
                    const logContainer = document.getElementById('undoLogList');
                    if (data.undoLogs.length === 0) {
                        logContainer.innerHTML = '<div style="font-size:0.85rem; color:var(--text-muted);">Undo stack is currently empty.</div>';
                    } else {
                        logContainer.innerHTML = data.undoLogs.map(log => `
                            <div class="undo-log-item">${log}</div>
                        `).join('');
                    }
                }

                document.getElementById('recordForm').addEventListener('submit', async (e) => {
                    e.preventDefault();
                    const payload = {
                        rollNo: document.getElementById('rollNo').value,
                        name: document.getElementById('name').value,
                        department: document.getElementById('department').value,
                        mathMarks: document.getElementById('mathMarks').value,
                        scienceMarks: document.getElementById('scienceMarks').value,
                        englishMarks: document.getElementById('englishMarks').value
                    };

                    const res = await fetch('/api/records', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(payload)
                    });

                    const data = await res.json();
                    alert(data.message);
                    if (data.success) {
                        document.getElementById('recordForm').reset();
                        loadData();
                    }
                });

                async function deleteRecord(rollNo) {
                    if (!confirm(`Are you sure you want to delete student roll #${rollNo}?`)) return;

                    const res = await fetch(`/api/records?rollNo=${rollNo}`, { method: 'DELETE' });
                    const data = await res.json();
                    alert(data.message);
                    loadData();
                }

                async function undoLast() {
                    const res = await fetch('/api/undo', { method: 'POST' });
                    const data = await res.json();
                    alert(data.message);
                    loadData();
                }

                function switchTab(tabId, btn) {
                    document.querySelectorAll('.tab-header button').forEach(b => b.classList.remove('active'));
                    btn.classList.add('active');
                    document.getElementById('recordsTab').style.display = tabId === 'recordsTab' ? 'block' : 'none';
                    document.getElementById('leaderboardTab').style.display = tabId === 'leaderboardTab' ? 'block' : 'none';
                }

                loadData();
            </script>
        </body>
        </html>
        """;
    }
}
