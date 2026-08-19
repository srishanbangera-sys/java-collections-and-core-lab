package librarymanagement;

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
// MODEL CLASSES
// ==========================================

/**
 * Represents a Book entity in the Library Management System.
 * Implements Comparable interface to support sorted views in TreeSet collections.
 */
class Book implements Comparable<Book> {
    private String isbn;
    private String title;
    private String author;
    private String genre;
    private int year;
    private boolean available;

    public Book(String isbn, String title, String author, String genre, int year) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.year = year;
        this.available = true;
    }

    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getGenre() { return genre; }
    public int getYear() { return year; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setYear(int year) { this.year = year; }

    // Implement Comparable interface for TreeSet ordering by Title
    @Override
    public int compareTo(Book o) {
        int titleComp = this.title.compareToIgnoreCase(o.title);
        if (titleComp != 0) return titleComp;
        return this.isbn.compareTo(o.isbn);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book)) return false;
        Book book = (Book) o;
        return Objects.equals(isbn, book.isbn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isbn);
    }
}

class BorrowRequest {
    private String requestId;
    private String isbn;
    private String borrowerName;
    private long requestTimestamp;

    public BorrowRequest(String requestId, String isbn, String borrowerName) {
        this.requestId = requestId;
        this.isbn = isbn;
        this.borrowerName = borrowerName;
        this.requestTimestamp = System.currentTimeMillis();
    }

    public String getRequestId() { return requestId; }
    public String getIsbn() { return isbn; }
    public String getBorrowerName() { return borrowerName; }
    public long getRequestTimestamp() { return requestTimestamp; }
}

// ==========================================
// LIBRARY SERVICE DEMONSTRATING COLLECTIONS
// ==========================================

/**
 * Service demonstrating multiple components of the Java Collections Framework:
 * ArrayList, HashMap, HashSet, LinkedList (as Queue), and TreeSet.
 */
class LibraryManagerService {

    // 1. ArrayList: Dynamic sequence for book catalog
    private final List<Book> catalogList = new CopyOnWriteArrayList<>();

    // 2. HashMap: O(1) Instant lookup by ISBN
    private final Map<String, Book> isbnMap = new ConcurrentHashMap<>();

    // 3. HashSet: Unique collection of genres
    private final Set<String> genreSet = Collections.synchronizedSet(new HashSet<>());

    // 4. Queue (LinkedList): FIFO Waitlist / Borrow Request Queue
    private final Queue<BorrowRequest> borrowQueue = new LinkedList<>();

    // 5. TreeSet: Automatically sorted view of books by Title
    private final SortedSet<Book> sortedCatalogTree = Collections.synchronizedSortedSet(new TreeSet<>());

    public LibraryManagerService() {
        // Seed Initial Data
        addBook(new Book("978-0134685991", "Effective Java", "Joshua Bloch", "Computer Science", 2018));
        addBook(new Book("978-0596009205", "Head First Design Patterns", "Eric Freeman", "Computer Science", 2004));
        addBook(new Book("978-0132350884", "Clean Code", "Robert C. Martin", "Software Engineering", 2008));
        addBook(new Book("978-0201633610", "Design Patterns (GoF)", "Erich Gamma", "Software Engineering", 1994));
        addBook(new Book("978-0321356680", "Java Concurrency in Practice", "Brian Goetz", "Computer Science", 2006));
    }

    /**
     * Add Book demonstrating ArrayList, HashMap, HashSet, and TreeSet updates.
     */
    public synchronized boolean addBook(Book book) {
        if (isbnMap.containsKey(book.getIsbn())) {
            return false; // ISBN already exists in HashMap
        }

        catalogList.add(book);           // 1. Added to ArrayList
        isbnMap.put(book.getIsbn(), book); // 2. Indexed in HashMap
        genreSet.add(book.getGenre());     // 3. Genre added to HashSet
        sortedCatalogTree.add(book);      // 4. Added to TreeSet (sorted)
        return true;
    }

    /**
     * Search book by ISBN using HashMap O(1) lookup.
     */
    public Book getBookByIsbn(String isbn) {
        return isbnMap.get(isbn); // HashMap lookup
    }

