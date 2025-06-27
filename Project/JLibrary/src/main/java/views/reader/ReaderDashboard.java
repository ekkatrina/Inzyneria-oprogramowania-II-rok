package views.reader;

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
import java.util.List;
import java.util.Optional;

public class ReaderDashboard extends javafx.application.Application {
    private User currentUser;
    private TableView<Book> booksTable;
    private TableView<Loan> loansTable;
    private TextField searchField;
    private CheckBox availableOnlyCheck;

    @Override
    public void start(Stage stage) {
        currentUser = AuthService.getCurrentUser();

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
                createBooksTab(),
                createMyLoansTab()
        );

        BorderPane root = new BorderPane();
        root.setTop(createToolbar());
        root.setCenter(tabPane);
        root.setStyle("-fx-background-color: #f8f8f8;");

        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.setTitle("Panel Czytelnika - " + currentUser.getUsername());
        stage.show();
    }

    private HBox createToolbar() {
        Label welcomeLabel = new Label("Witaj, " + currentUser.getUsername() + "!");
        welcomeLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Button logoutButton = new Button("Wyloguj");
        logoutButton.setStyle("-fx-font-size: 14; -fx-padding: 5 15; " +
                "-fx-background-color: #555555; -fx-text-fill: white;");
        logoutButton.setOnAction(e -> {
            Stage currentStage = (Stage) logoutButton.getScene().getWindow();
            currentStage.close();
            new LoginView().start(new Stage());
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(15, welcomeLabel, spacer, logoutButton);
        toolbar.setPadding(new Insets(15));
        toolbar.setStyle("-fx-background-color: #e0e0e0;");
        return toolbar;
    }

    private Tab createBooksTab() {
        Tab tab = new Tab("Dostępne książki");
        tab.setContent(createBooksView());
        tab.setClosable(false);
        return tab;
    }

    private Tab createMyLoansTab() {
        Tab tab = new Tab("Moje wypożyczenia");
        tab.setContent(createLoansView());
        tab.setClosable(false);

        tab.setOnSelectionChanged(event -> {
            if (tab.isSelected()) {
                refreshLoansTable();
            }
        });

        return tab;
    }

    private BorderPane createBooksView() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(20));

        searchField = new TextField();
        searchField.setPromptText("Szukaj po tytule lub autorze...");
        searchField.setStyle("-fx-font-size: 14px;");

        availableOnlyCheck = new CheckBox("Tylko dostępne");
        availableOnlyCheck.setSelected(true);
        availableOnlyCheck.setStyle("-fx-font-size: 14px;");

        Button searchButton = new Button("Szukaj");
        searchButton.setStyle("-fx-font-size: 14; -fx-padding: 5 15; -fx-background-color: #555555; -fx-text-fill: white;");
        searchButton.setOnAction(e -> searchBooks());

        HBox searchPanel = new HBox(10, searchField, availableOnlyCheck, searchButton);
        searchPanel.setPadding(new Insets(0, 0, 15, 0));
        searchPanel.setAlignment(Pos.CENTER_LEFT);

        TextField isbnField = new TextField();
        isbnField.setPromptText("Wpisz ISBN książki...");
        isbnField.setStyle("-fx-font-size: 14px;");

        Button isbnSearchButton = new Button("Szukaj po ISBN");
        isbnSearchButton.setStyle("-fx-font-size: 14; -fx-padding: 5 15; -fx-background-color: #555555; -fx-text-fill: white;");
        isbnSearchButton.setOnAction(e -> searchByISBN(isbnField.getText()));

        HBox isbnSearchPanel = new HBox(10, new Label("ISBN:"), isbnField, isbnSearchButton);
        isbnSearchPanel.setPadding(new Insets(0, 0, 15, 0));
        isbnSearchPanel.setAlignment(Pos.CENTER_LEFT);

        // Таблица с книгами
        booksTable = new TableView<>();
        booksTable.setStyle("-fx-font-size: 14px; -fx-background-color: white;");
        booksTable.setPlaceholder(new Label("Ładowanie danych..."));

        TableColumn<Book, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(80);
        idCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Book, String> titleCol = new TableColumn<>("Tytuł");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(250);

        TableColumn<Book, String> authorCol = new TableColumn<>("Autor");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
        authorCol.setPrefWidth(250);

        TableColumn<Book, String> isbnCol = new TableColumn<>("ISBN");
        isbnCol.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        isbnCol.setPrefWidth(150);

        TableColumn<Book, String> statusCol = new TableColumn<>("Dostępność");
        statusCol.setCellValueFactory(cell -> {
            boolean available = cell.getValue().isAvailable();
            return new javafx.beans.property.SimpleStringProperty(
                    available ? "Dostępna" : "Wypożyczona"
            );
        });
        statusCol.setPrefWidth(150);
        statusCol.setStyle("-fx-alignment: CENTER;");

        booksTable.getColumns().setAll(idCol, titleCol, authorCol, isbnCol, statusCol);
        booksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Кнопка заказа книги
        Button borrowBtn = new Button("Zamów książkę");
        borrowBtn.setStyle("-fx-font-size: 14; -fx-padding: 8 16; -fx-background-color: #555555; -fx-text-fill: white;");
        borrowBtn.setOnAction(e -> borrowSelectedBook());

        HBox buttonBox = new HBox(borrowBtn);
        buttonBox.setPadding(new Insets(15));
        buttonBox.setAlignment(Pos.CENTER);

        // Основной контейнер
        VBox contentBox = new VBox(10, searchPanel, isbnSearchPanel, booksTable, buttonBox);
        contentBox.setPadding(new Insets(10));
        contentBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0;");

        pane.setCenter(contentBox);
        pane.setPadding(new Insets(10));

        refreshBooksTable();
        return pane;
    }

    private void searchByISBN(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Brak ISBN", "Wprowadź numer ISBN książki");
            return;
        }

        new Thread(() -> {
            try {
                Book book = BookDAO.findBookByISBN(isbn.trim());

                Platform.runLater(() -> {
                    if (book != null) {
                        booksTable.getItems().setAll(book);
                        booksTable.setPlaceholder(new Label("Znaleziono książkę"));
                        booksTable.getSelectionModel().select(book);

                        showAlert(Alert.AlertType.INFORMATION, "Sukces",
                                "Znaleziono książkę: " + book.getTitle() + " autor: " + book.getAuthor());
                    } else {
                        showAlert(Alert.AlertType.INFORMATION, "Brak wyników",
                                "Nie znaleziono książki o podanym ISBN: " + isbn);
                        refreshBooksTable();
                    }
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Błąd",
                            "Wystąpił błąd podczas wyszukiwania książki: " + e.getMessage());
                });
            }
        }).start();
    }

    private void searchBooks() {
        new Thread(() -> {
            try {
                String query = searchField.getText();
                boolean onlyAvailable = availableOnlyCheck.isSelected();
                List<Book> books = BookDAO.searchBooks(query, onlyAvailable);

                Platform.runLater(() -> {
                    booksTable.getItems().setAll(books);
                    booksTable.setPlaceholder(new Label("Brak wyników wyszukiwania"));
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    booksTable.setPlaceholder(new Label("Błąd wyszukiwania"));
                    showAlert(Alert.AlertType.ERROR, "Błąd", "Nie można wyszukać książek: " + e.getMessage());
                });
            }
        }).start();
    }

    private void borrowSelectedBook() {
        Book selected = booksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return; // ничего не выбрано
        }

            Book refreshed;
        try {
            refreshed = BookDAO.findBookById(selected.getId());
            if (refreshed == null) {
                showAlert(Alert.AlertType.ERROR, "Błąd", "Niestety ta książka już nie jest dostępna.");
                refreshBooksTable();
                return;
            }
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Błąd", "Wystąpił błąd przy weryfikacji książki: " + ex.getMessage());
            return;
        }

        if (!refreshed.isAvailable()) {
            showAlert(Alert.AlertType.WARNING, "Niedostępna", "Ta książka jest już wypożyczona.");
            return;
        }

        try {
            LibraryService.requestBook(refreshed.getId(), currentUser.getId());
            refreshBooksTable();
            showAlert(Alert.AlertType.INFORMATION, "Sukces", "Zamówiono książkę: " + refreshed.getTitle());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Błąd", "Nie udało się zamówić książki: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private BorderPane createLoansView() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(20));

        loansTable = new TableView<>();
        loansTable.setStyle("-fx-font-size: 14px; -fx-background-color: white;");
        loansTable.setPlaceholder(new Label("Ładowanie danych..."));

        TableColumn<Loan, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(80);
        idCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Loan, String> bookCol = new TableColumn<>("Książka");
        bookCol.setCellValueFactory(cell -> {
            try {
                Book book = BookDAO.findBookById(cell.getValue().getBookId());
                return new javafx.beans.property.SimpleStringProperty(
                        book != null ? book.getTitle() : "Nieznana"
                );
            } catch (SQLException e) {
                return new javafx.beans.property.SimpleStringProperty("Błąd");
            }
        });
        bookCol.setPrefWidth(250);

        TableColumn<Loan, String> dateCol = new TableColumn<>("Data wypożyczenia");
        dateCol.setCellValueFactory(cell -> {
            LocalDate date = cell.getValue().getLoanDate();
            return new javafx.beans.property.SimpleStringProperty(
                    date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            );
        });
        dateCol.setPrefWidth(150);
        dateCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Loan, String> dueDateCol = new TableColumn<>("Termin zwrotu");
        dueDateCol.setCellValueFactory(cell -> {
            LocalDate dueDate = cell.getValue().getDueDate();
            return new javafx.beans.property.SimpleStringProperty(
                    dueDate != null ? dueDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "Brak"
            );
        });
        dueDateCol.setPrefWidth(150);
        dueDateCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Loan, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cell -> {
            Loan loan = cell.getValue();
            String statusText;

            switch (loan.getStatus()) {
                case PENDING: statusText = "Oczekuje"; break;
                case APPROVED: statusText = "Zatwierdzone"; break;
                case ISSUED:
                    statusText = loan.getDueDate() != null &&
                            loan.getDueDate().isBefore(LocalDate.now()) ?
                            "Przetrzymana" : "Wypożyczona";
                    break;
                case RETURNED: statusText = "Zwrócona"; break;
                case REJECTED: statusText = "Odrzucone"; break;
                default: statusText = "Nieznany";
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
        statusCol.setPrefWidth(150);
        statusCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Loan, String> librarianCol = new TableColumn<>("Bibliotekarz");
        librarianCol.setCellValueFactory(cell -> {
            try {
                User librarian = UserDAO.findUserById(cell.getValue().getLibrarianId());
                return new javafx.beans.property.SimpleStringProperty(
                        librarian != null ? librarian.getUsername() : "Nieprzypisany"
                );
            } catch (SQLException e) {
                return new javafx.beans.property.SimpleStringProperty("Błąd");
            }
        });
        librarianCol.setPrefWidth(150);

        loansTable.getColumns().setAll(idCol, bookCol, dateCol, dueDateCol, statusCol, librarianCol);
        loansTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button refreshButton = new Button("Odśwież");
        refreshButton.setStyle("-fx-font-size: 14; -fx-padding: 5 15; -fx-background-color: #555555; -fx-text-fill: white;");
        refreshButton.setOnAction(e -> refreshLoansTable());

        HBox buttonBox = new HBox(10, refreshButton);
        buttonBox.setPadding(new Insets(15));
        buttonBox.setAlignment(Pos.CENTER);

        VBox contentBox = new VBox(10, loansTable, buttonBox);
        contentBox.setPadding(new Insets(10));
        contentBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0;");

        pane.setCenter(contentBox);
        pane.setPadding(new Insets(10));

        return pane;
    }

    private void refreshBooksTable() {
        new Thread(() -> {
            try {
                List<Book> books = BookDAO.getAllBooks();
                Platform.runLater(() -> {
                    booksTable.getItems().setAll(books);
                    booksTable.setPlaceholder(new Label("Brak dostępnych książek"));
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    booksTable.setPlaceholder(new Label("Błąd ładowania danych"));
                    showAlert(Alert.AlertType.ERROR, "Błąd", "Nie można załadować książek: " + e.getMessage());
                });
            }
        }).start();
    }

    private void refreshLoansTable() {
        new Thread(() -> {
            try {
                List<Loan> loans = LoanDAO.findLoansByReader(currentUser.getId());
                Platform.runLater(() -> {
                    loansTable.getItems().setAll(loans);
                    loansTable.setPlaceholder(new Label("Brak wypożyczeń"));
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    loansTable.setPlaceholder(new Label("Błąd ładowania danych"));
                    showAlert(Alert.AlertType.ERROR, "Błąd", "Nie można załadować wypożyczeń: " + e.getMessage());
                });
            }
        }).start();
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