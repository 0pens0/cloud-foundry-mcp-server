package org.tanzu.cfpulse.cf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * Service for managing Cloud Foundry targeting operations.
 * Provides tools to set, get, and clear the target organization and space.
 */
@Service
public class CfTargetService extends CfBaseService {

    private static final Logger logger = LoggerFactory.getLogger(CfTargetService.class);

    public CfTargetService(CloudFoundryOperationsFactory operationsFactory) {
        super(operationsFactory);
    }

    /**
     * Set the target organization and space for Cloud Foundry operations.
     * This updates the default operations used when no organization/space is specified.
     */
    @Tool(description = "Set the target organization and space for Cloud Foundry operations. This affects all subsequent operations that don't specify organization/space parameters.")
    public void targetCf(
            @ToolParam(description = "Name of the Cloud Foundry organization") String organization,
            @ToolParam(description = "Name of the Cloud Foundry space") String space) {
        
        logger.info("Setting CF target: org={}, space={}", organization, space);
        
        // Validate that the organization and space exist
        try {
            // Test the connection by getting operations for this org/space
            var operations = getOperations(organization, space);
            
            // Try to list spaces in the organization to validate it exists
            operations.spaces().list().collectList().block();
            
            // Update the default operations in the factory
            operationsFactory.setDefaultTarget(organization, space);
            
            logger.info("Successfully set CF target: org={}, space={}", organization, space);
            
        } catch (Exception e) {
            logger.error("Failed to set CF target: org={}, space={}. Error: {}", organization, space, e.getMessage());
            throw new RuntimeException("Failed to set CF target: " + e.getMessage(), e);
        }
    }

    /**
     * Get the current target organization and space.
     */
    @Tool(description = "Get the current target organization and space for Cloud Foundry operations.")
    public TargetInfo getCurrentTarget() {
        String currentOrg = operationsFactory.getDefaultOrganization();
        String currentSpace = operationsFactory.getDefaultSpace();
        
        logger.debug("Current CF target: org={}, space={}", currentOrg, currentSpace);
        
        return new TargetInfo(currentOrg, currentSpace);
    }

    /**
     * Clear the current target, reverting to configuration defaults.
     */
    @Tool(description = "Clear the current target organization and space, reverting to configuration defaults.")
    public void clearTarget() {
        logger.info("Clearing CF target, reverting to configuration defaults");
        operationsFactory.clearDefaultTarget();
        logger.info("CF target cleared");
    }

    /**
     * Information about the current target.
     */
    public static class TargetInfo {
        private final String organization;
        private final String space;
        private final boolean isConfigured;

        public TargetInfo(String organization, String space) {
            this.organization = organization;
            this.space = space;
            this.isConfigured = organization != null && space != null && 
                              !organization.trim().isEmpty() && !space.trim().isEmpty();
        }

        public String getOrganization() {
            return organization;
        }

        public String getSpace() {
            return space;
        }

        public boolean isConfigured() {
            return isConfigured;
        }

        @Override
        public String toString() {
            if (isConfigured) {
                return String.format("Organization: %s, Space: %s", organization, space);
            } else {
                return "No target set (using configuration defaults)";
            }
        }
    }
}
