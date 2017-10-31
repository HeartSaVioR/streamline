package com.hortonworks.streamline.streams.logsearch;

public class LogSearchCriteria {
    private String appId;
    private String componentName;
    private String logLevel;
    private String searchString;
    private long from;
    private long to;
    private Integer start;
    private Integer limit;

    public String getAppId() {
        return appId;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public String getSearchString() {
        return searchString;
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    public Integer getStart() {
        return start;
    }

    public Integer getLimit() {
        return limit;
    }

    public LogSearchCriteria(String appId, String componentName, String logLevel, String searchString,
                             long from, long to, Integer start, Integer limit) {
        this.appId = appId;
        this.componentName = componentName;
        this.logLevel = logLevel;
        this.searchString = searchString;
        this.from = from;
        this.to = to;
        this.start = start;
        this.limit = limit;
    }

    public static class Builder {
        private String appId;
        private String componentName;
        private String logLevel;
        private String searchString;
        private long from;
        private long to;
        private Integer start;
        private Integer limit;

        public Builder(String appId, long from, long to) {
            this.appId = appId;
            this.from = from;
            this.to = to;
        }

        public Builder setComponentName(String componentName) {
            this.componentName = componentName;
            return this;
        }

        public Builder setLogLevel(String logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public Builder setSearchString(String searchString) {
            this.searchString = searchString;
            return this;
        }

        public Builder setStart(Integer start) {
            this.start = start;
            return this;
        }

        public Builder setLimit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public LogSearchCriteria build() {
            return new LogSearchCriteria(appId, componentName, logLevel, searchString, from, to, start, limit);
        }
    }
}
