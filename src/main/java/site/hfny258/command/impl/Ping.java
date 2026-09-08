package site.hfny258.command.impl;

import site.hfny258.command.Command;
import site.hfny258.command.CommandType;
import site.hfny258.protocal.Resp;
import site.hfny258.protocal.SimpleString;

public class Ping implements Command{

    @Override
    public CommandType getType() {
        return CommandType.PING;
    }

    @Override
    public void setContext(Resp[] array) {

    }

    @Override
    public Resp handle() {
        return new SimpleString("PONG");
    }
}
