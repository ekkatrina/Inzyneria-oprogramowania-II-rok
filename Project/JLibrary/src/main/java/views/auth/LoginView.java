package views.auth;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import models.User;
import services.AuthService;

public class LoginView extends Application {
    @Override
    public void start(Stage stage) {
        TextField usernameField = new TextField();
        usernameField.setPromptText("Wprowadź login");
        usernameField.setStyle("-fx-font-size: 14px;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Wprowadź hasło");
        passwordField.setStyle("-fx-font-size: 14px;");

        Button loginButton = new Button("Zaloguj się");
        loginButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 25; -fx-background-color: #555555; -fx-text-fill: white; -fx-background-radius: 20;");

        Hyperlink registerLink = new Hyperlink("Zarejestruj się");
        registerLink.setStyle("-fx-font-size: 13px; -fx-text-fill: #555555; -fx-border-color: transparent; -fx-underline: true;");

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 13px;");

        loginButton.setOnAction(e -> handleLogin(stage, usernameField, passwordField, statusLabel));
        registerLink.setOnAction(e -> new RegisterView().start(new Stage()));

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(25));

        grid.add(new Label("Login:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Hasło:"), 0, 1);
        grid.add(passwordField, 1, 1);

        HBox loginBox = new HBox(loginButton);
        loginBox.setAlignment(Pos.CENTER);
        grid.add(loginBox, 0, 2, 2, 1);

        HBox linkBox = new HBox(registerLink);
        linkBox.setAlignment(Pos.CENTER);
        grid.add(linkBox, 0, 3, 2, 1);

        grid.add(statusLabel, 0, 4, 2, 1);
        statusLabel.setAlignment(Pos.CENTER);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(100);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPrefWidth(200);
        grid.getColumnConstraints().addAll(col1, col2);

        VBox mainLayout = new VBox(grid);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #f8f8f8;");
        mainLayout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(mainLayout, 400, 300);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle("Logowanie");
        stage.show();
    }

    private void handleLogin(Stage stage, TextField usernameField, PasswordField passwordField, Label statusLabel) {
        try {
            String username = usernameField.getText();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                throw new RuntimeException("Wprowadź login i hasło");
            }

            User user = AuthService.login(username, password);
            AuthService.setCurrentUser(user); // Сохраняем пользователя
            openDashboard(stage, user.getRole());
        } catch (Exception ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private void openDashboard(Stage stage, String role) {
        try {
            switch (role) {
                case "ADMIN" -> new views.admin.AdminDashboard().start(stage);
                case "LIBRARIAN" -> new views.librarian.LibrarianDashboard().start(stage);
                default -> new views.reader.ReaderDashboard().start(stage);
            }
        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas otwierania panelu", e);
        }
    }
}