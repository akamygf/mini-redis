package site.hfny258.command;

import lombok.Getter;
import site.hfny258.command.impl.Ping;
import site.hfny258.command.impl.string.Get;
import site.hfny258.command.impl.string.Set;
import site.hfny258.server.core.RedisCore;

import java.util.function.Function;
@Getter
public enum CommandType {
    PING(core -> new Ping()), SET(Set::new), GET(Get::new);

    private final Function<RedisCore, Command> supplier;

    CommandType(Function<RedisCore, Command> supplier) {
        this.supplier = supplier;
    }
}
