package org.lhs.mabo.reRank;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.lhs.mabo.MongoDBUtils.MongodbUtil;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mabo on 2018-7-5.
 */
public class Utils {

    private static Logger logger = Logger.getLogger(Utils.class);
    private static MongodbUtil mongodbClient = MongodbUtil.getInstance("10.90.1.57", 27017, "eyre");
    private static MongoCollection abstractCollection = mongodbClient.getMongoCollection("short_abstracts_en");
    private static Set<String> set;

    public Utils() {

        logger.info("start to load short_abstracts_en...");
        set = new HashSet<>();
        //set = transferDBpediaToMongoDB("");
        FindIterable<Document> iter = abstractCollection.find();
        for (Document doc: iter) {

            set.add(doc.getString("head"));
        }
        logger.info("finish loading short_abstracts_en!!!");
    }

    public static void countMiss(String algorithmName) {

        long total = 0;
        long miss = 0;
        long c = 0;
        MongoCollection collection = mongodbClient.getMongoCollection(algorithmName);
        FindIterable<Document> iter = collection.find();
        for (Document doc: iter) {

            c++;
            if (c == 100000)
                logger.info("processed " + c);
            String entity = reConstructEntity(doc.getString("matchEntity"));
            if (set.contains(entity)) {
                total += 1;
            }else {
                miss += 1;
            }
        }

        logger.info(algorithmName + " total: " + total);
        logger.info(algorithmName + " miss: " + miss);

    }

    public static String reConstructEntity(String entity) {

        String newEntity = "";
        if (entity.startsWith("<dbpedia:") && entity.endsWith(">")) {

            newEntity = "<http://dbpedia.org/resource/" + entity.substring(entity.indexOf("<dbpedia:")+9);
        }
        return newEntity;
    }


    public static Set<String> transferDBpediaToMongoDB(String filePath) {

        Set<String> set = new HashSet<>();

        try {

            File file = new File(filePath);
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
                        //String tail = arr[2];
                        set.add(head);
                    }
                }

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return set;
    }


    public static void main(String[] args) {

        Utils utils = new Utils();
        String[] array = {"bm25f_ca_v2", "bm25", "bm25_ca_v2", "fsdm_elr_v2", "fsdm_v2", "lm", "lm_elr_v2",
                "mlm_all", "mlm_ca_v2", "prms", "sdm", "sdm_elr_v2"};
        for (int i = 0; i < array.length; i++) {

            utils.countMiss(array[i]);
        }


    }

}
