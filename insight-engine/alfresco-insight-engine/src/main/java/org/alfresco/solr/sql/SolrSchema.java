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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Date;

import org.alfresco.service.namespace.NamespaceException;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeImpl;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.rel.type.RelProtoDataType;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.apache.calcite.sql.type.SqlTypeFactoryImpl;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.LeafReader;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.AlfrescoSQLHandler;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
* The SolrSchema class creates the "alfresco" table and populates the fields from the index.
*/


public class SolrSchema extends AbstractSchema {
  private static Logger logger = LoggerFactory.getLogger(SolrSchema.class);
  final Properties properties;
  final SolrCore core;
  final String[] postfixes = {"_day", "_month", "_year"};
  final Set<String>  selectStarFields = new HashSet<String>();
  final boolean isSelectStar;

  SolrSchema(SolrCore core, Properties properties) {
    super();
    this.core = core;
    this.properties = properties;
    this.isSelectStar = Boolean.parseBoolean(properties.getProperty(AlfrescoSQLHandler.IS_SELECT_STAR));
    if(isSelectStar) {
        addSelectStarFields();
        String sql = properties.getProperty("stmt", "");
        //Add dynamic fields not part of the schema such as custom models and aspects.
        if(predicateExists(sql))
        {
            SolrSchemaUtil.extractPredicates(sql).forEach(action -> selectStarFields.add(action));
        }
    }
  }

  @Override
  protected Map<String, Table> getTableMap() {
    Map<String, Table> map = new HashMap<String, Table>();
    map.put("alfresco", new SolrTable(this, "alfresco"));
    return map;
  }


  RelProtoDataType getRelDataType(String collection) {

    final RelDataTypeFactory typeFactory = new SqlTypeFactoryImpl(RelDataTypeSystem.DEFAULT);
    final RelDataTypeFactory.FieldInfoBuilder fieldInfo = typeFactory.builder();
    //Add fields from schema.
    Map<String, String> fields = getIndexedFieldsInfo();
    boolean isDate = false;

    Set<Map.Entry<String, String>> set = fields.entrySet();
    boolean hasLockOwner = false;
    for(Map.Entry<String, String> entry : set)
    {
        String ltype = entry.getValue();
        RelDataType type;
        if(!hasLockOwner)
        {
            //Check to see if the field exists to avoid duplicating the field.
            hasLockOwner = lockOwnerFieldExists(entry.getKey());
        }
        switch (ltype)
        {
            case "solr.StrField":
            case "solr.TextField":
            case "org.alfresco.solr.AlfrescoFieldType":
              type = typeFactory.createJavaType(String.class);
              break;
            case "solr.TrieLongField":
              type = typeFactory.createJavaType(Long.class);
              break;
            case "solr.TrieDoubleField":
              type = typeFactory.createJavaType(Double.class);
              break;
            case "solr.TrieFloatField":
              type = typeFactory.createJavaType(Double.class);
              break;
            case "solr.TrieIntField":
              type = typeFactory.createJavaType(Long.class);
              break;
            case "java.lang.Integer":
                type = typeFactory.createJavaType(Long.class);
              break;
            case "java.lang.Float":
                type = typeFactory.createJavaType(Double.class);
                break;
            case "solr.TrieDateField":
                isDate = true;
                type = typeFactory.createJavaType(Date.class);
                break;
            case "java.util.Date":
                isDate = true;
                type = typeFactory.createJavaType(String.class);
                break;
            default:
              type = typeFactory.createJavaType(String.class);
        }
        addFieldInfo(fieldInfo, entry, type, null);
        if(isDate)
        {
            isDate = false;
            addTimeFields(fieldInfo, entry, typeFactory.createJavaType(String.class));
        }
    }

    fieldInfo.add("_query_",typeFactory.createJavaType(String.class));
    fieldInfo.add("score", typeFactory.createJavaType(Double.class));
    
    if(!hasLockOwner)
    {
        //Add fields that might be queried on that does not exist yet.
        fieldInfo.add("cm:lockOwner",typeFactory.createJavaType(String.class));
        fieldInfo.add("cm_lockOwner",typeFactory.createJavaType(String.class));
    }
    return RelDataTypeImpl.proto(fieldInfo.build());
  }

