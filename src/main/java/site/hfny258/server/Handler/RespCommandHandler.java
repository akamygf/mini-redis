package site.hfny258.server.Handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import site.hfny258.command.Command;
import site.hfny258.command.CommandType;
import site.hfny258.protocal.BulkString;
import site.hfny258.protocal.Errors;
import site.hfny258.protocal.Resp;
import site.hfny258.protocal.RespArray;
import site.hfny258.server.core.RedisCore;
@Slf4j
@Getter
public class RespCommandHandler extends SimpleChannelInboundHandler<Resp> {
    private final RedisCore redisCore;
    public RespCommandHandler(RedisCore redisCore) {
        this.redisCore = redisCore;
    }
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Resp resp) throws Exception {
        if(resp instanceof RespArray){
            RespArray respArray = (RespArray) resp;
            Resp response = processCommand(respArray);

            if (response != null) {
                channelHandlerContext.channel().writeAndFlush(response);
            }else{
                channelHandlerContext.channel().writeAndFlush(new Errors("不支持的命令"));
            }
        }
    }

    private Resp processCommand(RespArray respArray) {
        if(respArray.getContent().length == 0){
            return new Errors("命令不能为空");
        }

        try{
            Resp[] array = respArray.getContent();
            String commandName = new String(((BulkString)array[0]).getContent());
            commandName = commandName.toUpperCase();
            CommandType commandType;

            try {
                commandType = CommandType.valueOf(commandName);
            } catch (Exception e) {
                return new Errors("不支持的命令");
            }

            Command command = commandType.getSupplier().apply(redisCore);
            command.setContext(array);
            Resp result = command.handle();

            return result;
            } catch (Exception e) {
                return new Errors("命令执行异常");
            }
    }
}
