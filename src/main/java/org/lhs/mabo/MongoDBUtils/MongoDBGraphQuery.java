package org.lhs.mabo.MongoDBUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.lhs.mabo.graph.GraphNode;
import org.lhs.mabo.graph.GraphRelationship;
import org.lhs.mabo.graph.NodeProperty;

import java.util.*;

/**
 * Created by mabo on 2018-6-13.
 */
public class MongoDBGraphQuery {

    private static Logger logger = Logger.getLogger(MongoDBGraphQuery.class);
    private static MongodbUtil mongodbClient = MongodbUtil.getInstance("10.90.1.57", 27017, "eyre");
    private static MongoCollection idcollection = mongodbClient.getMongoCollection("entityID");
    private static MongoCollection queryCollection = mongodbClient.getMongoCollection("queries_stopped_v2");
    private static Map<String, String> typeMap = DBpediaToMongoDB.initRelsAndProps();

    public static String getNodeTextPresentation(GraphNode node) {

        String text = "";
        String nodeName = node.getNodeName();
        if (nodeName.contains("/") && nodeName.contains(">"))
            nodeName = nodeName.substring(nodeName.lastIndexOf("/")+1, nodeName.indexOf(">"));
        text = text + nodeName;
        Set<NodeProperty> propSet = node.getProperties();
        if (propSet!=null && propSet.size()>0) {

            for (NodeProperty property: propSet) {

                String propVal = (String)property.getPropertyValue();
                if (propVal.contains("<") && propVal.contains(">")) continue;
                text = text + " " + propVal;
            }
        }

        Set<GraphRelationship> relSet = node.getRelationships();
        if (relSet!=null && relSet.size()>0) {

            for (GraphRelationship relationship: relSet) {

                String relName = relationship.getRelationshipName();
                if (relName.equals("<http://dbpedia.org/ontology/wikiPageWikiLink>")) {

                    String tmpNodeName = relationship.getEndNode().getNodeName();
                    if (tmpNodeName.contains("/") && tmpNodeName.contains(">")) {
                        tmpNodeName = tmpNodeName.substring(tmpNodeName.lastIndexOf("/")+1, tmpNodeName.indexOf(">"));
                    }else {
                        continue;
                    }

                    text = text + " " + tmpNodeName;
                }
            }
        }

        return text;
    }


    public static String getNodeDisambiguations(GraphNode node) {

        String text = "";

        Set<GraphRelationship> relSet = node.getRelationships();
        if (relSet!=null && relSet.size()>0) {

            for (GraphRelationship relationship: relSet) {

                String relName = relationship.getRelationshipName();
                if (relName.equals("<http://dbpedia.org/ontology/wikiPageDisambiguates>")) {

                    String tmpNodeName = relationship.getEndNode().getNodeName();
                    if (tmpNodeName.contains("/") && tmpNodeName.contains(">")) {
                        tmpNodeName = tmpNodeName.substring(tmpNodeName.lastIndexOf("/")+1, tmpNodeName.indexOf(">"));
                    }else {
                        continue;
                    }

                    text = text + " " + tmpNodeName;
                }
            }
        }

        return text;
    }

