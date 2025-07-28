package org.tanzu.cfpulse.cf;

import org.cloudfoundry.operations.CloudFoundryOperations;

public abstract class CfBaseService {

    protected final CloudFoundryOperationsFactory operationsFactory;

    protected static final String ORG_PARAM = "Name of the Cloud Foundry organization. Optional - can be null or omitted to use the configured default organization.";
    protected static final String SPACE_PARAM = "Name of the Cloud Foundry space. Optional - can be null or omitted to use the configured default space.";
    protected static final String NAME_PARAM = "Name of the Cloud Foundry application";

    public CfBaseService(CloudFoundryOperationsFactory operationsFactory) {
        this.operationsFactory = operationsFactory;
    }

    protected CloudFoundryOperations getOperations(String organization, String space) {
        if (organization == null && space == null) {
            return operationsFactory.getDefaultOperations();
        }
        return operationsFactory.getOperations(organization, space);
    }
}