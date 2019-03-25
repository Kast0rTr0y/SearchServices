/*-
 * #%L
 * Alfresco Remote API
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.solr.query.stream;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.list;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AbstractAlfrescoDistributedTest;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.ContentPropertyValue;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.client.Transaction;
import org.alfresco.solr.sql.SelectStarDefaultField;
import org.alfresco.solr.sql.SolrSchemaUtil;
import org.alfresco.solr.stream.AlfrescoSolrStream;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.Date;
import java.util.Set;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Stream;

/**
 * @author Michael Suzuki
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public abstract class AbstractStreamTest extends AbstractAlfrescoDistributedTest
{
    protected Node node1;
    protected Node node2;
    protected Node node3;
    protected Node node4;

    protected Acl acl;
    protected Acl acl2;
    
    protected static final QName PROP_RATING = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "fiveStarRatingSchemeTotal");
    protected static final QName PROP_TRACK  = QName.createQName(NamespaceService.AUDIO_MODEL_1_0_URI, "trackNumber");
    protected static final QName PROP_MANUFACTURER  = QName.createQName(NamespaceService.EXIF_MODEL_1_0_URI, "manufacturer");
    protected static final QName PROP_EXPOSURE_TIME  = QName.createQName(NamespaceService.EXIF_MODEL_1_0_URI, "exposureTime");
    protected static final QName PROP_WITH_UNDERSCORE  = QName.createQName("mf", "freetext_underscore");
    protected static final QName PROP_AUTHOR_FT = QName.createQName("ft", "authorft");
    protected static final QName PROP_CUSTOM_FINANCE_MODEL_LOCATION  = QName.createQName("Finance", "Location");
    protected static final QName PROP_CUSTOM_FINANCE_MODEL_TITLE = QName.createQName("Finance", "Title");
    protected static final QName PROP_CUSTOM_FINANCE_MODEL_NO  = QName.createQName("Finance", "No");
    protected static final QName PROP_CUSTOM_FINANCE_MODEL_EMP  = QName.createQName("Finance", "Emp");
    protected static final QName PROP_CUSTOM_EXPENSE_MODEL_DATE = QName.createQName("http://www.mycompany.com/model/expense/1.0", "Recorded_At");

    protected int indexedNodesCount;

    @Before
    public void load() throws Exception
    {
        putHandleDefaults();
        /*
        * Create and index an AclChangeSet.
        */

        AclChangeSet aclChangeSet = getAclChangeSet(1);

        acl = getAcl(aclChangeSet);
        acl2 = getAcl(aclChangeSet);

        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, list("joel"), list("phil"), null);
        AclReaders aclReaders2 = getAclReaders(aclChangeSet, acl2, list("jim"), list("phil"), null);

        indexAclChangeSet(aclChangeSet,
                list(acl, acl2),
                list(aclReaders, aclReaders2));


        //Check for the ACL state stamp.
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCountAllCores(waitForQuery, 1, 80000);

        //Check that both ACL's are in the index
        BooleanQuery.Builder builder1 = new BooleanQuery.Builder();
        builder1.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_DOC_TYPE, SolrInformationServer.DOC_TYPE_ACL)), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery1 = builder1.build();
        waitForDocCountAllCores(waitForQuery1, 2, 80000);

        /*
        * Create and index a Transaction
        */

        //First create a transaction.
        Transaction txn = getTransaction(0, 4);

        node1 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        node2 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        node3 = getNode(txn, acl2, Node.SolrApiNodeStatus.UPDATED);
        node4 = getNode(txn, acl2, Node.SolrApiNodeStatus.UPDATED);

        //Next create the NodeMetaData for each node. TODO: Add more metadata

        NodeMetaData nodeMetaData1 = getNodeMetaData(node1, txn, acl, "mike", null, false);
        Date date1 = getDate(2000, 0, 2);
        nodeMetaData1.getProperties().put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date1)));
        nodeMetaData1.getProperties().put(PROP_EXPOSURE_TIME, new StringPropertyValue("10.0"));
        nodeMetaData1.getProperties().put(PROP_RATING, new StringPropertyValue("10"));
        nodeMetaData1.getProperties().put(PROP_TRACK, new StringPropertyValue("12"));
        nodeMetaData1.getProperties().put(PROP_MANUFACTURER, new StringPropertyValue("Nikon"));
        nodeMetaData1.getProperties().put(PROP_AUTHOR_FT, new StringPropertyValue("john snow"));
        nodeMetaData1.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("name1"));
        nodeMetaData1.getProperties().put(ContentModel.PROP_TITLE, new StringPropertyValue("title1"));
        nodeMetaData1.getProperties().put(ContentModel.PROP_CREATOR, new StringPropertyValue("creator1"));
        nodeMetaData1.getProperties().put(ContentModel.PROP_OWNER, new StringPropertyValue("michael"));
        nodeMetaData1.getProperties().put(PROP_CUSTOM_FINANCE_MODEL_EMP, new StringPropertyValue("emp1"));
        nodeMetaData1.getProperties().put(PROP_CUSTOM_FINANCE_MODEL_TITLE, new StringPropertyValue("title"));
        nodeMetaData1.getProperties().put(PROP_CUSTOM_EXPENSE_MODEL_DATE, new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date1)));
        HashSet aspects = new HashSet<QName>();
        aspects.add(ContentModel.ASPECT_AUDITABLE);
        nodeMetaData1.setAspects(aspects);
        
        NodeMetaData nodeMetaData2 = getNodeMetaData(node2, txn, acl, "mike", null, false);
        Date date2 = getDate(2000, 1, 2);
        nodeMetaData2.getProperties().put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date2)));
        nodeMetaData2.getProperties().put(PROP_EXPOSURE_TIME, new StringPropertyValue("11.0"));
        nodeMetaData2.getProperties().put(PROP_RATING, new StringPropertyValue("15"));
        nodeMetaData2.getProperties().put(PROP_TRACK, new StringPropertyValue("8"));
        nodeMetaData2.getProperties().put(PROP_MANUFACTURER, new StringPropertyValue("Nikon"));
        nodeMetaData2.getProperties().put(PROP_WITH_UNDERSCORE, new StringPropertyValue("camera"));
        nodeMetaData2.getProperties().put(PROP_AUTHOR_FT, new StringPropertyValue("john snow"));

        nodeMetaData2.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("name2"));
        nodeMetaData2.getProperties().put(ContentModel.PROP_TITLE, new StringPropertyValue("title2"));
        nodeMetaData2.getProperties().put(ContentModel.PROP_CREATOR, new StringPropertyValue("creator1"));
        nodeMetaData2.getProperties().put(ContentModel.PROP_OWNER, new StringPropertyValue("michael"));
        nodeMetaData1.getProperties().put(PROP_CUSTOM_FINANCE_MODEL_LOCATION, new StringPropertyValue("london"));
        nodeMetaData2.getProperties().put(PROP_CUSTOM_FINANCE_MODEL_EMP, new StringPropertyValue("emp1"));
        nodeMetaData2.getProperties().put(PROP_CUSTOM_EXPENSE_MODEL_DATE, new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date2)));

        NodeMetaData nodeMetaData3 = getNodeMetaData(node3, txn, acl2, "mike", null, false);
        Date date3 = getDate(2000, 2, 2);
        nodeMetaData3.getProperties().put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date3)));
        nodeMetaData3.getProperties().put(PROP_EXPOSURE_TIME, new StringPropertyValue("12.0"));
        nodeMetaData3.getProperties().put(PROP_RATING, new StringPropertyValue("10"));
        nodeMetaData3.getProperties().put(PROP_TRACK, new StringPropertyValue("6"));
        nodeMetaData3.getProperties().put(PROP_MANUFACTURER, new StringPropertyValue("Canon"));
        nodeMetaData3.getProperties().put(PROP_WITH_UNDERSCORE, new StringPropertyValue("portable"));
        nodeMetaData3.getProperties().put(PROP_AUTHOR_FT, new StringPropertyValue("gavin snow"));
        nodeMetaData3.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("name3"));
        nodeMetaData3.getProperties().put(ContentModel.PROP_CREATOR, new StringPropertyValue("creator2"));
        nodeMetaData3.getProperties().put(PROP_CUSTOM_FINANCE_MODEL_EMP, new StringPropertyValue("emp1"));
        nodeMetaData3.getProperties().put(PROP_CUSTOM_EXPENSE_MODEL_DATE, new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date3)));

        NodeMetaData nodeMetaData4 = getNodeMetaData(node4, txn, acl2, "mike", null, false);
        Date date4 = getDate(2000, 3, 2);
        nodeMetaData4.getProperties().put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date4)));
        nodeMetaData4.getProperties().put(PROP_RATING, new StringPropertyValue("20"));
        nodeMetaData4.getProperties().put(PROP_EXPOSURE_TIME, new StringPropertyValue("13.0"));
        nodeMetaData4.getProperties().put(PROP_TRACK, new StringPropertyValue("4"));
        nodeMetaData4.getProperties().put(PROP_MANUFACTURER, new StringPropertyValue("Nikon"));
        nodeMetaData4.getProperties().put(PROP_WITH_UNDERSCORE, new StringPropertyValue("camera"));
        nodeMetaData4.getProperties().put(PROP_AUTHOR_FT, new StringPropertyValue("richard green"));
        nodeMetaData4.getProperties().put(ContentModel.PROP_CREATOR, new StringPropertyValue("creator3"));
        nodeMetaData4.getProperties().put(ContentModel.PROP_CONTENT, new ContentPropertyValue(Locale.FRENCH, 5l, "UTF-8", "text/javascript", null));
        nodeMetaData4.getProperties().put(PROP_CUSTOM_FINANCE_MODEL_EMP, new StringPropertyValue("emp2"));
        nodeMetaData4.getProperties().put(PROP_CUSTOM_EXPENSE_MODEL_DATE, new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date4)));

        List<Node> nodes = asList(node1, node2, node3, node4);
        indexedNodesCount = nodes.size();

        //Index the transaction, nodes, and nodeMetaDatas.
        //Note that the content is automatically created by the test framework.
        indexTransaction(txn,
                nodes,
                asList(nodeMetaData1, nodeMetaData2, nodeMetaData3, nodeMetaData4));

        //Check for the TXN state stamp.
        builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!TX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_TXID, txn.getId(), txn.getId() + 1, true, false), BooleanClause.Occur.MUST));
        waitForQuery = builder.build();

        waitForDocCountAllCores(waitForQuery, 1, 80000);

        /*
        * Query the index for the content
        */

        waitForDocCountAllCores(new TermQuery(new Term(QueryConstants.FIELD_READER, "jim")), 1, 80000);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), 4, 80000);
    }

    @After
    public void clearData() throws Exception
    {
        deleteByQueryAllClients("*:*");
        commit(getStandaloneClients().iterator().next(), true);
    }

    protected void assertNodes(List<Tuple> tuples, Node... nodes) throws Exception {
        for(int i=0; i<nodes.length; i++) {
            Node node = nodes[i];
            Tuple tuple = tuples.get(i);
            if(node.getId() != tuple.getLong("DBID")) {
                throw new Exception("Incorrect Node ID, found "+tuple.getLong("DBID")+" expected "+node.getId());
            }
        }
    }

    protected void assertFieldNotNull(List<Tuple> tuples, String field) throws Exception  {
        for(Tuple tuple : tuples) {
            if(tuple.get(field) == null) {
                throw new Exception("Found unexpected null field:"+field);
            }
        }
    }


    protected Date getDate(int year, int month, int day)
    {
        GregorianCalendar calendar = new GregorianCalendar(year, month, day, 10, 0);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        return calendar.getTime();
    }


    /**
     * Get select star fields (hard coded + shared.properties).
     * @return
     */
    protected Set<String> getSelectStarFields()
    {
        /* Set containing the hard coded select * fields and the fields taken from shared.properties.
         */
        return Stream.concat(
                SolrSchemaUtil.fetchCustomFieldsFromSharedProperties().stream(),
                stream(SelectStarDefaultField.values()).map(s -> s.getFieldName()))
                .map(s -> s.replaceFirst(":","_"))
                .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
    }


    /**
     * Check that the formatted fields of the tuple is equal to the set of fields passed as argument.
     * @param tuples list of tuple
     * @param fields set of fields
     */
    protected void checkFormattedReturnedFields(List<Tuple> tuples, Set<String> fields)
    {
        for(Tuple t:tuples){
            Set<String> tupleFields = ((Set<String>) t.fields.keySet()).stream().map(
                    s -> s.replaceFirst(":", "_")).collect(Collectors.toSet());
            assertEquals(fields, tupleFields);
        }

    }

    /**
     * Build a sql query with alfresco user authentication and parses the response back into tuples.
     *
     * @param sql SQL query to post
     * @param alfrescoJson 
     * @return List<Tuple>
     */
    public List<Tuple> sqlQuery(String sql, String alfrescoJson)
    {
        return sqlQuery(sql, alfrescoJson, "UTC", 0);
    }

    public List<Tuple> sqlQuery(String sql, String alfrescoJson, String timezone, long now)
    {
        try {
            System.out.println("######### AFRESCO SQL #######");
            System.out.println(sql);
            List<SolrClient> clusterClients = getShardedClients();
            String shards = getShardsString();
            System.out.println("###########:" + shards);

            SolrParams params = params("stmt", sql, "qt", "/sql", "alfresco.shards", shards, "timeZone", timezone);

            if (now > 0) {
                ((ModifiableSolrParams) params).set("time", Long.toString(now));
            }

            AlfrescoSolrStream tupleStream = new AlfrescoSolrStream(((HttpSolrClient) clusterClients.get(0)).getBaseURL(), params);
            tupleStream.setJson(alfrescoJson);
            return getTuples(tupleStream);
        }
        catch (IOException exception)
        {
            throw new RuntimeException(exception);
        }
    }
}

