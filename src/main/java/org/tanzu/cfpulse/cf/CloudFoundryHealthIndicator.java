package org.tanzu.cfpulse.cf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Health indicator for Cloud Foundry connectivity.
 * Tests the connection to the Cloud Foundry API and reports health status.
 */
@Component
public class CloudFoundryHealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(CloudFoundryHealthIndicator.class);
    
    private final CloudFoundryOperationsFactory operationsFactory;
    private final String apiHost;
    private final AtomicLong lastSuccessfulCheck = new AtomicLong(0);
    private final AtomicLong lastFailedCheck = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);

    public CloudFoundryHealthIndicator(CloudFoundryOperationsFactory operationsFactory,
                                      @Value("${cf.apiHost}") String apiHost) {
        this.operationsFactory = operationsFactory;
        this.apiHost = apiHost;
    }


    /**
     * Perform a health check for Cloud Foundry connectivity.
     * 
     * @return true if the connection is healthy, false otherwise
     */
    public boolean isHealthy() {
        try {
            logger.debug("Performing Cloud Foundry health check for API: {}", apiHost);
            
            // Test CF connectivity by listing organizations (lightweight operation)
            operationsFactory.getDefaultOperations()
                .organizations()
                .list()
                .blockFirst(Duration.ofSeconds(10));
            
            // Update success metrics
            lastSuccessfulCheck.set(System.currentTimeMillis());
            failureCount.set(0); // Reset failure count on success
            
            logger.debug("Cloud Foundry health check successful");
            return true;
                
        } catch (Exception e) {
            // Update failure metrics
            lastFailedCheck.set(System.currentTimeMillis());
            failureCount.incrementAndGet();
            
            logger.warn("Cloud Foundry health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get detailed health information.
     * 
     * @return Health information as a string
     */
    public String getHealthInfo() {
        boolean healthy = isHealthy();
        return String.format("CloudFoundry Health: %s, API: %s, Last Success: %d, Failures: %d", 
                           healthy ? "UP" : "DOWN", 
                           apiHost, 
                           lastSuccessfulCheck.get(), 
                           failureCount.get());
    }
    
    /**
     * Get the time since the last successful health check.
     * 
     * @return Duration since last successful check, or null if never successful
     */
    public Duration getTimeSinceLastSuccess() {
        long lastSuccess = lastSuccessfulCheck.get();
        if (lastSuccess == 0) {
            return null;
        }
        return Duration.ofMillis(System.currentTimeMillis() - lastSuccess);
    }
    
    /**
     * Get the number of consecutive failures.
     * 
     * @return Number of consecutive failures
     */
    public long getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * Check if the Cloud Foundry connection has been healthy recently.
     * 
     * @param maxAge Maximum age for a successful check to be considered recent
     * @return true if the connection was healthy within the specified time
     */
    public boolean isHealthyRecently(Duration maxAge) {
        long lastSuccess = lastSuccessfulCheck.get();
        if (lastSuccess == 0) {
            return false;
        }
        return Duration.ofMillis(System.currentTimeMillis() - lastSuccess).compareTo(maxAge) <= 0;
    }
}
