package com.hortonworks.streamline.streams.logsearch;

import com.hortonworks.streamline.common.exception.ConfigException;

import java.util.List;
import java.util.Map;

public interface TopologyLogSearch {
    // Any one time initialization is done here
    void init (Map<String, Object> conf) throws ConfigException;

    List<LogSearchResult> search(LogSearchCriteria criteria);
}
