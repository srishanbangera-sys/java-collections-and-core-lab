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
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return String.format("RollNo: %-5d | Name: %-15s | Dept: %-20s | GPA: %.2f | Email: %s",
                rollNumber, name, department, gpa, email);
    }
}

public class StudentHashMapDemo {

    /**
     * Executes HashMap operations: put, get, putIfAbsent, getOrDefault, computeIfPresent, keySet, entrySet, remove, replace.
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
        printHeader("ACTIVE STUDENT ROSTER IN HASHMAP");
        studentMap.values().forEach(stu -> System.out.println(" -> " + stu));

        // ---------------------------------------------------------------------------------------
        // STEP 6: INTERACTIVE STUDENT MANAGEMENT (Scanner Input)
        // ---------------------------------------------------------------------------------------
        printHeader("6. INTERACTIVE STUDENT MANAGEMENT (Delete & Update)");

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n---------------------------------------------------------");
            System.out.println("  HASHMAP STUDENT MANAGEMENT MENU");
            System.out.println("---------------------------------------------------------");
            System.out.println("  1. View All Students (studentMap.values())");
            System.out.println("  2. Add New Student (studentMap.put)");
            System.out.println("  3. Update Student Record (studentMap.replace)");
            System.out.println("  4. Delete Student Record (studentMap.remove)");
            System.out.println("  5. Search Student (studentMap.get)");
            System.out.println("  6. Exit");
            System.out.print("Select an option (1-6): ");

            try {
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) continue;
                int choice = Integer.parseInt(input);

                switch (choice) {
                    case 1:
                        System.out.println("\n=== Current Student Roster (Total: " + studentMap.size() + ") ===");
                        if (studentMap.isEmpty()) {
                            System.out.println("No records in HashMap.");
                        } else {
                            studentMap.forEach((roll, stu) -> System.out.println("  " + stu));
                        }
                        break;

                    case 2:
                        System.out.print("Enter Roll Number: ");
                        int roll = Integer.parseInt(scanner.nextLine().trim());
                        if (studentMap.containsKey(roll)) {
                            System.out.println("Roll Number " + roll + " already exists in HashMap!");
                            break;
                        }
                        System.out.print("Enter Name: ");
                        String name = scanner.nextLine().trim();
                        System.out.print("Enter Department: ");
                        String dept = scanner.nextLine().trim();
                        System.out.print("Enter GPA: ");
                        double gpa = Double.parseDouble(scanner.nextLine().trim());
                        System.out.print("Enter Email: ");
                        String email = scanner.nextLine().trim();

                        Student newStu = new Student(roll, name, dept, gpa, email);
                        studentMap.put(roll, newStu);
                        System.out.println("SUCCESS: Added student using put(" + roll + ", ...)");
                        break;

                    case 3:
                        System.out.print("Enter Roll Number to Update: ");
                        int updateRoll = Integer.parseInt(scanner.nextLine().trim());
                        Student existing = studentMap.get(updateRoll);
                        if (existing == null) {
                            System.out.println("ERROR: No student found with Roll Number " + updateRoll);
                            break;
                        }
                        System.out.println("Current Details: " + existing);
                        System.out.print("Enter New Name (or press Enter to keep '" + existing.getName() + "'): ");
                        String newName = scanner.nextLine().trim();
                        if (!newName.isEmpty()) existing.setName(newName);

                        System.out.print("Enter New Department (or press Enter to keep '" + existing.getDepartment() + "'): ");
                        String newDept = scanner.nextLine().trim();
                        if (!newDept.isEmpty()) existing.setDepartment(newDept);

                        System.out.print("Enter New GPA (or press Enter to keep " + existing.getGpa() + "): ");
                        String newGpaStr = scanner.nextLine().trim();
                        if (!newGpaStr.isEmpty()) existing.setGpa(Double.parseDouble(newGpaStr));

                        System.out.print("Enter New Email (or press Enter to keep '" + existing.getEmail() + "'): ");
                        String newEmail = scanner.nextLine().trim();
                        if (!newEmail.isEmpty()) existing.setEmail(newEmail);

                        studentMap.replace(updateRoll, existing);
                        System.out.println("SUCCESS: Updated student record using replace(" + updateRoll + ", ...)");
                        System.out.println("Updated Record: " + studentMap.get(updateRoll));
                        break;

                    case 4:
                        System.out.print("Enter Roll Number to Delete: ");
                        int delRoll = Integer.parseInt(scanner.nextLine().trim());
                        Student removed = studentMap.remove(delRoll);
                        if (removed != null) {
                            System.out.println("SUCCESS: Deleted student '" + removed.getName() + "' (Roll " + delRoll + ") using remove(" + delRoll + ")");
                        } else {
                            System.out.println("ERROR: No student found with Roll Number " + delRoll);
                        }
                        break;

                    case 5:
                        System.out.print("Enter Roll Number to Search: ");
                        int sRoll = Integer.parseInt(scanner.nextLine().trim());
                        Student found = studentMap.get(sRoll);
                        if (found != null) {
                            System.out.println("FOUND -> " + found);
                        } else {
                            System.out.println("NOT FOUND: No record for Roll Number " + sRoll);
                        }
                        break;

                    case 6:
                        running = false;
                        System.out.println("Exiting HashMap Student System.");
                        break;

                    default:
                        System.out.println("Invalid option! Please enter a number between 1 and 6.");
                        break;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input format! Please enter valid numeric input.");
            }
        }

        System.out.println("\n==========================================================================================");
        System.out.println("                            HASHMAP DEMO COMPLETED SUCCESSFULLY");
        System.out.println("==========================================================================================");
    }

    private static void printHeader(String title) {
        System.out.println("\n>>> " + title);
        System.out.println("------------------------------------------------------------------------------------------");
    }
}
