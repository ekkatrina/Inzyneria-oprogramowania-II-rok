package services;

import dao.BookDAO;
import dao.LoanDAO;
import dao.UserDAO;
import models.Loan;
import models.User;
import utils.Database;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibraryService {

    public static void borrowBook(int bookId, int readerId, int librarianId) {
        try {
            User librarian = UserDAO.findUserById(librarianId);
            if (librarian == null) {
                throw new RuntimeException("Bibliotekarz nie istnieje");
            }

            if (!librarian.getRole().equals("LIBRARIAN") &&
                    !librarian.getRole().equals("ADMIN")) {
                throw new RuntimeException("Tylko bibliotekarz lub administrator może wypożyczać książki");
            }

            Loan loan = new Loan(bookId, readerId, librarianId, LocalDate.now());
            LoanDAO.addLoan(loan);

            BookDAO.markAsUnavailable(bookId);

        } catch (SQLException e) {
            throw new RuntimeException("Błąd bazy danych podczas wypożyczania: " + e.getMessage());
        }
    }

    public static void returnBook(int loanId) throws SQLException {
        Loan loan = LoanDAO.findLoanById(loanId);
        if (loan == null) {
            throw new RuntimeException("Wypożyczenie nie istnieje");
        }

        if (loan.getStatus() != Loan.Status.ISSUED) {
            throw new RuntimeException("Można zwracać tylko wydane książki");
        }

        loan.setStatus(Loan.Status.RETURNED);
        loan.setReturnDate(LocalDate.now());

        BookDAO.markAsAvailable(loan.getBookId());

        LoanDAO.updateLoan(loan);
    }

    public static Map<String, Integer> getLibraryStatistics() throws Exception {
        Map<String, Integer> stats = new HashMap<>();
        try {
            stats.put("books", BookDAO.getTotalBooksCount());
            stats.put("users", UserDAO.getTotalUsersCount());
            stats.put("activeLoans", LoanDAO.getActiveLoansCount());
            stats.put("overdueLoans", LoanDAO.getOverdueLoansCount());
            return stats;
        } catch (SQLException e) {
            throw new Exception("Błąd bazy danych");
        }
    }

    public static void requestBook(int bookId, int readerId) {
        try {
            Loan loan = new Loan(bookId, readerId, -1, LocalDate.now());
            loan.setStatus(Loan.Status.PENDING);
            LoanDAO.addLoan(loan);
        } catch (SQLException e) {
            throw new RuntimeException("Błąd przy składaniu zamówienia: " + e.getMessage());
        }
    }

    public static void rejectLoan(int loanId, int librarianId) throws SQLException {
        User librarian = UserDAO.findUserById(librarianId);
        if (librarian == null) {
            throw new RuntimeException("Библиотекарь не найден");
        }

        if (!librarian.getRole().equals("LIBRARIAN") &&
                !librarian.getRole().equals("ADMIN")) {
            throw new RuntimeException("Только библиотекарь или администратор может отклонять заявки");
        }

        Loan loan = LoanDAO.findLoanById(loanId);
        if (loan == null) {
            throw new RuntimeException("Заявка не найдена");
        }

        if (loan.getStatus() != Loan.Status.PENDING) {
            throw new RuntimeException("Можно отклонять только заявки в статусе Ожидает");
        }

        loan.setStatus(Loan.Status.REJECTED);
        loan.setLibrarianId(librarianId);

        BookDAO.markAsAvailable(loan.getBookId());

        LoanDAO.updateLoan(loan);
    }

    public static void approveLoan(int loanId, int librarianId) throws SQLException {
        Loan loan = LoanDAO.findLoanById(loanId);
        if (loan == null) {
            throw new RuntimeException("Wypożyczenie nie istnieje");
        }

        if (loan.getStatus() != Loan.Status.PENDING) {
            throw new RuntimeException("Można zatwierdzać tylko wnioski w statusie Oczekuje");
        }

        loan.setStatus(Loan.Status.APPROVED);
        loan.setLibrarianId(librarianId);
        LoanDAO.updateLoan(loan);
    }

    public static void issueBook(int loanId) throws SQLException {
        Loan loan = LoanDAO.findLoanById(loanId);
        if (loan == null) {
            throw new RuntimeException("Wypożyczenie nie istnieje");
        }

        if (loan.getStatus() != Loan.Status.APPROVED) {
            throw new RuntimeException("Można wydawać tylko zatwierdzone książki");
        }

        loan.setStatus(Loan.Status.ISSUED);
        loan.setDueDate(LocalDate.now().plusWeeks(2));

        BookDAO.markAsUnavailable(loan.getBookId());

        LoanDAO.updateLoan(loan);
    }

    public static List<Loan> getPendingLoans() throws SQLException {
        return LoanDAO.findLoansByStatus(Loan.Status.PENDING);
    }
}