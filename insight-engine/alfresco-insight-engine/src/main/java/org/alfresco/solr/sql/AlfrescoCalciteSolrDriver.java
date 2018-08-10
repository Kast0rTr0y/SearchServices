/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.alfresco.solr.sql;

import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.jdbc.Driver;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.solr.core.SolrCore;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

/**
 * JDBC driver for Calcite Solr.
 *
 * <p>It accepts connect strings that start with "jdbc:calcitesolr:".</p>
 */
public class AlfrescoCalciteSolrDriver extends Driver {

  private final static Map<String, SolrCore> cores = new HashMap();

  public final static String CONNECT_STRING_PREFIX = "jdbc:alfrescosolr:";

  private AlfrescoCalciteSolrDriver() {
    super();
  }

  static {
    new AlfrescoCalciteSolrDriver().register();
  }

  @Override
  protected String getConnectStringPrefix() {
    return CONNECT_STRING_PREFIX;
  }

  public static synchronized void registerCore(SolrCore core) {
    cores.put(core.getName(), core);
  }

  public static synchronized SolrCore getCore(String coreName) {
    return cores.get(coreName);
  }

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    if(!this.acceptsURL(url)) {
      return null;
    }

    String localCore = info.getProperty("localCore");
    SolrCore core = getCore(localCore);

    Connection connection = super.connect(url, info);
    CalciteConnection calciteConnection = (CalciteConnection) connection;
    final SchemaPlus rootSchema = calciteConnection.getRootSchema();

    /*
    *  Apache Calcite Documentation
    *
    *  The AlfrescoCalciteSolrDriver sets up the entire Calcite stack.
    *
    *  The SolrSchema is created first.
    *  The SolrSchema creates a single SolrTable which sets up the fields for the "alfresco" table
    *  The SolrTable.toRel() method returns a SolrTableScan
    *  The SolrTableScan.register() registers the SolrToEnumerableConverter and all the rules in the SolrRules class.
    *
    *  After the stack has been setup Calcite parses the query and follows the rules in the SolrRules class.
    *  These rules replace the standard query parse tree nodes, for specific parts of the query,
    *  with Solr specific operations. This is known a "pushing down" operations into the search engine and is the
    *  the key to the Insight Engines performance.
    *
    *  The following query operations are "pushed down" to Solr:
    *
    *  SolrFilter: This pushes down both the WHERE and HAVING predicates
    *  SolrAggregate: This pushes down the aggregations
    *  SolrProject: This pushes down the fields that need to be fetched
    *  SolrSort: This pushes down the the ordering of the result set.
    *
    *  In reality all the classes above do is gather information from the parse tree which will be passed to the
    *  SolrTable. But they also do something very important, which is to suppress the normal calcite operations.
    *  For example it stops Calcite from sorting result sets that have already been sorted by the pushed down
    *  operation.
    *
    *  The SolrToEnumerableConverter sends a visitor into the parseTree that interacts with the Solr specific nodes
    *  and gathers the information needed to build a Solr query.
    *
    *  It then marshals the data into parameters and uses the SolrMethod class to define and call
    *  SolrTable.query() with the proper parameters.
    *
    *  SolrTable.query() defines the logic for running the Solr query given the set of parameters that have been
    *  generated by the Solr specific nodes added to the query planner (SolrFilter...).
    *
    *  The query logic is defined using Solr Streaming Expressions which have been heavily adapted for Alfresco use.
    *
    *  Note the usage of AlfrescoExpressionStream in the SolrTable. This class wraps a standard streaming expression
    *  and applies the AlfrescoStreamExpressionProcessor to the stream which wraps Alfresco specific Streams around
    *  certain streams. This is how fields are translated from user facing fields to the Solr index fields.
    *
    *  SolrTable.query returns a SolrEnumerator which wraps the Streaming Expression created by the SolrTable
    *  and is used by Calcite to return the result tuples. The SolrEnumerator marshals datatypes into the
    *  correct format as described by the SolrSchema.
    */

    rootSchema.add("alfresco", new SolrSchema(core, info));

    // Set the default schema
    calciteConnection.setSchema("alfresco");

    return connection;
  }
}