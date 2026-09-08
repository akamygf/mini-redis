package site.hfny258.server.Handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import site.hfny258.protocal.Resp;

import java.util.List;
@Slf4j
public class RespDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        try {
            if(byteBuf.readableBytes() > 0){
                byteBuf.markReaderIndex();
            }

            if(byteBuf.readableBytes() < 4){
                return;
            }

            try {
                Resp resp = Resp.decode(byteBuf);
                if(resp != null){
                    log.info("decode: {}", resp);
                    list.add(resp);
                }
            }catch (Exception e){
                log.error("decode error", e);
                byteBuf.resetReaderIndex();
                return;
            }
        }catch (Exception e){
            log.error("decode error", e);
        }
    }
}
