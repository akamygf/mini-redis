package site.hfny258.command;

import site.hfny258.protocal.Resp;

public interface Command {
    CommandType getType();
    void setContext(Resp[] array);
    Resp handle();
}
