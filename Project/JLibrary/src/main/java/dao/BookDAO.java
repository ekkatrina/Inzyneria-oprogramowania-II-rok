package dao;

import models.Book;
import org.json.JSONObject;
import utils.Database;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {
    public static void addBook(Book book) throws SQLException {
        String sql = "INSERT INTO books (title, author, isbn, is_available) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setString(3, book.getIsbn());
            stmt.setBoolean(4, book.isAvailable());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    book.setId(rs.getInt(1));
                }
            }
        }
    }

    public static List<Book> getAllBooks() throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Book book = new Book();
                book.setId(rs.getInt("id"));
                book.setTitle(rs.getString("title"));
                book.setAuthor(rs.getString("author"));
                book.setIsbn(rs.getString("isbn"));
                book.setAvailable(rs.getBoolean("is_available"));
                books.add(book);
            }
        }
        return books;
    }

    public static Book findBookByISBN(String isbn) throws SQLException {
        String sql = "SELECT * FROM books WHERE isbn = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, isbn);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Book book = new Book();
                    book.setId(rs.getInt("id"));
                    book.setTitle(rs.getString("title"));
                    book.setAuthor(rs.getString("author"));
                    book.setIsbn(rs.getString("isbn"));
                    book.setAvailable(rs.getBoolean("is_available"));
                    return book;
                }
            }
        }
        return null; 
    }

    public static Book fetchBookFromAPI(String isbn) throws SQLException {
        try {
            String url = "https://openlibrary.org/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONObject bookData = json.optJSONObject("ISBN:" + isbn);

                if (bookData != null) {
                    String title = bookData.getString("title");
                    String author = "Nieznany";
                    if (bookData.has("authors") && bookData.getJSONArray("authors").length() > 0) {
                        author = bookData.getJSONArray("authors").getJSONObject(0).getString("name");
                    }

                    Book newBook = new Book(0, title, author, isbn, true);
                    addBook(newBook);
                    return newBook;
                }
            }
        } catch (Exception e) {
            throw new SQLException("Błąd podczas pobierania danych z API: " + e.getMessage());
        }
        return null;
    }

    public static void markAsUnavailable(int bookId) throws SQLException {
        String sql = "UPDATE books SET is_available = FALSE WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            stmt.executeUpdate();
        }
    }

    public static void markAsAvailable(int bookId) throws SQLException {
        String sql = "UPDATE books SET is_available = TRUE WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            stmt.executeUpdate();
        }
    }

    public static int getTotalBooksCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM books";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static Book findBookById(int id) throws SQLException {
        String sql = "SELECT * FROM books WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Book book = new Book();
                    book.setId(rs.getInt("id"));
                    book.setTitle(rs.getString("title"));
                    book.setAuthor(rs.getString("author"));
                    book.setIsbn(rs.getString("isbn"));
                    book.setAvailable(rs.getBoolean("is_available"));
                    return book;
                }
            }
        }
        return null;
    }

    public static List<Book> searchBooks(String query, boolean onlyAvailable) throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE (title LIKE ? OR author LIKE ?)";

        if (onlyAvailable) {
            sql += " AND is_available = TRUE";
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + query + "%");
            stmt.setString(2, "%" + query + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Book book = new Book();
                    book.setId(rs.getInt("id"));
                    book.setTitle(rs.getString("title"));
                    book.setAuthor(rs.getString("author"));
                    book.setIsbn(rs.getString("isbn"));
                    book.setAvailable(rs.getBoolean("is_available"));
                    books.add(book);
                }
            }
        }
        return books;
    }

    public static boolean deleteBook(int bookId) throws SQLException {
        String checkSql = "SELECT is_available FROM books WHERE id = ?";
        String deleteSql = "DELETE FROM books WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, bookId);

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getBoolean("is_available")) {
                    try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                        deleteStmt.setInt(1, bookId);
                        int rowsDeleted = deleteStmt.executeUpdate();
                        return rowsDeleted > 0;
                    }
                }
            }
        }
        return false;
    }
}