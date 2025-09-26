package org.tanzu.cfpulse.cf;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.networking.NetworkingClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.uaa.UaaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CloudFoundryOperationsFactory {

    private static final Logger logger = LoggerFactory.getLogger(CloudFoundryOperationsFactory.class);

    private final CloudFoundryClient cloudFoundryClient;
    private final DopplerClient dopplerClient;
    private final UaaClient uaaClient;
    private final NetworkingClient networkingClient;
    private final String configDefaultOrganization;
    private final String configDefaultSpace;
    
    private final ConcurrentHashMap<String, CloudFoundryOperations> operationsCache;
    private volatile CloudFoundryOperations defaultOperations;
    
    // Dynamic target that can be set at runtime
    private volatile String currentTargetOrganization;
    private volatile String currentTargetSpace;

    public CloudFoundryOperationsFactory(CloudFoundryClient cloudFoundryClient,
                                       DopplerClient dopplerClient,
                                       UaaClient uaaClient,
                                       NetworkingClient networkingClient,
                                       @Value("${cf.organization}") String configDefaultOrganization,
                                       @Value("${cf.space}") String configDefaultSpace) {
        this.cloudFoundryClient = cloudFoundryClient;
        this.dopplerClient = dopplerClient;
        this.uaaClient = uaaClient;
        this.networkingClient = networkingClient;
        this.configDefaultOrganization = configDefaultOrganization;
        this.configDefaultSpace = configDefaultSpace;
        this.operationsCache = new ConcurrentHashMap<>();
    }

    public CloudFoundryOperations getDefaultOperations() {
        if (defaultOperations == null) {
            synchronized (this) {
                if (defaultOperations == null) {
                    String org = getCurrentOrganization();
                    String space = getCurrentSpace();
                    logger.debug("Creating default CloudFoundryOperations for org={}, space={}", org, space);
                    defaultOperations = createOperations(org, space);
                }
            }
        }
        return defaultOperations;
    }

    public CloudFoundryOperations getOperations(String organization, String space) {
        String resolvedOrg = organization != null ? organization : getCurrentOrganization();
        String resolvedSpace = space != null ? space : getCurrentSpace();
        
        if (resolvedOrg.equals(getCurrentOrganization()) && resolvedSpace.equals(getCurrentSpace())) {
            return getDefaultOperations();
        }
        
        String cacheKey = createCacheKey(resolvedOrg, resolvedSpace);
        return operationsCache.computeIfAbsent(cacheKey, key -> {
            logger.debug("Creating new CloudFoundryOperations for org={}, space={}", 
                       resolvedOrg, resolvedSpace);
            return createOperations(resolvedOrg, resolvedSpace);
        });
    }

    public void clearCache() {
        logger.info("Clearing CloudFoundryOperations cache containing {} entries", operationsCache.size());
        operationsCache.clear();
        synchronized (this) {
            defaultOperations = null;
        }
    }

    public Set<String> getCachedContexts() {
        return Set.copyOf(operationsCache.keySet());
    }

    public int getCacheSize() {
        return operationsCache.size();
    }

    public String getDefaultSpace() {
        return getCurrentSpace();
    }

    public String getDefaultOrganization() {
        return getCurrentOrganization();
    }

    /**
     * Set the current target organization and space.
     * This will invalidate the default operations cache.
     */
    public void setDefaultTarget(String organization, String space) {
        logger.info("Setting default target: org={}, space={}", organization, space);
        this.currentTargetOrganization = organization;
        this.currentTargetSpace = space;
        
        // Invalidate the default operations cache
        synchronized (this) {
            this.defaultOperations = null;
        }
    }

    /**
     * Clear the current target, reverting to configuration defaults.
     */
    public void clearDefaultTarget() {
        logger.info("Clearing default target, reverting to configuration defaults");
        this.currentTargetOrganization = null;
        this.currentTargetSpace = null;
        
        // Invalidate the default operations cache
        synchronized (this) {
            this.defaultOperations = null;
        }
    }

    /**
     * Get the current organization (dynamic target or config default).
     */
    private String getCurrentOrganization() {
        return currentTargetOrganization != null ? currentTargetOrganization : configDefaultOrganization;
    }

    /**
     * Get the current space (dynamic target or config default).
     */
    private String getCurrentSpace() {
        return currentTargetSpace != null ? currentTargetSpace : configDefaultSpace;
    }

    private CloudFoundryOperations createOperations(String organization, String space) {
        return DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(cloudFoundryClient)
                .dopplerClient(dopplerClient)
                .uaaClient(uaaClient)
                .networkingClient(networkingClient)
                .organization(organization)
                .space(space)
                .build();
    }

    private String createCacheKey(String organization, String space) {
        return organization + ":" + space;
    }
}