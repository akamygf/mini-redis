package site.hfny258.server.core;

import site.hfny258.datastructure.RedisData;

import java.util.Set;

public interface RedisCore {
    Set<byte[]> key();
    void put(byte[] key, RedisData value);
    RedisData get(byte[] key);
    void remove(byte[] key);
    void selectDB(int dbIndex);
    int getDBNum();
    void setDBNum(int dbNum);
    int getCurrentDBIndex();
}
