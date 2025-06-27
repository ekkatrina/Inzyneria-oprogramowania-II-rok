package services;

import dao.BookDAO;
import dao.LoanDAO;
import java.sql.SQLException;
import java.util.List;
import models.Book;
import models.Loan;

public class ReportService {
    public static List<Book> getAvailableBooks() {
        try {
            return BookDAO.getAllBooks().stream()
                    .filter(Book::isAvailable)
                    .toList();
        } catch (SQLException e) {
            throw new RuntimeException("Błąd pobierania książek");
        }
    }

    public static List<Loan> getActiveLoans() {
        try {
            return LoanDAO.getAllLoans().stream()
                    .filter(loan -> loan.getReturnDate() == null)
                    .toList();
        } catch (SQLException e) {
            throw new RuntimeException("Błąd pobierania wypożyczeń");
        }
    }
}