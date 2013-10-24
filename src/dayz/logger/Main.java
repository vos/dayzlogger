package dayz.logger;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws Exception {
        Properties defaultProps = new Properties();
        InputStream in = Main.class.getResourceAsStream("/config_default.cfg");
        defaultProps.load(in);
        in.close();

        String configFilePath = args.length >= 2 && "-c".equals(args[0]) ? args[1] : "config.cfg";
        Properties appProps = new Properties(defaultProps);
        in = new FileInputStream(configFilePath);
        appProps.load(in);
        in.close();

        String dbHost = appProps.getProperty("db_host");
        String dbName = appProps.getProperty("db_name");
        String dbUser = appProps.getProperty("db_user");
        String dbPass = appProps.getProperty("db_pass");

        String characterDataTable = appProps.getProperty("character_data_table");
        String playerUidField = appProps.getProperty("player_uid_field");
        String lastUpdatedField = appProps.getProperty("last_updated_field");
        String aliveField = appProps.getProperty("alive_field");
        String logFields = appProps.getProperty("log_fields");

        long startTimeDiff = Integer.parseInt(appProps.getProperty("start_time_diff")) * 1000; // ms
        int waitTime = Integer.parseInt(appProps.getProperty("wait_time")) * 1000; // ms

        String separator = appProps.getProperty("separator");

        String dbUrl = String.format("jdbc:mysql://%s/%s", dbHost, dbName);
        Connection con = DriverManager.getConnection(dbUrl, dbUser, dbPass);

        Timestamp localTimestamp = new Timestamp(System.currentTimeMillis());
        ResultSet timestampResultSet = con.createStatement().executeQuery("SELECT CURRENT_TIMESTAMP");
        timestampResultSet.next();
        Timestamp serverTimestamp = timestampResultSet.getTimestamp(1);
        System.out.println("localTimestamp = " + localTimestamp);
        System.out.println("serverTimestamp = " + serverTimestamp);
        long timeDiff = serverTimestamp.getTime() - localTimestamp.getTime();
        System.out.println("timeDiff = " + timeDiff);

        String charQuery = String.format("SELECT %s, %s, %s FROM %s WHERE %s=1 AND %s > ?",
                playerUidField, lastUpdatedField, logFields, characterDataTable, aliveField, lastUpdatedField);
        PreparedStatement charStatement = con.prepareStatement(charQuery);

        long time = System.currentTimeMillis() - startTimeDiff;
        ResultSetMetaData metaData = null;
        int columnCount = 0;

        while (!Thread.interrupted()) {
            localTimestamp = new Timestamp(time);
            Timestamp timestamp = new Timestamp(time + timeDiff);
            time = System.currentTimeMillis();
            System.out.println("\nlocalTimestamp = " + localTimestamp);
            System.out.println("timestamp = " + timestamp);

            charStatement.setTimestamp(1, timestamp);
            System.out.println(charStatement);
            ResultSet charResultSet = charStatement.executeQuery();

            if (metaData == null) {
                metaData = charResultSet.getMetaData();
                columnCount = metaData.getColumnCount();
            }

            while (charResultSet.next()) {
                String playerUid = charResultSet.getString(1);
                Timestamp lastUpdated = charResultSet.getTimestamp(2);

                System.out.println("===============================");
                System.out.println("PlayerUID = " + playerUid);
                System.out.println("LastUpdated = " + lastUpdated);

                // player debug
                PreparedStatement playerStatement = con.prepareStatement("SELECT PlayerName FROM player_data WHERE PlayerUID = ?");
                playerStatement.setString(1, playerUid);
                ResultSet playerResultSet = playerStatement.executeQuery();
                playerResultSet.first();
                String playerName = playerResultSet.getString(1);
                System.out.println("PlayerName = " + playerName);
                playerStatement.close();

                // TODO: open log file

                // ignore PlayerUID and LastUpdated fields
                for (int i = 3; i <= columnCount - 2; i++) {
                    String columnName = metaData.getColumnName(i);
                    String columnTypeName = metaData.getColumnTypeName(i);
                    String value = charResultSet.getString(i);
                    System.out.println(columnName + "[" + columnTypeName + "] = " + value);
                    // TODO: write to log file
                }
            }

            Thread.sleep(waitTime);
        }

        charStatement.close();
        con.close();
    }
}