    /**
     * retrieve mongoDB data as GraphNode by DBpedia entity name
     * @param headEntity
     * @return
     */
    public static GraphNode queryByDBpediaheadEntity(String headEntity) {

        long headID = getNodeIDbyEntityName(headEntity);
        GraphNode headNode = new GraphNode(headID);
        headNode.setNodeName(headEntity);
        Set<NodeProperty> propSet = new HashSet<>();
        Set<GraphRelationship> relSet = new HashSet<>();
        for (Map.Entry<String, String> entry: typeMap.entrySet()) {

            String collectionName = entry.getKey().substring(0, entry.getKey().lastIndexOf("."));
            String type = entry.getValue();
            MongoCollection collection = mongodbClient.getMongoCollection(collectionName);
            FindIterable<Document> iter = collection.find(new BasicDBObject("head", headEntity));
            if (iter!=null) {

                for (Document doc: iter) {

                    String predicates = doc.getString("predicates")!=null ? doc.getString("predicates"): "null";
                    String tail = doc.getString("tail")!=null ? doc.getString("tail"): "null";
                    if (type.equals("PROP")) {

                        boolean store = true;
                        for (NodeProperty nodeProperty: propSet) {

                            String p = nodeProperty.getPropertyName();
                            String v = (String) nodeProperty.getPropertyValue();
                            if (p.equals(predicates) && v.equals(tail)) {
                                store = false;
                            }
                        }
                        if (store) {

                            NodeProperty nodeProperty = new NodeProperty(predicates, tail);
                            propSet.add(nodeProperty);
                        }
                    }else {

                        boolean store = true;
                        long tailID = getNodeIDbyEntityName(tail);
                        for (GraphRelationship relationship: relSet) {

                            long tmptailID = relationship.getEndNode().getId();
                            if (tmptailID == tailID && predicates.equals(relationship.getRelationshipName())) {
                                store = false;
                            }
                        }
                        if (store && tailID != -1) {

                            GraphNode tailNode = new GraphNode(tailID);
                            tailNode.setNodeName(tail);
                            GraphRelationship relationship = new GraphRelationship(predicates);
                            relationship.setStartNode(headNode);
                            relationship.setEndNode(tailNode);
                            relSet.add(relationship);
                        }

                    }
                }

            }

        }

        headNode.setProperties(propSet);
        headNode.setRelationships(relSet);
        return headNode;

    }


    public static void createNodeID(String idCollectionName) {

        MongoCollection idCollection = mongodbClient.getMongoCollection(idCollectionName);
        long id = 0;
        Map<String, Long> idMap = new HashMap<>();
        for (Map.Entry<String, String> entry: typeMap.entrySet()) {

            String collectionName = entry.getKey().substring(0, entry.getKey().lastIndexOf("."));
            MongoCollection collection = mongodbClient.getMongoCollection(collectionName);
            FindIterable<Document> iter = collection.find();
            if (iter!=null) {

                for (Document doc: iter) {

                    String head = doc.getString("head")!=null ? doc.getString("head"): "null";
                    if (!idMap.containsKey(head)) {

                        idMap.put(head, id++);
                    }
                }
                logger.info("after processing collection: " + collectionName + ", idMap size: " + idMap.size());
            }

        }

        List docList = new ArrayList<>();
        for (Map.Entry<String, Long> entry: idMap.entrySet()) {

            Document doc = new Document();
            doc.append("head", entry.getKey()).append("id", entry.getValue());
            docList.add(doc);
            if (docList.size() == 100000) {
                idCollection.insertMany(docList);
                logger.info("insert " + docList.size() + " documents into " + idCollectionName);
                docList.clear();

            }
        }
        if (docList.size()>0) {
            idCollection.insertMany(docList);
            logger.info("insert " + docList.size() + " documents into " + idCollectionName);
            docList.clear();
        }
        logger.info("finish!!!");
    }


    public static long getNodeIDbyEntityName(String entityName) {

        if (idcollection.find(new BasicDBObject("head", entityName))!=null) {

            Document tmpDoc = (Document) idcollection.find(new BasicDBObject("head", entityName)).first();
            if (tmpDoc!=null) {
                long id = tmpDoc.getLong("id");
                return id;
            }
            return -1;
        } else return -1;
    }

    /**
     *
     * @param algorithmName
     */
    public static void reRankByGraphRelationships(String algorithmName) {

        FindIterable<org.bson.Document> iter = queryCollection.find();
        for (Document doc: iter) {

            String queryID = doc.getString("queryID");
            logger.info("start to process " + queryID + "...");
            long startTime=System.currentTimeMillis();
            reRankByGraphRelationships(algorithmName, queryID);
            long endTime=System.currentTimeMillis();
            logger.info(algorithmName + ": process queryID: " + queryID + ", run times: " + (endTime - startTime) + "ms");
        }

    }


