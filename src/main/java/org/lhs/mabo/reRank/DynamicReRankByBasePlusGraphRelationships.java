package org.lhs.mabo.reRank;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.lhs.mabo.MongoDBUtils.MongodbUtil;

import java.util.*;

/**
 * Created by mabo on 2018-7-3.
 */
public class DynamicReRankByBasePlusGraphRelationships {

    private static Logger logger = Logger.getLogger(DynamicReRankByBasePlusGraphRelationships.class);
    private static MongodbUtil mongodbClient = MongodbUtil.getInstance("10.90.1.57", 27017, "eyre");
    private static MongoCollection queryCollection = mongodbClient.getMongoCollection("queries_stopped_v2");


    /**
     *
     * @param algorithmName
     */
    public static void reRankByBasePlusGraphRelationships(String algorithmName, double baseWeight, double graphRelWeight) {

        logger.info("start to process algorithm: " + algorithmName + "...");
        FindIterable<Document> iter = queryCollection.find();
        Set<String> querySet = new HashSet<>();
        for (Document doc: iter) {

            querySet.add(doc.getString("queryID"));
        }

        for (String queryID: querySet) {

            logger.info("start to process " + queryID + "...");
            long startTime=System.currentTimeMillis();
            reRankByBasePlusGraphRelationships(algorithmName, queryID, baseWeight, graphRelWeight);
            long endTime=System.currentTimeMillis();
            logger.info(algorithmName + ": process queryID: " + queryID + ", run times: " + (endTime - startTime) + "ms");
        }
    }


    public static void reRankByBasePlusGraphRelationships(String algorithmName, String queryID, double baseWeight, double graphRelWeight) {

        MongoCollection collection = mongodbClient.getMongoCollection(algorithmName);
        MongoCollection rrCollection = mongodbClient.getMongoCollection(algorithmName + "_reRank100");
        FindIterable<Document> iter = collection.find(new BasicDBObject("queryID", queryID));

        List<Document> baseDocList = new ArrayList<>();
        if (iter!=null) {

            for (Document doc: iter) {

                double number = Double.parseDouble(doc.getString("sortNo"));
                if (number > 100) continue;
                baseDocList.add(doc);
            }
        }

        Map<String, Double> oldMap = new HashMap<>();
        Map<String, Double> reRankMap = new HashMap<>();
        for (Document doc: baseDocList) {

            String matchEntity = doc.getString("matchEntity");
            double score = Double.parseDouble(doc.getString("score"));
            oldMap.put(matchEntity, score);
            Document reRankDoc = (Document) rrCollection.find(new BasicDBObject("queryID", queryID).append("matchEntity", matchEntity)).first();
            double reRankScore = Double.parseDouble(reRankDoc.getString("score"));
            reRankMap.put(matchEntity, reRankScore);
        }

        oldMap = normalizationToZeroAndOne(oldMap);
        reRankMap = normalizationToZeroAndOne(reRankMap);

        Map<String, Double> fuseMap = new HashMap<>();
        for (Map.Entry<String, Double> entry: oldMap.entrySet()) {

            String entity = entry.getKey();
            double baseVal = entry.getValue();
            double graphRelVal = reRankMap.get(entity);
            fuseMap.put(entry.getKey(), baseWeight*baseVal + graphRelWeight*graphRelVal);
        }

        if (fuseMap.size()>0) {

            Map<String, Double> sortedMap = sortMap(fuseMap);
            MongoCollection reCollection = mongodbClient.getMongoCollection("BG_" + algorithmName + "_reRank_" + String.format("%.2f", baseWeight) + "_" + String.format("%.2f", graphRelWeight));
            List docList = new ArrayList<>();
            long i = 0;
            for (Map.Entry<String, Double> entry: sortedMap.entrySet()) {

                i++;
                Document doc = new Document();
                doc.append("queryID", queryID).append("matchEntity", entry.getKey())
                        .append("sortNo", i + "").append("score", entry.getValue() + "");
                docList.add(doc);
                if (docList.size() == 10000) {

                    reCollection.insertMany(docList);
                    docList.clear();
                }
            }

            if (docList.size()>0) {

                reCollection.insertMany(docList);
            }
        }

    }


    public static Map normalizationToZeroAndOne(Map<String, Double> map) {

        Map<String, Double> newMap = new HashMap<>();
        double min  = -1;
        double max = -1;
        for (double d: map.values()) {

            if (d > max) max = d;
            if (d < min) min = d;
        }

        for (Map.Entry<String, Double> entry: map.entrySet()) {

            String key = entry.getKey();
            double val = entry.getValue();
            double nScore = (val - min)/(max - min);
            newMap.put(key, nScore);
        }
        return newMap;
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
        Collections.sort(list, comparator);
        Map<String, Double> newMap = new LinkedHashMap();
        for (int i = 0; i < list.size(); i++) {
            newMap.put(list.get(i).getKey(), list.get(i).getValue());
        }
        return newMap;
    }



    public static void main(String[] args) {

        /*String[] array = {"bm25f_ca_v2", "bm25", "bm25_ca_v2", "fsdm_elr_v2", "fsdm_v2", "lm", "lm_elr_v2",
                "mlm_all", "mlm_ca_v2", "prms", "sdm", "sdm_elr_v2"};

        double baseWeight = 0.5;
        double graphRelWeight = 1- baseWeight;

        while (baseWeight < 1) {

            for (int i = 0; i < array.length; i++) {

                reRankByBasePlusGraphRelationships(array[i], baseWeight, graphRelWeight);
            }
            baseWeight += 0.5;
            graphRelWeight = 1- baseWeight;
        }*/



    }

}
