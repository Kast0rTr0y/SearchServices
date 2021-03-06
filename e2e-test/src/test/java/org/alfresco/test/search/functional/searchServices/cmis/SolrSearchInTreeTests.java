package org.alfresco.test.search.functional.searchServices.cmis;

import org.alfresco.utility.Utility;
import org.alfresco.utility.data.provider.XMLDataConfig;
import org.alfresco.utility.data.provider.XMLTestDataProvider;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.QueryModel;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SolrSearchInTreeTests extends AbstractCmisE2ETest
{
    private FolderModel parentFolder, subFolder1, subFolder2, subFolder3;
    private FileModel subFile1, subFile2, subFile3, subFile4, subFile5, subFile6;
    
    @BeforeClass(alwaysRun = true)
    public void createTestData() throws Exception
    {
        // create input data
        parentFolder = FolderModel.getRandomFolderModel();
        subFolder1 = FolderModel.getRandomFolderModel();
        subFolder2 = FolderModel.getRandomFolderModel();
        subFolder3 = new FolderModel("subFolder-3");
        subFile5 = new FileModel("fifthFile.txt",FileType.TEXT_PLAIN, "fifthFile content");
        subFile1 = new FileModel("firstFile", FileType.MSEXCEL);
        subFile2 = FileModel.getRandomFileModel(FileType.MSPOWERPOINT2007);
        subFile3 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        subFile4 = new FileModel("fourthFile", "fourthFileTitle", "fourthFileDescription", FileType.MSWORD2007);
        subFile6 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);

        cmisApi.authenticateUser(testUser).usingSite(testSite).createFolder(parentFolder)
            .then().usingResource(parentFolder)
                .createFile(subFile5).assertThat().contentIs("fifthFile content")
                .createFolder(subFolder1)
                .createFolder(subFolder2)
                .createFolder(subFolder3)
                .createFile(subFile1)
                .createFile(subFile2)
                .createFile(subFile3)
                .createFile(subFile4)
                    .then().usingResource(subFolder1)
                        .createFile(subFile6)
                        .createFolder(FolderModel.getRandomFolderModel());
        // wait for solr index
        Utility.waitToLoopTime(getSolrWaitTimeInSeconds());
    }
    
    @AfterClass(alwaysRun = true)
    public void cleanupEnvironment()
    {
        dataContent.deleteSite(testSite);
    }
    
    @Test(dataProviderClass = XMLTestDataProvider.class, dataProvider = "getQueriesData")
    @XMLDataConfig(file = "src/test/resources/testdata/search-in-tree.xml")
    public void executeCMISQuery(QueryModel query) throws Exception
    {
        String currentQuery = String.format(query.getValue(), parentFolder.getNodeRef());
        cmisApi.authenticateUser(testUser);
        Assert.assertTrue(waitForIndexing(currentQuery, query.getResults()), String.format("Result count not as expected for query: %s", currentQuery));
    }
}
