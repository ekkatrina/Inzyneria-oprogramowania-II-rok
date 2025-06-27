package views.auth;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import services.AuthService;

public class RegisterView {
    public void start(Stage stage) {
        TextField usernameField = new TextField();
        usernameField.setPromptText("Wprowadź login");
        usernameField.setPrefWidth(200);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Wprowadź hasło");
        passwordField.setPrefWidth(200);

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Powtórz hasło");
        confirmField.setPrefWidth(200);

        Button registerButton = new Button("Zarejestruj");
        registerButton.setStyle("-fx-font-size: 14; -fx-padding: 8 16; -fx-background-color: #555555; -fx-text-fill: white; -fx-background-radius: 20;");
        registerButton.setPrefWidth(150);

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #d32f2f;");

        registerButton.setOnAction(e -> {
            try {
                String username = usernameField.getText().trim();
                String password = passwordField.getText();
                String confirm = confirmField.getText();

                if (username.isEmpty() || password.isEmpty()) {
                    throw new RuntimeException("Wypełnij wszystkie pola");
                }

                if (!password.equals(confirm)) {
                    throw new RuntimeException("Hasła nie są identyczne");
                }

                AuthService.register(username, password);
                stage.close();
            } catch (Exception ex) {
                statusLabel.setText(ex.getMessage());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(25));

        grid.add(new Label("Login:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Hasło:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("Potwierdź hasło:"), 0, 2);
        grid.add(confirmField, 1, 2);

        HBox buttonBox = new HBox(registerButton);
        buttonBox.setAlignment(Pos.CENTER);
        grid.add(buttonBox, 0, 3, 2, 1);

        grid.add(statusLabel, 0, 4, 2, 1);
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.NEVER);
        col1.setPrefWidth(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        VBox mainLayout = new VBox(grid);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #f8f8f8;");
        mainLayout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(mainLayout, 400, 300); // Увеличенная ширина окна
        stage.setScene(scene);
        stage.setTitle("Rejestracja");
        stage.setResizable(false);
        stage.show();
    }
}