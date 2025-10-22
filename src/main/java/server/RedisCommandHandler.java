package server;

import java.io.IOException;
import java.util.List;

public class RedisCommandHandler {
    public static RedisObject handle(RedisObject parsedCommand) throws IOException {
        if(!(parsedCommand instanceof Array commands)) throw new IOException("Command should be an array");

        List<RedisObject> redisObjects = commands.getElements();

        if(redisObjects == null || redisObjects.isEmpty()) throw new IOException("Empty command");

        if(!(redisObjects.getFirst() instanceof BulkString commandName)) throw new IOException("First input should be a command");

        String command = commandName.getValueAsString().toUpperCase();

        switch (command){
            case "ECHO" -> {
                if(redisObjects.size() <2)
                    throw new IOException("ECHO requires an argument");
                return redisObjects.get(1);
            }

            case "PING" ->{
                return new SimpleString("PONG");
            }
            default -> throw new IOException("Unsupported command");
        }
    }
}
