package org.tanzu.cfpulse.cf;

import org.cloudfoundry.operations.spaceadmin.GetSpaceQuotaRequest;
import org.cloudfoundry.operations.spaceadmin.SpaceQuota;
import org.cloudfoundry.operations.spaces.CreateSpaceRequest;
import org.cloudfoundry.operations.spaces.DeleteSpaceRequest;
import org.cloudfoundry.operations.spaces.RenameSpaceRequest;
import org.cloudfoundry.operations.spaces.SpaceSummary;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CfSpaceService extends CfBaseService {

    private static final String SPACE_LIST = "Returns the spaces in a Cloud Foundry organization (org). Organization parameter is optional - if not provided or null, the configured default organization will be used automatically.";
    private static final String GET_SPACE_QUOTA = "Returns a quota (set of resource limits) scoped to a Cloud Foundry space. Organization parameter is optional - if not provided or null, the configured default organization will be used automatically.";
    private static final String CREATE_SPACE = "Create a new Cloud Foundry space in an organization. Organization parameter is optional - if not provided or null, the configured default organization will be used automatically.";
    private static final String DELETE_SPACE = "Delete a Cloud Foundry space. Organization parameter is optional - if not provided or null, the configured default organization will be used automatically.";
    private static final String RENAME_SPACE = "Rename a Cloud Foundry space. Organization parameter is optional - if not provided or null, the configured default organization will be used automatically.";
    
    private static final String SPACE_QUOTA_NAME_PARAM = "Name of the Cloud Foundry space quota";
    private static final String SPACE_NAME_PARAM = "Name of the Cloud Foundry space";
    private static final String SPACE_QUOTA_PARAM = "Name of the space quota to apply to the new space (optional)";
    private static final String NEW_SPACE_NAME_PARAM = "New name for the Cloud Foundry space";

    public CfSpaceService(CloudFoundryOperationsFactory operationsFactory) {
        super(operationsFactory);
    }

    @Tool(description = SPACE_LIST)
    public List<SpaceSummary> spacesList(
            @ToolParam(description = ORG_PARAM, required = false) String organization) {
        return getOperations(organization, null).spaces().list().collectList().block();
    }

    @Tool(description = GET_SPACE_QUOTA)
    public SpaceQuota getSpaceQuota(@ToolParam(description = SPACE_QUOTA_NAME_PARAM) String spaceName,
                                   @ToolParam(description = ORG_PARAM, required = false) String organization) {
        GetSpaceQuotaRequest request = GetSpaceQuotaRequest.builder().name(spaceName).build();
        return getOperations(organization, null).spaceAdmin().get(request).block();
    }

    @Tool(description = CREATE_SPACE)
    public void createSpace(@ToolParam(description = SPACE_NAME_PARAM) String spaceName,
                           @ToolParam(description = ORG_PARAM, required = false) String organization,
                           @ToolParam(description = SPACE_QUOTA_PARAM, required = false) String spaceQuota) {
        CreateSpaceRequest.Builder builder = CreateSpaceRequest.builder().name(spaceName);
        if (organization != null) {
            builder.organization(organization);
        }
        if (spaceQuota != null) {
            builder.spaceQuota(spaceQuota);
        }
        CreateSpaceRequest request = builder.build();
        getOperations(organization, null).spaces().create(request).block();
    }

    @Tool(description = DELETE_SPACE)
    public void deleteSpace(@ToolParam(description = SPACE_NAME_PARAM) String spaceName,
                           @ToolParam(description = ORG_PARAM, required = false) String organization) {
        DeleteSpaceRequest request = DeleteSpaceRequest.builder().name(spaceName).build();
        getOperations(organization, null).spaces().delete(request).block();
    }

    @Tool(description = RENAME_SPACE)
    public void renameSpace(@ToolParam(description = SPACE_NAME_PARAM) String currentSpaceName,
                           @ToolParam(description = NEW_SPACE_NAME_PARAM) String newSpaceName,
                           @ToolParam(description = ORG_PARAM, required = false) String organization) {
        RenameSpaceRequest request = RenameSpaceRequest.builder()
                .name(currentSpaceName)
                .newName(newSpaceName)
                .build();
        getOperations(organization, null).spaces().rename(request).block();
    }
}