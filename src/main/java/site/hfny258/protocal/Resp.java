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
    public static Resp decoder(ByteBuf buffer){
        //判断是不是完整的命令
        if(buffer.readableBytes() < 0){
            throw new RuntimeException("没有完整的命令");
        }
        //拿到符号解析
        char c = (char)buffer.readByte();
        switch (c){
            case "+":
                return new SimpleString(getString(buffer));
            case "-":
                return new Errors(getString(buffer));
            case ":":
                return new RespInteger(getNumber(buffer));
            case "$":
                return null;
            case "*":
                return null;
        }
        //解析

        return null;
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
