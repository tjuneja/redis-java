package server;

import java.math.BigInteger;

public class RedisInteger implements RedisObject {
    private final BigInteger value;
    public RedisInteger(int i) {
        this.value = BigInteger.valueOf(i);
    }

    public BigInteger getValue(){
        return this.value;
    }
    @Override
    public String toString() {
        return "Integer{" + value +  '}';
    }


}
