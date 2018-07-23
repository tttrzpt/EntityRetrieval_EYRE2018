package org.lhs.mabo.MongoDBUtils;

import com.mongodb.client.MongoCollection;
import org.apache.log4j.Logger;
import org.bson.Document;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Created by mabo on 2018-6-12.
 */
public class DBpediaToMongoDB {

    private static Logger logger = Logger.getLogger(DBpediaToMongoDB.class);
    private static MongodbUtil mongodbClient = MongodbUtil.getInstance("10.90.1.57", 27017, "eyre");

    public static void transferDBpediaToMongoDB(String filePath) {

        String pat = "<http://(.+?)>";
        Pattern pattern = Pattern.compile(pat);
        String pat2 = "\"(.+?)\"@en";
        Pattern pattern2 = Pattern.compile(pat2);

        Map<String, String> typeMap = initRelsAndProps();
        try {

            File dir = new File(filePath);
            if (dir.isDirectory()) {

                for (File file: dir.listFiles()) {


                    List list = new ArrayList<>();
                    String fileName = file.getName();
                    if (!typeMap.containsKey(fileName)) continue;
                    String type = typeMap.get(fileName);

                    logger.info("start to process " + fileName + "......");
                    String collectionName = fileName.substring(0, fileName.indexOf("."));
                    MongoCollection collection = mongodbClient.getMongoCollection(collectionName);
                    FileInputStream fis = new FileInputStream(file.getAbsoluteFile());
                    InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                    BufferedReader br = new BufferedReader(isr);
                    String str = null;
                    while((str = br.readLine()) != null) {

                        if (str.startsWith("#")) continue;
                        if (str.endsWith(" .")) {

                            str = str.substring(0, str.length()-2);
                            //System.out.println(str);
                            //System.out.println(str);
                            if (str.contains(" ")) {

                                String[] arr = {"", "", ""};
                                if (!str.endsWith("\"@en")) {
                                    arr = str.split(" ");
                                }else {
                                    /*arr[0] = str.substring(0, str.indexOf(" "));
                                    str = str.substring(str.indexOf(" ")+1);
                                    arr[1] = str.substring(0, str.indexOf(">")+1);
                                    arr[2] = str.substring(str.indexOf("\"")+1, str.lastIndexOf("\"@en"));*/

                                    Matcher matcher = pattern.matcher(str);
                                    int i = 0;
                                    while (matcher.find()) {

                                        String tmpStr = matcher.group();
                                        //System.out.println("<>:" + tmpStr);
                                        //System.out.println("tmpStr:" + tmpStr);
                                        arr[i] = tmpStr;
                                        i++;
                                        if (i==2)break;
                                    }
                                    Matcher matcher2 = pattern2.matcher(str);
                                    while (matcher2.find()) {

                                        String tmpStr = matcher2.group();
                                        //System.out.println("en:" + tmpStr);
                                        arr[i] = tmpStr.substring(1, tmpStr.indexOf("\"@en"));
                                        i++;
                                    }
                                }
                                if (arr.length!=3) continue;
                                String head = arr[0];
                                String predicates = arr[1];
                                String tail = arr[2];
                                Document doc = new Document();
                                doc.append("head", head).append("predicates", predicates)
                                        .append("tail", tail).append("type", type);
                                list.add(doc);
                                if (list.size() % 100000 == 0) {
                                    collection.insertMany(list);
                                    logger.info("insert " + list.size() + " documents into " + collectionName);
                                    list.clear();
                                }
                            }
                        }

                    }
                    if (list.size()>0) {
                        collection.insertMany(list);
                        logger.info("insert " + list.size() + " documents into " + collectionName);
                        list.clear();
                    }

                }

            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public static Map initRelsAndProps() {

        Map map = new HashMap<>();

        map.put("labels_en.ttl", "PROP");
        map.put("category_labels_en.ttl", "PROP");
        map.put("mappingbased_literals_en.ttl", "PROP");
        map.put("persondata_en.ttl", "PROP");
        map.put("short_abstracts_en.ttl", "PROP");
        map.put("anchor_text_en.ttl", "PROP");
        map.put("instance_types_transitive_en.ttl", "PROP");
        map.put("page_links_en.ttl", "REL");
        map.put("article_categories_en.ttl", "PROP");
        map.put("long_abstracts_en.ttl", "PROP");
        map.put("disambiguations_en.ttl", "REL");
        map.put("mappingbased_objects_en.ttl", "REL");
        map.put("transitive_redirects_en.ttl", "REL");
        map.put("infobox_properties_en.ttl", "PROP");
        return map;
    }

    public static void main(String[] args) {

        transferDBpediaToMongoDB("/home/mabo/eyre2018/dbpedia-entity-download/");
        //transferDBpediaToMongoDB("F:/dbpedia-entity-download/1/");


    }

}
