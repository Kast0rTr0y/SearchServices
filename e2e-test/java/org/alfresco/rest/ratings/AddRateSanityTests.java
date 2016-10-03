package org.alfresco.rest.ratings;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.body.LikeRatingBody;
import org.alfresco.rest.body.LikeRatingBody.ratingTypes;
import org.alfresco.rest.requests.RestRatingsApi;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "ratings", "sanity" })
public class AddRateSanityTests extends RestTest
{
    @Autowired
    RestRatingsApi ratingsApi;

    private UserModel userModel;
    private SiteModel siteModel;
    private UserModel adminUser;
    private FolderModel folderModel;
    private FileModel document;
    private LikeRatingBody rating;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        userModel = dataUser.createUser(RandomStringUtils.randomAlphanumeric(20));
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        
        restClient.authenticateUser(adminUser);
        ratingsApi.useRestClient(restClient);
    }
    
    @BeforeMethod
    public void setUp() throws Exception {
        folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
    }

    @TestRail(section = {"rest-api", "ratings" }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Manager role is able to post like rating to a document")
    public void managerIsAbleToLikeDocument() throws Exception
    {
        rating = new LikeRatingBody(ratingTypes.likes.toString(), true);
        ratingsApi.addRate(document, rating);
        ratingsApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.CREATED);
    }
}