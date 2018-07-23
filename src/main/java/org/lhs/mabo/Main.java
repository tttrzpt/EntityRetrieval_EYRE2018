package org.lhs.mabo;

import org.apache.lucene.queryParser.ParseException;
import org.lhs.mabo.evaluation.NDCG_new;
import org.lhs.mabo.reRank.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mabo on 2018-5-30.
 */
public class Main {


    public static void main(String[] args) throws IOException, ParseException {

        //First step, transfer DBpedia dump files and DBpedia-entity v2 collection to MongoDB
        // refers to DBpediaToMongoDB.java and V2CollectionToMongoDB.java

        //Second step, reank top 100 match entities by queryID, refers to ReRank100ByGraphRelationships.java

        //Third step, run all combinations of baseWeight and graphWeight
        //baseWeight + graphWeight = 1.0, step = 0.5, for initial, baseWeight=0.05, graphWeight=0.95
        //refers to DynamicReRankByBasePlusGraphRelationships.java


        /*String[] array = {"bm25f_ca_v2", "bm25", "bm25_ca_v2", "fsdm_elr_v2", "fsdm_v2", "lm", "lm_elr_v2",
                "mlm_all", "mlm_ca_v2", "prms", "sdm", "sdm_elr_v2"};

        double baseWeight = 0.15;
        double graphRelWeight = 1.0 - baseWeight;

        while (baseWeight < 1.0) {

            for (int i = 0; i < array.length; i++) {

                DynamicReRankByBasePlusGraphRelationships.reRankByBasePlusGraphRelationships(array[i], baseWeight, graphRelWeight);
            }
            baseWeight += 0.05;
            graphRelWeight = 1.0 - baseWeight;
        }


        String[] array_base = {"bm25f_ca_v2", "bm25", "bm25_ca_v2", "fsdm_elr_v2", "fsdm_v2", "lm", "lm_elr_v2",
                "mlm_all", "mlm_ca_v2", "prms", "sdm", "sdm_elr_v2"};

        baseWeight = 0.05;
        graphRelWeight = 1- baseWeight;

        while (baseWeight < 1.0) {

            String[] tmp_array_base = new String[array_base.length];
            for (int i = 0; i < tmp_array_base.length; i++) {
                tmp_array_base[i] = "BG_" + array_base[i] + "_reRank_" + String.format("%.2f", baseWeight) + "_" + String.format("%.2f", graphRelWeight);
            }

            System.out.println("baseWeight=" + baseWeight + "   " + "graphRelWeight=" + graphRelWeight + ":");
            System.out.println("------------------------------");

            List list = new ArrayList<>();
            list.add("SemSearch_ES");
            System.out.println("SemSearch_ES@10");
            NDCG_new.runNDCGForAlgorithmArraysByCategory(tmp_array_base, 10, list);
            System.out.println("------------------------------");
            System.out.println("SemSearch_ES@100");
            NDCG_new.runNDCGForAlgorithmArraysByCategory(tmp_array_base, 100, list);
            System.out.println("------------------------------");
            list.clear();
            list.add("INEX_LD");
            System.out.println("INEX_LD@10");
            NDCG_new.runNDCGForAlgorithmArraysByCategory(tmp_array_base, 10, list);
            System.out.println("------------------------------");
            System.out.println("INEX_LD@100");
            NDCG_new.runNDCGForAlgorithmArraysByCategory(tmp_array_base, 100, list);
            System.out.println("------------------------------");
            list.clear();
            list.add("INEX_XER");
            list.add("SemSearch_LS");
            list.add("TREC_");
            System.out.println("ListSearch@10");
            NDCG_new.runNDCGForAlgorithmArraysByCategory(tmp_array_base, 10, list);
            System.out.println("------------------------------");
            System.out.println("ListSearch@100");
            NDCG_new.runNDCGForAlgorithmArraysByCategory(tmp_array_base, 100, list);
            System.out.println("------------------------------");
            list.clear();
            list.add("QALD2_");
            System.out.println("QALD2_@10");
            NDCG_new.runNDCGForAlgorithmArraysByCategory(tmp_array_base, 10, list);
            System.out.println("------------------------------");
            System.out.println("QALD2_@100");
            NDCG_new.runNDCGForAlgorithmArraysByCategory(tmp_array_base, 100, list);
            System.out.println("------------------------------");
            System.out.println("Total@10");
            for (int i = 0; i < tmp_array_base.length; i++) {

                System.out.println(NDCG_new.NdcgCalculation(tmp_array_base[i], 10));
            }
            System.out.println("------------------------------");
            System.out.println("Total@100");
            for (int i = 0; i < tmp_array_base.length; i++) {

                System.out.println(NDCG_new.NdcgCalculation(tmp_array_base[i], 100));
            }
            System.out.println("------------------------------");

            baseWeight += 0.05;
            graphRelWeight = 1- baseWeight;
        }*/





        /*String[] array_base = {"bm25f_ca_v2", "bm25", "bm25_ca_v2", "fsdm_elr_v2", "fsdm_v2", "lm", "lm_elr_v2",
                "mlm_all", "mlm_ca_v2", "prms", "sdm", "sdm_elr_v2"};*/


        /*String[] array_reRank100 = {"bm25f_ca_v2_reRank100", "bm25_reRank100", "bm25_ca_v2_reRank100", "fsdm_elr_v2_reRank100", "fsdm_v2_reRank100", "lm_reRank100", "lm_elr_v2_reRank100",
                "mlm_all_reRank100", "mlm_ca_v2_reRank100", "prms_reRank100", "sdm_reRank100", "sdm_elr_v2_reRank100"};

        String[] array_base_rel = {"bm25f_ca_v2_reRank_base_rel", "bm25_reRank_base_rel", "bm25_ca_v2_reRank_base_rel", "fsdm_elr_v2_reRank_base_rel",
                "fsdm_v2_reRank_base_rel", "lm_reRank_base_rel", "lm_elr_v2_reRank_base_rel", "mlm_all_reRank_base_rel", "mlm_ca_v2_reRank_base_rel",
                "prms_reRank_base_rel", "sdm_reRank_base_rel", "sdm_elr_v2_reRank_base_rel"};*/

        /*String[] array_base = {"bm25", "prms", "mlm_all", "lm", "sdm", "lm_elr_v2", "sdm_elr_v2",
                "mlm_ca_v2", "bm25_ca_v2", "fsdm_v2", "bm25f_ca_v2", "fsdm_elr_v2"};

        for (int i = 0; i < array_base.length; i++) {

            System.out.println(NDCG_new.NdcgCalculation(array_base[i], 10));
        }
        System.out.println("------------------------------");
        for (int i = 0; i < array_base.length; i++) {

            System.out.println(NDCG_new.NdcgCalculation(array_base[i], 100));
        }
        System.out.println("------------------------------");

        List list = new ArrayList<>();
        list.add("SemSearch_ES");
        NDCG_new.runNDCGForAlgorithmArraysByCategory(array_base, 10, list);
        System.out.println("------------------------------");
        NDCG_new.runNDCGForAlgorithmArraysByCategory(array_base, 100, list);
        System.out.println("------------------------------");
        list.clear();
        list.add("INEX_LD");
        NDCG_new.runNDCGForAlgorithmArraysByCategory(array_base, 10, list);
        System.out.println("------------------------------");
        NDCG_new.runNDCGForAlgorithmArraysByCategory(array_base, 100, list);
        System.out.println("------------------------------");
        list.clear();
        list.add("QALD2_");
        NDCG_new.runNDCGForAlgorithmArraysByCategory(array_base, 10, list);
        System.out.println("------------------------------");
        NDCG_new.runNDCGForAlgorithmArraysByCategory(array_base, 100, list);
        System.out.println("------------------------------");
        list.clear();
        list.add("INEX_XER");
        list.add("SemSearch_LS");
        list.add("TREC_");
        NDCG_new.runNDCGForAlgorithmArraysByCategory(array_base, 10, list);
        System.out.println("------------------------------");
        NDCG_new.runNDCGForAlgorithmArraysByCategory(array_base, 100, list);
        System.out.println("------------------------------");*/










        /*ReRank100ByGraphRelationships reRank = new ReRank100ByGraphRelationships();
        String[] array = {"bm25f_ca_v2", "bm25", "bm25_ca_v2", "fsdm_elr_v2", "fsdm_v2", "lm", "lm_elr_v2",
                "mlm_all", "mlm_ca_v2", "prms", "sdm", "sdm_elr_v2"};
        for (int i = 0; i < array.length; i++) {

            reRank.reRankByGraphRelationships(array[i]);
        }*/

        /*String[] array = {"bm25f_ca_v2", "bm25", "bm25_ca_v2", "fsdm_elr_v2", "fsdm_v2", "lm", "lm_elr_v2",
                "mlm_all", "mlm_ca_v2", "prms", "sdm", "sdm_elr_v2"};
        for (int i = 0; i < array.length; i++) {

            ReRankByBasePlusGraphRelationships.reRankByBasePlusGraphRelationships(array[i]);
        }*/




        //CreateDBPeidaIndex.indexCategories("/home/mabo/eyre2018/dbpedia-entity-download/article_categories_en.ttl", "/home/mabo/eyre2018/categoryIndex/");

        //DBpediaToMongoDB.transferDBpediaToMongoDB("/home/mabo/eyre2018/dbpedia-entity-download/");

        //MongoDBQuery.createNodeID("entityID");

    }


}
