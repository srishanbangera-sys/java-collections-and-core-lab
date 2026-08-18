package studentgrading;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

// ==========================================
// 1. DEMONSTRATION OF ANNOTATIONS
// ==========================================

/**
 * Custom Runtime Annotation to document Course metadata.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@interface CourseInfo {
    String courseCode() default "CS101";
    String department() default "Computer Science";
    int credits() default 3;
}

/**
 * Custom Annotation to define grading policy parameters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface GradePolicy {
    double minPassScore() default 50.0;
    double maxScore() default 100.0;
    boolean allowsRetake() default true;
}

/**
 * Marker Annotation for audited methods inspected via reflection.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface AuditLog {
    String action() default "GRADE_COMPUTATION";
}

// ==========================================
// 2. DEMONSTRATION OF ENUMERATIONS (ENUM)
// ==========================================

/**
 * Enum representing letter grades with grade points and score ranges.
 */
enum Grade {
    A_PLUS("A+", 4.0, 90.0, 100.0, "Outstanding Performance"),
    A("A", 3.7, 80.0, 89.9, "Excellent Performance"),
    B("B", 3.0, 70.0, 79.9, "Good Performance"),
    C("C", 2.0, 60.0, 69.9, "Satisfactory Performance"),
    D("D", 1.0, 50.0, 59.9, "Pass"),
    F("F", 0.0, 0.0, 49.9, "Fail - Needs Retake");

    private final String letter;
    private final double gradePoint;
    private final double minScore;
    private final double maxScore;
    private final String description;

    Grade(String letter, double gradePoint, double minScore, double maxScore, String description) {
        this.letter = letter;
        this.gradePoint = gradePoint;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.description = description;
    }

    public String getLetter() { return letter; }
    public double getGradePoint() { return gradePoint; }
    public double getMinScore() { return minScore; }
    public double getMaxScore() { return maxScore; }
    public String getDescription() { return description; }

    /**
     * Determines Grade Enum based on double score.
     */
    public static Grade fromScore(double score) {
        for (Grade g : Grade.values()) {
            if (score >= g.minScore && score <= g.maxScore) {
                return g;
            }
        }
        return score > 100.0 ? A_PLUS : F;
    }
}

/**
 * Enum for Academic Standing.
 */
enum AcademicStanding {
    DEANS_LIST("Dean's High Honors List", "#10b981"),
    GOOD_STANDING("Good Academic Standing", "#3b82f6"),
    SATISFACTORY("Satisfactory", "#f59e0b"),
    ACADEMIC_PROBATION("Academic Probation", "#ef4444");

    private final String label;
    private final String colorHex;

    AcademicStanding(String label, String colorHex) {
        this.label = label;
        this.colorHex = colorHex;
    }

    public String getLabel() { return label; }
    public String getColorHex() { return colorHex; }

    public static AcademicStanding computeStanding(double gpa) {
        if (gpa >= 3.5) return DEANS_LIST;
        if (gpa >= 3.0) return GOOD_STANDING;
        if (gpa >= 2.0) return SATISFACTORY;
        return ACADEMIC_PROBATION;
    }
}

// ==========================================
// 3. STUDENT MODEL & AUTOBOXING SERVICE
// ==========================================

class StudentGradeRecord {
    private String id;
    private String name;
    private String courseName;
    
    // DEMONSTRATION OF AUTOBOXING:
    // Using Wrapper classes (Double, Integer) for fields to show implicit conversion
    private Double rawScore;     // Wrapper Double
    private Integer credits;     // Wrapper Integer
    private Grade grade;         // Enum
    private Double gradePoint;   // Wrapper Double

