package views.librarian;

import dao.BookDAO;
import dao.LoanDAO;
import dao.UserDAO;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import models.Book;
import models.Loan;
import models.User;
import services.AuthService;
import services.LibraryService;
import views.auth.LoginView;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LibrarianDashboard extends javafx.application.Application {
    private User currentUser;
    private TableView<Loan> loansTable;
    private TableView<Book> booksTable;
    private TextField searchField;
    private TextField bookSearchField;

    @Override
    public void start(Stage stage) {
        currentUser = AuthService.getCurrentUser();

        BorderPane root = new BorderPane();
        root.setTop(createToolbar());
        root.setCenter(createMainView());
        root.setStyle("-fx-background-color: #f8f8f8;");

        Scene scene = new Scene(root, 1200, 800);
        stage.setScene(scene);
        stage.setTitle("Panel Bibliotekarza - " + currentUser.getUsername());
        stage.show();
    }

    private HBox createToolbar() {
        Label welcomeLabel = new Label("Witaj, " + currentUser.getUsername() + "!");
        welcomeLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Button logoutButton = new Button("Wyloguj");
        logoutButton.setStyle("-fx-font-size: 14; -fx-padding: 5 15; -fx-background-color: #555555; -fx-text-fill: white;");
        logoutButton.setOnAction(e -> {
            Stage currentStage = (Stage) logoutButton.getScene().getWindow();
            currentStage.close();
            new LoginView().start(new Stage());
        });

        Button refreshButton = new Button("Odśwież");
        refreshButton.setStyle("-fx-font-size: 14; -fx-padding: 5 15; -fx-background-color: #555555; -fx-text-fill: white;");
        refreshButton.setOnAction(e -> {
            refreshLoansTable();
            refreshBooksTable();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(15, welcomeLabel, spacer, refreshButton, logoutButton);
        toolbar.setPadding(new Insets(15));
        toolbar.setStyle("-fx-background-color: #e0e0e0;");
        return toolbar;
    }

    private TabPane createMainView() {
        TabPane tabPane = new TabPane();

        Tab loansTab = new Tab("Wypożyczenia");
        loansTab.setContent(createLoansView());
        loansTab.setClosable(false);

        Tab booksTab = new Tab("Książki");
        booksTab.setContent(createBooksView());
        booksTab.setClosable(false);

        tabPane.getTabs().addAll(loansTab, booksTab);
        return tabPane;
    }

    private BorderPane createLoansView() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(20));

        HBox searchPanel = new HBox(10);
        searchPanel.setPadding(new Insets(0, 0, 15, 0));
        searchPanel.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("ID czytelnika lub nazwa użytkownika");
        searchField.setPrefWidth(250);

        Button searchButton = new Button("Szukaj");
        searchButton.setStyle("-fx-font-size: 14; -fx-padding: 5 15; -fx-background-color: #555555; -fx-text-fill: white;");
        searchButton.setOnAction(e -> searchLoans(searchField.getText()));

        Button clearButton = new Button("Wyczyść");
        clearButton.setStyle("-fx-font-size: 14; -fx-padding: 5 15;");
        clearButton.setOnAction(e -> {
            searchField.clear();
            refreshLoansTable();
        });

        searchPanel.getChildren().addAll(
                new Label("Wyszukaj czytelnika:"),
                searchField,
                searchButton,
                clearButton
        );

        loansTable = new TableView<>();
        loansTable.setStyle("-fx-font-size: 14px; -fx-background-color: white;");
        loansTable.setPlaceholder(new Label("Ładowanie danych..."));
        loansTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        TableColumn<Loan, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(60);
        idCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Loan, String> bookCol = new TableColumn<>("Książka");
        bookCol.setCellValueFactory(cell -> {
            try {
                Book book = BookDAO.findBookById(cell.getValue().getBookId());
                return new javafx.beans.property.SimpleStringProperty(book != null ? book.getTitle() : "Nieznana");
            } catch (SQLException e) {
                return new javafx.beans.property.SimpleStringProperty("Błąd");
            }
        });
        bookCol.setPrefWidth(200);

        TableColumn<Loan, String> readerCol = new TableColumn<>("Czytelnik");
        readerCol.setCellValueFactory(cell -> {
            try {
                User reader = UserDAO.findUserById(cell.getValue().getReaderId());
                return new javafx.beans.property.SimpleStringProperty(reader != null ? reader.getUsername() : "Nieznany");
            } catch (SQLException e) {
                return new javafx.beans.property.SimpleStringProperty("Błąd");
            }
        });
        readerCol.setPrefWidth(150);

        TableColumn<Loan, String> loanDateCol = new TableColumn<>("Data zamówienia");
        loanDateCol.setCellValueFactory(cell -> {
            LocalDate date = cell.getValue().getLoanDate();
            return new javafx.beans.property.SimpleStringProperty(date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        });
        loanDateCol.setPrefWidth(120);
        loanDateCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Loan, String> dueDateCol = new TableColumn<>("Termin zwrotu");
        dueDateCol.setCellValueFactory(cell -> {
            LocalDate dueDate = cell.getValue().getDueDate();
            return new javafx.beans.property.SimpleStringProperty(dueDate != null ? dueDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "Brak");
        });
        dueDateCol.setPrefWidth(120);
        dueDateCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Loan, String> returnDateCol = new TableColumn<>("Data zwrotu");
        returnDateCol.setCellValueFactory(cell -> {
            LocalDate returnDate = cell.getValue().getReturnDate();
            return new javafx.beans.property.SimpleStringProperty(returnDate != null ? returnDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "Brak");
        });
        returnDateCol.setPrefWidth(120);
        returnDateCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Loan, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cell -> {
            Loan loan = cell.getValue();
            String statusText;

            switch (loan.getStatus()) {
                case PENDING:
                    statusText = "Oczekuje";
                    break;
                case APPROVED:
                    statusText = "Zatwierdzone";
                    break;
                case ISSUED:
                    statusText = loan.getDueDate() != null && loan.getDueDate().isBefore(LocalDate.now()) ? "Przetrzymana" : "Wypożyczona";
                    break;
                case RETURNED:
                    statusText = "Zwrócona";
                    break;
                case REJECTED:
                    statusText = "Odrzucone";
                    break;
                default:
                    statusText = "Nieznany";
            }

            return new javafx.beans.property.SimpleStringProperty(statusText);
        });

        statusCol.setCellFactory(column -> new TableCell<Loan, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Loan loan = getTableView().getItems().get(getIndex());

                    switch (loan.getStatus()) {
                        case PENDING:
                            setTextFill(Color.ORANGE);
                            setStyle("-fx-font-weight: bold");
                            break;
                        case APPROVED:
                            setTextFill(Color.BLUE);
                            setStyle("-fx-font-weight: bold");
                            break;
                        case ISSUED:
                            if (loan.getDueDate() != null && loan.getDueDate().isBefore(LocalDate.now())) {
                                setTextFill(Color.RED);
                                setStyle("-fx-font-weight: bold");
                            } else {
                                setTextFill(Color.GREEN);
                                setStyle("-fx-font-weight: bold");
                            }
                            break;
                        case REJECTED:
                            setTextFill(Color.RED);
                            setStyle("-fx-font-weight: bold");
                            break;
                        case RETURNED:
                            setTextFill(Color.GRAY);
                            break;
                        default:
                            setTextFill(Color.BLACK);
                    }
                }
            }
        });
        statusCol.setPrefWidth(120);
        statusCol.setStyle("-fx-alignment: CENTER;");

        loansTable.getColumns().setAll(idCol, bookCol, readerCol, loanDateCol, dueDateCol, returnDateCol, statusCol);
        loansTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button approveBtn = new Button("Zatwierdź");
        approveBtn.setStyle("-fx-font-size: 14; -fx-padding: 5 15; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        approveBtn.setOnAction(e -> approveLoan());

        Button issueBtn = new Button("Wydaj książkę");
        issueBtn.setStyle("-fx-font-size: 14; -fx-padding: 5 15; -fx-background-color: #2196F3; -fx-text-fill: white;");
        issueBtn.setOnAction(e -> issueBook());

        Button returnBtn = new Button("Przyjmij zwrot");
        returnBtn.setStyle("-fx-font-size: 14; -fx-padding: 5 15; -fx-background-color: #FF9800; -fx-text-fill: white;");
        returnBtn.setOnAction(e -> returnBook());

        Button rejectBtn = new Button("Odrzuć wniosek");
        rejectBtn.setStyle("-fx-font-size: 14; -fx-padding: 5 15; -fx-background-color: #F44336; -fx-text-fill: white;");
        rejectBtn.setOnAction(e -> rejectLoan());

        HBox buttonBox = new HBox(10, approveBtn, issueBtn, returnBtn, rejectBtn);
        buttonBox.setPadding(new Insets(15));
        buttonBox.setAlignment(Pos.CENTER);

        VBox contentBox = new VBox(10, searchPanel, loansTable, buttonBox);
        contentBox.setPadding(new Insets(10));
        contentBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0;");

        pane.setCenter(contentBox);
        pane.setPadding(new Insets(10));

        refreshLoansTable();
        return pane;
    }

    private BorderPane createBooksView() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(20));

        HBox searchPanel = new HBox(10);
        searchPanel.setPadding(new Insets(0, 0, 15, 0));
        searchPanel.setAlignment(Pos.CENTER_LEFT);

        bookSearchField = new TextField();
        bookSearchField.setPromptText("Tytuł, autor lub ISBN");
        bookSearchField.setPrefWidth(250);

        Button searchButton = new Button("Szukaj");
        searchButton.setStyle("-fx-font-size: 14; -fx-padding: 5 15; -fx-background-color: #555555; -fx-text-fill: white;");
        searchButton.setOnAction(e -> searchBooks(bookSearchField.getText()));

        Button clearButton = new Button("Wyczyść");
        clearButton.setStyle("-fx-font-size: 14; -fx-padding: 5 15;");
        clearButton.setOnAction(e -> {
            bookSearchField.clear();
            refreshBooksTable();
        });

        Button addManualButton = new Button("Dodaj ręcznie");
        addManualButton.setStyle("-fx-font-size: 14; -fx-padding: 5 15; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        addManualButton.setOnAction(e -> showAddBookDialog(false));

        Button addApiButton = new Button("Dodaj przez ISBN");
        addApiButton.setStyle("-fx-font-size: 14; -fx-padding: 5 15; -fx-background-color: #2196F3; -fx-text-fill: white;");
        addApiButton.setOnAction(e -> showAddBookDialog(true));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchPanel.getChildren().addAll(
                new Label("Wyszukaj książkę:"),
                bookSearchField,
                searchButton,
                clearButton,
                spacer,
                addManualButton,
                addApiButton
        );

        booksTable = new TableView<>();
        booksTable.setStyle("-fx-font-size: 14px; -fx-background-color: white;");
        booksTable.setPlaceholder(new Label("Ładowanie danych..."));
        booksTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        TableColumn<Book, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(60);
        idCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Book, String> titleCol = new TableColumn<>("Tytuł");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(250);

        TableColumn<Book, String> authorCol = new TableColumn<>("Autor");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
        authorCol.setPrefWidth(200);

        TableColumn<Book, String> isbnCol = new TableColumn<>("ISBN");
        isbnCol.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        isbnCol.setPrefWidth(150);

        TableColumn<Book, String> statusCol = new TableColumn<>("Dostępność");
        statusCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        cell.getValue().isAvailable() ? "Dostępna" : "Wypożyczona"
                )
        );
        statusCol.setCellFactory(column -> new TableCell<Book, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("Dostępna")) {
                        setTextFill(Color.GREEN);
                    } else {
                        setTextFill(Color.RED);
                    }
                }
            }
        });
        statusCol.setPrefWidth(100);
        statusCol.setStyle("-fx-alignment: CENTER;");

        booksTable.getColumns().setAll(idCol, titleCol, authorCol, isbnCol, statusCol);
        booksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button deleteButton = new Button("Usuń książkę");
        deleteButton.setStyle("-fx-font-size: 14; -fx-padding: 5 15; -fx-background-color: #F44336; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> deleteBook());

        HBox buttonBox = new HBox(10, deleteButton);
        buttonBox.setPadding(new Insets(15));
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox contentBox = new VBox(10, searchPanel, booksTable, buttonBox);
        contentBox.setPadding(new Insets(10));
        contentBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0;");

        pane.setCenter(contentBox);
        pane.setPadding(new Insets(10));

        refreshBooksTable();
        return pane;
    }

    private void showAddBookDialog(boolean useApi) {
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle(useApi ? "Dodaj książkę przez ISBN" : "Dodaj książkę ręcznie");
        dialog.setHeaderText(null);

        ButtonType addButtonType = new ButtonType(useApi ? "Wyszukaj i dodaj" : "Dodaj", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField titleField = new TextField();
        titleField.setPromptText("Tytuł");
        TextField authorField = new TextField();
        authorField.setPromptText("Autor");
        TextField isbnField = new TextField();
        isbnField.setPromptText("ISBN");

        if (useApi) {
            grid.add(new Label("ISBN:"), 0, 0);
            grid.add(isbnField, 1, 0);
            titleField.setDisable(true);
            authorField.setDisable(true);
        } else {
            grid.add(new Label("Tytuł:"), 0, 0);
            grid.add(titleField, 1, 0);
            grid.add(new Label("Autor:"), 0, 1);
            grid.add(authorField, 1, 1);
            grid.add(new Label("ISBN:"), 0, 2);
            grid.add(isbnField, 1, 2);
        }

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(() -> (useApi ? isbnField : titleField).requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    if (useApi) {
                        String isbn = isbnField.getText().trim();
                        if (isbn.isEmpty()) {
                            showAlert(Alert.AlertType.ERROR, "Błąd", "Wprowadź ISBN");
                            return null;
                        }
                        return BookDAO.fetchBookFromAPI(isbn);
                    } else {
                        String title = titleField.getText().trim();
                        String author = authorField.getText().trim();
                        String isbn = isbnField.getText().trim();

                        if (title.isEmpty() || author.isEmpty()) {
                            showAlert(Alert.AlertType.ERROR, "Błąd", "Wprowadź tytuł i autora");
                            return null;
                        }
                        return new Book(0, title, author, isbn, true);
                    }
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Błąd", "Nie udało się dodać książki: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        Optional<Book> result = dialog.showAndWait();
        result.ifPresent(book -> {
            try {
                if (book.getId() == 0) { // Nowa książka (nie z API)
                    BookDAO.addBook(book);
                }
                refreshBooksTable();
                showAlert(Alert.AlertType.INFORMATION, "Sukces", "Książka została dodana");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Błąd", "Nie udało się zapisać książki: " + e.getMessage());
            }
        });
    }

    private void deleteBook() {
        Book selected = booksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Brak wyboru", "Wybierz książkę do usunięcia");
            return;
        }

        if (!selected.isAvailable()) {
            showAlert(Alert.AlertType.WARNING, "Nie można usunąć", "Książka jest wypożyczona i nie może zostać usunięta");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Potwierdzenie usunięcia");
        confirmDialog.setHeaderText(null);
        confirmDialog.setContentText("Czy na pewno chcesz usunąć książkę: " + selected.getTitle() + "?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean deleted = BookDAO.deleteBook(selected.getId());
                if (!deleted) {
                    showAlert(Alert.AlertType.WARNING, "Nie można usunąć", "Książka mogła zostać wypożyczona w międzyczasie");
                }
                refreshBooksTable();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Błąd", "Nie udało się usunąć książki: " + e.getMessage());
            }
        }
    }


    private void searchBooks(String query) {
        new Thread(() -> {
            try {
                List<Book> books;
                if (query == null || query.trim().isEmpty()) {
                    books = BookDAO.getAllBooks();
                } else {
                    books = BookDAO.searchBooks(query, false);
                }

                Platform.runLater(() -> {
                    booksTable.getItems().setAll(books);
                    booksTable.setPlaceholder(new Label(books.isEmpty() ? "Brak wyników wyszukiwania" : ""));
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    booksTable.setPlaceholder(new Label("Błąd wyszukiwania"));
                    showAlert(Alert.AlertType.ERROR, "Błąd", "Nie można wyszukać książek: " + e.getMessage());
                });
            }
        }).start();
    }

    private void refreshBooksTable() {
        searchBooks("");
    }

    private void searchLoans(String query) {
        new Thread(() -> {
            try {
                List<Loan> loans;
                if (query == null || query.trim().isEmpty()) {
                    loans = LoanDAO.getAllLoans();
                } else {
                    try {
                        int userId = Integer.parseInt(query);
                        loans = LoanDAO.findLoansByReader(userId);
                    } catch (NumberFormatException e) {
                        List<User> users = UserDAO.findUsersByUsername(query);
                        if (users.isEmpty()) {
                            loans = List.of();
                        } else {
                            loans = new ArrayList<>();
                            for (User user : users) {
                                loans.addAll(LoanDAO.findLoansByReader(user.getId()));
                            }
                        }
                    }
                }

                List<Loan> finalLoans = loans;
                Platform.runLater(() -> {
                    loansTable.getItems().setAll(finalLoans);
                    loansTable.setPlaceholder(new Label(finalLoans.isEmpty() ? "Brak wyników wyszukiwania" : ""));
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    loansTable.setPlaceholder(new Label("Błąd wyszukiwania"));
                    showAlert(Alert.AlertType.ERROR, "Błąd", "Nie można wyszukać zamówień: " + e.getMessage());
                });
            }
        }).start();
    }

    private void refreshLoansTable() {
        searchLoans("");
    }

    private void approveLoan() {
        Loan selected = loansTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Brak wyboru", "Wybierz zamówienie do zatwierdzenia");
            return;
        }

        if (selected.getStatus() != Loan.Status.PENDING) {
            showAlert(Alert.AlertType.WARNING, "Błąd", "Można zatwierdzać tylko wnioski w statusie Oczekuje");
            return;
        }

        try {
            LibraryService.approveLoan(selected.getId(), currentUser.getId());
            refreshLoansTable();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Błąd", "Nie udało się zatwierdzić wniosku: " + e.getMessage());
        }
    }

    private void issueBook() {
        Loan selected = loansTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Brak wyboru", "Wybierz zamówienie do wydania");
            return;
        }

        if (selected.getStatus() != Loan.Status.APPROVED) {
            showAlert(Alert.AlertType.WARNING, "Błąd", "Można wydawać tylko zatwierdzone książki");
            return;
        }

        try {
            LibraryService.issueBook(selected.getId());
            refreshLoansTable();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Błąd", "Nie udało się wydać książki: " + e.getMessage());
        }
    }

    private void returnBook() {
        Loan selected = loansTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Brak wyboru", "Wybierz wypożyczenie do zwrotu");
            return;
        }

        if (selected.getStatus() != Loan.Status.ISSUED) {
            showAlert(Alert.AlertType.WARNING, "Błąd", "Można przyjmować tylko wydane książki");
            return;
        }

        try {
            LibraryService.returnBook(selected.getId());
            refreshLoansTable();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Błąd", "Nie udało się przyjąć książki: " + e.getMessage());
        }
    }

    private void rejectLoan() {
        Loan selected = loansTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Brak wyboru", "Wybierz wniosek do odrzucenia");
            return;
        }

        if (selected.getStatus() != Loan.Status.PENDING) {
            showAlert(Alert.AlertType.WARNING, "Błąd", "Można odrzucać tylko wnioski w statusie Oczekuje");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION, "Czy na pewno chcesz odrzucić wniosek na książkę: " + getBookTitle(selected.getBookId()) + "?", ButtonType.YES, ButtonType.NO);
        confirmDialog.setTitle("Potwierdzenie odrzucenia");
        confirmDialog.setHeaderText(null);

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                LibraryService.rejectLoan(selected.getId(), currentUser.getId());
                refreshLoansTable();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Błąd", "Nie udało się odrzucić wniosku: " + e.getMessage());
            }
        }
    }

    private String getBookTitle(int bookId) {
        try {
            Book book = BookDAO.findBookById(bookId);
            return book != null ? book.getTitle() : "Nieznana książka";
        } catch (SQLException e) {
            return "Błąd ładowania tytułu";
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}