    /**
     *
     * @param algorithmName
     * @param queryID
     */
    public static void reRankByGraphRelationships(String algorithmName, String queryID) {

        MongoCollection collection = mongodbClient.getMongoCollection(algorithmName);
        FindIterable<Document> iter = collection.find(new BasicDBObject("queryID", queryID));
        Map<String, String> map = new HashMap<>();
        if (iter!=null) {

            for (Document doc: iter) {

                map.put(doc.getString("matchEntity"), doc.getString("sortNo"));
            }
        }

        Map newMap = new HashMap<>();
        if (map.size()>0) {

            for (Map.Entry<String, String> entry: map.entrySet()) {

                String entity = entry.getKey();
                String reConEntity = reConstructEntity(entity);
                double sortNo = Double.parseDouble(entry.getValue());
                double reRankScore = 1.0/log2(sortNo+1);
                GraphNode node = queryByDBpediaheadEntity(reConEntity);
                if (node!=null && node.getRelationships()!=null && node.getRelationships().size()>0) {

                    Set<GraphRelationship> relSet = node.getRelationships();
                    for (GraphRelationship relationship: relSet) {

                        String relNodeName = relationship.getEndNode().getNodeName();
                        String deConEntity = deConstructEntity(relNodeName);
                        if (!deConEntity.equals(entity) && map.containsKey(deConEntity)) {

                            double tmpSortNo = Double.parseDouble(map.get(deConEntity));
                            reRankScore += 1.0/log2(tmpSortNo+1);
                        }
                    }
                }
                newMap.put(entity, reRankScore);

            }

        }

        if (newMap.size()>0) {

            Map<String, Double> sortedMap = sortMap(newMap);
            MongoCollection rrCollection = mongodbClient.getMongoCollection(algorithmName + "_reRank");
            List docList = new ArrayList<>();
            long i = 0;
            for (Map.Entry entry: sortedMap.entrySet()) {

                i++;
                Document doc = new Document();
                doc.append("queryID", queryID).append("matchEntity", entry.getKey())
                        .append("sortNo", i + "").append("score", entry.getValue() + "");
                docList.add(doc);
                if (docList.size() == 10000) {

                    rrCollection.insertMany(docList);
                    docList.clear();
                }
            }

            if (docList.size()>0) {

                rrCollection.insertMany(docList);
            }
        }

    }


    public static String reConstructEntity(String entity) {

        String newEntity = "";
        if (entity.startsWith("<dbpedia:") && entity.endsWith(">")) {

            newEntity = "<http://dbpedia.org/resource/" + entity.substring(entity.indexOf("<dbpedia:")+9);
        }
        return newEntity;
    }

    public static String deConstructEntity(String entity) {

        String newEntity = "";
        if (entity.startsWith("<http://dbpedia.org/resource/") && entity.endsWith(">")) {

            newEntity = "<dbpedia:" + entity.substring(entity.indexOf("<http://dbpedia.org/resource/")+29);
        }
        return newEntity;
    }

    /**
     *
     * @param value
     * @return
     */
    public static double log2(double value) {

        return Math.log(value) / Math.log(2);
    }


    public static Map<String, Double> sortMap(Map oldMap) {

        List<Map.Entry<String, Double>> list = new ArrayList<>(oldMap.entrySet());

        Comparator<Map.Entry<String, Double>> comparator = new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> arg0,
                               Map.Entry<String, Double> arg1) {
                if((arg1.getValue() - arg0.getValue())<0)
                    return -1;
                else if((arg1.getValue() - arg0.getValue())>0)
                    return 1;
                else return 0;
            }
        };
        Collections.sort(list,comparator);
        Map<String, Double> newMap = new LinkedHashMap();
        for (int i = 0; i < list.size(); i++) {
            newMap.put(list.get(i).getKey(), list.get(i).getValue());
        }
        return newMap;
    }




    public static void main(String[] args) {

        long startTime=System.currentTimeMillis();
        GraphNode node = queryByDBpediaheadEntity("<http://dbpedia.org/resource/List_of_Apollo_astronauts>");
        long endTime=System.currentTimeMillis();
        logger.info("run times: " + (endTime - startTime)/1000 + "s");

        System.out.println(getNodeTextPresentation(node));
        System.out.println("----------------------------");
        System.out.println(getNodeDisambiguations(node));
        System.out.println();



        //reRankByGraphRelationships("bm25_ca_v2");

    }
}
