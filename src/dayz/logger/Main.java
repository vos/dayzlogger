package dayz.logger;

public class Main {

    public static void main(String[] args) throws Exception {
        String configFileName = args.length >= 2 && "-c".equals(args[0]) ? args[1] : "config.cfg";
        Config config = Config.loadFromFile(configFileName);

        Logger logger = new Logger(config);
        logger.start();
    }
}
