package site.hfny258.command.impl.string;

import lombok.extern.slf4j.Slf4j;
import site.hfny258.command.Command;
import site.hfny258.command.CommandType;
import site.hfny258.datastructure.RedisBytes;
import site.hfny258.datastructure.RedisData;
import site.hfny258.datastructure.RedisString;
import site.hfny258.internal.Sds;
import site.hfny258.protocal.BulkString;
import site.hfny258.protocal.Resp;
import site.hfny258.protocal.SimpleString;
import site.hfny258.server.core.RedisCore;
@Slf4j
public class Set implements Command {
    private RedisBytes key;
    private RedisBytes value;
    private RedisCore redisCore;

    public Set(RedisCore redisCore) {
        this.redisCore = redisCore;
    }
    @Override
    public CommandType getType() {
        return CommandType.SET;
    }

    @Override
    public void setContext(Resp[] array) {
        if(array.length <3){
            throw new IllegalStateException("Invalid number of arguments");
        }
        key = ((BulkString) array[1]).getContent();
        value = ((BulkString) array[2]).getContent();
    }

    @Override
    public Resp handle() {
        if(redisCore.get(key) != null){
            RedisData data = redisCore.get(key);
            if(data instanceof RedisString){
                RedisString redisString = (RedisString) data;
                redisString.setSds(new Sds(value.getBytes()));
                return new BulkString(new RedisBytes(value.getBytes()));
            }
        }
        redisCore.put(key, new RedisString(new Sds(value.getBytes())));
        log.info("SET {} {}", key, value);

        return new SimpleString("OK");
    }

}
