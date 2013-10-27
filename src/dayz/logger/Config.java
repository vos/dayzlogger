package dayz.logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    public String fileName;

    public String dbHost;
    public String dbName;
    public String dbUser;
    public String dbPass;

    public String charDataTable;
    public String playerUidField;
    public String lastUpdatedField;
    public String aliveField;
    public String logFields;

    public long startTimeDiff;
    public long waitTime;

    public String logDir;
    public String logValueSeparator;
    public boolean autoFlush;

    public boolean usePruning;
    public String pruningPlaceholder;

    public long cleanupDelay;

    public String playerNameQuery;

    private void load(String fileName) throws IOException {
        this.fileName = fileName;

        // load default config
        Properties defaultProps = new Properties();
        InputStream in = Main.class.getResourceAsStream("/config_default.cfg");
        defaultProps.load(in);
        in.close();

        // load user config
        Properties userProps = new Properties(defaultProps);
        in = new FileInputStream(fileName);
        userProps.load(in);
        in.close();

        dbHost = userProps.getProperty("db_host");
        dbName = userProps.getProperty("db_name");
        dbUser = userProps.getProperty("db_user");
        dbPass = userProps.getProperty("db_pass");

        charDataTable = userProps.getProperty("character_data_table");
        playerUidField = userProps.getProperty("player_uid_field");
        lastUpdatedField = userProps.getProperty("last_updated_field");
        aliveField = userProps.getProperty("alive_field");
        logFields = userProps.getProperty("log_fields");

        startTimeDiff = Integer.parseInt(userProps.getProperty("start_time_diff")) * 1000; // ms
        waitTime = Integer.parseInt(userProps.getProperty("wait_time")) * 1000; // ms

        logDir = userProps.getProperty("log_dir");
        logValueSeparator = userProps.getProperty("log_value_separator");
        autoFlush = Boolean.parseBoolean(userProps.getProperty("auto_flush"));

        usePruning = Boolean.parseBoolean(userProps.getProperty("use_pruning"));
        pruningPlaceholder = userProps.getProperty("pruning_placeholder");

        cleanupDelay = Integer.parseInt(userProps.getProperty("cleanup_delay")) * 60_000; // ms

        playerNameQuery = userProps.getProperty("player_name_query");
    }

    public static Config loadFromFile(String fileName) throws IOException {
        Config config = new Config();
        config.load(fileName);
        return config;
    }
}
