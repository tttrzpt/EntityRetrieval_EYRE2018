package org.lhs.mabo.reRank;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.lhs.mabo.MongoDBUtils.MongodbUtil;

import java.io.*;
import java.util.*;

/**
 * Created by mabo on 2018-7-3.
 */
public class ReRank100ByGraphRelationships {

    private static Logger logger = Logger.getLogger(ReRank100ByGraphRelationships.class);
    private static MongodbUtil mongodbClient = MongodbUtil.getInstance("10.90.1.57", 27017, "eyre");
    private static MongoCollection idcollection = mongodbClient.getMongoCollection("entityID");
    private static MongoCollection queryCollection = mongodbClient.getMongoCollection("queries_stopped_v2");
    private static Map<String, Long> idMap;
    private static Map<String, String> typeMap = initRels();


    public ReRank100ByGraphRelationships() {

        logger.info("start to load entityIDMap...");
        idMap = new HashMap<>();
        FindIterable<Document> iter = idcollection.find();
        for (Document doc: iter) {

            idMap.put(doc.getString("head"), doc.getLong("id"));
        }
        logger.info("finish loading entityIDMap...");
    }


    public static Map<String, Set<String>> transferDBpediaToMongoDB(File file) {

        Map<String, Set<String>> map = new HashMap<>();

        try {

            String fileName = file.getName();
            logger.info("start to process " + fileName + "......");
            FileInputStream fis = new FileInputStream(file.getAbsoluteFile());
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String str = null;
            while((str = br.readLine()) != null) {

                if (str.startsWith("#")) continue;
                if (str.endsWith(" .")) {

                    str = str.substring(0, str.length()-2);
                    if (str.contains(" ")) {

                        String[] arr = {"", "", ""};
                        arr = str.split(" ");
                        if (arr.length!=3) continue;
                        String head = arr[0];
                        //String predicates = arr[1];
                        String tail = arr[2];
                        if (!map.containsKey(head)) {

                            map.put(head, new HashSet<String>());
                        }else {

                            Set set = map.get(head);
                            set.add(tail);
                            map.put(head, set);
                        }
                    }
                }

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }


    public static List<Map> transferDBpediaFilesToMongoDB(String filePath) {

        List<Map> list = new ArrayList<>();
        File dir = new File(filePath);
        for (File file: dir.listFiles()){

            Map map = transferDBpediaToMongoDB(file);
            list.add(map);
        }

        return list;
    }

    /**
     *
     * @param algorithmName
     */
    public static void reRankByGraphRelationships(String algorithmName) {

        FindIterable<Document> iter = queryCollection.find();
        Set<String> querySet = new HashSet<>();
        for (Document doc: iter) {

            querySet.add(doc.getString("queryID"));
        }

        for (String queryID: querySet) {

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
        Map<String, Double> map = new HashMap<>();
        if (iter!=null) {

            for (Document doc: iter) {

                double number = Double.parseDouble(doc.getString("sortNo"));
                if (number > 200) continue; //控制查询效率
                map.put(doc.getString("matchEntity"), number);
            }
        }

        Map newMap = new HashMap<>();
        if (map.size()>0) {

            for (Map.Entry<String, Double> entry: map.entrySet()) {

                String entity = entry.getKey();
                String reConEntity = reConstructEntity(entity); //entity格式转换
                double sortNo = entry.getValue();
                double reRankScore = 1.0/log2(sortNo+1);
                Set<String> relSet = getRelEntities(reConEntity);
                if (relSet!=null && relSet.size()>0) {

                    for (String relEntity: relSet) {

                        String deConEntity = deConstructEntity(relEntity); //entity格式反转换
                        if (!deConEntity.equals(entity) && map.containsKey(deConEntity)) {

                            double tmpSortNo = map.get(deConEntity);
                            reRankScore += 1.0/log2(tmpSortNo+1);
                        }
                    }
                }
                newMap.put(entity, reRankScore);

            }

        }


        if (newMap.size()>0) {

            Map<String, Double> sortedMap = sortMap(newMap);
            MongoCollection rrCollection = mongodbClient.getMongoCollection(algorithmName + "_reRank100");
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
     * 计算以2为底的log
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

    public static Set getRelEntities(String headEntity) {

        Set<String> relSet = new HashSet<>();
        for (Map.Entry<String, String> entry: typeMap.entrySet()) {

            String collectionName = entry.getKey().substring(0, entry.getKey().lastIndexOf("."));
            MongoCollection collection = mongodbClient.getMongoCollection(collectionName);
            FindIterable<Document> iter = collection.find(new BasicDBObject("head", headEntity));
            if (iter!=null) {

                for (Document doc: iter) {


                    String tail = doc.getString("tail")!=null ? doc.getString("tail"): "null";

                    boolean store = true;
                    if (relSet.contains(tail)) {
                        store = false;
                    }
                    if (store) {

                        relSet.add(tail);
                    }
                }
            }
        }
        return relSet;
    }


    public static Map initRels() {

        Map map = new HashMap<>();
        map.put("page_links_en.ttl", "REL");
        map.put("disambiguations_en.ttl", "REL");
        map.put("mappingbased_objects_en.ttl", "REL");
        map.put("transitive_redirects_en.ttl", "REL");
        return map;
    }


    public static void main(String[] args) {

        /*ReRank100ByGraphRelationships reRank = new ReRank100ByGraphRelationships();
        reRank.reRankByGraphRelationships("fsdm_elr_v2");*/
    }

}
