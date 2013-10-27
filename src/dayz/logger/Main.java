package dayz.logger;

import ch.qos.logback.classic.Level;
import org.slf4j.LoggerFactory;

public class Main {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        System.out.println("DayZ Logger v1.0 beta");

        String logLevelName = getArgument(args, "-d", null);
        if (logLevelName != null) {
            ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            rootLogger.setLevel(Level.toLevel(logLevelName, Level.DEBUG));
        }

        String configFileName = getArgument(args, "-c", "config.cfg");
        Config config = Config.loadFromFile(configFileName);

        Logger logger = new Logger(config);
        logger.start();
    }

    private static String getArgument(String[] args, String name, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (name.equalsIgnoreCase(args[i])) {
                return args[i + 1]; // value
            }
        }
        // argument not found, return default value
        return defaultValue;
    }
}
