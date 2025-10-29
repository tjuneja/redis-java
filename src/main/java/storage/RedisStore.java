package storage;

import java.awt.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
}
