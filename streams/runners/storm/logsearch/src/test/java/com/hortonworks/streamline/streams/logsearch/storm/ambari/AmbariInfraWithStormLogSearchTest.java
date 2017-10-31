package com.hortonworks.streamline.streams.logsearch.storm.ambari;

import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.verification.FindRequestsResult;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.hortonworks.streamline.streams.logsearch.LogSearchCriteria;
import com.hortonworks.streamline.streams.logsearch.LogSearchResult;
import mockit.Deencapsulation;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_NAME_LOG_LEVEL;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_NAME_LOG_MESSAGE;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_NAME_LOG_TIME;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_NAME_STREAMLINE_COMPONENT_NAME;
import static com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch.COLUMN_NAME_STREAMLINE_TOPOLOGY_ID;
import static org.junit.Assert.*;

public class AmbariInfraWithStormLogSearchTest {
    private final String TEST_SOLR_API_PATH = "/solr";
    private final String TEST_COLLECTION_NAME = "test_collection";
    private final String STUB_REQUEST_API_PATH = TEST_SOLR_API_PATH + "/" + TEST_COLLECTION_NAME + "/select";

    private AmbariInfraWithStormLogSearch logSearch;
    private String buildTestSolrApiUrl;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(18886);

    public static final String TEST_APP_ID = "1";
    public static final long TEST_FROM = System.currentTimeMillis() - (1000 * 60 * 30);
    public static final long TEST_TO = System.currentTimeMillis();

