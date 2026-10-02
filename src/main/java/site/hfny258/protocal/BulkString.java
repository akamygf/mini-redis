package site.hfny258.protocal;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import site.hfny258.datastructure.RedisBytes;

@Getter
public class BulkString extends Resp{
    public static final byte[] NULL_BYTE = "-1\r\n".getBytes();
    public static final byte[] EMPY_BYTE = "0\r\n\r\n".getBytes();
    private RedisBytes content;

    public BulkString(RedisBytes content) {
        this.content = content;
    }
    public BulkString(byte[] content) {
        this.content = new RedisBytes(content);
    }
    @Override
    public void encoder(Resp resp, ByteBuf byteBuf) {
        byteBuf.writeByte('$');
        if(content == null){
            byteBuf.writeBytes(NULL_BYTE);
        }
        else {
            int length = content.getBytes().length;

            if(length == 0){
                byteBuf.writeBytes(EMPY_BYTE);
            }
            else {
                byteBuf.writeBytes(String.valueOf(length).getBytes());
                byteBuf.writeBytes(CRLF);
                byteBuf.writeBytes(content.getBytes());
                byteBuf.writeBytes(CRLF);
            }
        }
    }

}
