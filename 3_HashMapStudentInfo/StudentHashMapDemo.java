package studenthashmap;

import java.util.*;

/**
 * Project 3: HashMap Student Information System (Console Application)
 * Demonstrates storing and managing Student records in a HashMap using Roll Numbers as Keys.
 */
/**
 * Student model for HashMap key-value mapping.
 * Uses integer rollNumber as unique lookup key.
 */
class Student {
    private int rollNumber;
    private String name;
    private String department;
    private double gpa;
    private String email;

    public Student(int rollNumber, String name, String department, double gpa, String email) {
        this.rollNumber = rollNumber;
        this.name = name;
        this.department = department;
        this.gpa = gpa;
        this.email = email;
    }

    public int getRollNumber() { return rollNumber; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public double getGpa() { return gpa; }
    public String getEmail() { return email; }

    public void setGpa(double gpa) { this.gpa = gpa; }
    public void setDepartment(String department) { this.department = department; }

    @Override
    public String toString() {
        return String.format("RollNo: %-5d | Name: %-15s | Dept: %-20s | GPA: %.2f | Email: %s",
                rollNumber, name, department, gpa, email);
    }
}

public class StudentHashMapDemo {

    /**
     * Executes HashMap operations: put, get, putIfAbsent, getOrDefault, computeIfPresent, keySet, entrySet.
     */
    public static void main(String[] args) {
        System.out.println("==========================================================================================");
        System.out.println("                   DEMONSTRATION OF HASHMAP IN JAVA (STUDENT INFO)");
        System.out.println("==========================================================================================");

        // 1. Instantiating HashMap (Roll Number -> Student object mapping)
        Map<Integer, Student> studentMap = new HashMap<>();

        // ---------------------------------------------------------------------------------------
        // STEP 1: INSERTION USING put() AND putIfAbsent()
        // ---------------------------------------------------------------------------------------
        printHeader("1. INSERTING STUDENT RECORDS (put & putIfAbsent)");
        
        studentMap.put(101, new Student(101, "Alice Smith", "Computer Science", 3.85, "alice@univ.edu"));
        studentMap.put(102, new Student(102, "Bob Johnson", "Electrical Eng", 3.42, "bob@univ.edu"));
        studentMap.put(103, new Student(103, "Charlie Davis", "Mechanical Eng", 3.15, "charlie@univ.edu"));
        studentMap.put(104, new Student(104, "Diana Prince", "Computer Science", 3.95, "diana@univ.edu"));
        studentMap.put(105, new Student(105, "Evan Wright", "Civil Engineering", 2.90, "evan@univ.edu"));

        System.out.println("Successfully added 5 initial student records.");
        System.out.println("Current Total Records in HashMap: " + studentMap.size());

        // Demonstrating putIfAbsent (Will not overwrite existing key 101)
        Student duplicateAlice = new Student(101, "Alice Fake", "Art", 1.0, "fake@univ.edu");
        Student previousValue = studentMap.putIfAbsent(101, duplicateAlice);
        System.out.println("\nExecuting putIfAbsent(101, ...): Existing record retained! Existing -> " + previousValue.getName());

        // ---------------------------------------------------------------------------------------
        // STEP 2: LOOKUP & SEARCH OPERATIONS
        // ---------------------------------------------------------------------------------------
        printHeader("2. DIRECT O(1) LOOKUP & SEARCH (get, containsKey, containsValue)");

        int searchRoll = 104;
        System.out.println("Searching for Roll Number " + searchRoll + " using get():");
        Student foundStudent = studentMap.get(searchRoll);
        if (foundStudent != null) {
            System.out.println("  FOUND -> " + foundStudent);
        }

        // Checking Key existence
        int checkRoll = 999;
        System.out.println("\nChecking containsKey(" + checkRoll + "): " + studentMap.containsKey(checkRoll));

        // Safe query using getOrDefault
        Student defaultStudent = studentMap.getOrDefault(checkRoll, 
                new Student(999, "Not Found", "N/A", 0.0, "n/a"));
        System.out.println("Executing getOrDefault(" + checkRoll + "): " + defaultStudent.getName());

        // ---------------------------------------------------------------------------------------
        // STEP 3: UPDATING RECORDS (replace & computeIfPresent)
        // ---------------------------------------------------------------------------------------
        printHeader("3. UPDATING RECORDS (replace & computeIfPresent)");

        System.out.println("Original Record for Roll 102: " + studentMap.get(102));
        
        // Update GPA for student 102
        Student updatedBob = new Student(102, "Bob Johnson", "Data Science", 3.65, "bob@univ.edu");
        studentMap.replace(102, updatedBob);
        System.out.println("Updated Record for Roll 102 (after replace): " + studentMap.get(102));

        // Functional update using computeIfPresent (Boost GPA for Roll 103)
        studentMap.computeIfPresent(103, (roll, stu) -> {
            stu.setGpa(stu.getGpa() + 0.2);
            return stu;
        });
        System.out.println("Updated GPA for Roll 103 (computeIfPresent +0.2): " + studentMap.get(103));

        // ---------------------------------------------------------------------------------------
        // STEP 4: ITERATION TECHNIQUES (keySet, values, entrySet)
        // ---------------------------------------------------------------------------------------
        printHeader("4. ITERATING OVER HASHMAP");

        System.out.println("a) Iterating via keySet() [Roll Numbers]:");
        System.out.print("   Roll Numbers: ");
        for (Integer roll : studentMap.keySet()) {
            System.out.print("[" + roll + "] ");
        }
        System.out.println();

        System.out.println("\nb) Iterating via entrySet() [Key-Value Pairs]:");
        System.out.println("------------------------------------------------------------------------------------------");
        for (Map.Entry<Integer, Student> entry : studentMap.entrySet()) {
            System.out.println("   Key (RollNo): " + entry.getKey() + " ===> Value: " + entry.getValue());
        }
        System.out.println("------------------------------------------------------------------------------------------");

        // ---------------------------------------------------------------------------------------
        // STEP 5: DELETION AND BULK OPERATIONS
        // ---------------------------------------------------------------------------------------
        printHeader("5. DELETION & REMOVAL OPERATIONS");

        int removeRoll = 105;
        Student removedStudent = studentMap.remove(removeRoll);
        System.out.println("Removed Student with Roll Number " + removeRoll + ": " + removedStudent.getName());
        System.out.println("Total Records after removal: " + studentMap.size());

        // Final Active Roster Print
        printHeader("FINAL ACTIVE STUDENT ROSTER IN HASHMAP");
        studentMap.values().forEach(stu -> System.out.println(" -> " + stu));

        System.out.println("\n==========================================================================================");
        System.out.println("                            HASHMAP DEMO COMPLETED SUCCESSFULLY");
        System.out.println("==========================================================================================");
    }

    private static void printHeader(String title) {
        System.out.println("\n>>> " + title);
        System.out.println("------------------------------------------------------------------------------------------");
    }
}
