package com.hortonworks.streamline.streams.logsearch;

public class LogSearchResult {
    private String appId;
    private String componentName;
    private String logLevel;
    private String logMessage;

    // TODO: long? String?
    private long timestamp;

    private LogSearchResult(String appId, String componentName, String logLevel,
                            String logMessage, long timestamp) {
        this.appId = appId;
        this.componentName = componentName;
        this.logLevel = logLevel;
        this.logMessage = logMessage;
        this.timestamp = timestamp;
    }

    public String getAppId() {
        return appId;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public String getLogMessage() {
        return logMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
