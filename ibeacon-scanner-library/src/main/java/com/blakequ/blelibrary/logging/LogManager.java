/*
 * Copyright 2015 Radius Networks, Inc.
 * Copyright 2015 Andrew Reitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blakequ.blelibrary.logging;

/**
 * Manager for logging in the Altbeacon library.
 *
 * @author Andrew Reitz
 * @since 2.2
 */
public final class LogManager {
    private static LogLevel logLevel = LogLevel.VERBOSE;
    private static final Logger sLogger = new AndroidLogger();

    public static LogLevel getLogLevel() {
        return logLevel;
    }

    public static void setLogLevel(LogLevel logLevel) {
        LogManager.logLevel = logLevel;
    }

    private static boolean sVerboseLoggingEnabled = true;

    /**
     * Indicates whether verbose logging is enabled.   If not, expensive calculations to create
     * log strings should be avoided.
     * @return
     */
    public static boolean isVerboseLoggingEnabled() {
        return sVerboseLoggingEnabled;
    }

    /**
     * Sets whether verbose logging is enabled.  If not, expensive calculations to create
     * log strings should be avoided.
     *
     * @param enabled
     */
    public static void setVerboseLoggingEnabled(boolean enabled) {
        sVerboseLoggingEnabled = enabled;
    }
    /**
     * Gets the currently set logger
     *
     * @see com.blakequ.blelibrary.logging.Logger
     * @return logger
     */
    public static Logger getLogger() {
        return sLogger;
    }


    private LogManager() {
        // no instances
    }

    /**
     * Send a verbose log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param args    Arguments for string formatting.
     */
    public static void v(String tag, String message, Object... args) {
        if (logLevel == LogLevel.VERBOSE){
            sLogger.v(tag, message, args);
        }
    }

    /**
     * Send a verbose log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param t       An exception to log.
     * @param args    Arguments for string formatting.
     */
    public static void v(Throwable t, String tag, String message, Object... args) {
        if (logLevel == LogLevel.VERBOSE){
            sLogger.v(t, tag, message, args);
        }
    }

    /**
     * Send a debug log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param args    Arguments for string formatting.
     */
    public static void d(String tag, String message, Object... args) {
        if (logLevel == LogLevel.VERBOSE){
            sLogger.d(tag, message, args);
        }
    }

    /**
     * Send a debug log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param t       An exception to log.
     * @param args    Arguments for string formatting.
     */
    public static void d(Throwable t, String tag, String message, Object... args) {
        if (logLevel == LogLevel.VERBOSE){
            sLogger.d(t, tag, message, args);
        }
    }

    /**
     * Send a info log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param args    Arguments for string formatting.
     */
    public static void i(String tag, String message, Object... args) {
        if (logLevel == LogLevel.VERBOSE){
            sLogger.i(tag, message, args);
        }
    }

    /**
     * Send a info log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param t       An exception to log.
     * @param args    Arguments for string formatting.
     */
    public static void i(Throwable t, String tag, String message, Object... args) {
        if (logLevel == LogLevel.VERBOSE){
            sLogger.i(t, tag, message, args);
        }
    }

    /**
     * Send a warning log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param args    Arguments for string formatting.
     */
    public static void w(String tag, String message, Object... args) {
        if (logLevel != LogLevel.ERROR && logLevel != LogLevel.NONE){
            sLogger.w(tag, message, args);
        }
    }

    /**
     * Send a warning log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param t       An exception to log.
     * @param args    Arguments for string formatting.
     */
    public static void w(Throwable t, String tag, String message, Object... args) {
        if (logLevel != LogLevel.ERROR && logLevel != LogLevel.NONE){
            sLogger.w(t, tag, message, args);
        }
    }

    /**
     * Send a error log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param args    Arguments for string formatting.
     */
    public static void e(String tag, String message, Object... args) {
        if (logLevel != LogLevel.NONE){
            sLogger.e(tag, message, args);
        }
    }

    /**
     * Send a error log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param t       An exception to log.
     * @param args    Arguments for string formatting.
     */
    public static void e(Throwable t, String tag, String message, Object... args) {
        if (logLevel != LogLevel.NONE){
            sLogger.e(t, tag, message, args);
        }
    }

    /**
     * 打印日志类型
     */
    public enum LogLevel{
        /**print all log*/
        VERBOSE(0),
        /**any print warning and error*/
        WARNING(1),
        /**only print error*/
        ERROR(2),
        /**not print any log*/
        NONE(3);

        public int type;
        LogLevel(int type){
            this.type = type;
        }
    }
}
