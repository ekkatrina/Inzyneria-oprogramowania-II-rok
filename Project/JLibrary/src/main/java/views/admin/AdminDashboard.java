package views.admin;

import dao.UserDAO;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Pair;
import models.User;
import services.AdminService;
import services.LibraryService;
import views.admin.components.StatCard;
import views.auth.LoginView;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdminDashboard extends javafx.application.Application {
    private TableView<User> userTable;
    private StatCard booksCard;
    private StatCard usersCard;
    private StatCard loansCard;
    private StatCard overdueCard;

    @Override
    public void start(Stage stage) {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
                createUserManagementTab(),
                createStatisticsTab()
        );

        BorderPane root = new BorderPane();
        root.setTop(createToolbar());
        root.setCenter(tabPane);
        root.setStyle("-fx-background-color: #f8f8f8;");

        Scene scene = new Scene(root, 1000, 700);
        stage.setScene(scene);
        stage.setTitle("Panel Administracyjny - JLibrary");
        stage.show();
    }

    private HBox createToolbar() {
        Button logoutButton = new Button("Wyloguj");
        logoutButton.setStyle("-fx-font-size: 14; -fx-padding: 5 15; " +
                "-fx-background-color: #555555; -fx-text-fill: white;");
        logoutButton.setOnAction(e -> {
            Stage currentStage = (Stage) logoutButton.getScene().getWindow();
            currentStage.close();
            new LoginView().start(new Stage());
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS); // Растягивающийся разделитель

        HBox toolbar = new HBox(spacer, logoutButton);
        toolbar.setPadding(new Insets(15));
        toolbar.setStyle("-fx-background-color: #e0e0e0;");
        return toolbar;
    }

    private Tab createUserManagementTab() {
        Tab tab = new Tab("Zarządzanie użytkownikami");
        tab.setContent(createUserManagementView());
        tab.setClosable(false);
        return tab;
    }

    private Tab createStatisticsTab() {
        Tab tab = new Tab("Statystyki");
        tab.setContent(createStatisticsView());
        tab.setClosable(false);

        tab.setOnSelectionChanged(event -> {
            if (tab.isSelected()) {
                loadStatistics();
            }
        });

        return tab;
    }

    private BorderPane createUserManagementView() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(20));

        userTable = new TableView<>();
        userTable.setStyle("-fx-font-size: 14px; -fx-background-color: white;");
        userTable.setPlaceholder(new Label("Brak danych do wyświetlenia"));

        // Настройка колонок
        TableColumn<User, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(80);
        idCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<User, String> usernameCol = new TableColumn<>("Login");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        usernameCol.setPrefWidth(250);

        TableColumn<User, String> roleCol = new TableColumn<>("Rola");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        roleCol.setPrefWidth(150);
        roleCol.setStyle("-fx-alignment: CENTER;");

        userTable.getColumns().setAll(idCol, usernameCol, roleCol);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button addLibrarianBtn = new Button("Dodaj bibliotekarza");
        addLibrarianBtn.setStyle("-fx-font-size: 14; -fx-padding: 8 16; -fx-background-color: #555555; -fx-text-fill: white;");

        Button deleteUserBtn = new Button("Usuń użytkownika");
        deleteUserBtn.setStyle("-fx-font-size: 14; -fx-padding: 8 16; -fx-background-color: #555555; -fx-text-fill: white;");

        addLibrarianBtn.setOnAction(e -> showAddLibrarianDialog());
        deleteUserBtn.setOnAction(e -> deleteSelectedUser());

        HBox buttonBox = new HBox(20, addLibrarianBtn, deleteUserBtn);
        buttonBox.setPadding(new Insets(15));
        buttonBox.setAlignment(Pos.CENTER);

        VBox contentBox = new VBox(10, userTable, buttonBox);
        contentBox.setPadding(new Insets(10));
        contentBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0;");

        pane.setCenter(contentBox);
        pane.setPadding(new Insets(10));

        refreshUserTable();
        return pane;
    }

    private void showAddLibrarianDialog() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Dodaj nowego bibliotekarza");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 30, 20, 30));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Wprowadź login");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Wprowadź hasło");

        grid.add(new Label("Login:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Hasło:"), 0, 1);
        grid.add(passwordField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        ButtonType addButton = new ButtonType("Dodaj", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == addButton) {
                return new Pair<>(
                        usernameField.getText().trim(),
                        passwordField.getText() // Используем getText() для PasswordField
                );
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();
        result.ifPresent(credentials -> {
            try {
                AdminService.addLibrarian(credentials.getKey(), credentials.getValue());
                refreshUserTable();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Błąd", e.getMessage());
            }
            passwordField.clear();
        });
    }

    private void deleteSelectedUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Brak wyboru", "Wybierz użytkownika do usunięcia");
            return;
        }

        Alert confirmDialog = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Czy na pewno chcesz usunąć użytkownika: " + selected.getUsername() + "?",
                ButtonType.YES, ButtonType.NO
        );
        confirmDialog.setTitle("Potwierdzenie usunięcia");
        confirmDialog.setHeaderText(null);

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            deleteUser(selected.getId());
        }
    }

    private void deleteUser(int userId) {
        try {
            User userToDelete = UserDAO.findUserById(userId);
            if (userToDelete == null) {
                showAlert(Alert.AlertType.ERROR, "Błąd", "Użytkownik nie istnieje");
                return;
            }

            if ("ADMIN".equals(userToDelete.getRole())) {
                showAlert(Alert.AlertType.ERROR, "Błąd", "Nie można usunąć administratora");
                return;
            }

            if (AdminService.deleteUser(userId)) {
                refreshUserTable();
            } else {
                throw new Exception("Nie udało się usunąć użytkownika");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Błąd", e.getMessage());
        }
    }

    private void refreshUserTable() {
        new Thread(() -> {
            try {
                List<User> users = AdminService.getAllUsers();
                Platform.runLater(() -> {
                    userTable.getItems().setAll(users);
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Błąd", "Nie można załadować użytkowników: " + e.getMessage())
                );
            }
        }).start();
    }

    private BorderPane createStatisticsView() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(20));

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(30);
        grid.setVgap(30);

        booksCard = new StatCard("Liczba książek");
        usersCard = new StatCard("Liczba użytkowników");
        loansCard = new StatCard("Aktywne wypożyczenia");
        overdueCard = new StatCard("Opóźnienia");

        grid.add(booksCard, 0, 0);
        grid.add(usersCard, 1, 0);
        grid.add(loansCard, 0, 1);
        grid.add(overdueCard, 1, 1);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(50);
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(50);
        grid.getRowConstraints().addAll(row1, row2);

        VBox statsBox = new VBox(grid);
        statsBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0;");
        statsBox.setPadding(new Insets(20));

        pane.setCenter(statsBox);
        loadStatistics();
        return pane;
    }

    private void loadStatistics() {
        new Thread(() -> {
            try {
                Map<String, Integer> stats = LibraryService.getLibraryStatistics();
                Platform.runLater(() -> {
                    booksCard.setValue(String.valueOf(stats.getOrDefault("books", 0)));
                    usersCard.setValue(String.valueOf(stats.getOrDefault("users", 0)));
                    loansCard.setValue(String.valueOf(stats.getOrDefault("activeLoans", 0)));
                    overdueCard.setValue(String.valueOf(stats.getOrDefault("overdueLoans", 0)));
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Błąd", "Nie można załadować statystyk: " + e.getMessage())
                );
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