package views.admin.components;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class StatCard extends VBox {
    private Label valueLabel;

    public StatCard(String title) {
        setSpacing(5);
        setStyle("-fx-border-color: #ccc; -fx-border-radius: 5; -fx-padding: 10;");
        setPrefSize(200, 100);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold;");

        valueLabel = new Label("0");
        valueLabel.setStyle("-fx-font-size: 24;");

        getChildren().addAll(titleLabel, valueLabel);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }
}