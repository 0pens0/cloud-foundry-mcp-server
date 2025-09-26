package org.tanzu.cfpulse;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tanzu.cfpulse.cf.*;
import org.tanzu.cfpulse.clone.CfApplicationCloner;

import java.util.List;

@Configuration
public class McpServerConfig {

    @Bean
    public List<ToolCallback> registerTools(
            CfApplicationService cfApplicationService,
            CfOrganizationService cfOrganizationService,
            CfServiceInstanceService cfServiceInstanceService,
            CfSpaceService cfSpaceService,
            CfRouteService cfRouteService,
            CfNetworkPolicyService cfNetworkPolicyService,
            CfApplicationCloner cfApplicationCloner,
            CfTargetService cfTargetService) {

        return List.of(ToolCallbacks.from(cfApplicationService, cfOrganizationService, cfServiceInstanceService,
                cfSpaceService, cfRouteService, cfNetworkPolicyService, cfApplicationCloner, cfTargetService));
    }
}
