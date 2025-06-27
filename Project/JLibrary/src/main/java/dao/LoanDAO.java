package dao;

import models.Loan;
import utils.Database;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LoanDAO {
    public static void addLoan(Loan loan) throws SQLException {
        String sql = "INSERT INTO loans (book_id, reader_id, librarian_id, loan_date, status, due_date) " +
                "VALUES (?, ?, NULL, ?, ?, NULL)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, loan.getBookId());
            stmt.setInt(2, loan.getReaderId());
            stmt.setDate(3, Date.valueOf(loan.getLoanDate()));
            stmt.setString(4, loan.getStatus().name());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    loan.setId(rs.getInt(1));
                }
            }
        }
    }

    public static List<Loan> findLoansByStatus(Loan.Status status) throws SQLException {
        List<Loan> loans = new ArrayList<>();
        String sql = "SELECT * FROM loans WHERE status = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Loan loan = mapLoanFromResultSet(rs);
                    loans.add(loan);
                }
            }
        }
        return loans;
    }

    public static void updateLoan(Loan loan) throws SQLException {
        String sql = "UPDATE loans SET book_id = ?, reader_id = ?, librarian_id = ?, " +
                "loan_date = ?, return_date = ?, status = ?, due_date = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, loan.getBookId());
            stmt.setInt(2, loan.getReaderId());
            stmt.setInt(3, loan.getLibrarianId());
            stmt.setDate(4, Date.valueOf(loan.getLoanDate()));
            stmt.setDate(5, loan.getReturnDate() != null ? Date.valueOf(loan.getReturnDate()) : null);
            stmt.setString(6, loan.getStatus().name());
            stmt.setDate(7, loan.getDueDate() != null ? Date.valueOf(loan.getDueDate()) : null);
            stmt.setInt(8, loan.getId());

            stmt.executeUpdate();
        }
    }

    private static Loan mapLoanFromResultSet(ResultSet rs) throws SQLException {
        Loan loan = new Loan();
        loan.setId(rs.getInt("id"));
        loan.setBookId(rs.getInt("book_id"));
        loan.setReaderId(rs.getInt("reader_id"));
        loan.setLibrarianId(rs.getInt("librarian_id"));
        loan.setLoanDate(rs.getDate("loan_date").toLocalDate());
        if (rs.getDate("return_date") != null) {
            loan.setReturnDate(rs.getDate("return_date").toLocalDate());
        }
        loan.setStatus(Loan.Status.valueOf(rs.getString("status")));
        if (rs.getDate("due_date") != null) {
            loan.setDueDate(rs.getDate("due_date").toLocalDate());
        }
        return loan;
    }

    public static void markAsReturned(int loanId) throws SQLException {
        String sql = "UPDATE loans SET return_date = CURRENT_DATE WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, loanId);
            stmt.executeUpdate();
        }
    }

    public static Loan findLoanById(int loanId) throws SQLException {
        String sql = "SELECT * FROM loans WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, loanId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapLoanFromResultSet(rs);
                }
            }
        }
        return null;
    }

    public static List<Loan> getAllLoans() throws SQLException {
        List<Loan> loans = new ArrayList<>();
        String sql = "SELECT * FROM loans";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Loan loan = mapLoanFromResultSet(rs);
                loans.add(loan);
            }
        }
        return loans;
    }

    public static int getActiveLoansCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM loans WHERE is_returned = FALSE";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static List<Loan> findLoansByReader(int readerId) throws SQLException {
        List<Loan> loans = new ArrayList<>();
        String sql = "SELECT * FROM loans WHERE reader_id = ? ORDER BY loan_date DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, readerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    loans.add(mapLoanFromResultSet(rs));
                }
            }
        }
        return loans;
    }

    public static int getOverdueLoansCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM loans WHERE return_date IS NULL AND loan_date < ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(LocalDate.now().minusDays(14)));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}