  /**
   * Checks if the field already exists in the virtual schema.
   * @param entry
   * @return
   */
  public static boolean lockOwnerFieldExists(String entry)
  {
      if(null != entry)
      {
          return "cm_lockOwner".contentEquals(entry)|| "cm:lockOwner".contentEquals(entry);
      }
      return false;
  }

private void addTimeFields(RelDataTypeFactory.FieldInfoBuilder fieldInfo, Map.Entry<String, String> entry, RelDataType type) {
    for(String postfix : postfixes) {
      addFieldInfo(fieldInfo, entry, type, postfix);
    }
  }

  private void addFieldInfo(RelDataTypeFactory.FieldInfoBuilder fieldInfo, Map.Entry<String, String> entry, RelDataType type, String postfix) {

      String formatted = entry.getKey();
      try
      {
          String[] withPrefix = QName.splitPrefixedQName(entry.getKey());
          String prefix = withPrefix[0];
          if (prefix != null && !prefix.isEmpty())
          {
              formatted = withPrefix[0]+"_"+withPrefix[1]+getPostfix(postfix);
          }
          
          //Potentially remove prefix, just shortname if unique
          //QueryParserUtils.matchPropertyDefinition will throw an error if duplicate
          
      } catch (NamespaceException e) {
          //ignore invalid qnames
      }
      
      if(isSelectStar) 
      {
          if(!selectStarFields.contains(entry.getKey()) && !selectStarFields.contains(formatted)) {
              return;
          }
      }
      fieldInfo.add(entry.getKey()+getPostfix(postfix), type).nullable(true);
      if(!formatted.contentEquals(entry.getKey() + getPostfix(postfix)))
      {
          fieldInfo.add(formatted, type).nullable(true);
      }
  }

  private String getPostfix(String postfix) {
    if(postfix != null) {
      return postfix;
    } else {
      return "";
    }
  }

  private Map<String, String> getIndexedFieldsInfo() throws RuntimeException {

    RefCounted<SolrIndexSearcher> refCounted = core.getSearcher();
    SolrIndexSearcher searcher = null;
    try {
      searcher = refCounted.get();
      LeafReader reader = searcher.getSlowAtomicReader();
      IndexSchema schema = searcher.getSchema();

      Set<String> fieldNames = new TreeSet<>();
      for (FieldInfo fieldInfo : reader.getFieldInfos()) {
        fieldNames.add(fieldInfo.name);
      }
      Map<String, String> fieldMap = new HashMap<>();
      for (String fieldName : fieldNames) {
        SchemaField sfield = schema.getFieldOrNull(fieldName);
        FieldType ftype = (sfield == null) ? null : sfield.getType();

        String alfrescoPropertyFromSchemaField = null;
        try
        {
            alfrescoPropertyFromSchemaField = AlfrescoSolrDataModel.getInstance().getAlfrescoPropertyFromSchemaField(fieldName);
        }
        catch (NamespaceException ne)
        {
              //Field name may have been created but now deactivated, e.g custom model.
              logger.warn("Unable to resolve field: " + fieldName);
        }

        if (isNotBlank(alfrescoPropertyFromSchemaField) && ftype != null)
        {
            String className = ftype.getClassArg();
            if (isNotBlank(className))
            {
                // Add the field
                fieldMap.put(alfrescoPropertyFromSchemaField, className);
            }
        }
      }

      return fieldMap;
    } finally {
      refCounted.decref();
    }
  }

    private void addSelectStarFields()
    {
        selectStarFields.add("cm_name");
        selectStarFields.add("cm_created");
        selectStarFields.add("cm_creator");
        selectStarFields.add("cm_modified");
        selectStarFields.add("cm_modifier");
        selectStarFields.add("cm_owner");
        selectStarFields.add("OWNER");
        selectStarFields.add("TYPE");
        selectStarFields.add("LID");
        selectStarFields.add("DBID");
        selectStarFields.add("cm_title");
        selectStarFields.add("cm_description");
        selectStarFields.add("cm_content.size");
        selectStarFields.add("cm_content.mimetype");
        selectStarFields.add("cm_content.encoding");
        selectStarFields.add("cm_content.locale");
        selectStarFields.add("cm_lockOwner");
        selectStarFields.add("SITE");
        selectStarFields.add("PRIMARYPARENT");
        selectStarFields.add("PARENT");
        selectStarFields.add("PATH");
        selectStarFields.add("ASPECT");
        selectStarFields.add("QNAME");
    }

  public boolean predicateExists(String sql)
  {
      
      if(sql != null && sql.toLowerCase().contains("where"))
      {
          return true;
      }
      return false;
  }
}
