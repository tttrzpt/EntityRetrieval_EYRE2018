package org.lhs.mabo.evaluation;

import com.mongodb.BasicDBObject;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.lhs.mabo.MongoDBUtils.MongodbUtil;

import java.io.IOException;
import java.util.*;

/**
 * Created by mabo on 2018-5-30.
 */
public class NDCG_new {

    private static Logger logger = Logger.getLogger(NDCG_new.class);
    private static MongodbUtil mongodbClient = MongodbUtil.getInstance("10.90.1.57", 27017, "eyre");
    private static MongoCollection queryCollection = mongodbClient.getMongoCollection("queries_v2");
    private static MongoCollection qrelsCollection = mongodbClient.getMongoCollection("qrels_v2");

    /**
     *
     * @param algorithmName
     * @param k if compute NDCG@10，k = 10
     */
    public static float NdcgCalculation(String algorithmName, int k) {

        List<String> queryIDList = getAllQueryIDs();
        List<Float> valList = new ArrayList<>();
        int c = 0;
        for (String queryID: queryIDList) {

            c++;
            Map<String, Integer> qrelsMap = getQrelsByQueryID(queryID);
            Map<Integer, String> algoMatchMap;
            if (algorithmName.endsWith("_doc2vec")) {
                algoMatchMap = getAlgorithmMatchesByQueryIDForDoc2vec(algorithmName, queryID);
            }else {
                algoMatchMap = getAlgorithmMatchesByQueryID(algorithmName, queryID);
            }


            int[] gain =  new int[k];
            for (int i = 0; i < k; i++) {

                String matchEntity = algoMatchMap.get(i+1);
                if (qrelsMap.containsKey(matchEntity)) {
                    gain[i] = qrelsMap.get(matchEntity);
                }else {
                    gain[i] = 0;
                }
            }
            //DCG
            float[] dcg = getDCG(gain);
            //IDCG
            Integer[] idealGain = getIdealGain(qrelsMap, algoMatchMap, k);
            float[] idcg = getIdealDCG(idealGain);
            //NDCG
            float ndcgVal = getNDCG(dcg, idcg);
            valList.add(ndcgVal);

        }
        float sum = 0;
        for (float f: valList) {
            sum += f;
        }
        float ndcgVal = sum/valList.size();
        return ndcgVal;
    }

    /**
     * calculate four query categories NDCG
     * @param algorithmName
     * @param k
     * @param prefixes query category prefixes
     * @return
     */
    public static float NdcgCalculationForCategorizedQueries(String algorithmName, int k, List<String> prefixes) {

        List<String> queryIDList = getAllQueryIDs();
        List<String> newQueryIDList = new ArrayList<>();
        for (String queryID: queryIDList) {

            for (String prefix: prefixes) {
                if (queryID.startsWith(prefix))
                    newQueryIDList.add(queryID);
            }
        }
        List<Float> valList = new ArrayList<>();
        int c = 0;
        for (String queryID: newQueryIDList) {

            for (String prefix: prefixes) {
                if (!queryID.startsWith(prefix)) continue;
            }
            c++;
            Map<String, Integer> qrelsMap = getQrelsByQueryID(queryID);
            Map<Integer, String> algoMatchMap;
            algoMatchMap = getAlgorithmMatchesByQueryID(algorithmName, queryID);

            int[] gain =  new int[k];
            for (int i = 0; i < k; i++) {

                String matchEntity = algoMatchMap.get(i+1);
                if (qrelsMap.containsKey(matchEntity)) {
                    gain[i] = qrelsMap.get(matchEntity);
                }else {
                    gain[i] = 0;
                }
            }
            //DCG
            float[] dcg = getDCG(gain);
            //IDCG
            Integer[] idealGain = getIdealGain(qrelsMap, algoMatchMap, k);
            float[] idcg = getIdealDCG(idealGain);
            //NDCG
            float ndcgVal = getNDCG(dcg, idcg);
            valList.add(ndcgVal);
        }
        float sum = 0;
        for (float f: valList) {
            sum += f;
        }
        float ndcgVal = sum/valList.size();
        return ndcgVal;
    }