    @Before
    public void setUp() throws Exception {
        logSearch = new AmbariInfraWithStormLogSearch();

        Map<String, Object> conf = new HashMap<>();
        buildTestSolrApiUrl = "http://localhost:18886" + TEST_SOLR_API_PATH;
        conf.put(AmbariInfraWithStormLogSearch.SOLR_API_URL_KEY, buildTestSolrApiUrl);
        conf.put(AmbariInfraWithStormLogSearch.COLLECTION_NAME, TEST_COLLECTION_NAME);

        logSearch.init(conf);

        // we are doing some hack to change parser, since default wt (javabin) would be faster
        // but not good to construct custom result by ourselves
        HttpSolrClient solrClient = Deencapsulation.getField(logSearch, "solr");
        solrClient.setParser(new XMLResponseParser());
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testSearchWithMinimumParameters() throws Exception {
        stubSolrUrl();

        LogSearchCriteria logSearchCriteria = new LogSearchCriteria.Builder(TEST_APP_ID, TEST_FROM, TEST_TO).build();
        List<LogSearchResult> results = logSearch.search(logSearchCriteria);
        verifyResults(results);

        // please note that space should be escaped to '+' since Wiremock doesn't handle it when matching...
        String dateRangeValue = "%s:[%s+TO+%s]";

        Instant fromInstant = Instant.ofEpochMilli(TEST_FROM);
        Instant toInstant = Instant.ofEpochMilli(TEST_TO);

        dateRangeValue = String.format(dateRangeValue, COLUMN_NAME_LOG_TIME, fromInstant.toString(), toInstant.toString());

        List<LoggedRequest> requests = wireMockRule.findAll(getRequestedFor(urlPathEqualTo(STUB_REQUEST_API_PATH)));
        assertEquals(1, requests.size());

        LoggedRequest request = requests.get(0);

        QueryParameter qParam = request.queryParameter("q");
        assertTrue(qParam.containsValue(COLUMN_NAME_LOG_MESSAGE + ":*"));

        QueryParameter fqParam = request.queryParameter("fq");
        assertTrue(fqParam.containsValue(COLUMN_NAME_STREAMLINE_TOPOLOGY_ID + ":" + TEST_APP_ID));
        assertTrue(fqParam.containsValue(COLUMN_NAME_STREAMLINE_COMPONENT_NAME + ":*"));
        assertTrue(fqParam.containsValue(COLUMN_NAME_LOG_LEVEL + ":*"));
        assertTrue(fqParam.containsValue(dateRangeValue));

        QueryParameter sortParam = request.queryParameter("sort");
        assertTrue(sortParam.containsValue(COLUMN_NAME_LOG_TIME + "+asc"));
    }

    @Test
    public void testSearchWithFullParameters() throws Exception {
        stubSolrUrl();

        int testStart = 100;
        int testLimit = 2000;
        String testLogLevel = "INFO";
        String testSearchString = "helloworld";
        String testComponentName = "testComponent";

        LogSearchCriteria logSearchCriteria = new LogSearchCriteria.Builder(TEST_APP_ID, TEST_FROM, TEST_TO)
            .setLogLevel(testLogLevel)
            .setSearchString(testSearchString)
            .setComponentName(testComponentName)
            .setStart(testStart)
            .setLimit(testLimit)
            .build();

        List<LogSearchResult> results = logSearch.search(logSearchCriteria);

        // note that the result doesn't change given that we just provide same result from file
        verifyResults(results);

        // please note that space should be escaped to '+' since Wiremock doesn't handle it when matching...
        String dateRangeValue = "%s:[%s+TO+%s]";

        Instant fromInstant = Instant.ofEpochMilli(TEST_FROM);
        Instant toInstant = Instant.ofEpochMilli(TEST_TO);

        dateRangeValue = String.format(dateRangeValue, COLUMN_NAME_LOG_TIME, fromInstant.toString(), toInstant.toString());

        List<LoggedRequest> requests = wireMockRule.findAll(getRequestedFor(urlPathEqualTo(STUB_REQUEST_API_PATH)));
        assertEquals(1, requests.size());

        LoggedRequest request = requests.get(0);

        QueryParameter qParam = request.queryParameter("q");
        assertTrue(qParam.containsValue(COLUMN_NAME_LOG_MESSAGE + ":" + testSearchString));

        QueryParameter fqParam = request.queryParameter("fq");
        assertTrue(fqParam.containsValue(COLUMN_NAME_STREAMLINE_TOPOLOGY_ID + ":" + TEST_APP_ID));
        assertTrue(fqParam.containsValue(COLUMN_NAME_STREAMLINE_COMPONENT_NAME + ":" + testComponentName));
        assertTrue(fqParam.containsValue(COLUMN_NAME_LOG_LEVEL + ":" + testLogLevel));
        assertTrue(fqParam.containsValue(COLUMN_NAME_LOG_LEVEL + ":" + testLogLevel));
        assertTrue(fqParam.containsValue(dateRangeValue));

        QueryParameter sortParam = request.queryParameter("sort");
        assertTrue(sortParam.containsValue(COLUMN_NAME_LOG_TIME + "+asc"));

        QueryParameter startParam = request.queryParameter("start");
        assertTrue(startParam.containsValue(String.valueOf(testStart)));

        QueryParameter rowsParam = request.queryParameter("rows");
        assertTrue(rowsParam.containsValue(String.valueOf(testLimit)));
    }

    private void verifyResults(List<LogSearchResult> results) {
        // 5 static rows are presented in pre-stored result for making just test simpler...
        // please refer 'ambari-infra-log-search-output.xml'
        assertEquals(5, results.size());

        LogSearchResult logSearchResult = results.get(0);
        assertEquals("1", logSearchResult.getAppId());
        assertNull(logSearchResult.getComponentName());
        assertEquals("INFO", logSearchResult.getLogLevel());
        assertTrue(logSearchResult.getLogMessage().startsWith("Timeouts disabled for"));
        assertEquals(Instant.parse("2017-10-31T07:12:04.628Z"), Instant.ofEpochMilli(logSearchResult.getTimestamp()));

        logSearchResult = results.get(1);
        assertEquals("1", logSearchResult.getAppId());
        assertNull(logSearchResult.getComponentName());
        assertEquals("DEBUG", logSearchResult.getLogLevel());
        assertTrue(logSearchResult.getLogMessage().startsWith("Started with log levels: "));
        assertEquals(Instant.parse("2017-10-31T07:12:04.63Z"), Instant.ofEpochMilli(logSearchResult.getTimestamp()));

        logSearchResult = results.get(2);
        assertEquals("1", logSearchResult.getAppId());
        assertEquals("TRUCK_EVENTS", logSearchResult.getComponentName());
        assertEquals("INFO", logSearchResult.getLogLevel());
        assertTrue(logSearchResult.getLogMessage().startsWith("Opening spout 1-TRUCK_EVENTS"));
        assertEquals(Instant.parse("2017-10-31T07:12:04.633Z"), Instant.ofEpochMilli(logSearchResult.getTimestamp()));

        logSearchResult = results.get(3);
        assertEquals("1", logSearchResult.getAppId());
        assertEquals("TRUCK_EVENTS", logSearchResult.getComponentName());
        assertEquals("INFO", logSearchResult.getLogLevel());
        assertTrue(logSearchResult.getLogMessage().startsWith("Kafka Spout opened with the following"));
        assertEquals(Instant.parse("2017-10-31T07:12:04.634Z"), Instant.ofEpochMilli(logSearchResult.getTimestamp()));

        logSearchResult = results.get(4);
        assertEquals("1", logSearchResult.getAppId());
        assertNull(logSearchResult.getComponentName());
        assertEquals("INFO", logSearchResult.getLogLevel());
        assertTrue(logSearchResult.getLogMessage().startsWith("Preparing bolt __system"));
        assertEquals(Instant.parse("2017-10-31T07:12:04.636Z"), Instant.ofEpochMilli(logSearchResult.getTimestamp()));
    }

    private void stubSolrUrl() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/ambari-infra-log-search-output.xml")) {
            String body = IOUtils.toString(is, Charset.forName("UTF-8"));

            wireMockRule.stubFor(get(urlPathEqualTo(STUB_REQUEST_API_PATH))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/xml; charset=UTF-8")
                            .withHeader("Transfer-Encoding", "chunked")
                            .withBody(body)));
        }
    }

}
