package server;

import storage.RedisStore;
import types.Array;
import types.BulkString;
import types.RedisObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BlockingManager {

    private static final BlockingManager INSTANCE = new BlockingManager();

    private final Map<String, LinkedList<BlockedClient>> blockedClients= new HashMap<>();

    private final Map<SocketChannel, BlockedClient> channelByClient = new HashMap<>();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    private BlockingManager(){
        executorService.scheduleAtFixedRate(this::checkTimeouts, 100, 100, TimeUnit.MILLISECONDS);
    }

    private void checkTimeouts() {
        List<BlockedClient> timeoutList = new ArrayList<>();

        for(LinkedList<BlockedClient> clients: blockedClients.values()){
            Iterator<BlockedClient> iterator = clients.iterator();
            while (iterator.hasNext()){
                BlockedClient client = iterator.next();
                if(client.isTimedOut()){
                    timeoutList.add(client);
                    iterator.remove();
                }
            }
        }

        for (BlockedClient client: timeoutList){
            try {
                sendTimeoutResponse(client);
                cleanup(client);
            } catch (IOException e) {
                System.err.println("Error sending timeout response: " + e.getMessage());
            }

        }

    }

    private void sendTimeoutResponse(BlockedClient client) throws IOException {
        SocketChannel channel = client.getChannel();
        RedisObject object = new Array(null);
        ByteBuffer buffer = ByteBuffer.wrap(RedisSerializer.serialize(object).getBytes());
        channel.write(buffer);
    }

    public static BlockingManager getInstance(){
        return INSTANCE;
    }

    public void blockClient(SocketChannel channel, List<String> keys, int timeout){
        BlockedClient client = new BlockedClient(channel, keys, timeout);

        for (String key: keys){
            blockedClients.computeIfAbsent(key, k -> new LinkedList<>()).addLast(client);

        }
        channelByClient.put(channel,client);
        System.out.println("Client blocked on keys: " + keys + " with timeout: " + timeout);
    }

    public void notifyWhenKeyAvailable(String key) throws IOException {
        LinkedList<BlockedClient> clients = blockedClients.get(key);
        if(clients == null || clients.isEmpty()){
            return;
        }

        BlockedClient client = clients.pollFirst();
        byte[] value = null;
        for (String k: client.getKeys()){
            value = RedisStore.lpopValue(k);

            if(value != null){
                sendUnblockedResponse(k, value, client);
                cleanup(client);
                return;
            }
        }


        // Race condition
        blockClient(client.getChannel(), client.getKeys(), (int)(client.getTimeoutAt() - System.currentTimeMillis())/1000);
    }

    private void cleanup(BlockedClient client) {
        channelByClient.remove(client.getChannel());

        for(String key: client.getKeys()){
            LinkedList<BlockedClient> clients = blockedClients.get(key);
            if(clients != null){
                clients.remove(client);
                if(clients.isEmpty()){
                    blockedClients.remove(key);
                }
            }
        }
    }

    private void sendUnblockedResponse(String key, byte[] value, BlockedClient client) throws IOException {
        SocketChannel channel = client.getChannel();
        List<RedisObject> redisObjects = Arrays.asList(
                new BulkString(key.getBytes()),
                new BulkString(value)
        );

        String serializedResponse = RedisSerializer.serialize(new Array(redisObjects));
        ByteBuffer response = ByteBuffer.wrap(serializedResponse.getBytes());
        channel.write(response);
    }

}