    /**
     *
     * @return
     */
    public static List<String> getAllQueryIDs() {

        DistinctIterable<String> iter = queryCollection.distinct("queryID", new BasicDBObject(), String.class);
        List list = new ArrayList<>();
        if (iter!=null) {

            for (String str: iter)
                list.add(str);
        }
        return list;
    }

    /**
     *
     * @param queryID
     * @return
     */
    public static Map getQrelsByQueryID(String queryID) {

        Map map = new HashMap<>();
        FindIterable<Document> iter = qrelsCollection.find(new BasicDBObject("queryID", queryID));
        if (iter!=null) {

            for (Document doc: iter) {

                String matchEntity = doc.getString("matchEntity")!=null ? doc.getString("matchEntity"): "";
                String judgement = doc.getString("judgement")!=null ? doc.getString("judgement"): "";
                if (!matchEntity.equals("") && !judgement.equals("")) {
                    map.put(matchEntity, Integer.parseInt(judgement));
                }
            }
        }
        return map;
    }

    /**
     *
     * @param algorithmName
     * @return
     */
    public static Map getAlgorithmMatchesByQueryID(String algorithmName, String queryID) {

        Map map = new HashMap<>();
        MongoCollection collection = mongodbClient.getMongoCollection(algorithmName);
        FindIterable<Document> iter = collection.find(new BasicDBObject("queryID", queryID));
        if (iter!=null) {

            for (Document doc: iter) {

                String matchEntity = doc.getString("matchEntity")!=null ? doc.getString("matchEntity"): "";
                String sortNo = doc.getString("sortNo")!=null ? doc.getString("sortNo"): "";
                if (!matchEntity.equals("") && !sortNo.equals("")) {
                    map.put(Integer.parseInt(sortNo), matchEntity);
                }
            }
        }
        return map;
    }

    /**
     *
     * @param algorithmName
     * @param queryID
     * @return
     */
    public static Map getAlgorithmMatchesByQueryIDForDoc2vec(String algorithmName, String queryID) {

        Map map = new HashMap<>();
        MongoCollection collection = mongodbClient.getMongoCollection(algorithmName);
        FindIterable<Document> iter = collection.find(new BasicDBObject("queryID", queryID));
        if (iter!=null) {

            for (Document doc: iter) {

                String matchEntity = doc.getString("matchEntity")!=null ? doc.getString("matchEntity"): "";
                int sortNo = doc.getInteger("sortNo")!=null ? doc.getInteger("sortNo"): -1;
                if (!matchEntity.equals("") && !(sortNo == -1)) {
                    map.put(sortNo, matchEntity);
                }
            }
        }
        return map;
    }


    public static String reConstructEntity(String entity) {

        String newEntity = "";
        if (entity.startsWith("<http://dbpedia.org/resource/") && entity.endsWith(">")) {

            newEntity = "<dbpedia:" + entity.substring(entity.indexOf("<http://dbpedia.org/resource/")+29);
        }
        return newEntity;
    }


    /**
     * 计算DCG
     * @param gain
     * @return
     */
    public static float[] getDCG(int[] gain) {

        float[] discountedgain = new float[gain.length];
        float[] dcg = new float[gain.length];
        for (int i = 0; i< gain.length; i++)
        {
            if (i == 0)
                //discountedgain[i] = (float) (Math.pow(2, gain[i])-1);
                discountedgain[i] = (float)gain[i];
            else
                //discountedgain[i] = (float) ((Math.pow(2, gain[i])-1)/((float)Math.log(i+2)/Math.log(2)));
                discountedgain[i] = (float) ((gain[i])/((float)Math.log(i+2)/Math.log(2)));
        }

        for (int i=0;i<gain.length;i++)
        {
            if (i == 0)
                dcg[i] = discountedgain[i];
            else
                dcg[i] = dcg[i-1] + discountedgain[i];
        }
        return dcg;
    }

