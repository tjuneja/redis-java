package storage;

import server.BlockingManager;
import types.Array;
import types.BulkString;
import types.RedisObject;

import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RedisStore {

    private static final Map<String, RedisValue> map = new ConcurrentHashMap<>();
    private static final Map<String, Long> expirationMap = new ConcurrentHashMap<>();
    private final static ScheduledExecutorService schedulerService = Executors.newScheduledThreadPool(1);

    static {
        schedulerService.scheduleAtFixedRate(RedisStore::expireKeys, 100, 100, TimeUnit.MILLISECONDS);
    }


    /**
     * String operations
     *
     */

    public static void set(String key, byte[] value){
        setValue(key, value, -1);
    }

    public static void setValue(String key, byte[] value, long expiry){
        map.put(key, new RedisString(value));

        if(expiry > 0) expirationMap.put(key, System.currentTimeMillis() + expiry);
        else expirationMap.remove(key);
    }

    public static byte[] getValue(String key){
        System.out.println("Fetching key : "+key);
        if(!map.containsKey(key)) return null;
        if(isExpired(key)){
            System.out.println("Expired key : "+key);
            delete(key);
            return null;
        }
        return ((RedisString)map.get(key)).getValue();
    }

    private static void delete(String key){
        map.remove(key);
        expirationMap.remove(key);
    }

    /**
     * List operations
     */

    public static RedisList getOrCreateList(String key){
        RedisValue redisValue = map.get(key);

        if(redisValue == null){
            RedisList list = new RedisList();
            map.put(key, list);
            return list;
        }

        if(redisValue.isExpired()){
            map.remove(key);
            RedisList list = new RedisList();
            map.put(key, list);
            return list;
        }

        return (RedisList) redisValue;

    }
    public static RedisList getList(String key) {
        RedisValue value = map.get(key);

        if (value == null) return null;

        if (value.isExpired()) {
            delete(key);
            return null;
        }

        if (value.getType() != RedisValue.RedisValueType.LIST) {
            throw new IllegalArgumentException("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        return (RedisList) value;
    }


    public static RedisValue.RedisValueType getType (String key){
        RedisValue value = map.get(key);
        if(value != null)
            return value.getType();
        return null;
    }

    public static byte[] lpopValue(String key){
        RedisList list = getList(key);
        if(list == null) return null;
        return list.lpop();
    }



    private static void expireKeys(){
        Set<String> deletionSet = new HashSet<>();
        for (String entry: expirationMap.keySet()){
            if(expirationMap.get(entry) <=System.currentTimeMillis()){
                deletionSet.add(entry);
            }
        }
        deletionSet.forEach(RedisStore::delete);
    }

    private static boolean isExpired(String key) {
        Long expiryTime = expirationMap.get(key);
        System.out.println("Current time");
        if(expiryTime == null) return false;
        return System.currentTimeMillis() > expiryTime;
    }

    public static Array getRange(String key, String start, String end) {
        RedisList redisList = getList(key);
        System.out.println("Got redis list : "+ redisList);
        if(redisList == null)
            return new Array();

        int startIdx,endIdx;
        try {
            startIdx = Integer.parseInt(start);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Start idx not a valid number");
        }
        try {
            endIdx = Integer.parseInt(end);
        } catch (NumberFormatException e) {
            throw new RuntimeException("End index not a valid number");
        }

        List<byte[]> range = redisList.range(startIdx, endIdx);
        List<RedisObject> objects = new ArrayList<>();
        for (byte[] val:  range){
            objects.add(new BulkString(val));
        }
        return new Array(objects);
    }

    public static void notifyListModified(String key) throws IOException {
        BlockingManager.getInstance().notifyWhenKeyAvailable(key);
    }


}
