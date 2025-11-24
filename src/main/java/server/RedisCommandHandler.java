package server;

import storage.RedisList;
import storage.RedisStore;
import types.*;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RedisCommandHandler {
    public static RedisObject handle(RedisObject parsedCommand, SocketChannel channel) throws IOException {
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
                return handSetOperation(redisObjects);
            }
            case "GET" -> {
                return handleGetOperation(redisObjects);
            }
            case "RPUSH" ->{
                RedisObject object = handleRpush(redisObjects);
                String key = ((BulkString)redisObjects.get(1)).getValueAsString();
                RedisStore.notifyListModified(key);
                return object;
            }
            case "LRANGE" ->{
                return handleLrange(redisObjects);
            }
            case "LPUSH" -> {
                RedisObject object = handleLpush(redisObjects);
                String key = ((BulkString)redisObjects.get(1)).getValueAsString();
                RedisStore.notifyListModified(key);
                return object;

            }
            case "LLEN" ->{
                return listLength(redisObjects);
            }
            case "LPOP" ->{
                return handleLpop(redisObjects);
            }

            case "BLPOP" -> {
                return handleBlpop(redisObjects, channel);
            }
            default -> throw new IOException("Unsupported command");
        }
    }

    private static RedisObject handleBlpop(List<RedisObject> redisObjects, SocketChannel channel) throws IOException {
        if(redisObjects.size() < 3){
            throw new IOException("BLPOP should have least 3 arguments");
        }
        int timeout;

        try {
            String timeoutValue = ((BulkString)redisObjects.getLast()).getValueAsString();
            timeout = Integer.parseInt(timeoutValue);

            if(timeout < 0){
                throw new IOException("Timeout value should be >= 0");
            }
        }catch (NumberFormatException e){
            throw new IOException("Timeout value invalid");
        }

        List<String> keys = new ArrayList<>();

        for (int i = 1; i <redisObjects.size()-1 ; i++) {
            keys.add(((BulkString)redisObjects.get(i)).getValueAsString());
        }

        for (String key: keys){
            byte[] value = RedisStore.lpopValue(key);
            if(value != null){
                List<RedisObject> response = Arrays.asList(
                        new BulkString(key.getBytes()),
                        new BulkString(value)
                );
                return new Array(response);
            }
        }

        BlockingManager.getInstance().blockClient(channel, keys, timeout);
        return new Array(null);
    }

    private static RedisObject handleLpop(List<RedisObject> redisObjects) throws IOException {
        if (redisObjects.size() < 2)
            throw new IOException("LPOP should have least 2 arguments");
        String key  = ((BulkString)redisObjects.get(1)).getValueAsString();
        RedisList redisList = RedisStore.getOrCreateList(key);
        if(redisObjects.size() == 3){
            String startIdx = ((BulkString)redisObjects.get(2)).getValueAsString();
            int idx = Integer.parseInt(startIdx);
            List<byte[]> popped = redisList.lpop(idx);


            List<RedisObject> res = new ArrayList<>();
            for (byte[] elem : popped){
                res.add(new BulkString(elem));
            }
            return new Array(res);
        }else{
            byte[] popped = redisList.lpop();
            return new BulkString(popped);
        }
    }

    private static RedisObject listLength(List<RedisObject> redisObjects) throws IOException {
        if (redisObjects.size() < 2)
            throw new IOException("LLEN should have atleast 2 argurments");
        String key  = ((BulkString)redisObjects.get(1)).getValueAsString();
        RedisList redisList = RedisStore.getOrCreateList(key);
        return new RedisInteger(redisList.size());

    }

    private static RedisObject handleLrange(List<RedisObject> redisObjects) throws IOException {
        if (redisObjects.size() < 3)
            throw new IOException("LRANGE should have atleast 4 argurments");
        String key  = ((BulkString)redisObjects.get(1)).getValueAsString();
        String start = ((BulkString)redisObjects.get(2)).getValueAsString();
        String end = ((BulkString)redisObjects.get(3)).getValueAsString();
        return RedisStore.getRange(key, start, end);
    }

    private static RedisObject handleLpush(List<RedisObject> redisObjects) throws IOException {
        if(redisObjects.size() <3)
            throw new IOException("LPUSH should have atleast 3 argurments");

        String key = ((BulkString)redisObjects.get(1)).getValueAsString();
        byte[][] values = new byte[redisObjects.size() -2][];

        for(int i = 2; i < redisObjects.size();i++){
            System.out.println("Pushing element : "+  ((BulkString)redisObjects.get(i)).getValueAsString());
            values[i-2] = ((BulkString)redisObjects.get(i)).getValue();
        }
        RedisList redisList = RedisStore.getOrCreateList(key);
        int length = redisList.lpush(values);
        return new RedisInteger(length);

    }

    private static RedisObject handleRpush(List<RedisObject> redisObjects) throws IOException {
        if(redisObjects.size() <3)
            throw new IOException("RPUSH should have atleast 3 argurments");

        String key = ((BulkString)redisObjects.get(1)).getValueAsString();
        byte[][] values = new byte[redisObjects.size() -2][];

        for(int i = 2; i < redisObjects.size();i++){
            System.out.println("Pushing element : "+  ((BulkString)redisObjects.get(i)).getValueAsString());
            values[i-2] = ((BulkString)redisObjects.get(i)).getValue();
        }
        RedisList redisList = RedisStore.getOrCreateList(key);
        int length = redisList.rpush(values);
        return new RedisInteger(length);
    }

    private static BulkString handleGetOperation(List<RedisObject> redisObjects) throws IOException {
        if(redisObjects.size() < 2)
            throw new IOException("GET command should have at least 1 argument");
        String key = ((BulkString) redisObjects.get(1)).getValueAsString();
        byte[] value = RedisStore.getValue(key);
        return new BulkString(value);
    }

    private static SimpleString handSetOperation(List<RedisObject> redisObjects) throws IOException {
        if(redisObjects.size() < 3)
            throw new IOException("SET command should have 2 arguments");
        String key = ((BulkString) redisObjects.get(1)).getValueAsString();
        String value = ((BulkString) redisObjects.get(2)).getValueAsString();
        long ex =-1;
        if(redisObjects.size()>3){
            String expiryKey = ((BulkString) redisObjects.get(3)).getValueAsString();
            if(expiryKey.equalsIgnoreCase("EX")
               // || expiryKey.equalsIgnoreCase("NX")
            ){
                String expiry = ((BulkString) redisObjects.get(4)).getValueAsString();
                if(expiry != null && !expiry.isEmpty()) ex = Long.parseLong(expiry)*1000;
            } else {
                String expiry = ((BulkString) redisObjects.get(4)).getValueAsString();
                if(expiry != null && !expiry.isEmpty()) ex = Long.parseLong(expiry);
            }
        }
        RedisStore.setValue(key, value.getBytes(), ex);
        return new SimpleString("OK");
    }
}
