package org.lhs.mabo.MongoDBUtils;

import com.mongodb.client.MongoCollection;
import org.apache.log4j.Logger;
import org.bson.Document;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mabo on 2018-5-30.
 */
public class V2CollectionToMongoDB {

    private static Logger logger = Logger.getLogger(V2CollectionToMongoDB.class);
    private static MongodbUtil mongodbClient = MongodbUtil.getInstance("10.90.1.57", 27017, "eyre");
    private static MongoCollection qrels_Collection = mongodbClient.getMongoCollection("qrels_v2");
    private static MongoCollection queries_Collection = mongodbClient.getMongoCollection("queries_v2");
    private static MongoCollection queries_stopped_Collection = mongodbClient.getMongoCollection("queries_stopped_v2");

    /**
     * 将查询结果人工打分文件存入mongoDB
     * @param filePath
     */
    public static void transferQrelsFileToMongoDB(String filePath) {

        try {

            List list = new ArrayList<>();
            FileInputStream fis = new FileInputStream(filePath);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            /*FileReader reader = new FileReader(filePath);
            BufferedReader br = new BufferedReader(reader);*/
            String str = null;
            while((str = br.readLine()) != null) {

                String[] data = str.split("\t");
                if (data.length>0) {

                    Document doc = new Document();
                    doc.append("queryID", data[0]).append("Q", data[1])
                            .append("matchEntity", data[2]).append("judgement", data[3]);
                    list.add(doc);
                }
            }
            qrels_Collection.insertMany(list);
            logger.info("insert " + list.size() + " documents!");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 将查询id-查询内容文件存入mongoDB
     * @param filePath
     */
    public static void transferQueriesFileToMongoDB(String filePath) {

        try {

            List list = new ArrayList<>();
            FileInputStream fis = new FileInputStream(filePath);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            /*FileReader reader = new FileReader(filePath);
            BufferedReader br = new BufferedReader(reader);*/
            String str = null;
            while((str = br.readLine()) != null) {

                String[] data = str.split("\t");
                if (data.length>0) {

                    Document doc = new Document();
                    doc.append("queryID", data[0]).append("queryContent", data[1]);
                    list.add(doc);
                }
            }
            queries_Collection.insertMany(list);
            logger.info("insert " + list.size() + " documents!");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * transfer v2 baseline to mongodb
     * @param filePath
     */
    public static void transferRunResultsFileToMongoDB(String filePath) {

        try {

            File dir = new File(filePath);
            if (dir.isDirectory()) {

                for (File file: dir.listFiles()) {


                    List list = new ArrayList<>();
                    String fileName = file.getName();
                    if (fileName.contains("-"))
                        fileName = fileName.replaceAll("-", "_");
                    logger.info("start to process " + fileName + "......");
                    MongoCollection collection = mongodbClient.getMongoCollection(fileName.substring(0, fileName.indexOf(".")));
                    //MongoCollection collection = mongodbClient.getMongoCollection("test");
                    FileInputStream fis = new FileInputStream(file.getAbsoluteFile());
                    InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                    BufferedReader br = new BufferedReader(isr);
                    /*FileReader reader = new FileReader(file.getAbsoluteFile());
                    BufferedReader br = new BufferedReader(reader);*/
                    String str = null;
                    while((str = br.readLine()) != null) {

                        String[] data = str.split(" ");
                        if (data.length>0) {

                            Document doc = new Document();
                            doc.append("queryID", data[0]).append("Q", data[1])
                                    .append("matchEntity", data[2]).append("sortNo", data[3]).append("score", data[4]);
                            list.add(doc);
                        }
                    }
                    collection.insertMany(list);
                    logger.info("insert " + list.size() + " documents into " + fileName);
                }

            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static void main(String[] args) {

        /*transferQrelsFileToMongoDB("C:\\Users\\mabo\\Desktop\\CIKM Wokshop\\DBpedia-Entity" +
                "\\DBpedia-Entity-master\\collection\\v2\\qrels-v2.txt");*/
        /*transferQueriesFileToMongoDB("C:\\Users\\mabo\\Desktop\\CIKM Wokshop\\DBpedia-Entity" +
                "\\DBpedia-Entity-master\\collection\\v2\\queries-v2.txt");*/
        //transferRunResultsFileToMongoDB("C:\\Users\\mabo\\Desktop\\CIKM Wokshop\\DBpedia-Entity\\DBpedia-Entity-master\\runs\\v2\\s\\");


    }

}
