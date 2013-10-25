package dayz.logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;

public class CharData {
    public static final Charset LOG_FILE_CHARSET = Charset.forName("UTF-8");
    public static final String LOG_FILE_EXTENSION = ".log";

    private final Logger logger;

    private final String playerUid;
    private Timestamp lastUpdated;
    private final String[] values;

    private final String[] previousValues;
    private boolean changed;

    private Path logFileName;
    private BufferedWriter logWriter;

    public String getPlayerUid() {
        return playerUid;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Path getLogFileName() {
        return logFileName;
    }

    public CharData(Logger logger, String playerUid, int valueCount) {
        this.logger = logger;
        this.playerUid = playerUid;
        values = new String[valueCount];
        previousValues = new String[valueCount];
        changed = false;
    }

    public String getValue(int index) {
        return values[index];
    }

    public void setValue(int index, String value) {
        previousValues[index] = values[index];
        if (!value.equals(values[index])) {
            values[index] = value;
            changed = true;
        }
    }

    public boolean hasChanged() {
        return changed;
    }

    public void openLogWriter(Path logFileDir) throws IOException {
        // create directories if necessary
        if (!Files.isDirectory(logFileDir)) {
            Files.createDirectories(logFileDir);
        }
        logFileName = logFileDir.resolve(playerUid + LOG_FILE_EXTENSION);
        logWriter = Files.newBufferedWriter(logFileName, LOG_FILE_CHARSET,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public void writeLogData(String timestamp) throws IOException {
        logWriter.write(timestamp);
        logWriter.write(logger.config.logValueSeparator);
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            if (logger.config.usePruning && value.equals(previousValues[i])) {
                if (!logger.config.pruningPlaceholder.isEmpty()) {
                    logWriter.write(logger.config.pruningPlaceholder);
                }
            } else {
                logWriter.write(value);
            }
            if (i != values.length - 1) {
                logWriter.write(logger.config.logValueSeparator);
            }
        }
        logWriter.newLine();
        logWriter.flush();
        changed = false;
    }

    public void closeLogWriter() throws IOException {
        logWriter.close();
        logWriter = null;
    }

    public void rotateLogFile(Path logFileDir) throws IOException {
        closeLogWriter();
        // reset all values so the new log file will start fresh with the first entry
        resetValues();
        openLogWriter(logFileDir);
    }

    private void resetValues() {
        for (int i = 0; i < values.length; i++) {
            values[i] = null;
            previousValues[i] = null;
        }
        changed = false;
    }
}
