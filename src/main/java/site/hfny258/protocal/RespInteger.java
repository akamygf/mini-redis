package site.hfny258.protocal;

import io.netty.buffer.ByteBuf;

public class RespInteger extends Resp{
    private int content;

    public RespInteger(int content) {
        this.content = content;
    }
    @Override
    public void encoder(Resp resp, ByteBuf byteBuf) {
        byteBuf.writeByte(':');
        byteBuf.writeBytes(String.valueOf(((RespInteger)resp).content).getBytes());
        byteBuf.writeBytes(CRLF);
    }
}
