package server;

public class SimpleString implements RedisObject{
    private final String value;
    public SimpleString(String s){
        this.value = s;
    }

    public String getValue(){
        return this.value;
    }
    @Override
    public String toString() {
        return "SimpleString{" + value +  '}';
    }
}