    /**
     * Search books by keyword in Title or Author using ArrayList filtering.
     */
    public List<Book> searchBooks(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(catalogList);
        }
        String q = query.toLowerCase();
        List<Book> results = new ArrayList<>();
        for (Book b : catalogList) {
            if (b.getTitle().toLowerCase().contains(q) || 
                b.getAuthor().toLowerCase().contains(q) || 
                b.getGenre().toLowerCase().contains(q)) {
                results.add(b);
            }
        }
        return results;
    }

    /**
     * Request Borrow: Uses Queue (LinkedList) when book is un-available or queued.
     */
    public synchronized Map<String, Object> requestBorrow(String isbn, String borrowerName) {
        Map<String, Object> res = new HashMap<>();
        Book book = isbnMap.get(isbn);

        if (book == null) {
            res.put("success", false);
            res.put("message", "Book not found!");
            return res;
        }

        if (book.isAvailable()) {
            book.setAvailable(false);
            res.put("success", true);
            res.put("status", "BORROWED");
            res.put("message", "Book successfully issued to " + borrowerName);
        } else {
            // Book is currently issued -> Add to Waitlist Queue (LinkedList)
            String reqId = "REQ-" + (borrowQueue.size() + 101);
            BorrowRequest req = new BorrowRequest(reqId, isbn, borrowerName);
            borrowQueue.offer(req); // Queue offer (FIFO)

            res.put("success", true);
            res.put("status", "QUEUED");
            res.put("message", "Book currently borrowed. " + borrowerName + " added to Waitlist Queue at position #" + borrowQueue.size());
        }
        return res;
    }

    /**
     * Return Book: Fulfills next request in Queue if waitlist is present.
     */
    public synchronized Map<String, Object> returnBook(String isbn) {
        Map<String, Object> res = new HashMap<>();
        Book book = isbnMap.get(isbn);

        if (book == null) {
            res.put("success", false);
            res.put("message", "Book not found!");
            return res;
        }

        // Check if there are pending requests in Queue for this book
        BorrowRequest nextReq = null;
        for (BorrowRequest req : borrowQueue) {
            if (req.getIsbn().equals(isbn)) {
                nextReq = req;
                break;
            }
        }

        if (nextReq != null) {
            borrowQueue.remove(nextReq); // Remove fulfilled request from Queue
            book.setAvailable(false);    // Immediately re-issued to next requester
            res.put("success", true);
            res.put("reissued", true);
            res.put("borrower", nextReq.getBorrowerName());
            res.put("message", "Book returned and automatically re-issued to " + nextReq.getBorrowerName() + " from Waitlist Queue!");
        } else {
            book.setAvailable(true);
            res.put("success", true);
            res.put("reissued", false);
            res.put("message", "Book successfully returned to catalog and available for borrowing.");
        }
        return res;
    }

    /**
     * Delete Book from catalog, HashMap, TreeSet, and Queue, updating HashSet genres.
     */
    public synchronized boolean deleteBook(String isbn) {
        Book book = isbnMap.remove(isbn);
        if (book == null) return false;

        catalogList.remove(book);
        sortedCatalogTree.remove(book);
        borrowQueue.removeIf(req -> req.getIsbn().equals(isbn));

        rebuildGenreSet();
        return true;
    }

    /**
     * Update Book details maintaining TreeSet ordering and HashSet genres.
     */
    public synchronized boolean updateBook(String isbn, String title, String author, String genre, int year) {
        Book book = isbnMap.get(isbn);
        if (book == null) return false;

        sortedCatalogTree.remove(book);

        book.setTitle(title);
        book.setAuthor(author);
        book.setGenre(genre);
        book.setYear(year);

        sortedCatalogTree.add(book);
        rebuildGenreSet();
        return true;
    }

    private void rebuildGenreSet() {
        genreSet.clear();
        for (Book b : catalogList) {
            genreSet.add(b.getGenre());
        }
    }

    // Getters for Collections UI inspection
    public List<Book> getCatalogList() { return catalogList; }
    public Map<String, Book> getIsbnMap() { return isbnMap; }
    public Set<String> getGenreSet() { return genreSet; }
    public Queue<BorrowRequest> getBorrowQueue() { return borrowQueue; }
    public SortedSet<Book> getSortedCatalogTree() { return sortedCatalogTree; }

    public Map<String, Object> getCollectionsStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("ArrayList (Catalog Count)", catalogList.size());
        stats.put("HashMap (Indexed ISBN Keys)", isbnMap.size());
        stats.put("HashSet (Unique Genres)", genreSet.size());
        stats.put("TreeSet (Sorted Items)", sortedCatalogTree.size());
        stats.put("Queue/LinkedList (Waitlist Pending)", borrowQueue.size());
        return stats;
    }
}