    public StudentGradeRecord(String id, String name, String courseName, Double rawScore, Integer credits) {
        this.id = id;
        this.name = name;
        this.courseName = courseName;
        this.credits = credits;
        
        // AUTOBOXING / UNBOXING DEMONSTRATION:
        // rawScore (Double) is unboxed to primitive double when passed to Grade.fromScore(double)
        this.rawScore = rawScore;
        this.grade = Grade.fromScore(rawScore); // Implicit unboxing Double -> double
        
        // Primitive double returned by getGradePoint() is autoboxed into wrapper Double
        this.gradePoint = this.grade.getGradePoint(); // Autoboxing double -> Double
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCourseName() { return courseName; }
    public Double getRawScore() { return rawScore; }
    public Integer getCredits() { return credits; }
    public Grade getGrade() { return grade; }
    public Double getGradePoint() { return gradePoint; }
}

@CourseInfo(courseCode = "CS302", department = "Software Engineering", credits = 4)
@GradePolicy(minPassScore = 50.0, maxScore = 100.0, allowsRetake = true)
class GradeCalculatorService {

    /**
     * Demonstrates Autoboxing and Unboxing in mathematical calculations.
     */
    @AuditLog(action = "GPA_COMPUTATION_AUTOBOXED")
    public static Map<String, Object> calculateSummary(List<StudentGradeRecord> records) {
        // Wrapper object initializations (Autoboxing primitive values)
        Double totalWeightedPoints = 0.0; // Primitive double 0.0 autoboxed to Double
        Integer totalCredits = 0;         // Primitive int 0 autoboxed to Integer
        
        for (StudentGradeRecord rec : records) {
            // UNBOXING: rec.getGradePoint() (Double) and rec.getCredits() (Integer) are unboxed to primitives for arithmetic
            double pt = rec.getGradePoint(); // Unboxing Double -> double
            int cred = rec.getCredits();     // Unboxing Integer -> int

            // Computation using primitives
            double courseWeighted = pt * cred;

            // AUTOBOXING: Result added to Double & Integer wrappers
            totalWeightedPoints += courseWeighted; // Unboxed, added, autoboxed back to Double
            totalCredits += cred;                 // Unboxed, added, autoboxed back to Integer
        }

        // Unboxing Double and Integer for division
        double gpa = (totalCredits > 0) ? (totalWeightedPoints / totalCredits) : 0.0;
        
        // Compute Standing Enum
        AcademicStanding standing = AcademicStanding.computeStanding(gpa);

        Map<String, Object> result = new HashMap<>();
        result.put("gpa", Double.valueOf(Math.round(gpa * 100.0) / 100.0)); // Explicit Autoboxing
        result.put("totalCredits", totalCredits);
        result.put("totalPoints", Double.valueOf(Math.round(totalWeightedPoints * 100.0) / 100.0));
        result.put("standing", standing.getLabel());
        result.put("standingColor", standing.getColorHex());
        return result;
    }
    
    /**
     * Reflective Inspector to extract annotations at runtime.
     */
    public static List<Map<String, String>> inspectAnnotations() {
        List<Map<String, String>> list = new ArrayList<>();

        // Inspect Class-level Annotations
        Class<?> clazz = GradeCalculatorService.class;
        if (clazz.isAnnotationPresent(CourseInfo.class)) {
            CourseInfo info = clazz.getAnnotation(CourseInfo.class);
            Map<String, String> map = new LinkedHashMap<>();
            map.put("Target", "Class: GradeCalculatorService");
            map.put("Annotation", "@CourseInfo");
            map.put("Details", "CourseCode=" + info.courseCode() + ", Dept=" + info.department() + ", Credits=" + info.credits());
            list.add(map);
        }

        if (clazz.isAnnotationPresent(GradePolicy.class)) {
            GradePolicy policy = clazz.getAnnotation(GradePolicy.class);
            Map<String, String> map = new LinkedHashMap<>();
            map.put("Target", "Class: GradeCalculatorService");
            map.put("Annotation", "@GradePolicy");
            map.put("Details", "MinPass=" + policy.minPassScore() + ", MaxScore=" + policy.maxScore() + ", AllowsRetake=" + policy.allowsRetake());
            list.add(map);
        }

        // Inspect Method-level Annotations
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(AuditLog.class)) {
                AuditLog audit = method.getAnnotation(AuditLog.class);
                Map<String, String> map = new LinkedHashMap<>();
                map.put("Target", "Method: " + method.getName() + "()");
                map.put("Annotation", "@AuditLog");
                map.put("Details", "Action=" + audit.action());
                list.add(map);
            }
        }

        return list;
    }
}

