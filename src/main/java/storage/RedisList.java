package storage;

import java.util.LinkedList;

public class RedisList implements RedisValue{
    private final Long expiresAt;
    private final LinkedList<byte[]> list ;

    public RedisList(){
        this(null);
    }


    public RedisList(Long expiresAt) {
        this.list = new LinkedList<>();
        this.expiresAt = expiresAt;
    }

    public int rpush(byte[] data){
        list.addLast(data);
        return list.size();
    }

    public int rpush(byte[] ...values){

        for (byte[] value: values){
            list.addLast(value);
        }
        return list.size();


    }


    @Override
    public RedisValueType getType() {
        return RedisValueType.LIST;
    }

    @Override
    public boolean isExpired() {
        return expiresAt !=null && System.currentTimeMillis() > expiresAt;
    }
}
