package site.hfny258.datastructure;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class RedisBytes {
    private byte[] bytes;
    public static final Charset charset = Charset.forName("UTF-8");

    public RedisBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RedisBytes that = (RedisBytes) o;
        return Arrays.equals(bytes, that.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getString() {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
