package storage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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

    public List<byte[]> range(int startIdx, int endIdx) {
        int len = list.size();
        if (startIdx <0) startIdx += len;
        if(endIdx < 0) endIdx += len;
        if(startIdx > endIdx) return List.of();

        startIdx = Math.min(startIdx, len-1);
        endIdx = Math.min(endIdx, len-1);
        List<byte[]> result = new ArrayList<>(endIdx-startIdx+1);

        if(startIdx > endIdx) return List.of();
        int i=0;
        for(byte[] value : list){
            if(i>=startIdx && i<=endIdx){
                result.add(value);
            }
            if(i>endIdx) break;
            i++;
        }

        return result;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("RedisList{  ");
        for(int i = 0; i < list.size(); i++){
            if(i > 0) sb.append(",");
            sb.append(new String(list.get(i), StandardCharsets.UTF_8));
        }
        sb.append("}");
        return sb.toString();
    }

    public int lpush(byte[] ...values) {
        for (byte[] value: values){
            list.addFirst(value);
        }
        return list.size();
    }

    public int size(){
        return list.size();
    }

    public byte[] lpop(){
        return list.pollFirst();
    }
}
