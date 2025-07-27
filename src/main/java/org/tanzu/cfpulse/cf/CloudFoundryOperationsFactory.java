package org.tanzu.cfpulse.cf;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
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
    private final String defaultOrganization;
    private final String defaultSpace;
    
    private final ConcurrentHashMap<String, CloudFoundryOperations> operationsCache;
    private volatile CloudFoundryOperations defaultOperations;

    public CloudFoundryOperationsFactory(CloudFoundryClient cloudFoundryClient,
                                       DopplerClient dopplerClient,
                                       UaaClient uaaClient,
                                       @Value("${cf.organization}") String defaultOrganization,
                                       @Value("${cf.space}") String defaultSpace) {
        this.cloudFoundryClient = cloudFoundryClient;
        this.dopplerClient = dopplerClient;
        this.uaaClient = uaaClient;
        this.defaultOrganization = defaultOrganization;
        this.defaultSpace = defaultSpace;
        this.operationsCache = new ConcurrentHashMap<>();
    }

    public CloudFoundryOperations getDefaultOperations() {
        if (defaultOperations == null) {
            synchronized (this) {
                if (defaultOperations == null) {
                    logger.debug("Creating default CloudFoundryOperations for org={}, space={}", 
                               defaultOrganization, defaultSpace);
                    defaultOperations = createOperations(defaultOrganization, defaultSpace);
                }
            }
        }
        return defaultOperations;
    }

    public CloudFoundryOperations getOperations(String organization, String space) {
        String resolvedOrg = organization != null ? organization : defaultOrganization;
        String resolvedSpace = space != null ? space : defaultSpace;
        
        if (resolvedOrg.equals(defaultOrganization) && resolvedSpace.equals(defaultSpace)) {
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
        return defaultSpace;
    }

    private CloudFoundryOperations createOperations(String organization, String space) {
        return DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(cloudFoundryClient)
                .dopplerClient(dopplerClient)
                .uaaClient(uaaClient)
                .organization(organization)
                .space(space)
                .build();
    }

    private String createCacheKey(String organization, String space) {
        return organization + ":" + space;
    }
}