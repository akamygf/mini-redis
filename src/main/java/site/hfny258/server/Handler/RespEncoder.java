package site.hfny258.server.Handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import site.hfny258.protocal.Resp;
@Slf4j
public class RespEncoder extends MessageToByteEncoder<Resp> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Resp resp, ByteBuf byteBuf) throws Exception {
        try {
            resp.encoder(resp, byteBuf);
        } catch (Exception e) {
            log.error("encode error", e);
            channelHandlerContext.channel().close();
        }
    }
}
