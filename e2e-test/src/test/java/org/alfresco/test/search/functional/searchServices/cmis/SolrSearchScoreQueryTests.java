package org.alfresco.test.search.functional.searchServices.cmis;

import java.math.BigDecimal;

import org.alfresco.search.TestGroup;
import org.alfresco.utility.data.provider.XMLDataConfig;
import org.alfresco.utility.data.provider.XMLTestData;
import org.alfresco.utility.data.provider.XMLTestDataProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * Testing SCORE function queries to solve issues related to
 * https://issues.alfresco.com/jira/browse/ACE-2479
 * 
 * @author aborroy
 *
 */
public class SolrSearchScoreQueryTests extends AbstractCmisE2ETest
{
	
    private static Logger LOGGER = LoggerFactory.getLogger(SolrSearchScoreQueryTests.class);
    private XMLTestData testData;
    
    @AfterClass(alwaysRun = true)
    public void cleanupEnvironment()
    {
        if (testData != null)
        {
            testData.cleanup(dataContent);
        }
        else
        {
            LOGGER.warn("testData is inexplicably null - skipping clean up.");
        }
    }
    
    /* These tests does not require common data preparation from AbstractE2EFunctionalTest
     * as it's including every test data required in search-score-funtion.xml
     * @see org.alfresco.test.search.functional.AbstractE2EFunctionalTest#dataPreparation()
     */
    @Override
    public void setup()
    {
    }
    
    @Test(dataProviderClass = XMLTestDataProvider.class, dataProvider = "getAllData")
    @XMLDataConfig(file = "src/test/resources/testdata/search-score-function.xml")
    public void prepareDataForScoreSearch(XMLTestData testData) throws Exception
    {
        this.testData = testData;
        this.testData.createUsers(dataUser);
        this.testData.createSitesStructure(dataSite, dataContent, dataUser);
        testUser = dataUser.getCurrentUser();
        cmisApi.authenticateUser(testUser);
    }

    /**
     * Verify that results are ordered
     * @throws Exception
     */
    @Test(dependsOnMethods = "prepareDataForScoreSearch")
    public void scoreQueryOrdered() throws Exception
    {
        
        String query = "SELECT cmis:objectId, SCORE() AS orderCriteria "
                + "FROM cmis:document "
                + "WHERE CONTAINS('Quidditch') " 
                + "ORDER BY orderCriteria";

        Assert.assertTrue(waitForIndexing(query, 3), String.format("Result count not as expected for query: %s", query));

        cmisApi.withQuery(query).assertColumnIsOrdered().isOrderedAsc("orderCriteria");

    }

    /**
     * Verify that results are inverse ordered
     * 
     * @throws Exception
     */
    @Test(dependsOnMethods = "prepareDataForScoreSearch")
    public void scoreQueryOrderedDesc() throws Exception
    {

        String query = "SELECT cmis:objectId, SCORE() AS orderCriteria "
                + "FROM cmis:document "
                + "WHERE CONTAINS('Quidditch') "
                + "ORDER BY orderCriteria DESC";

        Assert.assertTrue(waitForIndexing(query, 3), String.format("Result count not as expected for query: %s", query));

        cmisApi.withQuery(query).assertColumnIsOrdered().isOrderedDesc("orderCriteria");

    }

    /**
     * Verify that all SCORE results are between 0 and 1
     * 
     * @throws Exception
     */
    @Test(groups = { TestGroup.ACS_62n }, dependsOnMethods = "prepareDataForScoreSearch")
    public void scoreQueryInRange() throws Exception
    {

        String query = "SELECT cmis:objectId, SCORE() " 
                     + "FROM cmis:document " 
                     + "WHERE CONTAINS('Quidditch')";
    	
        Assert.assertTrue(waitForIndexing(query, 3), String.format("Result count not as expected for query: %s", query));

        cmisApi.withQuery(query).assertColumnValuesRange().isReturningValuesInRange("SEARCH_SCORE", BigDecimal.ZERO, BigDecimal.ONE);

    }
    
    /**
     * Verify that all SCORE results are between 0 and 1
     * @throws Exception
     */
    @Test(groups = { TestGroup.ACS_62n }, dependsOnMethods = "prepareDataForScoreSearch")
    public void scoreQueryAliasInRange() throws Exception
    {
        
        String query = "SELECT cmis:objectId, SCORE() AS orderCriteria "
                    + "FROM cmis:document "
                    + "WHERE CONTAINS('Quidditch')";

        Assert.assertTrue(waitForIndexing(query, 3), String.format("Result count not as expected for query: %s", query));

        cmisApi.withQuery(query).assertColumnValuesRange().isReturningValuesInRange("orderCriteria", BigDecimal.ZERO, BigDecimal.ONE);        

    }

    /**
     * Verify that SCORE is valid name for an alias
     * Currently only supported with double quotes
     * 
     * @throws Exception
     */
    @Test(dependsOnMethods = "prepareDataForScoreSearch")
    public void scoreQueryScoreAsAlias() throws Exception
    {

        String query = "SELECT cmis:objectId, SCORE() AS \"score\" " 
                    + "FROM cmis:document " 
                    + "WHERE CONTAINS('Quidditch')";

        Assert.assertTrue(waitForIndexing(query, 3), String.format("Result count not as expected for query: %s", query));
 
        cmisApi.withQuery(query).assertResultsCount().equals(3);

    }	

}
