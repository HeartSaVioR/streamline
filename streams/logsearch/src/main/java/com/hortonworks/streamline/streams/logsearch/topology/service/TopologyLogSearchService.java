package com.hortonworks.streamline.streams.logsearch.topology.service;

import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.container.ContainingNamespaceAwareContainer;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.logsearch.LogSearchCriteria;
import com.hortonworks.streamline.streams.logsearch.LogSearchResult;
import com.hortonworks.streamline.streams.logsearch.TopologyLogSearch;
import com.hortonworks.streamline.streams.logsearch.container.TopologyLogSearchContainer;

import javax.security.auth.Subject;
import java.util.List;

public class TopologyLogSearchService implements ContainingNamespaceAwareContainer {
  private final EnvironmentService environmentService;
  private final TopologyLogSearchContainer topologyLogSearchContainer;

  public TopologyLogSearchService(EnvironmentService environmentService, Subject subject) {
    this.environmentService = environmentService;
    this.topologyLogSearchContainer = new TopologyLogSearchContainer(environmentService, subject);
  }

  public List<LogSearchResult> search(Topology topology, LogSearchCriteria criteria) {
    TopologyLogSearch topologyLogSearch = getTopologyLogSearchInstance(topology);
    return topologyLogSearch.search(criteria);
  }

  @Override
  public void invalidateInstance(Long namespaceId) {
    try {
      topologyLogSearchContainer.invalidateInstance(namespaceId);
    } catch (Throwable e) {
      // swallow
    }
  }

  private TopologyLogSearch getTopologyLogSearchInstance(Topology topology) {
    Namespace namespace = environmentService.getNamespace(topology.getNamespaceId());
    if (namespace == null) {
      throw new RuntimeException("Corresponding namespace not found: " + topology.getNamespaceId());
    }

    TopologyLogSearch topologyLogSearch = topologyLogSearchContainer.findInstance(namespace);
    if (topologyLogSearch == null) {
      throw new RuntimeException("Can't find Topology Log Search for such namespace " + topology.getNamespaceId());
    }
    return topologyLogSearch;
  }
}
