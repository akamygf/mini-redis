package site.hfny258.protocal;

import io.netty.buffer.ByteBuf;

public class Errors extends Resp{
    private final String content;

    public Errors(String content) {
        this.content = content;
    }
    @Override
    public void encoder(Resp resp, ByteBuf byteBuf) {
        byteBuf.writeByte('-');
        byteBuf.writeBytes(((Errors)resp).content.getBytes());
        byteBuf.writeBytes(CRLF);
    }

}
