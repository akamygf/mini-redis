package site.hfny258;

import site.hfny258.server.RedisMiniServer;
import site.hfny258.server.RedisServer;

public class RedisServerLauncher {
    public static void main(String[] args){
        RedisServer redisServer = new RedisMiniServer(6379, "localhost");
        redisServer.start();
    }
}