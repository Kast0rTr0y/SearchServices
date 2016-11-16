package org.alfresco.rest.ratings;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestRatingModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.SANITY })
public class AddRatingSanityTests extends RestTest
{
    private UserModel userModel;
    private SiteModel siteModel;
    private UserModel adminUser;
    private FolderModel folderModel;
    private FileModel document;
    private ListUserWithRoles usersWithRoles;
    private RestRatingModel returnedRatingModel; //placeholder for returned model

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        userModel = dataUser.createUser(RandomStringUtils.randomAlphanumeric(20));
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();

        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                UserRole.SiteContributor);
    }

    @BeforeMethod
    public void setUp() throws Exception
    {
        folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Manager role is able to post like rating to a document")
    public void managerIsAbleToLikeDocument() throws Exception
    {
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                                                        .onCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        returnedRatingModel.assertThat().field("myRating").is("true")
        	                 .and().field("id").is("likes")
        	                 .and().field("aggregate").isNotEmpty();        
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Collaborator role is able to post like rating to a document")
    public void collaboratorIsAbleToLikeDocument() throws Exception
    {
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                                                       .onCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        returnedRatingModel.assertThat().field("myRating").is("true")
                      		 .and().field("id").is("likes")
                      		 .and().field("aggregate").isNotEmpty();        
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Contributor role is able to post like rating to a document")
    public void contributorIsAbleToLikeDocument() throws Exception
    {
         returnedRatingModel =restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).onCoreAPI().usingResource(document).likeDocument();
         restClient.assertStatusCodeIs(HttpStatus.CREATED);
         
         returnedRatingModel.assertThat().field("myRating").is("true")
                            .and().field("id").is("likes")
                            .and().field("aggregate").isNotEmpty(); 
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Consumer role is able to post like rating to a document")
    public void consumerIsAbleToLikeDocument() throws Exception
    {
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).onCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        returnedRatingModel.assertThat().field("myRating").is("true")
                           .and().field("id").is("likes")
                           .and().field("aggregate").isNotEmpty(); 
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify admin user is able to post like rating to a document")
    public void adminIsAbleToLikeDocument() throws Exception
    {
        returnedRatingModel = restClient.authenticateUser(adminUser).onCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        returnedRatingModel.assertThat().field("myRating").is("true")
                           .and().field("id").is("likes")
                           .and().field("aggregate").isNotEmpty(); 
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify unauthenticated user is not able to post like rating to a document")
    @Bug(id = "MNT-16904")
    public void unauthenticatedUserIsNotAbleToLikeDocument() throws Exception
    {
        restClient.authenticateUser(new UserModel("random user", "random password")).onCoreAPI().usingResource(document).likeDocument();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Manager role is able to post stars rating to a document")
    public void managerIsAbleToAddStarsToDocument() throws Exception
    {
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager)).onCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("5")
                      		 .and().field("id").is("fiveStar")
                      		 .and().field("aggregate").isNotEmpty();        
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Collaborator role is able to post stars rating to a document")
    public void collaboratorIsAbleToAddStarsToDocument() throws Exception
    {
       returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator)).onCoreAPI().usingResource(document).rateStarsToDocument(5);
       restClient.assertStatusCodeIs(HttpStatus.CREATED);
       returnedRatingModel.assertThat().field("myRating").is("5")
                          .and().field("id").is("fiveStar")
                          .and().field("aggregate").isNotEmpty();
       
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Contributor role is able to post stars rating to a document")
    public void contributorIsAbleToAddStarsToDocument() throws Exception
    {
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor)).onCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("5")
                           .and().field("id").is("fiveStar")
                           .and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify user with Consumer role is able to post stars rating to a document")
    public void consumerIsAbleToAddStarsToDocument() throws Exception
    {
        returnedRatingModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).onCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is("5")
                           .and().field("id").is("fiveStar")
                           .and().field("aggregate").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify admin user is able to post stars rating to a document")
    public void adminIsAbleToAddStarsToDocument() throws Exception
    {    	
        returnedRatingModel = restClient.authenticateUser(adminUser).onCoreAPI().usingResource(document).rateStarsToDocument(3);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        returnedRatingModel.assertThat().field("myRating").is(String.valueOf(3))
                      		 .and().field("id").is("fiveStar")
                      		 .and().field("aggregate").isNotEmpty();;        
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.RATINGS }, executionType = ExecutionType.SANITY, description = "Verify unauthenticated user is not able to post stars rating to a document")
    @Bug(id = "MNT-16904")
    public void unauthenticatedUserIsNotAbleToRateStarsToDocument() throws Exception
    {
        restClient.authenticateUser(new UserModel("random user", "random password")).onCoreAPI().usingResource(document).rateStarsToDocument(5);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}