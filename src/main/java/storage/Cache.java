package storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Cache {

    private static Map<String, byte[]> map = new ConcurrentHashMap<>();


    public static void setValue(String key, byte[] value){
        map.put(key, value);
    }

    public static byte[] getValue(String key){
        return map.get(key);
    }


}
