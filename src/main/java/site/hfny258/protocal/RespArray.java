package site.hfny258.protocal;

import io.netty.buffer.ByteBuf;
import lombok.Getter;

@Getter
public class RespArray extends Resp{
    private Resp[] content;

    public RespArray(Resp[] content) {
        this.content = content;
    }

    @Override
    public void encoder(Resp resp, ByteBuf byteBuf) {
        byteBuf.writeByte('*');
        byteBuf.writeBytes(Integer.toString(content.length).getBytes());
        for (Resp c : content) {
            c.encoder(c, byteBuf);
        }
        byteBuf.writeBytes(CRLF);
    }

}
