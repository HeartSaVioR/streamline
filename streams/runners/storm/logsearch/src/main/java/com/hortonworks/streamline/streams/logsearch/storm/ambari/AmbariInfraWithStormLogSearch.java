package com.hortonworks.streamline.streams.logsearch.storm.ambari;

import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.logsearch.LogSearchCriteria;
import com.hortonworks.streamline.streams.logsearch.LogSearchResult;
import com.hortonworks.streamline.streams.logsearch.TopologyLogSearch;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Implementation of TopologyLogSearch for Ambari Infra (Solr) with Storm.
 * <p/>
 * This class assumes that worker logs are collected via LogFeeder with known configuration, and pushed to Ambari Infra (Solr).
 */
public class AmbariInfraWithStormLogSearch implements TopologyLogSearch {
    // the configuration keys
    static final String SOLR_API_URL_KEY = "solrApiUrl";
    static final String COLLECTION_NAME = "appId";

    public static final String COLUMN_NAME_STREAMLINE_TOPOLOGY_ID = "ws_streamline_topology_id";
    // FIXME: that should need more parsing...
    public static final String COLUMN_NAME_STREAMLINE_COMPONENT_NAME = "ws_streamline_component_name";
    public static final String COLUMN_NAME_LOG_TIME = "logtime";
    public static final String COLUMN_NAME_LOG_LEVEL = "level";
    public static final String COLUMN_NAME_LOG_MESSAGE = "log_message";

    public static final String DEFAULT_COLLECTION_NAME = "hadoop_logs";
    private HttpSolrClient solr;

    public AmbariInfraWithStormLogSearch() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Map<String, Object> conf) throws ConfigException {
        String solrApiUrl = null;
        String collectionName = null;
        if (conf != null) {
            solrApiUrl = (String) conf.get(SOLR_API_URL_KEY);
            collectionName = (String) conf.get(COLLECTION_NAME);
            if (collectionName == null) {
                collectionName = DEFAULT_COLLECTION_NAME;
            }
        }

        if (solrApiUrl == null || collectionName == null) {
            throw new ConfigException("'solrApiUrl' must be presented in configuration.");
        }

        solr = new HttpSolrClient.Builder(solrApiUrl + "/" + collectionName).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LogSearchResult> search(LogSearchCriteria logSearchCriteria) {
        SolrQuery query = new SolrQuery();
        query.setQuery(buildColumnAndValue(COLUMN_NAME_LOG_MESSAGE, logSearchCriteria.getSearchString()));
        query.addFilterQuery(buildColumnAndValue(COLUMN_NAME_STREAMLINE_TOPOLOGY_ID, logSearchCriteria.getAppId()));
        query.addFilterQuery(buildColumnAndValue(COLUMN_NAME_LOG_LEVEL, logSearchCriteria.getLogLevel()));
        query.addFilterQuery(buildColumnAndValue(COLUMN_NAME_STREAMLINE_COMPONENT_NAME, logSearchCriteria.getComponentName()));

        query.addFilterQuery(buildColumnAndValueWithDateRange(COLUMN_NAME_LOG_TIME, logSearchCriteria.getFrom(), logSearchCriteria.getTo()));
        query.addSort(COLUMN_NAME_LOG_TIME, SolrQuery.ORDER.asc);

        if (logSearchCriteria.getStart() != null) {
            query.setStart(logSearchCriteria.getStart());
        }
        if (logSearchCriteria.getLimit() != null) {
            query.setRows(logSearchCriteria.getLimit());
        }

        List<LogSearchResult> results = new ArrayList<>();
        try {
            QueryResponse response = solr.query(query);

            SolrDocumentList docList = response.getResults();

            for (SolrDocument document : docList) {
                String appId = (String) document.getFieldValue(COLUMN_NAME_STREAMLINE_TOPOLOGY_ID);
                String componentName = (String) document.getFieldValue(COLUMN_NAME_STREAMLINE_COMPONENT_NAME);
                String logLevel = (String) document.getFieldValue(COLUMN_NAME_LOG_LEVEL);
                String logMessage = (String) document.getFieldValue(COLUMN_NAME_LOG_MESSAGE);
                Date logDate = (Date) document.getFieldValue(COLUMN_NAME_LOG_TIME);
                long timestamp = logDate.toInstant().toEpochMilli();

                LogSearchResult result = new LogSearchResult(appId, componentName, logLevel, logMessage, timestamp);
                results.add(result);
            }

        } catch (SolrServerException | IOException e) {
            // TODO: any fine-grained control needed?
            throw new RuntimeException(e);
        }

        return results;
    }

    private String buildColumnAndValue(String column, String value) {
        if (value == null || value.isEmpty()) {
            value = "*";
        }
        return column + ":" + value;
    }

    private String buildColumnAndValueWithDateRange(String column, long from, long to) {
        String value = "%s:[%s TO %s]";

        Instant fromInstant = Instant.ofEpochMilli(from);
        Instant toInstant = Instant.ofEpochMilli(to);

        return String.format(value, column, fromInstant.toString(), toInstant.toString());
    }

}
