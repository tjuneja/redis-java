package server;

import java.nio.channels.SocketChannel;
import java.util.List;

public class BlockedClient {
    private final SocketChannel channel;
    private final List<String> keys;
    private final long timeoutAt;
    private final long blockedAt;
    private final double timeout;


    public BlockedClient(SocketChannel channel, List<String> keys, double timeout){
        this.channel = channel;
        this.keys = keys;
        this.blockedAt = System.currentTimeMillis();

        if(timeout == 0){
            this.timeoutAt = Long.MAX_VALUE;
        }else{
            this.timeoutAt = (long) (System.currentTimeMillis()+(timeout*1000L));
        }
        this.timeout = timeout;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public List<String> getKeys() {
        return keys;
    }

    public boolean isTimedOut() {
        return System.currentTimeMillis() > timeoutAt;
    }

    public long getBlockedAt() {
        return blockedAt;
    }

    public long getTimeoutAt() {
        return timeoutAt;
    }

}
