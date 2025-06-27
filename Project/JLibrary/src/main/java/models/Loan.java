package models;

import java.time.LocalDate;

public class Loan {
    public enum Status {
        PENDING, APPROVED, REJECTED, ISSUED, RETURNED
    }

    private Status status;
    private LocalDate dueDate;
    private int id;
    private int bookId;
    private int readerId;
    private int librarianId;
    private boolean returned;
    private LocalDate loanDate;
    private LocalDate returnDate;

    public Loan() {}

    public Loan(int bookId, int readerId, int librarianId) {
        this.bookId = bookId;
        this.readerId = readerId;
        this.librarianId = librarianId;
        this.loanDate = LocalDate.now();
    }

    public Loan(int bookId, int readerId, int librarianId, LocalDate loanDate) {
        this.bookId = bookId;
        this.readerId = readerId;
        this.librarianId = librarianId;
        this.loanDate = loanDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public int getReaderId() {
        return readerId;
    }

    public void setReaderId(int readerId) {
        this.readerId = readerId;
    }

    public int getLibrarianId() {
        return librarianId;
    }

    public void setLibrarianId(int librarianId) {
        this.librarianId = librarianId;
    }

    public LocalDate getLoanDate() {
        return loanDate;
    }

    public void setLoanDate(LocalDate loanDate) {
        this.loanDate = loanDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public boolean isReturned() {
        return returned;
    }

    public void setReturned(boolean returned) {
        this.returned = returned;
    }
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}