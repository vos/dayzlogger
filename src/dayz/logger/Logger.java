package dayz.logger;

import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Logger {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Logger.class);

    final Config config;
    private final Map<String, CharData> charDataMap;

    public Logger(Config config) {
        this.config = config;
        charDataMap = new HashMap<>();
    }

    public void start() throws Exception {
        // connect to database
        String dbUrl = String.format("jdbc:mysql://%s/%s", config.dbHost, config.dbName);
        Connection con = DriverManager.getConnection(dbUrl, config.dbUser, config.dbPass);

        // compute time difference between this machine and the database server
        Timestamp localTimestamp = new Timestamp(System.currentTimeMillis());
        ResultSet timestampResultSet = con.createStatement().executeQuery("SELECT CURRENT_TIMESTAMP");
        timestampResultSet.next();
        Timestamp serverTimestamp = timestampResultSet.getTimestamp(1);
        log.debug("localTimestamp = {}", localTimestamp);
        log.debug("serverTimestamp = {}", serverTimestamp);
        long timeDiff = serverTimestamp.getTime() - localTimestamp.getTime();
        log.debug("timeDiff = {} ms", timeDiff);

        // character query
        String charQuery = String.format("SELECT %s, %s, %s FROM %s WHERE %s=1 AND %s > ?",
                config.playerUidField, config.lastUpdatedField, config.logFields,
                config.charDataTable, config.aliveField, config.lastUpdatedField);
        PreparedStatement charStatement = con.prepareStatement(charQuery);

        Path logDir = Paths.get(config.logDir);
        long time = System.currentTimeMillis() - config.startTimeDiff;
        ResultSetMetaData metaData = null;
        int columnCount = 0;

        // enter query loop
        while (!Thread.interrupted()) {
            Profiler profiler = new Profiler("query loop");
            profiler.setLogger(log);
            profiler.start("query");

            if (log.isDebugEnabled()) {
                localTimestamp = new Timestamp(time);
                log.debug("localTimestamp = {}", localTimestamp);
            }
            Timestamp timestamp = new Timestamp(time + timeDiff);
            time = System.currentTimeMillis();
            log.debug("timestamp = {}", timestamp);

            charStatement.setTimestamp(1, timestamp);
            log.debug("{}", charStatement);
            ResultSet charResultSet = charStatement.executeQuery();

            if (metaData == null) {
                metaData = charResultSet.getMetaData();
                columnCount = metaData.getColumnCount();
            }

            profiler.start("log");
            while (charResultSet.next()) {
                String playerUid = charResultSet.getString(1);
                Timestamp lastUpdated = charResultSet.getTimestamp(2);
                String lastUpdatedStr = lastUpdated.toString(); // yyyy-mm-dd hh:mm:ss.fffffffff
                String lastUpdatedDate = lastUpdatedStr.substring(0, 10); // yyyy-mm-dd
                String lastUpdatedTime = lastUpdatedStr.substring(11, 19); // hh:mm:ss

                log.debug("===============================");
                log.debug("PlayerUID = {}", playerUid);
                log.debug("LastUpdated = {}", lastUpdatedStr);

                // player debug
                if (log.isDebugEnabled()) {
                    PreparedStatement playerStatement = con.prepareStatement("SELECT PlayerName FROM player_data WHERE PlayerUID = ?");
                    playerStatement.setString(1, playerUid);
                    ResultSet playerResultSet = playerStatement.executeQuery();
                    playerResultSet.first();
                    String playerName = playerResultSet.getString(1);
                    log.debug("PlayerName = {}", playerName);
                    playerStatement.close();
                }

                CharData charData = charDataMap.get(playerUid);
                if (charData == null) {
                    charData = new CharData(this, playerUid, columnCount - 2);
                    charDataMap.put(playerUid, charData);

                    Path logFileDir = logDir.resolve(lastUpdatedDate);
                    if (!Files.isDirectory(logFileDir)) {
                        Files.createDirectories(logFileDir);
                    }
                    try {
                        charData.openLogWriter(logFileDir);
                    } catch (IOException e) {
                        log.error("cannot open log writer " + charData.getLogFileName(), e);
                    }
                    // TODO: rotate log file dir when date changes
                }
                charData.setLastUpdated(lastUpdated);

                boolean charValueChanged = false;
                // skip PlayerUID and LastUpdated fields
                for (int i = 3; i <= columnCount; i++) {
                    String value = charResultSet.getString(i);
                    if (log.isTraceEnabled()) {
                        String columnName = metaData.getColumnName(i);
                        String columnTypeName = metaData.getColumnTypeName(i);
                        log.trace("{}[{}] = {}", columnName, columnTypeName, value);
                    }
                    int charValueIndex = i - 3;
                    if (!value.equals(charData.getValue(charValueIndex))) {
                        charData.setValue(charValueIndex, value);
                        charValueChanged = true;
                    }
                }
                if (charValueChanged) {
                    try {
                        charData.writeLogData(lastUpdatedTime);
                    } catch (IOException e) {
                        log.error("error while writing log data to " + charData.getLogFileName(), e);
                    }
                } else {
                    log.debug("no char value changed => do not log");
                }
            }

            profiler.start("cleanup");
            cleanup(timestamp);

            profiler.stop();
            profiler.log();

            long waitTime = config.waitTime - profiler.elapsedTime() / 1_000_000; // ms
            log.debug("waitTime = {} ms", waitTime);
            if (waitTime > 0) {
                Thread.sleep(waitTime);
            }
        }

        charStatement.close();
        con.close();
    }

    private void cleanup(Timestamp timestamp) {
        long time = timestamp.getTime();
        Iterator<Map.Entry<String, CharData>> charDataIterator = charDataMap.entrySet().iterator();
        while (charDataIterator.hasNext()) {
            Map.Entry<String, CharData> charDataEntry = charDataIterator.next();
            CharData charData = charDataEntry.getValue();
            if (time - charData.getLastUpdated().getTime() > config.cleanupDelay) {
                log.debug("cleanup of PlayerUID = {}", charData.getPlayerUid());
                try {
                    charData.closeLogWriter();
                } catch (IOException e) {
                    log.error("cannot close log writer " + charData.getLogFileName(), e);
                }
                charDataIterator.remove();
            }
        }
    }
}