    /**
     * IdealGain
     * @param qrelsMap
     * @param algoMatchMap
     * @param k
     * @return
     */
    public static Integer[] getIdealGain(Map<String, Integer> qrelsMap, Map<Integer, String> algoMatchMap, int k) {

        Integer[] idealGain = new Integer[k];
        for (int i=0; i<idealGain.length; i++) {
            idealGain[i] = 0;
        }
        Map<String, Integer> matchScoreMap = new HashMap<>();
        for (String str: algoMatchMap.values()) {

            if (qrelsMap.containsKey(str)) {
                matchScoreMap.put(str, qrelsMap.get(str));
            }else {
                matchScoreMap.put(str, 0);
            }
        }

        Integer[] tmpGain = matchScoreMap.values().toArray(idealGain);
        for (int i=0; i<idealGain.length; i++) {
            if (tmpGain[i] == null)
                tmpGain[i] = 0;
        }
        Arrays.sort(tmpGain, Collections.reverseOrder());

        for (int i = 0; i < idealGain.length; i++) {

            idealGain[i] = tmpGain[i];
        }
        return idealGain;
    }

    public static float[] getIdealDCG(Integer[] ideaGain) {

        float[] discountedigain = new float[ideaGain.length];
        float[] idcg = new float[ideaGain.length];
        for (int i = 0; i< ideaGain.length; i++)
        {
            if (i == 0)
                //discountedigain[i] = (float) (Math.pow(2, ideaGain[i])-1);
                discountedigain[i] = (float) ideaGain[i];
            else
                //discountedigain[i] = (float) ((Math.pow(2, ideaGain[i])-1)/((float)Math.log(i+2)/Math.log(2)));
                discountedigain[i] = (float) ((ideaGain[i])/((float)Math.log(i+2)/Math.log(2)));
        }

        for (int i=0;i<ideaGain.length;i++)
        {
            if (i == 0)
                idcg[i] = discountedigain[i];
            else
                idcg[i] = idcg[i-1] + discountedigain[i];
        }
        return idcg;
    }


    public static float getNDCG(float[] dcg, float[] idealdcg) {

        float[] ndcg = new float[dcg.length];
        for (int i=0; i<dcg.length; i++ ) {

            if (idealdcg[i] == 0 || dcg[i] == 0) {
                ndcg[i] = 0;
            }else {
                ndcg[i] = dcg[i]/idealdcg[i];
            }
        }
        return evaluateAvgNDGC(ndcg);
    }

    public static float evaluateAvgNDGC(float[] ndcg)
    {
        float avgndcg=0;
        float sum=0;
        for (int i=0; i < ndcg.length; i++) {
            sum += ndcg[i];
        }
        avgndcg = (sum / ndcg.length);
        return avgndcg;
    }

    /**
     * Map assending sort by key
     * @param oldMap
     * @return
     */
    public static Map<Integer, String> sortMapByKey(Map oldMap) {

        ArrayList<Map.Entry<Integer, String>> list = new ArrayList<Map.Entry<Integer, String>>(oldMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Integer, String>>() {

            @Override
            public int compare(Map.Entry<Integer, String> arg0,
                               Map.Entry<Integer, String> arg1) {
                return (int) (arg0.getKey() - arg1.getKey());
            }
        });
        Map<Integer, String> newMap = new LinkedHashMap();
        for (int i = 0; i < list.size(); i++) {
            newMap.put(list.get(i).getKey(), list.get(i).getValue());
        }
        return newMap;
    }


    /**
     *
     * SemSearch_ES: Named entity queries(113: SemSearch_ES)
     * INEX-LD: IR-style keyword queries(154: INEX_)
     * QALD2: Natural language questions(140: QALD2_)
     * ListSearch: Queries that seek a particular list of entities(43: SemSearch_LS)
     * @param array
     * @param k NDCG@k
     */
    public static void runNDCGForAlgorithmArrays(String[] array, int k) {

        for (int i = 0; i < array.length; i++) {

            System.out.println(array[i] + " NDCG score@" + k + ": " + NdcgCalculation(array[i], k));
        }

    }

    /**
     *
     * @param array
     * @param k
     * @param prefixes
     */
    public static void runNDCGForAlgorithmArraysByCategory(String[] array, int k, List<String> prefixes) {

        for (int i = 0; i < array.length; i++) {

            //System.out.println(prefix + ": " + array[i] + " NDCG score@" + k + ": "+ NdcgCalculationForCategorizedQueries(array[i], k, prefix));
            System.out.println(NdcgCalculationForCategorizedQueries(array[i], k, prefixes));
        }

    }



}
