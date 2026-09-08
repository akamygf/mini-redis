package site.hfny258.protocal;

import io.netty.buffer.ByteBuf;

import static javax.swing.UIManager.getString;

public abstract class Resp {
    public static final byte[] CRLF = "\r\n".getBytes();
    //Simple String
    //Errors
    //Redis Integer
    //Bulk String
    //Resp Array
    public static Resp decode(ByteBuf buffer){
        //判断是不是完整的命令
        if(buffer.readableBytes() < 0){
            throw new RuntimeException("没有完整的命令");
        }
        //拿到符号解析
        char c = (char)buffer.readByte();
        switch (c){
            case '+':
                return new SimpleString(getString(buffer));
            case '-':
                return new Errors(getString(buffer));
            case ':':
                return new RespInteger(getNumber(buffer));
            case '$':
                int length = getNumber(buffer);
                if(buffer.readableBytes()<length+2){
                    throw new IllegalStateException("没有完整的命令");
                }

                byte[] content;
                if(length == -1){
                    return null;
                }else {
                    content = new byte[length];
                    buffer.readBytes(content);
                }
                if(buffer.readByte() != '\r' || buffer.readByte() != '\n'){
                    throw new IllegalStateException("没有完整的命令");
                }

                return new BulkString(content);
            case '*':
                int number = getNumber(buffer);
                Resp [] array = new Resp[number];
                for(int i = 0; i < number; i++){
                    array[i] = decode(buffer);
                }
                return new RespArray(array);
            default:
                throw new RuntimeException("未知的命令");
        }
        //解析
    }

    public abstract void encoder(Resp resp, ByteBuf byteBuf);

    static String getString(ByteBuf buffer){
        char c;
        StringBuilder result = new StringBuilder();
        while ((c = (char)buffer.readByte()) != '\n' && buffer.readableBytes()>0){
            result.append(c);
        }
        if(buffer.readableBytes()<=0 || buffer.readableBytes() != '\n'){
            throw new IllegalStateException("没有找到换行符");
        }
        return result.toString();
    }

    static int getNumber(ByteBuf buffer){
        char c;
        c = (char)buffer.readByte();
        boolean positive = true;
        int value = 0;
        if(c == '-'){
            positive = false;
        }else {
            value = c - '0';
        }while((c = (char)buffer.readByte()) != '\n' && buffer.readableBytes()>0){
            value = value * 10 + (c - '0');
        }
        if(buffer.readableBytes()<=0 || buffer.readableBytes() != '\n'){
            throw new IllegalStateException("没有找到换行符");
        }
        return positive ? value : -value;
    }
}
