package org.lhs.mabo.MongoDBUtils;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by mabo on 2016-7-12.
 */
public class MongodbUtil {
    private static MongodbUtil instance;
    static private String mongodbIp;
    static private int port;
    static private String db;
    static private List<ServerAddress> serverAddressesList;
    static private List<MongoCredential> credentials;
    static private MongoClientOptions.Builder builder = null;
    static private MongoClient mongoClient;
    static private MongoDatabase database;

    public static final String module = MongodbUtil.class.getName();


    private MongodbUtil(String mongodbIp, int port, String db) {
        this.mongodbIp = mongodbIp;
        this.port = port;
        this.db = db;
        this.serverAddressesList =null;
    }
    public static MongodbUtil getInstance(String mongodbIp, int port, String db){
        if(instance == null||(!MongodbUtil.mongodbIp.equalsIgnoreCase(mongodbIp)||MongodbUtil.port!=port)){
            instance = new MongodbUtil(mongodbIp,port,db);
            if(mongoClient!=null){
                mongoClient.close();
                mongoClient = null;
                database = null;
            }
        }else{
            if(!MongodbUtil.db.equalsIgnoreCase(db)){
                MongodbUtil.db = db;
                database = null;
            }
        }
        return instance;
    }

    public MongoClient getMongoClient() {
        Logger mongoLogger =Logger.getLogger("com.mongodb");
        mongoLogger.setLevel(Level.SEVERE);
        if(mongoClient!=null) return mongoClient;
        else {

            try{
                if(this.credentials != null){
                    if(builder == null){
                        mongoClient = new MongoClient(serverAddressesList, credentials);
                    }else{
                        MongoClientOptions myOptions = builder.build();
                        mongoClient = new MongoClient(serverAddressesList,credentials,myOptions);
                    }
                }else{
                    if(this.mongodbIp != null){
                        if(builder == null){
                            mongoClient = new MongoClient(mongodbIp, port);
                        }else{
                            MongoClientOptions myOptions = builder.build();
                            mongoClient = new MongoClient(new ServerAddress(mongodbIp,port),myOptions);
                        }
                    }else{
                        if(builder == null){
                            mongoClient = new MongoClient(serverAddressesList);
                        }else{
                            MongoClientOptions myOptions = builder.build();
                            mongoClient = new MongoClient(serverAddressesList,myOptions);
                        }
                    }
                }
            }catch (Exception e){
            }
            return mongoClient;
        }
    }


    public MongoDatabase getMongoDatabase() {
        if(mongoClient==null) getMongoClient();
        database = mongoClient.getDatabase(db);
        return database;
    }

    public MongoCollection getMongoCollection(String tableName) {
        if(database==null) getMongoDatabase();
        MongoCollection<Document> collection = database.getCollection(tableName);
        return collection;

    }

}
