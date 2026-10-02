package site.hfny258.command.impl.string;

import lombok.extern.slf4j.Slf4j;
import site.hfny258.command.Command;
import site.hfny258.command.CommandType;
import site.hfny258.datastructure.RedisBytes;
import site.hfny258.datastructure.RedisData;
import site.hfny258.datastructure.RedisString;
import site.hfny258.protocal.BulkString;
import site.hfny258.protocal.Errors;
import site.hfny258.protocal.Resp;
import site.hfny258.server.core.RedisCore;

import static site.hfny258.protocal.BulkString.NULL_BYTE;

@Slf4j
public class Get implements Command {
    private RedisCore redisCore;
    private RedisBytes key;

    public Get(RedisCore redisCore) {
        this.redisCore = redisCore;
    }
    @Override
    public CommandType getType() {
        return CommandType.GET;
    }

    @Override
    public void setContext(Resp[] array) {
        key = ((BulkString) array[1]).getContent();
    }

    @Override
    public Resp handle() {
        try {
            RedisData data  = redisCore.get(key);
            if(data == null){
                return new BulkString(NULL_BYTE);
            }
            if(data instanceof RedisString){
                RedisString redisString = (RedisString) data;
                return new BulkString(redisString.getValue().getBytes());
            }
        }catch (Exception e){
            log.error("handle error", e);
            return new Errors("ERR internal server error");
        }
        return new Errors("ERR unknow error");
    }
}