// ==========================================
// HTTP SERVER & REST ENDPOINTS
// ==========================================

public class LibraryManagementApp {
    private static final int PORT = 8082;
    private static final LibraryManagerService libraryService = new LibraryManagerService();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/", new StaticUIHandler());
        server.createContext("/api/books", new BooksApiHandler());
        server.createContext("/api/borrow", new BorrowApiHandler());
        server.createContext("/api/return", new ReturnApiHandler());
        server.createContext("/api/collections", new CollectionsApiHandler());

        server.setExecutor(null);
        System.out.println("==================================================================");
        System.out.println("  LIBRARY MANAGEMENT SYSTEM (Java Collections Framework Demo)");
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

    static class BooksApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if (method.equalsIgnoreCase("GET")) {
                String query = getQueryParam(exchange.getRequestURI().getQuery(), "q");
                List<Book> books = libraryService.searchBooks(query);

                Map<String, Object> resp = new HashMap<>();
                resp.put("books", books);
                resp.put("stats", libraryService.getCollectionsStats());
                resp.put("genres", libraryService.getGenreSet());

                sendJsonResponse(exchange, 200, toJson(resp));
            } 
            else if (method.equalsIgnoreCase("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseJsonOrForm(body);

                String isbn = params.get("isbn");
                String title = params.get("title");
                String author = params.get("author");
                String genre = params.get("genre");
                int year = Integer.parseInt(params.getOrDefault("year", "2024"));

                Book newBook = new Book(isbn, title, author, genre, year);
                boolean added = libraryService.addBook(newBook);

                Map<String, Object> resp = new HashMap<>();
                resp.put("success", added);
                resp.put("message", added ? "Book added to ArrayList, HashMap, HashSet & TreeSet!" : "Book with this ISBN already exists in HashMap!");

                sendJsonResponse(exchange, added ? 201 : 400, toJson(resp));
            } 
            else if (method.equalsIgnoreCase("PUT")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseJsonOrForm(body);

                String isbn = params.get("isbn");
                String title = params.get("title");
                String author = params.get("author");
                String genre = params.get("genre");
                int year = Integer.parseInt(params.getOrDefault("year", "2024"));

                boolean updated = libraryService.updateBook(isbn, title, author, genre, year);

                Map<String, Object> resp = new HashMap<>();
                resp.put("success", updated);
                resp.put("message", updated ? "Book updated successfully in HashMap & TreeSet!" : "Book not found!");

                sendJsonResponse(exchange, updated ? 200 : 404, toJson(resp));
            } 
            else if (method.equalsIgnoreCase("DELETE")) {
                String query = exchange.getRequestURI().getQuery();
                String isbn = getQueryParam(query, "isbn");
                if (isbn == null) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, String> params = parseJsonOrForm(body);
                    isbn = params.get("isbn");
                }

                if (isbn != null) {
                    boolean deleted = libraryService.deleteBook(isbn);
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("success", deleted);
                    resp.put("message", deleted ? "Book deleted from ArrayList, HashMap, HashSet & TreeSet!" : "Book not found!");
                    sendJsonResponse(exchange, deleted ? 200 : 404, toJson(resp));
                } else {
                    sendResponse(exchange, 400, "Missing isbn parameter");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class BorrowApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseJsonOrForm(body);

                String isbn = params.get("isbn");
                String borrower = params.get("borrower");

                Map<String, Object> res = libraryService.requestBorrow(isbn, borrower);
                sendJsonResponse(exchange, 200, toJson(res));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class ReturnApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseJsonOrForm(body);

                String isbn = params.get("isbn");

                Map<String, Object> res = libraryService.returnBook(isbn);
                sendJsonResponse(exchange, 200, toJson(res));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class CollectionsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                Map<String, Object> collectionsData = new LinkedHashMap<>();
                collectionsData.put("stats", libraryService.getCollectionsStats());
                
                // Waitlist Queue data
                List<Map<String, String>> queueList = new ArrayList<>();
                for (BorrowRequest req : libraryService.getBorrowQueue()) {
                    Map<String, String> m = new HashMap<>();
                    m.put("requestId", req.getRequestId());
                    m.put("isbn", req.getIsbn());
                    m.put("borrower", req.getBorrowerName());
                    Book b = libraryService.getBookByIsbn(req.getIsbn());
                    m.put("bookTitle", b != null ? b.getTitle() : "Unknown");
                    queueList.add(m);
                }
                collectionsData.put("waitlistQueue", queueList);
                collectionsData.put("genresHashSet", libraryService.getGenreSet());
                
                sendJsonResponse(exchange, 200, toJson(collectionsData));
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

        if (obj instanceof Book) {
            Book b = (Book) obj;
            return String.format("{\"isbn\":\"%s\",\"title\":\"%s\",\"author\":\"%s\",\"genre\":\"%s\",\"year\":%d,\"available\":%b}",
                    escapeJson(b.getIsbn()), escapeJson(b.getTitle()), escapeJson(b.getAuthor()),
                    escapeJson(b.getGenre()), b.getYear(), b.isAvailable());
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
            <title>Library Management System - Java Collections Framework</title>
            <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap" rel="stylesheet">
            <style>
                :root {
                    --bg-dark: #090d16;
                    --card-bg: #131b2e;
                    --accent-emerald: #10b981;
                    --accent-cyan: #06b6d4;
                    --text-light: #f8fafc;
                    --text-muted: #94a3b8;
                    --border-color: #1e293b;
                }
                * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Outfit', sans-serif; }
                body { background: var(--bg-dark); color: var(--text-light); padding: 2rem; min-height: 100vh; }
                .container { max-width: 1300px; margin: 0 auto; }
                
                header {
                    text-align: center;
                    margin-bottom: 2.5rem;
                    padding-bottom: 1.5rem;
                    border-bottom: 1px solid var(--border-color);
                }
                header h1 {
                    font-size: 2.5rem;
                    background: linear-gradient(135deg, var(--accent-emerald), var(--accent-cyan));
                    -webkit-background-clip: text;
                    -webkit-text-fill-color: transparent;
                    margin-bottom: 0.5rem;
                }
                header p { color: var(--text-muted); font-size: 1.1rem; }

                .collections-bar {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                    gap: 1rem;
                    margin-bottom: 2rem;
                }
                .stat-box {
                    background: var(--card-bg);
                    border: 1px solid var(--border-color);
                    border-radius: 12px;
                    padding: 1.2rem;
                    position: relative;
                    overflow: hidden;
                }
                .stat-box::before {
                    content: ''; position: absolute; top:0; left:0; width: 4px; height: 100%;
                    background: linear-gradient(to bottom, var(--accent-emerald), var(--accent-cyan));
                }
                .stat-box .type { font-size: 0.8rem; text-transform: uppercase; color: var(--accent-cyan); font-weight: 700; letter-spacing: 1px; }
                .stat-box .title { font-size: 0.95rem; color: var(--text-muted); margin: 0.2rem 0; }
                .stat-box .value { font-size: 1.8rem; font-weight: 700; color: var(--text-light); }

                .dashboard-grid {
                    display: grid;
                    grid-template-columns: 1fr 2.5fr;
                    gap: 2rem;
                }
                @media(max-width: 950px) { .dashboard-grid { grid-template-columns: 1fr; } }

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
                }

                .form-group { margin-bottom: 1rem; }
                .form-group label { display: block; font-size: 0.85rem; color: var(--text-muted); margin-bottom: 0.4rem; }
                .form-group input {
                    width: 100%;
                    padding: 0.75rem 1rem;
                    background: #090d16;
                    border: 1px solid var(--border-color);
                    border-radius: 8px;
                    color: white;
                }
                .form-group input:focus { outline: none; border-color: var(--accent-emerald); }

                button.btn {
                    width: 100%;
                    padding: 0.8rem;
                    background: linear-gradient(135deg, var(--accent-emerald), var(--accent-cyan));
                    border: none;
                    border-radius: 8px;
                    color: #090d16;
                    font-weight: 700;
                    font-size: 0.95rem;
                    cursor: pointer;
                    transition: transform 0.2s;
                }
                button.btn:hover { transform: translateY(-2px); }

                .toolbar { display: flex; gap: 1rem; margin-bottom: 1.2rem; flex-wrap: wrap; }
                .search-input {
                    flex: 1; min-width: 200px;
                    padding: 0.75rem 1rem; background: #090d16; border: 1px solid var(--border-color);
                    border-radius: 8px; color: white;
                }

                table { width: 100%; border-collapse: collapse; margin-top: 0.5rem; }
                th, td { padding: 0.85rem; text-align: left; border-bottom: 1px solid var(--border-color); font-size: 0.9rem; }
                th { color: var(--text-muted); background: rgba(0,0,0,0.3); font-weight: 500; }

                .pill-avail { background: rgba(16, 185, 129, 0.2); color: #10b981; border: 1px solid #10b981; padding: 0.25rem 0.6rem; border-radius: 999px; font-size: 0.75rem; font-weight: 700; }
                .pill-issued { background: rgba(239, 68, 68, 0.2); color: #ef4444; border: 1px solid #ef4444; padding: 0.25rem 0.6rem; border-radius: 999px; font-size: 0.75rem; font-weight: 700; }

                .modal {
                    display: none; position: fixed; top:0; left:0; width:100%; height:100%;
                    background: rgba(0,0,0,0.7); backdrop-filter: blur(4px);
                    justify-content: center; align-items: center; z-index: 100;
                }
                .modal-content {
                    background: var(--card-bg); border: 1px solid var(--border-color);
                    border-radius: 12px; padding: 1.5rem; width: 90%; max-width: 450px;
                }

                .genre-tag {
                    display: inline-block; background: rgba(6, 182, 212, 0.15); color: var(--accent-cyan);
                    padding: 0.3rem 0.7rem; border-radius: 6px; font-size: 0.8rem; margin-right: 0.4rem; margin-bottom: 0.4rem;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <header>
                    <h1>📚 Library Management System</h1>
                    <p>Comprehensive Java Collections Framework Demonstration (ArrayList, HashMap, HashSet, TreeSet, Queue)</p>
                </header>

                <div class="stats-bar" id="statsBar">
                    <!-- Dynamic Collections Stats -->
                </div>

                <div class="dashboard-grid">
                    <div>
                        <div class="card" style="margin-bottom: 1.5rem;">
                            <div class="card-title">➕ Add New Book</div>
                            <form id="addBookForm">
                                <div class="form-group">
                                    <label>ISBN (Unique HashMap Key)</label>
                                    <input type="text" id="isbn" placeholder="978-0134685991" required>
                                </div>
                                <div class="form-group">
                                    <label>Book Title (TreeSet Sorted Field)</label>
                                    <input type="text" id="title" placeholder="Java Performance" required>
                                </div>
                                <div class="form-group">
                                    <label>Author</label>
                                    <input type="text" id="author" placeholder="Scott Oaks" required>
                                </div>
                                <div class="form-group">
                                    <label>Genre (Added to HashSet)</label>
                                    <input type="text" id="genre" placeholder="Computer Science" required>
                                </div>
                                <div class="form-group">
                                    <label>Publication Year</label>
                                    <input type="number" id="year" value="2020" required>
                                </div>
                                <button type="submit" class="btn">Add to Collections Catalog</button>
                            </form>
                        </div>

                        <div class="card">
                            <div class="card-title">🏷️ Unique Genres (HashSet)</div>
                            <div id="genresList"></div>
                        </div>
                    </div>

                    <div>
                        <div class="card" style="margin-bottom: 1.5rem;">
                            <div class="card-title">📚 Catalog (ArrayList + HashMap Indexing)</div>
                            <div class="toolbar">
                                <input type="text" class="search-input" id="searchInput" placeholder="Search title, author or genre..." oninput="loadBooks()">
                            </div>
                            <table>
                                <thead>
                                    <tr>
                                        <th>ISBN & Title</th>
                                        <th>Author & Genre</th>
                                        <th>Year</th>
                                        <th>Status</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody id="booksTable">
                                </tbody>
                            </table>
                        </div>

                        <div class="card">
                            <div class="card-title">⏳ Active Waitlist Queue (LinkedList / FIFO Queue)</div>
                            <table style="margin-top:0;">
                                <thead>
                                    <tr>
                                        <th>Request ID</th>
                                        <th>Book Title</th>
                                        <th>Borrower</th>
                                    </tr>
                                </thead>
                                <tbody id="queueTable">
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            <div class="modal" id="borrowModal">
                <div class="modal-content">
                    <h3 style="margin-bottom: 1rem;">Borrow Book</h3>
                    <input type="hidden" id="modalIsbn">
                    <div class="form-group">
                        <label>Enter Borrower Name</label>
                        <input type="text" id="modalBorrower" placeholder="e.g. Sarah Connor" required>
                    </div>
                    <div style="display:flex; gap:1rem; margin-top:1.5rem;">
                        <button class="btn" onclick="submitBorrow()">Confirm Request</button>
                        <button class="btn" style="background:#334155; color:white;" onclick="closeModal()">Cancel</button>
                    </div>
                </div>
            </div>

            <div class="modal" id="editBookModal">
                <div class="modal-content">
                    <h3 style="margin-bottom: 1rem;">✏️ Edit Book Details</h3>
                    <form id="editBookForm">
                        <input type="hidden" id="editIsbn">
                        <div class="form-group">
                            <label>Book Title</label>
                            <input type="text" id="editTitle" required>
                        </div>
                        <div class="form-group">
                            <label>Author</label>
                            <input type="text" id="editAuthor" required>
                        </div>
                        <div class="form-group">
                            <label>Genre</label>
                            <input type="text" id="editGenre" required>
                        </div>
                        <div class="form-group">
                            <label>Publication Year</label>
                            <input type="number" id="editYear" required>
                        </div>
                        <div style="display:flex; gap:1rem; margin-top:1.5rem;">
                            <button type="submit" class="btn">Update Book</button>
                            <button type="button" class="btn" style="background:#334155; color:white;" onclick="closeEditModal()">Cancel</button>
                        </div>
                    </form>
                </div>
            </div>

            <script>
                let currentBorrowIsbn = '';
                let cachedBooks = [];

                async function loadBooks() {
                    const q = document.getElementById('searchInput').value;
                    const res = await fetch(`/api/books?q=${encodeURIComponent(q)}`);
                    const data = await res.json();
                    cachedBooks = data.books || [];

                    const stats = data.stats;
                    const statsBar = document.getElementById('statsBar');
                    statsBar.innerHTML = Object.entries(stats).map(([k, v]) => `
                        <div class="stat-box">
                            <div class="type">${k.split(' ')[0]}</div>
                            <div class="title">${k.substring(k.indexOf(' ')+1)}</div>
                            <div class="value">${v}</div>
                        </div>
                    `).join('');

                    const tbody = document.getElementById('booksTable');
                    tbody.innerHTML = cachedBooks.map(b => `
                        <tr>
                            <td><strong>${b.title}</strong><br><span style="font-size:0.75rem; color:var(--text-muted);">${b.isbn}</span></td>
                            <td>${b.author}<br><span style="font-size:0.8rem; color:var(--accent-cyan);">${b.genre}</span></td>
                            <td>${b.year}</td>
                            <td>${b.available ? '<span class="pill-avail">Available</span>' : '<span class="pill-issued">Borrowed</span>'}</td>
                            <td>
                                ${b.available ? 
                                    `<button class="action-btn btn-borrow" onclick="openBorrowModal('${b.isbn}')">Borrow</button>` :
                                    `<button class="action-btn btn-return" onclick="returnBook('${b.isbn}')">Return</button>`
                                }
                                <button class="action-btn btn-edit" onclick="openEditBookModal('${b.isbn}')">Edit</button>
                                <button class="action-btn btn-danger" onclick="deleteBook('${b.isbn}')">Delete</button>
                            </td>
                        </tr>
                    `).join('');

                    const genresDiv = document.getElementById('genresList');
                    genresDiv.innerHTML = data.genres.map(g => `<span class="genre-tag">${g}</span>`).join('');
                    loadQueue();
                }

                async function loadQueue() {
                    const res = await fetch('/api/collections');
                    const data = await res.json();
                    const tbody = document.getElementById('queueTable');
                    tbody.innerHTML = data.waitlistQueue.length === 0 ? '<tr><td colspan="3" style="color:var(--text-muted); text-align:center;">Queue is empty.</td></tr>' : data.waitlistQueue.map(q => `<tr><td><code>${q.requestId}</code></td><td>${q.bookTitle}</td><td><strong>${q.borrower}</strong></td></tr>`).join('');
                }

                document.getElementById('addBookForm').addEventListener('submit', async (e) => {
                    e.preventDefault();
                    const payload = {
                        isbn: document.getElementById('isbn').value, title: document.getElementById('title').value,
                        author: document.getElementById('author').value, genre: document.getElementById('genre').value,
                        year: document.getElementById('year').value
                    };
                    const res = await fetch('/api/books', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
                    const data = await res.json();
                    alert(data.message);
                    if (data.success) { document.getElementById('addBookForm').reset(); loadBooks(); }
                });

                function openBorrowModal(isbn) { currentBorrowIsbn = isbn; document.getElementById('modalIsbn').value = isbn; document.getElementById('borrowModal').style.display = 'flex'; }
                function closeModal() { document.getElementById('borrowModal').style.display = 'none'; }

                function openEditBookModal(isbn) {
                    const book = cachedBooks.find(b => b.isbn === isbn);
                    if (!book) return;
                    document.getElementById('editIsbn').value = book.isbn;
                    document.getElementById('editTitle').value = book.title;
                    document.getElementById('editAuthor').value = book.author;
                    document.getElementById('editGenre').value = book.genre;
                    document.getElementById('editYear').value = book.year;
                    document.getElementById('editBookModal').style.display = 'flex';
                }
                function closeEditModal() { document.getElementById('editBookModal').style.display = 'none'; }

                document.getElementById('editBookForm').addEventListener('submit', async (e) => {
                    e.preventDefault();
                    const payload = { isbn: document.getElementById('editIsbn').value, title: document.getElementById('editTitle').value, author: document.getElementById('editAuthor').value, genre: document.getElementById('editGenre').value, year: document.getElementById('editYear').value };
                    const res = await fetch('/api/books', { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
                    const data = await res.json();
                    alert(data.message);
                    closeEditModal();
                    loadBooks();
                });

                async function deleteBook(isbn) {
                    if (!confirm('Are you sure you want to delete book with ISBN ' + isbn + '?')) return;
                    const res = await fetch('/api/books?isbn=' + encodeURIComponent(isbn), { method: 'DELETE' });
                    const data = await res.json();
                    alert(data.message);
                    loadBooks();
                }

                async function submitBorrow() {
                    const borrower = document.getElementById('modalBorrower').value;
                    if (!borrower) return alert('Please enter borrower name');
                    const res = await fetch('/api/borrow', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ isbn: currentBorrowIsbn, borrower }) });
                    const data = await res.json();
                    alert(data.message);
                    closeModal();
                    loadBooks();
                }

                async function returnBook(isbn) {
                    const res = await fetch('/api/return', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ isbn }) });
                    const data = await res.json();
                    alert(data.message);
                    loadBooks();
                }

                loadBooks();
            </script>
        </body>
        </html>
        """;
    }
}
