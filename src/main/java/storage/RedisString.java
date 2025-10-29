package storage;

public class RedisString implements RedisValue{
    private final byte[] value;
    private final Long expiresAt;

    public RedisString(byte[] value){
        this.value = value;
        this.expiresAt = null;
    }
    public RedisString(byte[] value, Long expiresAt) {
        this.value = value;
        this.expiresAt = expiresAt;
    }


    @Override
    public RedisValueType getType() {
        return RedisValueType.STRING;
    }

    @Override
    public boolean isExpired() {
        return expiresAt != null && System.currentTimeMillis() > expiresAt;
    }

    public byte[] getValue() {
        return value;
    }
}
