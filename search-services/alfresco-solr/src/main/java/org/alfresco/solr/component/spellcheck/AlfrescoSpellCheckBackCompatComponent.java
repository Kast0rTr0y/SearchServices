
package org.alfresco.solr.component.spellcheck;

import java.util.HashMap;
import java.util.Map;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.*;

/**
 * @author Joel Bernstein
 * @since 5.2
 */

public class AlfrescoSpellCheckBackCompatComponent extends SearchComponent
{
    public void prepare(ResponseBuilder responseBuilder) {

    }

    public void process(ResponseBuilder responseBuilder) {
        responseBuilder.req.getParams().get("spellcheck");
        NamedList response = responseBuilder.rsp.getValues();
        NamedList spellcheck = (NamedList)response.get("spellcheck");
        NamedList collations = (NamedList)spellcheck.get("collations");
        NamedList suggest = (NamedList)spellcheck.get("suggest");
        NamedList collationList = collations != null ? collations : suggest;
        NamedList spellCheckExtras = new NamedList();

        for(int i=0; i<collationList.size(); i++) {
            if("collation".equals(collationList.getName(i))) {
                NamedList collation = (NamedList) collationList.getVal(i);
                String collationQuery = (String) collation.get("collationQuery");
                String collationQueryString = (String) collation.get("collationQueryString");
                spellCheckExtras.add(collationQuery, collationQueryString);
            }
        }

        response.add("spellcheck-extras", spellCheckExtras);
    }

    public String getDescription() {
        return null;
    }

    public void finishStage(ResponseBuilder rb) {
        if (!rb.req.getParams().getBool(SpellCheckComponent.COMPONENT_NAME, false) || rb.stage != ResponseBuilder.STAGE_GET_FIELDS)
            return;

        Map extras = new HashMap();
        for (ShardRequest sreq : rb.finished) {
            for (ShardResponse srsp : sreq.responses) {
                NamedList nl = null;

                try {
                    nl = (NamedList) srsp.getSolrResponse().getResponse().get("spellcheck-extras");
                } catch (Exception e) {
                    if (rb.req.getParams().getBool(ShardParams.SHARDS_TOLERANT, false)) {
                        continue; // looks like a shard did not return anything
                    }
                    throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                            "Unable to read spelling info for shard: " + srsp.getShard(), e);
                }

                if (nl != null) {
                    collectExtras(nl, extras);
                }
            }
        }

        if(extras.size() == 0) {
            return;
        }

        NamedList response = rb.rsp.getValues();
        NamedList spellcheck = (NamedList)response.get("spellcheck");
        NamedList collations = (NamedList)spellcheck.get("collations");
        NamedList suggest = (NamedList)spellcheck.get("suggest");

        NamedList collationList = collations != null ? collations : suggest;

        for(int i=0; i<collationList.size(); i++) {
            if("collation".equals(collationList.getName(i))) {
                NamedList collation = (NamedList) collationList.getVal(i);
                String collationQuery = (String) collation.get("collationQuery");
                String collationQueryString = (String) extras.get(collationQuery);
                collation.add("collationQueryString", collationQueryString);
            }
        }
    }

    private void collectExtras(NamedList spellcheckExtras, Map map) {
        for(int i=0; i<spellcheckExtras.size(); i++) {
            map.put(spellcheckExtras.getName(i), spellcheckExtras.getVal(i));
        }
    }
}
