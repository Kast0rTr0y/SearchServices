#!/bin/bash
set -e

[ "$DEBUG" ] && set -x

nicebranch=`echo "$bamboo_planRepository_1_branch" | sed 's/\//_/'`
DOCKER_RESOURCES_PATH="${1:-packaging/target/docker-resources}"

if [ "${nicebranch}" = "master" ] || [ "${nicebranch#release}" != "${nicebranch}" ]
then
 
   tag_version=`echo "$bamboo_maven_version"`
   if [ "${bamboo_shortJobName}" = "Release" ]
   then
      tag_version=`echo "$bamboo_release_version"`
   fi

   dockerImage="quay.io/alfresco/insight-engine:$tag_version"
   echo "Building $dockerImage from $nicebranch using version $tag_version"

   docker build -t $dockerImage ${DOCKER_RESOURCES_PATH}

   echo "Running tests"
   docker run --rm "$dockerImage" [ -d /opt/alfresco-insight-engine/solr ] || (echo "solr dir does not exist" && exit 1)
   docker run --rm "$dockerImage" [ -d /opt/alfresco-insight-engine/data/alfrescoModels ] || (echo "alfrescoModels dir does not exist" && exit 1)
   docker run --rm "$dockerImage" [ -e /opt/alfresco-insight-engine/solr.in.sh ] || (echo "solr.in.sh does not exist" && exit 1)
   docker run --rm "$dockerImage" grep -q Alfresco /opt/alfresco-insight-engine/solr.in.sh || (echo "solr.in.sh does not contain Alfresco config" && exit 1)
   docker run --rm "$dockerImage" grep -q Alfresco /opt/alfresco-insight-engine/solr.in.cmd || (echo "solr.in.cmd does not containAlfresco config" && exit 1)
   docker run --rm "$dockerImage" grep -q LOG4J_PROPS /opt/alfresco-insight-engine/solr.in.sh || (echo "solr.in.sh does not contain LOG4J_PROPS" && exit 1)
   docker run --rm "$dockerImage" grep -q LOG4J_CONFIG /opt/alfresco-insight-engine/solr.in.cmd || (echo "solr.in.cmd does not contain LOG4J_CONFIG" && exit 1)
   docker run --rm "$dockerImage" [ -e /opt/alfresco-insight-engine/solrhome/conf/shared.properties ] || (echo "shared.properties does not exist" && exit 1)
   docker run --rm "$dockerImage" /opt/alfresco-insight-engine/solr/bin/solr start

   echo "Publishing $dockerImage..."
   docker push "$dockerImage"
   
   echo "Docker SUCCESS"
else
    echo "Only building and publishing docker images from master. Skipping for ${nicebranch}"
fi
