package model.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileLogger extends Logger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private String filePath;

    public FileLogger(String filePath) {
        super(null);
        this.filePath = filePath;
        new File("logs").mkdirs();
    }

    @Override
    protected void handle(String message, Type level) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String formatted = String.format("[%s] [%s] %s", timestamp, level, message);

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, true))) {
            writer.println(formatted);
        } catch (IOException e) {
            System.out.println("Impossible d'ecrire dans le fichier : " + e.getMessage());
        }
    }
}
