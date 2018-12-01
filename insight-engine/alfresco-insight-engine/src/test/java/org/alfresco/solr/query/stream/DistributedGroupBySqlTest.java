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

import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.io.Tuple;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

/**
 * @author Joel
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedGroupBySqlTest extends AbstractStreamTest
{
    @Rule
    public JettyServerRule jetty = new JettyServerRule(1, this);
    
    @Test
    public void testSearch() throws Exception
    {
        String sql = "select ACLID, count(*) from alfresco where `cm:content` = 'world' group by ACLID";
        String alfrescoJson = "{ \"authorities\": [ \"jim\", \"joel\" ], \"tenants\": [ \"\" ] }";
        List<Tuple> tuples = sqlQuery(sql, alfrescoJson);

        //We are grouping by ACLID so there will be two with a count(*) of 2.
        //The first 2 nodes on indexed with the same acl and the second 2 nodes are indexed with their same acl.
        assertTrue(tuples.size() == 2);
        assertTrue(tuples.get(0).getLong("EXPR$1") == 2);
        assertTrue(tuples.get(1).getLong("EXPR$1") == 2);
        
        if(node1.getAclId() != tuples.get(0).getLong("ACLID")) {
            throw new Exception("Incorrect Acl ID, found "+tuples.get(0).getLong("ACLID")+" expected "+node1.getAclId());
        }
        if(node3.getAclId() != tuples.get(1).getLong("ACLID")) {
            throw new Exception("Incorrect Acl ID, found "+tuples.get(0).getLong("ACLID")+" expected "+node3.getAclId());
        }

        sql = "select ACLID, count(*) AS barb from alfresco where `cm:content` = 'hello world' group by ACLID";
        tuples = sqlQuery(sql, alfrescoJson);

        //Now uses an alias
        assertTrue(tuples.size() == 2);
        assertTrue(tuples.get(0).getLong("barb") == 2);
        assertTrue(tuples.get(1).getLong("barb") == 2);

        assertTrue(tuples.get(0).get("barb") instanceof Long);
        assertTrue(tuples.get(0).get("ACLID") instanceof Long);

        // date formats + group and sort by date
        sql = "select " +
                    "cm_created,  " +
                    "count(*) as ExposureCount,  " +
                    "sum(exif_exposureTime) as TotalExposure,  " +
                    "avg(exif_exposureTime) as AvgExposure,  " +
                    "min(exif_exposureTime) as MinExposure,  " +
                    "max(exif_exposureTime) as MaxExposure  " +
                "from alfresco  " +
                "where  " +
                    "exif_exposureTime > 0 and  " +
                    "cm_content='world' and  " +
                    "cm_created BETWEEN '2000-01-01T01:01:01Z' AND '2010-02-14T23:59:59Z'" +
                "group by cm_created having count(*) > 0 ";

        tuples = sqlQuery(sql, alfrescoJson);
        assertEquals(4, tuples.size());
        for (int i = 0; i < 4; i++) {
            Tuple tuple = tuples.get(i);
            assertEquals(1L, tuple.getLong("ExposureCount").longValue());
            assertEquals(10 + i, tuple.getDouble("MaxExposure"),0);
            assertEquals(10 + i, tuple.getDouble("MinExposure"),0);
            assertEquals(10 + i, tuple.getDouble("AvgExposure"),0);
            assertEquals(10 + i, tuple.getDouble("TotalExposure"),0);
            assertEquals("2000-0" + (i + 1) + "-02T10:00:00Z", tuple.getString("cm_created"));
        }

        sql = "select " +
                "cm_created,  " +
                "count(*) as ExposureCount,  " +
                "sum(exif_exposureTime) as TotalExposure,  " +
                "avg(exif_exposureTime) as AvgExposure,  " +
                "min(exif_exposureTime) as MinExposure,  " +
                "max(exif_exposureTime) as MaxExposure  " +
                "from alfresco  " +
                "where  " +
                "exif_exposureTime > 0 and  " +
                "cm_content='world' and  " +
                "cm_created >= '2000-01-01T01:01:01Z' AND cm_created <= '2010-02-14T23:59:59Z' " +
                "group by cm_created having count(*) > 0 ";

        tuples = sqlQuery(sql, alfrescoJson);
        assertEquals(4, tuples.size());
        for (int i = 0; i < 4; i++) {
            Tuple tuple = tuples.get(i);
            assertEquals(1L, tuple.getLong("ExposureCount").longValue());
            assertEquals(10 + i, tuple.getDouble("MaxExposure"),0);
            assertEquals(10 + i, tuple.getDouble("MinExposure"),0);
            assertEquals(10 + i, tuple.getDouble("AvgExposure"),0);
            assertEquals(10 + i, tuple.getDouble("TotalExposure"),0);
            assertEquals("2000-0" + (i + 1) + "-02T10:00:00Z", tuple.getString("cm_created"));
        }

        sql = "select " +
                    "cm_created, " +
                    "count(*) " +
                "from alfresco " +
                "where " +
                    "cm_created >= '2000-01-01T01:01:01Z' " +
                    "group by cm_created " +
                    "order by cm_created desc";
        tuples = sqlQuery(sql, alfrescoJson);

        for (int i = 0; i < 4; i++) {
            Tuple tuple = tuples.get(i);

            // Test the descending order
            assertEquals("2000-0" + (4 - i) + "-02T10:00:00Z", tuple.getString("cm_created"));
        }

        //Test that phrases are working
        sql = "select ACLID, count(*) AS barb from alfresco where `cm:content` = 'world hello' group by ACLID";
        tuples = sqlQuery(sql, alfrescoJson);

        //Now uses an alias
        assertTrue(tuples.size() == 0);


        sql = "select SITE, count(*) AS docsPerSite from alfresco where `cm:content` = 'world' group by SITE having count(*) > 1 AND count(*) < 10000";
        tuples = sqlQuery(sql, alfrescoJson);
        assertTrue(tuples.size() == 1);
        assertTrue("_REPOSITORY_".equals(tuples.get(0).getString(("SITE"))));
        assertTrue(tuples.get(0).getLong("docsPerSite") == 4);

        sql = "select `cm:fiveStarRatingSchemeTotal` as rating, avg(`audio:trackNumber`) as track from alfresco where `cm:content` = 'world' group by `cm:fiveStarRatingSchemeTotal`";
        tuples = sqlQuery(sql, alfrescoJson);
        assertTrue(tuples.size() == 3);


        assertTrue(tuples.get(0).getDouble("rating") == 10);
        assertTrue(tuples.get(0).getLong(("track")) == 9);

        assertTrue(tuples.get(0).get("rating") instanceof Double);
        assertTrue(tuples.get(0).get("track") instanceof Long);

        assertTrue(tuples.get(1).getDouble("rating") == 15);
        assertTrue(tuples.get(1).getLong(("track")) == 8);
        assertTrue(tuples.get(2).getDouble("rating") == 20);
        assertTrue(tuples.get(2).getLong(("track")) == 4);

        sql = "select cm_fiveStarRatingSchemeTotal, avg(audio_trackNumber) as track from alfresco where cm_content = 'world' group by cm_fiveStarRatingSchemeTotal";
        tuples = sqlQuery(sql, alfrescoJson);
        assertTrue(tuples.size() == 3);
        assertTrue(tuples.get(0).getDouble("cm_fiveStarRatingSchemeTotal") == 10);
        assertTrue(tuples.get(0).getLong(("track")) == 9);


        assertTrue(tuples.get(1).getDouble("cm_fiveStarRatingSchemeTotal") == 15);
        assertTrue(tuples.get(1).getLong(("track")) == 8);
        assertTrue(tuples.get(2).getDouble("cm_fiveStarRatingSchemeTotal") == 20);
        assertTrue(tuples.get(2).getLong(("track")) == 4);

        sql = "select exif_manufacturer as manu, count(*) as tot, max(cm_fiveStarRatingSchemeTotal) AS cre from alfresco where cm_content = 'world' group by exif_manufacturer order by tot asc";
        tuples = sqlQuery(sql, alfrescoJson);
        assertTrue(tuples.size() == 2);
        assertTrue("canon".equals(tuples.get(0).getString(("manu"))));
        assertTrue(tuples.get(0).getLong("tot") == 1);
        assertTrue(tuples.get(0).getDouble("cre") == 10);

        assertTrue(tuples.get(0).get("tot") instanceof Long);
        assertTrue(tuples.get(0).get("cre") instanceof Double);


        assertTrue("nikon".equals(tuples.get(1).getString(("manu"))));
        assertTrue(tuples.get(1).getLong("tot") == 3);
        assertTrue(tuples.get(1).getDouble("cre") == 20);

        sql = "select exif_manufacturer as manu, sum(cm_fiveStarRatingSchemeTotal), min(cm_fiveStarRatingSchemeTotal) from alfresco where cm_content = 'world' group by exif_manufacturer order by manu asc";
        tuples = sqlQuery(sql, alfrescoJson);
        assertTrue(tuples.size() == 2);
        assertTrue("canon".equals(tuples.get(0).getString(("manu"))));
        assertTrue(tuples.get(0).getDouble("EXPR$1") == 10);
        assertTrue(tuples.get(0).getDouble("EXPR$2") == 10);

        assertTrue(tuples.get(0).get("EXPR$1") instanceof Double);
        assertTrue(tuples.get(0).get("EXPR$2") instanceof Double);


        assertTrue("nikon".equals(tuples.get(1).getString(("manu"))));
        assertTrue(tuples.get(1).getDouble("EXPR$1") == 45);
        assertTrue(tuples.get(1).getDouble("EXPR$2") == 10);


        sql = "select `cm_content.mimetype`, count(*) from alfresco group by `cm_content.mimetype` having count(*) < 4 order by count(*) asc";
        tuples = sqlQuery(sql, alfrescoJson);
        assertTrue(tuples.size() == 2);
        assertTrue("text/javascript".equals(tuples.get(0).getString(("cm_content.mimetype"))));
        assertTrue(tuples.get(0).getDouble("EXPR$1") == 1);

        assertTrue("text/plain".equals(tuples.get(1).getString(("cm_content.mimetype"))));
        assertTrue(tuples.get(1).getDouble("EXPR$1") == 3);

        sql = "select `cm_content.mimetype`, count(*) as mcount from alfresco group by `cm_content.mimetype` having count(*) = 3";
        tuples = sqlQuery(sql, alfrescoJson);
        assertTrue(tuples.size() == 1);
        assertTrue("text/plain".equals(tuples.get(0).getString(("cm_content.mimetype"))));
        assertTrue(tuples.get(0).getDouble("mcount") == 3);

        sql = "select cm_name, count(*) from alfresco where `cm:name` = 'name*' group by cm_name";
        tuples = sqlQuery(sql, alfrescoJson);
        assertTrue(tuples.size() == 3);
        tuples.forEach(tuple -> assertTrue(tuple.getString("cm_name").startsWith("name")));

        sql = "select cm_name, count(*) from alfresco where cm_name = '*3' group by cm_name";
        tuples = sqlQuery(sql, alfrescoJson);
        assertTrue(tuples.size() == 1);
        assertTrue("name3".equals(tuples.get(0).getString(("cm_name"))));

        sql = "select TYPE, count(*) from alfresco group by TYPE";
        tuples = sqlQuery(sql, alfrescoJson);
        assertTrue(tuples.size() == 1);
        assertTrue(tuples.get(0).getString("TYPE").equals("{http://www.alfresco.org/test/solrtest}testSuperType"));
        assertTrue(tuples.get(0).getLong("EXPR$1") == 4);

        sql = "select cm_creator, count(*) from alfresco group by cm_creator having count(*) = 2";
        tuples = sqlQuery(sql, alfrescoJson);
        assertTrue(tuples.size() == 1);
        assertTrue("creator1".equals(tuples.get(0).getString(("cm_creator"))));
        sql = "select cm_name, count(*) from alfresco group by cm_name having (count(*) > 1 AND cm_name = 'bill') order by count(*) asc";
        try {
            sqlQuery(sql, alfrescoJson);
            throw new Exception("Exception should have been thrown");
        } catch (Throwable e) {
            if(e.getMessage().equals("Exception should have been thrown")) {
                throw e;
            } else {
                assertTrue(e.getMessage().contains("HAVING clause can only be applied to aggregate functions."));
            }
        }
    }

    @Test
    public void sqlSearchNestedGroupByTokenizedField() throws Exception
    {

        String alfrescoJson = "{ \"authorities\": [ \"jim\", \"joel\" ], \"tenants\": [ \"\" ] }";
        String sql = "select ft_authorft as Author, cm_name as Name from alfresco group by ft_authorft, cm_name";
        List<Tuple> tuples = sqlQuery(sql, alfrescoJson);

        assertTrue(tuples.size() == 3);
        assertTrue(tuples.get(0).fields.size() == 2);
        assertTrue(tuples.get(1).fields.size() == 2);
        assertTrue(tuples.get(2).fields.size() == 2);
        assertTrue("gavin snow".equals(tuples.get(0).getString("Author")));
        assertTrue("john snow".equals(tuples.get(1).getString("Author")));
        assertTrue("john snow".equals(tuples.get(2).getString("Author")));
        assertTrue("name3".equals(tuples.get(0).getString("Name")));
        assertTrue("name1".equals(tuples.get(1).getString("Name")));
        assertTrue("name2".equals(tuples.get(2).getString("Name")));


        String sql2 = "select cm_name as Name, ft_authorft as Author from alfresco group by cm_name, ft_authorft";
        List<Tuple> tuples2 = sqlQuery(sql2, alfrescoJson);
        assertTrue(tuples2.size() == 3);
        assertTrue("gavin snow".equals(tuples2.get(2).getString("Author")));
        assertTrue("name3".equals(tuples2.get(2).getString("Name")));

    }

    @Test 
    public void sqlSearchWithGrouping_propertyWithUnderscore_shouldReturnGroupedAggregation() throws Exception
    {
        //Primary syntax replacing first ':' with _
        String alfrescoJson = "{ \"authorities\": [ \"jim\", \"joel\" ], \"tenants\": [ \"\" ] }";
        String sql = "select mf_freetext_underscore as underscoreField, count(*) as numFound from alfresco where cm_content = 'world' group by mf_freetext_underscore order by numFound asc";

        List<Tuple> tuples = sqlQuery(sql, alfrescoJson);

        assertTrue(tuples.size() == 2);
        assertTrue("portable".equals(tuples.get(0).getString(("underscoreField"))));
        assertTrue(tuples.get(0).getLong("numFound") == 1);
        assertTrue(tuples.get(0).get("numFound") instanceof Long);
        assertTrue("camera".equals(tuples.get(1).getString(("underscoreField"))));
        assertTrue(tuples.get(1).getLong("numFound") == 2);

        //Alternative syntax escaping ':'
        sql = "select `mf:freetext_underscore` as underscoreField, count(*) as numFound from alfresco where `cm:content` = 'world' group by `mf:freetext_underscore` order by numFound asc";

        tuples = sqlQuery(sql, alfrescoJson);

        assertTrue(tuples.size() == 2);
        assertTrue("portable".equals(tuples.get(0).getString(("underscoreField"))));
        assertTrue(tuples.get(0).getLong("numFound") == 1);
        assertTrue(tuples.get(0).get("numFound") instanceof Long);
        assertTrue("camera".equals(tuples.get(1).getString(("underscoreField"))));
        assertTrue(tuples.get(1).getLong("numFound") == 2);

        sql = "select mf_freetext_underscore as underscoreField, count(*) as numFound from alfresco where `cm:content` = 'world' group by mf_freetext_underscore order by numFound asc";

        tuples = sqlQuery(sql, alfrescoJson);

        assertTrue(tuples.size() == 2);
        assertTrue("portable".equals(tuples.get(0).getString(("underscoreField"))));
        assertTrue(tuples.get(0).getLong("numFound") == 1);
        assertTrue(tuples.get(0).get("numFound") instanceof Long);
        assertTrue("camera".equals(tuples.get(1).getString(("underscoreField"))));
        assertTrue(tuples.get(1).getLong("numFound") == 2);
    }
}

