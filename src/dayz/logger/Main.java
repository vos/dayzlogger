package dayz.logger;

import org.slf4j.LoggerFactory;

public class Main {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        log.info("DayZ Logger v0.1 alpha");

        String configFileName = args.length >= 2 && "-c".equals(args[0]) ? args[1] : "config.cfg";
        Config config = Config.loadFromFile(configFileName);

        Logger logger = new Logger(config);
        logger.start();
    }
}
