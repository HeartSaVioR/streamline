package com.hortonworks.streamline.streams.logsearch.container;

import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.cluster.catalog.Component;
import com.hortonworks.streamline.streams.cluster.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.container.NamespaceAwareContainer;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.logsearch.TopologyLogSearch;
import com.hortonworks.streamline.streams.logsearch.container.mapping.MappedTopologyLogSearchImpl;

import javax.security.auth.Subject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TopologyLogSearchContainer extends NamespaceAwareContainer<TopologyLogSearch> {
    public static final String COMPONENT_NAME_INFRA_SOLR = ComponentPropertyPattern.INFRA_SOLR.name();
    public static final String SOLR_API_URL_KEY = "solrApiUrl";

    private final Subject subject;

    public TopologyLogSearchContainer(EnvironmentService environmentService, Subject subject) {
        super(environmentService);
        this.subject = subject;
    }

    @Override
    protected TopologyLogSearch initializeInstance(Namespace namespace) {
        MappedTopologyLogSearchImpl topologyLogSearchImpl;
        TopologyLogSearch topologyLogSearch;

        String logSearchService = namespace.getLogSearchService();
        if (logSearchService != null && !logSearchService.isEmpty()) {
            try {
                topologyLogSearchImpl = MappedTopologyLogSearchImpl.valueOf(logSearchService);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Unsupported log search service: " + logSearchService, e);
            }

            Map<String, Object> confLogSearch = buildAmbariInfraLogSearchConfigMap(namespace, logSearchService, subject);
            String className = topologyLogSearchImpl.getClassName();
            topologyLogSearch = initTopologyLogSearch(confLogSearch, className);
        } else {
            // TODO: provide DUMMY implementation
            topologyLogSearch = null;
        }

        return topologyLogSearch;
    }

    private TopologyLogSearch initTopologyLogSearch(Map<String, Object> conf, String className) {
        try {
            TopologyLogSearch topologyLogSearch = instantiate(className);
            topologyLogSearch.init(conf);
            return topologyLogSearch;
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException | ConfigException e) {
            throw new RuntimeException("Can't initialize Topology log search instance - Class Name: " + className, e);
        }
    }

    private Map<String, Object> buildAmbariInfraLogSearchConfigMap(Namespace namespace, String logSearchServiceName,
                                                                   Subject subject) {
        // Assuming that a namespace has one mapping of log search service
        Service logSearchService = getFirstOccurenceServiceForNamespace(namespace, logSearchServiceName);
        if (logSearchService == null) {
            throw new RuntimeException("Log search service " + logSearchServiceName + " is not associated to the namespace " +
                    namespace.getName() + "(" + namespace.getId() + ")");
        }

        Component infraSolr = getComponent(logSearchService, COMPONENT_NAME_INFRA_SOLR)
                .orElseThrow(() -> new RuntimeException(logSearchService + " doesn't have " + COMPONENT_NAME_INFRA_SOLR + " as component"));

        Collection<ComponentProcess> solrProcesses = environmentService.listComponentProcesses(infraSolr.getId());
        if (solrProcesses.isEmpty()) {
            throw new RuntimeException(logSearchService + " doesn't have any process for " + COMPONENT_NAME_INFRA_SOLR + " as component");
        }

        ComponentProcess solrProcess = solrProcesses.iterator().next();
        String solrHost = solrProcess.getHost();
        Integer solrPort = solrProcess.getPort();

        assertHostAndPort(COMPONENT_NAME_INFRA_SOLR, solrHost, solrPort);

        Map<String, Object> confForLogSearchService = new HashMap<>();
        confForLogSearchService.put(SOLR_API_URL_KEY, buildAmbariInfraSolrRestApiRootUrl(solrHost, solrPort));
        confForLogSearchService.put(TopologyLayoutConstants.SUBJECT_OBJECT, subject);
        return confForLogSearchService;
    }

    private String buildAmbariInfraSolrRestApiRootUrl(String host, Integer port) {
        return "http://" + host + ":" + port + "/solr";
    }
}
