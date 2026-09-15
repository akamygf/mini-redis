package site.hfny258.database;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import site.hfny258.datastructure.RedisData;
import site.hfny258.internal.Dict;

import java.util.Set;

@Getter
@Setter
@Slf4j
public class RedisDB {
    private final Dict<byte[], RedisData> data;

    private final int id;

    public RedisDB(int id) {
        this.id = id;
        this.data = new Dict<>();
    }

    public Set<byte[]> keys() {
        return data.keySet();
    }

    public boolean exists(byte[] key) {
        return data.containsKey(key);
    }

    public void put(byte[] key, RedisData value) {
        data.put(key, value);
    }

    public RedisData get(byte[] key) {
        return data.get(key);
    }

    public void remove(byte[] key) {
        data.remove(key);
    }

    public int size() {
        return data.size();
    }
}
