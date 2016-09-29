package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author iulia.cojocea
 */
@Test(groups = { "rest-api", "sites", "sanity" })
public class GetSiteMembersSanityTests extends RestTest
{
    @Autowired
    RestSitesApi siteAPI;

    @Autowired
    DataUser dataUser;

    @Autowired
    DataSite dataSite;

    private SiteModel siteModel;
    private UserModel adminUser;
    private ListUserWithRoles usersWithRoles;
    private UserModel userModel;

    @BeforeClass(alwaysRun=true)
    public void initTest() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        siteAPI.useRestClient(restClient);
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }

    @TestRail(section = {"rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Manager role gets site members and gets status code OK (200)")
    public void getSiteMembersWithManagerRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        siteAPI.getSiteMembers(siteModel);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = {"rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Collaborator role gets site members and gets status code OK (200)")
    public void getSiteMembersWithCollaboratorRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        siteAPI.getSiteMembers(siteModel);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = {"rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Contributor role gets site members and gets status code OK (200)")
    public void getSiteMembersWithContributorRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        siteAPI.getSiteMembers(siteModel);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = {"rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Consumer role gets site members and gets status code OK (200)")
    public void getSiteMembersWithConsumerRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        siteAPI.getSiteMembers(siteModel);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = {"rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with admin usere gets site members and gets status code OK (200)")
    public void getSiteMembersWithAdminUser() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUser);
        siteAPI.getSiteMembers(siteModel);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = {"rest-api", "sites" }, executionType = ExecutionType.SANITY, 
            description = "Failed authentication get site members call returns status code 401 with Manager role")
    public void getSiteMembersWithManagerRoleFailedAuth() throws JsonToModelConversionException, Exception
    {
        userModel = dataUser.createRandomTestUser();
        userModel.setPassword("user wrong password");
        dataUser.addUserToSite(userModel, siteModel, UserRole.SiteManager);
        restClient.authenticateUser(userModel);
        siteAPI.getSiteMembers(siteModel);
        siteAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
