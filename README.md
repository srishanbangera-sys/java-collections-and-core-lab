# 🎓 Java Advanced Topics & Collections Projects

This repository contains four separate Java projects demonstrating core Java concepts, object-oriented design, annotations, enums, autoboxing, HashMaps, and the Java Collections Framework.

---

## 📁 Project Overview

### 🛠️ Java Collections & Concepts Mapping

| Collection / Concept | Data Structure / Feature | Used In Module | Purpose / Real-World Use Case |
| :--- | :--- | :--- | :--- |
| `List` / `ArrayList` | Dynamic Array | Library & Student Record | Sequential catalog storage & record listing |
| `Map` / `HashMap` | Hash Table | All Modules | O(1) Instant key-based lookup by ISBN, ID, or Roll No |
| `Set` / `HashSet` | Unique Hash Bucket | Library System | Maintaining distinct genre lists |
| `SortedSet` / `TreeSet` | Red-Black Tree | Library & Student Record | Sorted catalog by Title & Leaderboard by Total Marks |
| `Queue` / `LinkedList` | FIFO Queue | Library System | Waitlist reservation processing |
| `Deque` / `ArrayDeque` | LIFO Stack | Student Record | Operation undo history & audit trail |
| `Enum` | Enumeration | Student Grading | Letter grade definitions, boundaries & GPA points |
| `Annotations & Reflection` | Meta-programming | Student Grading | Dynamic runtime inspection of `@CourseInfo` & `@GradePolicy` |
| `Autoboxing / Unboxing` | Wrapper Conversion | Student Grading | Seamless primitive-wrapper conversions in calculations |

---

### 1. Student Grading System (`1_StudentGradingSystem/`)
- **Key Concepts**: 
  - **Enumerations (`Enum`)**: `Grade` (A+, A, B, C, D, F with points/ranges) & `AcademicStanding`.
  - **Autoboxing / Unboxing**: Implicit conversions between primitives (`double`, `int`) and wrappers (`Double`, `Integer`) in GPA calculations.
  - **Annotations & Reflection**: Custom `@CourseInfo`, `@GradePolicy`, and `@AuditLog` annotations inspected via Reflection API.
- **Interface**: Web-Based UI (Built-in `com.sun.net.httpserver.HttpServer` on port `8081`).
- **How to Run**:
  ```bash
  javac 1_StudentGradingSystem/StudentGradingApp.java
  java -cp 1_StudentGradingSystem studentgrading.StudentGradingApp
  ```
  Open browser at: `http://localhost:8081`

---

### 2. Library Management System (`2_LibraryManagementSystem/`)
- **Key Concepts**:
  - **`List` / `ArrayList`**: Master catalog of books.
  - **`Map` / `HashMap`**: O(1) Instant lookup by ISBN.
  - **`Set` / `HashSet`**: Unique collection of genres.
  - **`SortedSet` / `TreeSet`**: Sorted catalog view by Title using `Comparable`.
  - **`Queue` / `LinkedList`**: FIFO waitlist reservation queue for borrowed books.
- **Interface**: Web-Based UI (Built-in `com.sun.net.httpserver.HttpServer` on port `8082`).
- **How to Run**:
  ```bash
  javac 2_LibraryManagementSystem/LibraryManagementApp.java
  java -cp 2_LibraryManagementSystem librarymanagement.LibraryManagementApp
  ```
  Open browser at: `http://localhost:8082`

---

### 3. HashMap Student Info (`3_HashMapStudentInfo/`)
- **Key Concepts**:
  - **`HashMap<Integer, Student>`**: Roll Numbers as unique Keys mapping to Student objects.
  - **Map Methods**: `put()`, `get()`, `putIfAbsent()`, `getOrDefault()`, `replace()`, `computeIfPresent()`, `remove()`, `keySet()`, `values()`, `entrySet()`.
- **Interface**: Console / Terminal Program (No UI needed).
- **How to Run**:
  ```bash
  javac -d 3_HashMapStudentInfo 3_HashMapStudentInfo/StudentHashMapDemo.java
  java -cp 3_HashMapStudentInfo studenthashmap.StudentHashMapDemo
  ```

---

### 4. Student Record System (`4_StudentRecordSystem/`)
- **Key Concepts**:
  - **`ArrayList`**: Active student record store.
  - **`HashMap`**: Instant indexing by Roll Number.
  - **`ArrayDeque`**: LIFO Stack for operation Undo history & audit log.
  - **`TreeSet`**: Dynamic Leaderboard sorted by Total Marks descending.
- **Interface**: Mini Web Project UI (Built-in `com.sun.net.httpserver.HttpServer` on port `8084`).
- **How to Run**:
  ```bash
  javac 4_StudentRecordSystem/StudentRecordApp.java
  java -cp 4_StudentRecordSystem studentrecord.StudentRecordApp
  ```
  Open browser at: `http://localhost:8084`
