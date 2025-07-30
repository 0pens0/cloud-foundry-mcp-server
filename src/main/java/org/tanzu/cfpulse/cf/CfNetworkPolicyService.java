package org.tanzu.cfpulse.cf;

import org.cloudfoundry.operations.networkpolicies.AddNetworkPolicyRequest;
import org.cloudfoundry.operations.networkpolicies.ListNetworkPoliciesRequest;
import org.cloudfoundry.operations.networkpolicies.Policy;
import org.cloudfoundry.operations.networkpolicies.RemoveNetworkPolicyRequest;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CfNetworkPolicyService extends CfBaseService {

    private static final String ADD_NETWORK_POLICY = "Add a network policy to allow communication between applications. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String LIST_NETWORK_POLICIES = "List network policies in a Cloud Foundry space. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    private static final String REMOVE_NETWORK_POLICY = "Remove a network policy between applications. Organization and space parameters are optional - if not provided or null, the configured default org/space will be used automatically.";
    
    private static final String SOURCE_APP_PARAM = "Name of the source application that will initiate the network connection";
    private static final String DEST_APP_PARAM = "Name of the destination application that will receive the network connection";
    private static final String PROTOCOL_PARAM = "Network protocol for the policy (tcp or udp)";
    private static final String PORTS_PARAM = "Port or port range for the policy (e.g., '8080' or '8080-8090')";

    public CfNetworkPolicyService(CloudFoundryOperationsFactory operationsFactory) {
        super(operationsFactory);
    }

    @Tool(description = ADD_NETWORK_POLICY)
    public void addNetworkPolicy(@ToolParam(description = SOURCE_APP_PARAM) String sourceApp,
                                @ToolParam(description = DEST_APP_PARAM) String destinationApp,
                                @ToolParam(description = PROTOCOL_PARAM) String protocol,
                                @ToolParam(description = PORTS_PARAM) String ports,
                                @ToolParam(description = ORG_PARAM, required = false) String organization,
                                @ToolParam(description = SPACE_PARAM, required = false) String space) {
        String[] portParts = ports.split("-");
        Integer startPort = Integer.parseInt(portParts[0]);
        Integer endPort = portParts.length > 1 ? Integer.parseInt(portParts[1]) : null;
        
        AddNetworkPolicyRequest request = AddNetworkPolicyRequest.builder()
                .source(sourceApp)
                .destination(destinationApp)
                .protocol(protocol)
                .startPort(startPort)
                .endPort(endPort)
                .build();
        getOperations(organization, space).networkPolicies().add(request).blockLast();
    }

    @Tool(description = LIST_NETWORK_POLICIES)
    public List<Policy> listNetworkPolicies(@ToolParam(description = ORG_PARAM, required = false) String organization,
                                           @ToolParam(description = SPACE_PARAM, required = false) String space) {
        ListNetworkPoliciesRequest request = ListNetworkPoliciesRequest.builder().build();
        return getOperations(organization, space).networkPolicies().list(request).collectList().block();
    }

    @Tool(description = REMOVE_NETWORK_POLICY)
    public void removeNetworkPolicy(@ToolParam(description = SOURCE_APP_PARAM) String sourceApp,
                                   @ToolParam(description = DEST_APP_PARAM) String destinationApp,
                                   @ToolParam(description = PROTOCOL_PARAM) String protocol,
                                   @ToolParam(description = PORTS_PARAM) String ports,
                                   @ToolParam(description = ORG_PARAM, required = false) String organization,
                                   @ToolParam(description = SPACE_PARAM, required = false) String space) {
        String[] portParts = ports.split("-");
        Integer startPort = Integer.parseInt(portParts[0]);
        Integer endPort = portParts.length > 1 ? Integer.parseInt(portParts[1]) : null;
        
        RemoveNetworkPolicyRequest request = RemoveNetworkPolicyRequest.builder()
                .source(sourceApp)
                .destination(destinationApp)
                .protocol(protocol)
                .startPort(startPort)
                .endPort(endPort)
                .build();
        getOperations(organization, space).networkPolicies().remove(request).blockLast();
    }
}