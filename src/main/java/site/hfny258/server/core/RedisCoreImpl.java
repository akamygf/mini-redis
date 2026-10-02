package site.hfny258.server.core;

import site.hfny258.database.RedisDB;
import site.hfny258.datastructure.RedisBytes;
import site.hfny258.datastructure.RedisData;

import java.util.List;
import java.util.Set;

public class RedisCoreImpl implements RedisCore{
    private final List<RedisDB> database;

    private final int dbNum;
    private int currentDBIndex = 0;

    public RedisCoreImpl(int dbNum){
        this.dbNum = dbNum;
        this.database = new java.util.ArrayList<>(dbNum);
        for (int i = 0; i < dbNum; i++) {
            database.add(new RedisDB(i));
        }
    }

    @Override
    public Set<RedisBytes> keys() {
        int dbIndex = getCurrentDBIndex();
        RedisDB redisDB = database.get(dbIndex);
        return redisDB.keys();
    }

    @Override
    public void put(RedisBytes key, RedisData value) {
        int dbIndex = getCurrentDBIndex();
        RedisDB redisDB = database.get(dbIndex);
        redisDB.put(key, value);

    }

    @Override
    public RedisData get(RedisBytes key) {
        int dbIndex = getCurrentDBIndex();
        RedisDB redisDB = database.get(dbIndex);
        if(redisDB.exists(key)){
            return redisDB.get(key);
        }
        return null;
    }

    @Override
    public void remove(RedisBytes key) {
        int dbIndex = getCurrentDBIndex();
        RedisDB redisDB = database.get(dbIndex);
        redisDB.remove(key);
    }

    @Override
    public void selectDB(int dbIndex) {
        if(dbIndex >= 0 && dbIndex < dbNum){
            currentDBIndex = dbIndex;
        }
        else{
            throw new RuntimeException("dbIndex out of range");
        }
    }

    @Override
    public int getDBNum() {
        return dbNum;
    }

    @Override
    public void setDBNum(int dbNum) {

    }

    @Override
    public int getCurrentDBIndex() {
        return currentDBIndex;
    }
}
