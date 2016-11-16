package org.alfresco.rest.ratings;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestRatingModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.ErrorModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
public class GetRatingSanityTests extends RestTest
{

    private SiteModel siteModel;
    private UserModel adminUser;
    private FolderModel folderModel;
    private FileModel document;   
    private ListUserWithRoles usersWithRoles;
    private RestRatingModel restRatingModel;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, 
                UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);                
    }
    
    @BeforeMethod()
    public void setUp() throws DataPreparationException, Exception {
        folderModel = dataContent.usingUser(adminUser).usingSite(siteModel).createFolder();
        document = dataContent.usingUser(adminUser).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Manager role is able to retrieve rating of a document")
    public void managerIsAbleToRetrieveRating() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));

        restClient.onCoreAPI().usingResource(document).likeDocument();
        restClient.onCoreAPI().usingResource(document).rateStarsToDocument(5);
        
        restRatingModel = restClient.onCoreAPI().usingResource(document).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("likes").and().field("myRating").is("true");
        
        restRatingModel = restClient.onCoreAPI().usingResource(document).getFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("fiveStar").and().field("myRating").is("5");
    }   
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Collaborator role is able to retrieve rating of a document")
    public void collaboratorIsAbleToRetrieveRating() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));

        restClient.onCoreAPI().usingResource(document).likeDocument();
        restClient.onCoreAPI().usingResource(document).rateStarsToDocument(5);
        
        restRatingModel = restClient.onCoreAPI().usingResource(document).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("likes").and().field("myRating").is("true");
        
        restRatingModel = restClient.onCoreAPI().usingResource(document).getFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("fiveStar").and().field("myRating").is("5");
    }   
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Contributor role is able to retrieve rating of a document")
    public void contributorIsAbleToRetrieveRating() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));

        restClient.onCoreAPI().usingResource(document).likeDocument();
        restClient.onCoreAPI().usingResource(document).rateStarsToDocument(5);
        
        restRatingModel = restClient.onCoreAPI().usingResource(document).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("likes").and().field("myRating").is("true");
        
        restRatingModel = restClient.onCoreAPI().usingResource(document).getFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("fiveStar").and().field("myRating").is("5");
    }   
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Consumer role is able to retrieve rating of a document")
    public void consumerIsAbleToRetrieveRating() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));

        restClient.onCoreAPI().usingResource(document).likeDocument();
        restClient.onCoreAPI().usingResource(document).rateStarsToDocument(5);
        
        restRatingModel = restClient.onCoreAPI().usingResource(document).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("likes").and().field("myRating").is("true");
        
        restRatingModel = restClient.onCoreAPI().usingResource(document).getFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("fiveStar").and().field("myRating").is("5");
    }   
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify admin user is able to retrieve rating of a document")
    public void adminIsAbleToRetrieveRating() throws Exception
    {
        document = dataContent.usingUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);

        restClient.authenticateUser(adminUser);
        restClient.onCoreAPI().usingResource(document).likeDocument();
        restClient.onCoreAPI().usingResource(document).rateStarsToDocument(5);

        restRatingModel = restClient.onCoreAPI().usingResource(document).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("likes").and().field("myRating").is("true");
        
        restRatingModel = restClient.onCoreAPI().usingResource(document).getFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restRatingModel.assertThat().field("id").is("fiveStar").and().field("myRating").is("5");
    }   
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.SANITY, 
            description = "Verify unauthenticated user is not able to retrieve rating of a document")
    @Bug(id = "MNT-16904")
    public void unauthenticatedUserIsNotAbleToRetrieveRating() throws Exception
    {
        restClient.authenticateUser(adminUser);
        restClient.onCoreAPI().usingResource(document).likeDocument();
        restClient.onCoreAPI().usingResource(document).rateStarsToDocument(5);

        restClient.authenticateUser(new UserModel("random user", "random password"));
        
        restClient.onCoreAPI().usingResource(document).getLikeRating();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError().containsSummary(ErrorModel.AUTHENTICATION_FAILED);
        
        restClient.onCoreAPI().usingResource(document).getFiveStarRating();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError().containsSummary(ErrorModel.AUTHENTICATION_FAILED);
    }   
}