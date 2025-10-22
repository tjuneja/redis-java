import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Main {

  ServerSocketChannel serverChannel;
  Selector selector;
  ByteBuffer readBuffer;

  public  void start(int port) throws IOException {
    setupServer(port);
  }

  private void setupServer(int port) throws IOException {
      serverChannel = ServerSocketChannel.open();
      selector = Selector.open();
      serverChannel.configureBlocking(false);
      serverChannel.bind(new InetSocketAddress(port));
      serverChannel.register(selector, SelectionKey.OP_ACCEPT);
      System.out.println("Server started on port : "+ port);
      eventLoop();
  }

    private void eventLoop() throws IOException {
      while (true){
          selector.select();

          Set<SelectionKey> selectedKeys = selector.selectedKeys();
          Iterator<SelectionKey> iterator = selectedKeys.iterator();

          while (iterator.hasNext()){
              SelectionKey key = iterator.next();
              iterator.remove();;

              if(!key.isValid()) continue;

              try {
                  if(key.isAcceptable()){
                      handleAccept(key);
                  } else if (key.isReadable()) {
                      handleRead(key);
                  } else if (key.isWritable()) {
                      handleWrite(key);
                  }
              }catch (IOException e){
                  System.out.println();
                  closeChannel(key);
              }
          }
      }


    }

    private void closeChannel(SelectionKey key) {
      try {
          key.channel().close();
          key.cancel();

      }catch (IOException e){
          e.printStackTrace();
      }
    }

    private void handleWrite(SelectionKey key) {

    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        readBuffer.clear();

        int read = clientChannel.read(readBuffer);

        if(read == -1){
            System.out.println("Client disconnected : "+clientChannel.getRemoteAddress());
            closeChannel(key);
            return;
        }
        if (read > 0){
            readBuffer.flip();
            byte[] data = new byte[readBuffer.remaining()];

            readBuffer.get(data);
            String str = new String(data);
            System.out.println("Data received : "+ str);

            //Echo back
            String response = "+PONG\\r\\n";
            ByteBuffer writeBuffer = ByteBuffer.wrap(response.getBytes());
            clientChannel.write(writeBuffer);
         }

    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();

        if(clientChannel != null){
            System.out.println("Accepted connection from : "+ clientChannel.getRemoteAddress());
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ);
        }

    }

    public static void main(String[] args){
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        int port = 6379;
        try {
          serverSocket = new ServerSocket(port);
          // Since the tester restarts your program quite often, setting SO_REUSEADDR
          // ensures that we don't run into 'Address already in use' errors
          serverSocket.setReuseAddress(true);
          // Wait for connection from client.
          clientSocket = serverSocket.accept();
        } catch (IOException e) {
          System.out.println("IOException: " + e.getMessage());
        } finally {
          try {
            if (clientSocket != null) {
              clientSocket.close();
            }
          } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
          }
        }
  }
}
