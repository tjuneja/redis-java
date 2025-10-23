package storage;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Cache {

    private static final Map<String, byte[]> map = new ConcurrentHashMap<>();
    private static final Map<String, Long> expirationMap = new ConcurrentHashMap<>();
    private final static ScheduledExecutorService schedulerService = Executors.newScheduledThreadPool(1);

    static {
        schedulerService.scheduleAtFixedRate(Cache::expireKeys, 100, 100, TimeUnit.MILLISECONDS);
    }

    public static void setValue(String key, byte[] value, long expiry){
        map.put(key, value);

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
        return map.get(key);
    }

    private static void delete(String key){
        map.remove(key);
        expirationMap.remove(key);
    }

    private static void expireKeys(){
        Set<String> deletionSet = new HashSet<>();
        for (String entry: expirationMap.keySet()){
            if(expirationMap.get(entry) <=System.currentTimeMillis()){
                deletionSet.add(entry);
            }
        }
        deletionSet.forEach(Cache::delete);
    }

    private static boolean isExpired(String key) {
        Long expiryTime = expirationMap.get(key);
        System.out.println("Current time");
        if(expiryTime == null) return false;
        return System.currentTimeMillis() > expiryTime;
    }
}
