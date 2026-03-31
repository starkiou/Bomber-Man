package model.logger;

public abstract class Logger {
    protected Logger nextLogger;

    public Logger(Logger nextLogger) {
        this.nextLogger = nextLogger;
    }

    public void log(String message, Type level) {
        handle(message, level);
        if (nextLogger != null) {
            nextLogger.log(message, level);
        }
    }

    public Logger setNext(Logger next) {
        this.nextLogger = next;
        return next;
    }

    protected abstract void handle(String message, Type level);
}
