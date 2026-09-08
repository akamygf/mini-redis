package site.hfny258.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;
import site.hfny258.server.Handler.RespCommandHandler;
import site.hfny258.server.Handler.RespDecoder;
import site.hfny258.server.Handler.RespEncoder;
import site.hfny258.server.Handler.StringHandler;
import site.hfny258.server.core.RedisCore;
import site.hfny258.server.core.RedisCoreImpl;

@Slf4j
public class RedisMiniServer implements RedisServer{
    private  String host;
    private  int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workGroup;
    private Channel severChannel;
    private RedisCore redisCore;

    public RedisMiniServer(int port, String host){
        this.port = port;
        this.host = host;
        this.bossGroup = new NioEventLoopGroup(1);
        this.workGroup = new NioEventLoopGroup(4);
        this.redisCore = new RedisCoreImpl();

    }


    @Override
    public void start(){
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new RespDecoder());
                        pipeline.addLast(new RespCommandHandler(redisCore));
                        pipeline.addLast(new RespEncoder());
                    }
                });
        try {
            severChannel = serverBootstrap.bind(host, port).sync().channel();
            log.info("Redis server started at{}:{}", host, port);
        }
        catch (InterruptedException e){
            log.error("Redis server stop error", e);
            stop();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop(){
        try {
            if(severChannel != null){
                severChannel.close().sync();
            }
            if(workGroup != null){
                workGroup.shutdownGracefully().sync();
            }
            if(bossGroup != null){
                bossGroup.shutdownGracefully().sync();
            }
        }catch (InterruptedException e){
            log.error("Redis server stop error", e);
            Thread.currentThread().interrupt();
        }
    }

}