// ==========================================
// 4. MAIN APPLICATION & EMBEDDED HTTP SERVER
// ==========================================

public class StudentGradingApp {
    private static final int PORT = 8081;
    private static final List<StudentGradeRecord> records = new CopyOnWriteArrayList<>();

    static {
        // Seed Initial Data using Autoboxing (primitive numbers autoboxed to Double & Integer)
        records.add(new StudentGradeRecord("STU101", "Alice Smith", "Data Structures", 94.5, 4));
        records.add(new StudentGradeRecord("STU102", "Bob Johnson", "Java Programming", 83.0, 3));
        records.add(new StudentGradeRecord("STU103", "Charlie Brown", "Database Systems", 72.0, 3));
        records.add(new StudentGradeRecord("STU104", "Diana Prince", "Computer Networks", 58.0, 3));
        records.add(new StudentGradeRecord("STU105", "Evan Wright", "Operating Systems", 42.0, 4));
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Serve Frontend UI
        server.createContext("/", new StaticUIHandler());
        
        // REST API Endpoints
        server.createContext("/api/students", new StudentsApiHandler());
        server.createContext("/api/annotations", new AnnotationsApiHandler());

        server.setExecutor(null);
        System.out.println("==================================================================");
        System.out.println("  STUDENT GRADING SYSTEM (Enums, Autoboxing & Annotations Demo)");
        System.out.println("  Server started on http://localhost:" + PORT);
        System.out.println("==================================================================");
        server.start();
    }

    // ---------------------------------------------------------
    // HTTP HANDLERS
    // ---------------------------------------------------------

