module org.example.sae_s4_groupe_b {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;


    opens org.example.sae_s4_groupe_b to javafx.fxml;
    exports org.example.sae_s4_groupe_b;
}