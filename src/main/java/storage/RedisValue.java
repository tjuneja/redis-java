package storage;

public interface RedisValue {
    RedisValueType getType();
    boolean isExpired();

    enum RedisValueType {
        STRING,
        LIST,
        SET,
        HASH,
        ZSET,
        STREAM
    }

}
