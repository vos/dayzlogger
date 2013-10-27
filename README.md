DayZ Logger v1.0 beta
=====================

DayZ Logger is a simple logging tool for DayZ servers, it is intended as a console application and there is no need for an UI.

With DayZ Logger you can track all player character changes and save everything to local log files. You can easily customize what data to query and how often to log them. Every player will get their own log file for each day, so you will not end up with one big log file after some time.

## Requirements
* DayZ Logger is a simple Java console application, so all you need to get it to work is the Java Runtime Environment (JRE) installed on your computer. The minimum required version is JRE 7.

* Access to your DayZ MySQL database. For this application to work it is sufficient to use an MySQL user who has only the SELECT privilege, because it will only query data and never change anything in the database.

* For the character query to work properly, the character data table needs a "timestamp on update" field to determine recently active players. If your character data table does not already have a field like this, you can add it with a SQL query:
```sql
ALTER TABLE `character_data` ADD `LastUpdated` TIMESTAMP on update CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
```

## Installation
Just extract the ZIP file anywhere on your local hard drive.

### Config file
Many options and default behaviours can be changed with the provided config file (default name config.cfg).
All options have default values, for more information take a look at the provided config.cfg as an example.

## Startup and command line arguments
The application can be started from the console like any other JAR file, or use one of the provided Windows BAT files to let it detect your JRE installation and launch it like any Windows application.

```
java -jar DayZLogger.jar [-c <filename>] [-d <debug-level>]
```
The filename can be any valid filename or path to a filename relative to your working directory.
The debug-level can be used to get more detailed messages shown in the console window, valid values are:
- ALL - shows detailed messages about every action (**Attention**: with this option enabled you will get player name logging, adjust the option *player_name_query* in the debug section of your config file)
- TRACE - same as ALL
- DEBUG - shows debug messages like log file infos
- INFO - default, shows only relevant messages to the user
- WARN - shows only warning and errors messages
- ERROR - shows only error messages
- OFF - shows no messages at all (not recommended)

for example
```
java -jar DayZLogger.jar -c config.cfg -d DEBUG
```

It is recommended to run DayZ Logger on the DayZ database server itself. If you just have remote access to the database server, you can run DayZ Logger from anywhere you want.

## Performance and storage usage
DayZ Logger is a light weight application, it has almost no CPU impact and a small memory profile as it only caches the current and last values of every player currently playing on the DayZ server and file handles to each log file. Log files will be kept open for appending data until the cleanup routine will close it and remove the player from the cache after an configurable idle time (default 15 minutes).

With data pruning enabled (disabled by default, see config file) the storage consumption is about 100 KB a day for every active player (about 5 MB with 50 active players each day). Furthermore old logs can be archived or removed to compress the data even more.

## Copyright and License
Copyright (c) 2013 Alexander Vos.

Distributed under the The MIT License (MIT).  See the LICENSE file for more information.
