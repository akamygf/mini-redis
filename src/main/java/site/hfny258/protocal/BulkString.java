package site.hfny258.protocal;

import io.netty.buffer.ByteBuf;
import lombok.Getter;

@Getter
public class BulkString extends Resp{
    private static final byte[] NULL_BYTE = "-1\r\n".getBytes();
    private static final byte[] EMPY_BYTE = "0\r\n\r\n".getBytes();
    private byte[] content;

    public BulkString(byte[] content) {
        this.content = content;
    }
    @Override
    public void encoder(Resp resp, ByteBuf byteBuf) {
        byteBuf.writeByte('$');
        if(content == null){
            byteBuf.writeBytes(NULL_BYTE);
        }
        else {
            int length = content.length;

            if(length == 0){
                byteBuf.writeBytes(EMPY_BYTE);
            }
            else {
                byteBuf.writeBytes(String.valueOf(length).getBytes());
                byteBuf.writeBytes(content);
                byteBuf.writeBytes(CRLF);
            }
        }
    }

}
