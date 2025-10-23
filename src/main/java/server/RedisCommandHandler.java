package server;

import storage.Cache;
import types.Array;
import types.BulkString;
import types.RedisObject;
import types.SimpleString;

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
            case "SET" -> {
                if(redisObjects.size() < 3)
                    throw new IOException("SET command should have 2 arguments");
                String key = ((BulkString)redisObjects.get(1)).getValueAsString();
                String value = ((BulkString)redisObjects.get(2)).getValueAsString();
                long ex =-1;
                if(redisObjects.size()>3){
                    String expiryKey = ((BulkString)redisObjects.get(3)).getValueAsString();
                    if(expiryKey.equalsIgnoreCase("EX")
                       // || expiryKey.equalsIgnoreCase("NX")
                    ){
                        String expiry = ((BulkString)redisObjects.get(4)).getValueAsString();
                        if(expiry != null && !expiry.isEmpty()) ex = Long.parseLong(expiry)*1000;
                    } else {
                        String expiry = ((BulkString)redisObjects.get(4)).getValueAsString();
                        if(expiry != null && !expiry.isEmpty()) ex = Long.parseLong(expiry);
                    }
                }
                Cache.setValue(key, value.getBytes(), ex);
                return new SimpleString("OK");
            }
            case "GET" -> {
                if(redisObjects.size() < 2)
                    throw new IOException("GET command should have at least 1 argument");
                String key = ((BulkString)redisObjects.get(1)).getValueAsString();
                byte[] value = Cache.getValue(key);
                return new BulkString(value);
            }
            default -> throw new IOException("Unsupported command");
        }
    }
}
