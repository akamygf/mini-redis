package site.hfny258.server.core;

import site.hfny258.datastructure.RedisBytes;
import site.hfny258.datastructure.RedisData;

import java.util.Set;

public interface RedisCore {
    Set<RedisBytes> keys();
    void put(RedisBytes key, RedisData value);
    RedisData get(RedisBytes key);
    void remove(RedisBytes key);
    void selectDB(int dbIndex);
    int getDBNum();
    void setDBNum(int dbNum);
    int getCurrentDBIndex();
}
