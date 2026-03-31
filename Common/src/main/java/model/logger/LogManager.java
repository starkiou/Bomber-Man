package model.logger;

public class LogManager {
    private static LogManager instance;
    private Logger chain;

    private LogManager() {
        initChain();
    }

    public static synchronized LogManager getInstance() {
        if(instance == null) {
            instance = new LogManager();
        }
        return instance;
    }

    private void initChain() {
        FileLogger file = new FileLogger("logs/app.log");
        ConsoleLogger console = new ConsoleLogger(file);
        this.chain = console;
    }

    public void log(String message, Type level) {
        chain.log(message, level);
    }

    public void info(String message)    {
        log(message, Type.INFO);
    }
    public void warning(String message) {
        log(message, Type.WARNING);
    }
    public void error(String message)   {
        log(message, Type.ERROR);
    }

}