    static class StaticUIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 450, "Method Not Allowed");
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

    static class StudentsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if (method.equalsIgnoreCase("GET")) {
                // Return JSON list of records and summary
                Map<String, Object> responseData = new HashMap<>();
                
                List<Map<String, Object>> studentList = new ArrayList<>();
                for (StudentGradeRecord r : records) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("name", r.getName());
                    m.put("courseName", r.getCourseName());
                    m.put("rawScore", r.getRawScore());       // Double wrapper
                    m.put("credits", r.getCredits());         // Integer wrapper
                    m.put("gradeLetter", r.getGrade().getLetter()); // Enum method
                    m.put("gradePoint", r.getGradePoint());   // Double wrapper
                    m.put("description", r.getGrade().getDescription()); // Enum method
                    studentList.add(m);
                }

                responseData.put("students", studentList);
                responseData.put("summary", GradeCalculatorService.calculateSummary(records));

                sendJsonResponse(exchange, 200, toJson(responseData));
            } 
            else if (method.equalsIgnoreCase("POST")) {
                // Read request body to add student
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseFormOrJson(body);

                String id = params.getOrDefault("id", "STU" + (records.size() + 101));
                String name = params.getOrDefault("name", "Unknown");
                String course = params.getOrDefault("courseName", "General Course");
                
                // AUTOBOXING DEMONSTRATION:
                // Double.valueOf() converts primitive/String into Double wrapper object explicitly
                Double score = Double.valueOf(params.getOrDefault("rawScore", "0.0"));
                Integer cred = Integer.valueOf(params.getOrDefault("credits", "3"));

                StudentGradeRecord newRec = new StudentGradeRecord(id, name, course, score, cred);
                records.add(newRec);

                Map<String, Object> resp = new HashMap<>();
                resp.put("success", true);
                resp.put("message", "Student record added successfully!");
                sendJsonResponse(exchange, 201, toJson(resp));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class AnnotationsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                List<Map<String, String>> annotations = GradeCalculatorService.inspectAnnotations();
                sendJsonResponse(exchange, 200, toJson(annotations));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    // ---------------------------------------------------------
    // UTILITY METHODS
    // ---------------------------------------------------------

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

    private static Map<String, String> parseFormOrJson(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.trim().isEmpty()) return map;

        if (body.startsWith("{")) {
            // Simple manual JSON string key-value parser for basic payloads
            String clean = body.replaceAll("[{}\"]", "");
            String[] pairs = clean.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    map.put(kv[0].trim(), kv[1].trim());
                }
            }
        } else {
            // Form URL encoded
            String[] pairs = body.split("&");
            for (String pair : pairs) {
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
        
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                sb.append(toJson(list.get(i)));
                if (i < list.size() - 1) sb.append(",");
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

        return "\"" + escapeJson(obj.toString()) + "\"";
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ---------------------------------------------------------
    // FRONTEND HTML/CSS/JS UI
    // ---------------------------------------------------------

    private static String getFrontendHTML() {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Student Grading System - Enums, Autoboxing & Annotations</title>
            <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap" rel="stylesheet">
            <style>
                :root {
                    --bg-dark: #0f172a;
                    --card-bg: #1e293b;
                    --accent-blue: #38bdf8;
                    --accent-purple: #a855f7;
                    --text-light: #f8fafc;
                    --text-muted: #94a3b8;
                    --border-color: #334155;
                }
                * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Outfit', sans-serif; }
                body { background: var(--bg-dark); color: var(--text-light); padding: 2rem; min-height: 100vh; }
                .container { max-width: 1200px; margin: 0 auto; }
                
                header {
                    text-align: center;
                    margin-bottom: 2.5rem;
                    padding-bottom: 1.5rem;
                    border-bottom: 1px solid var(--border-color);
                }
                header h1 {
                    font-size: 2.5rem;
                    background: linear-gradient(135deg, var(--accent-blue), var(--accent-purple));
                    -webkit-background-clip: text;
                    -webkit-text-fill-color: transparent;
                    margin-bottom: 0.5rem;
                }
                header p { color: var(--text-muted); font-size: 1.1rem; }

                .badge-container {
                    display: flex;
                    justify-content: center;
                    gap: 1rem;
                    margin-top: 1rem;
                    flex-wrap: wrap;
                }
                .tech-badge {
                    background: rgba(56, 189, 248, 0.1);
                    border: 1px solid var(--accent-blue);
                    color: var(--accent-blue);
                    padding: 0.4rem 1rem;
                    border-radius: 9999px;
                    font-size: 0.85rem;
                    font-weight: 600;
                    letter-spacing: 0.5px;
                }

                .dashboard-grid {
                    display: grid;
                    grid-template-columns: 1fr 2fr;
                    gap: 2rem;
                }
                @media(max-width: 900px) { .dashboard-grid { grid-template-columns: 1fr; } }

                .card {
                    background: var(--card-bg);
                    border: 1px solid var(--border-color);
                    border-radius: 16px;
                    padding: 1.5rem;
                    box-shadow: 0 10px 25px -5px rgba(0,0,0,0.3);
                }
                .card-title {
                    font-size: 1.25rem;
                    font-weight: 600;
                    margin-bottom: 1.2rem;
                    color: var(--text-light);
                    display: flex;
                    align-items: center;
                    gap: 0.5rem;
                }

                .form-group { margin-bottom: 1rem; }
                .form-group label { display: block; font-size: 0.9rem; color: var(--text-muted); margin-bottom: 0.4rem; }
                .form-group input, .form-group select {
                    width: 100%;
                    padding: 0.75rem 1rem;
                    background: #0f172a;
                    border: 1px solid var(--border-color);
                    border-radius: 8px;
                    color: white;
                    font-size: 0.95rem;
                }
                .form-group input:focus { outline: none; border-color: var(--accent-blue); }

                button.btn {
                    width: 100%;
                    padding: 0.8rem;
                    background: linear-gradient(135deg, var(--accent-blue), var(--accent-purple));
                    border: none;
                    border-radius: 8px;
                    color: white;
                    font-weight: 600;
                    font-size: 1rem;
                    cursor: pointer;
                    transition: transform 0.2s, opacity 0.2s;
                }
                button.btn:hover { opacity: 0.9; transform: translateY(-2px); }

                .summary-banner {
                    display: flex;
                    justify-content: space-between;
                    background: linear-gradient(135deg, rgba(56, 189, 248, 0.1), rgba(168, 85, 247, 0.1));
                    border: 1px solid var(--border-color);
                    border-radius: 12px;
                    padding: 1.2rem 1.5rem;
                    margin-bottom: 1.5rem;
                }
                .summary-item { text-align: center; }
                .summary-item .label { font-size: 0.85rem; color: var(--text-muted); }
                .summary-item .value { font-size: 1.6rem; font-weight: 700; color: var(--text-light); margin-top: 0.2rem; }

                table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
                th, td { padding: 0.85rem 1rem; text-align: left; border-bottom: 1px solid var(--border-color); font-size: 0.95rem; }
                th { color: var(--text-muted); font-weight: 500; background: rgba(0,0,0,0.2); }

                .grade-badge {
                    display: inline-block;
                    padding: 0.25rem 0.6rem;
                    border-radius: 6px;
                    font-weight: 700;
                    font-size: 0.85rem;
                }
                .grade-A_PLUS, .grade-A { background: rgba(16, 185, 129, 0.2); color: #10b981; border: 1px solid #10b981; }
                .grade-B { background: rgba(59, 130, 246, 0.2); color: #3b82f6; border: 1px solid #3b82f6; }
                .grade-C { background: rgba(245, 158, 11, 0.2); color: #f59e0b; border: 1px solid #f59e0b; }
                .grade-D { background: rgba(234, 179, 8, 0.2); color: #eab308; border: 1px solid #eab308; }
                .grade-F { background: rgba(239, 68, 68, 0.2); color: #ef4444; border: 1px solid #ef4444; }

                .tab-nav { display: flex; gap: 1rem; margin-bottom: 1rem; border-bottom: 1px solid var(--border-color); }
                .tab-nav button {
                    background: none; border: none; color: var(--text-muted);
                    padding: 0.6rem 1rem; cursor: pointer; font-size: 1rem; font-weight: 600;
                }
                .tab-nav button.active { color: var(--accent-blue); border-bottom: 2px solid var(--accent-blue); }

                .annotation-card {
                    background: #0f172a;
                    border-left: 4px solid var(--accent-purple);
                    padding: 1rem;
                    margin-bottom: 0.8rem;
                    border-radius: 6px;
                }
                .annotation-card h4 { color: var(--accent-blue); margin-bottom: 0.3rem; }
                .annotation-card code { color: #f472b6; font-family: monospace; font-size: 0.9rem; }
            </style>
        </head>
        <body>
            <div class="container">
                <header>
                    <h1>🎓 Student Grading System</h1>
                    <p>Demonstrating Java Enumerations, Autoboxing/Unboxing & Runtime Annotations</p>
                    <div class="badge-container">
                        <span class="tech-badge">Java Enum (Grade & Standing)</span>
                        <span class="tech-badge">Autoboxing (Double & Integer)</span>
                        <span class="tech-badge">Annotations (@CourseInfo, @GradePolicy)</span>
                        <span class="tech-badge">Reflection API</span>
                    </div>
                </header>

                <div class="dashboard-grid">
                    <!-- Left Form Panel -->
                    <div class="card">
                        <div class="card-title">➕ Add Student Grade</div>
                        <form id="gradeForm">
                            <div class="form-group">
                                <label>Student ID</label>
                                <input type="text" id="stuId" placeholder="e.g. STU106" required>
                            </div>
                            <div class="form-group">
                                <label>Student Name</label>
                                <input type="text" id="stuName" placeholder="e.g. John Doe" required>
                            </div>
                            <div class="form-group">
                                <label>Course Name</label>
                                <input type="text" id="courseName" placeholder="e.g. Web Development" required>
                            </div>
                            <div class="form-group">
                                <label>Raw Score (0 - 100)</label>
                                <input type="number" step="0.1" id="rawScore" placeholder="85.5" required>
                            </div>
                            <div class="form-group">
                                <label>Credits</label>
                                <input type="number" id="credits" value="3" min="1" max="6" required>
                            </div>
                            <button type="submit" class="btn">Calculate & Save Grade</button>
                        </form>
                    </div>

                    <!-- Right Main Content Panel -->
                    <div class="card">
                        <div class="tab-nav">
                            <button class="active" onclick="switchTab('gradesTab', this)">📊 Student Grades & GPA</button>
                            <button onclick="switchTab('annotationsTab', this)">🔍 Runtime Annotations Inspector</button>
                        </div>

                        <!-- Grades Tab -->
                        <div id="gradesTab">
                            <div class="summary-banner">
                                <div class="summary-item">
                                    <div class="label">Overall Cumulative GPA</div>
                                    <div class="value" id="summaryGpa">0.00</div>
                                </div>
                                <div class="summary-item">
                                    <div class="label">Total Credits</div>
                                    <div class="value" id="summaryCredits">0</div>
                                </div>
                                <div class="summary-item">
                                    <div class="label">Academic Standing</div>
                                    <div class="value" id="summaryStanding" style="font-size:1.1rem; margin-top:0.4rem;">-</div>
                                </div>
                            </div>

                            <table>
                                <thead>
                                    <tr>
                                        <th>ID & Name</th>
                                        <th>Course</th>
                                        <th>Score</th>
                                        <th>Grade Enum</th>
                                        <th>Grade Point</th>
                                    </tr>
                                </thead>
                                <tbody id="studentTableBody">
                                    <!-- Dynamic rows -->
                                </tbody>
                            </table>
                        </div>

                        <!-- Annotations Inspector Tab -->
                        <div id="annotationsTab" style="display:none;">
                            <p style="color: var(--text-muted); margin-bottom: 1rem;">
                                The following custom annotations were defined in Java source code and extracted dynamically via <strong>Java Reflection API</strong>:
                            </p>
                            <div id="annotationList">
                                <!-- Dynamic annotations -->
                            </div>
                        </div>

                    </div>
                </div>
            </div>

            <script>
                async function loadGrades() {
                    const res = await fetch('/api/students');
                    const data = await res.json();
                    
                    // Render Summary
                    document.getElementById('summaryGpa').textContent = data.summary.gpa.toFixed(2);
                    document.getElementById('summaryCredits').textContent = data.summary.totalCredits;
                    
                    const standingEl = document.getElementById('summaryStanding');
                    standingEl.textContent = data.summary.standing;
                    standingEl.style.color = data.summary.standingColor;

                    // Render Table
                    const tbody = document.getElementById('studentTableBody');
                    tbody.innerHTML = data.students.map(s => `
                        <tr>
                            <td>
                                <strong>${s.name}</strong><br>
                                <span style="font-size:0.8rem; color:var(--text-muted);">${s.id}</span>
                            </td>
                            <td>${s.courseName} (${s.credits} cr)</td>
                            <td><strong>${s.rawScore}</strong></td>
                            <td>
                                <span class="grade-badge grade-${s.gradeLetter.replace('+', '_PLUS')}">
                                    ${s.gradeLetter}
                                </span>
                                <div style="font-size:0.75rem; color:var(--text-muted);">${s.description}</div>
                            </td>
                            <td><strong>${s.gradePoint.toFixed(1)}</strong></td>
                        </tr>
                    `).join('');
                }

                async function loadAnnotations() {
                    const res = await fetch('/api/annotations');
                    const list = await res.json();
                    
                    const container = document.getElementById('annotationList');
                    container.innerHTML = list.map(item => `
                        <div class="annotation-card">
                            <h4>${item.Annotation} on ${item.Target}</h4>
                            <code>${item.Details}</code>
                        </div>
                    `).join('');
                }

                document.getElementById('gradeForm').addEventListener('submit', async (e) => {
                    e.preventDefault();
                    const payload = {
                        id: document.getElementById('stuId').value,
                        name: document.getElementById('stuName').value,
                        courseName: document.getElementById('courseName').value,
                        rawScore: document.getElementById('rawScore').value,
                        credits: document.getElementById('credits').value
                    };

                    await fetch('/api/students', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(payload)
                    });

                    document.getElementById('gradeForm').reset();
                    loadGrades();
                });

                function switchTab(tabId, btn) {
                    document.querySelectorAll('.tab-nav button').forEach(b => b.classList.remove('active'));
                    btn.classList.add('active');
                    document.getElementById('gradesTab').style.display = tabId === 'gradesTab' ? 'block' : 'none';
                    document.getElementById('annotationsTab').style.display = tabId === 'annotationsTab' ? 'block' : 'none';
                    if (tabId === 'annotationsTab') loadAnnotations();
                }

                loadGrades();
            </script>
        </body>
        </html>
        """;
    }
}
