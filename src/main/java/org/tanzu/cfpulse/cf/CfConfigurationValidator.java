package org.tanzu.cfpulse.cf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Validates Cloud Foundry configuration on application startup
 */
@Component
public class CfConfigurationValidator {

    private static final Logger logger = LoggerFactory.getLogger(CfConfigurationValidator.class);

    private final String apiHost;
    private final String username;
    private final String password;
    private final String organization;
    private final String space;

    public CfConfigurationValidator(
            @Value("${cf.apiHost:}") String apiHost,
            @Value("${cf.username:}") String username,
            @Value("${cf.password:}") String password,
            @Value("${cf.organization:}") String organization,
            @Value("${cf.space:}") String space) {
        this.apiHost = apiHost;
        this.username = username;
        this.password = password;
        this.organization = organization;
        this.space = space;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        logger.info("Validating Cloud Foundry configuration...");
        
        boolean hasErrors = false;
        
        if (!StringUtils.hasText(apiHost)) {
            logger.error("CF API host is not configured. Set cf.apiHost or CF_APIHOST environment variable.");
            hasErrors = true;
        }
        
        if (!StringUtils.hasText(username)) {
            logger.error("CF username is not configured. Set cf.username or CF_USERNAME environment variable.");
            hasErrors = true;
        }
        
        if (!StringUtils.hasText(password)) {
            logger.error("CF password is not configured. Set cf.password or CF_PASSWORD environment variable.");
            hasErrors = true;
        }
        
        if (!StringUtils.hasText(organization)) {
            logger.warn("CF organization is not configured. Set cf.organization or CF_ORG environment variable.");
        }
        
        if (!StringUtils.hasText(space)) {
            logger.warn("CF space is not configured. Set cf.space or CF_SPACE environment variable.");
        }
        
        if (hasErrors) {
            logger.error("Cloud Foundry configuration validation failed. Please check the configuration and restart the application.");
            throw new IllegalStateException("Cloud Foundry configuration is incomplete. Check logs for details.");
        }
        
        logger.info("Cloud Foundry configuration validation passed. API: {}, Org: {}, Space: {}", 
                   apiHost, organization, space);
    }
}




