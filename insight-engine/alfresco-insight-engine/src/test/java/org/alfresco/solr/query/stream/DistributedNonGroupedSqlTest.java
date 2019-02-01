/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
 *
 * This file is part of Alfresco
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
 */
package org.alfresco.solr.query.stream;

import java.util.List;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.io.Tuple;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Tuna Aksoy
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedNonGroupedSqlTest extends AbstractStreamTest
{
    @BeforeClass
    private static void initData() throws Throwable
    {
        initSolrServers(1, getClassName(), null);
    }

    @AfterClass
    private static void destroyData()
    {
        dismissSolrServers();
    }

    @Test
    public void testSearch() throws Exception
    {
        String alfrescoJson = "{ \"authorities\": [ \"jim\", \"joel\" ], \"tenants\": [ \"\" ] }";
        String alfrescoJson2 = "{ \"authorities\": [ \"joel\" ], \"tenants\": [ \"\" ] }";

        // Test count
        String sql = "select count(*) from alfresco where `cm:content` = 'world'";
        List<Tuple> tuples = sqlQuery(sql, alfrescoJson);
        assertEquals(1, tuples.size());
        assertEquals(4, (long) tuples.get(0).getLong("EXPR$0"));

        tuples = sqlQuery(sql, alfrescoJson2);
        assertEquals(1, tuples.size());
        assertEquals(2, (long) tuples.get(0).getLong("EXPR$0"));


        //Test phrases are working properly
        sql = "select count(*) from alfresco where `cm:content` = 'hello world'";
        tuples = sqlQuery(sql, alfrescoJson);
        assertEquals(1, tuples.size());
        assertEquals(4, (long) tuples.get(0).getLong("EXPR$0"));

        sql = "select count(*) from alfresco where `cm:content` = '(world hello)'";
        tuples = sqlQuery(sql, alfrescoJson);
        assertEquals(1, tuples.size());
        assertEquals(4, (long) tuples.get(0).getLong("EXPR$0"));


        sql = "select count(*) from alfresco where `cm:content` = 'world hello'";
        tuples = sqlQuery(sql, alfrescoJson);
        assertEquals(1, tuples.size());
        assertEquals(0, (long) tuples.get(0).getLong("EXPR$0"));


        // Test max
        sql = "select max(`cm:fiveStarRatingSchemeTotal`) as maxResult from alfresco where `cm:content` = 'world'";
        tuples = sqlQuery(sql, alfrescoJson);
        assertEquals(1, tuples.size());
        assertEquals(20, (double) tuples.get(0).getDouble("maxResult"), 0.0);

        tuples = sqlQuery(sql, alfrescoJson2);
        assertEquals(1, tuples.size());
        assertEquals(15, (double) tuples.get(0).getDouble("maxResult"), 0.0);

        // Test min
        sql = "select min(cm_fiveStarRatingSchemeTotal) from alfresco where `cm:content` = 'world'";
        tuples = sqlQuery(sql, alfrescoJson);
        assertEquals(1, tuples.size());
        assertEquals(10, (double) tuples.get(0).getDouble("EXPR$0"), 0.0);

        tuples = sqlQuery(sql, alfrescoJson2);
        assertEquals(1, tuples.size());
        assertEquals(10, (double) tuples.get(0).getDouble("EXPR$0"), 0.0);

        // Test avg
        sql = "select avg(`cm:fiveStarRatingSchemeTotal`) as avgResult from alfresco where `cm:content` = 'world'";
        tuples = sqlQuery(sql, alfrescoJson);
        assertEquals(1, tuples.size());
        assertEquals(13.75, tuples.get(0).getDouble("avgResult"), 0.0);

        tuples = sqlQuery(sql, alfrescoJson2);
        assertEquals(1, tuples.size());
        assertEquals(12.5, tuples.get(0).getDouble("avgResult"), 0.0);
    }
}
