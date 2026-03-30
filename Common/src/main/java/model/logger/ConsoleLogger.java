package model.logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ConsoleLogger extends Logger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ConsoleLogger(Logger nextLogger) {
        super(nextLogger);
    }

    @Override
    public void handle(String message, Type level) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String formatted = String.format("[%s] [%s] %s", timestamp, level, message);

        System.out.println(formatted);
    }

}
