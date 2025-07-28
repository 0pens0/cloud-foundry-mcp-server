package org.tanzu.cfpulse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tanzu.cfpulse.cf.*;

import java.util.List;

@Configuration
public class McpServerConfig {

    private static final Logger logger = LoggerFactory.getLogger(McpServerConfig.class);

    @Bean
    public List<ToolCallback> registerTools(
            CfApplicationService cfApplicationService,
            CfOrganizationService cfOrganizationService,
            CfServiceInstanceService cfServiceInstanceService,
            CfSpaceService cfSpaceService,
            CfRouteService cfRouteService,
            CfNetworkPolicyService cfNetworkPolicyService,
            HybridBuildpackCloner hybridBuildpackCloner) {

        return List.of(ToolCallbacks.from(cfApplicationService, cfOrganizationService, cfServiceInstanceService,
                cfSpaceService, cfRouteService, cfNetworkPolicyService, hybridBuildpackCloner));
    }
}
