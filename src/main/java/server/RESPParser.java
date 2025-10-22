package server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RESPParser {


    public RedisObject parse(String input) throws IOException {
        return parse(input.getBytes(StandardCharsets.UTF_8));
    }

    private RedisObject parse(byte[] bytes) throws IOException {
        try(ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)){
            return parse(inputStream);
        }
    }

    private RedisObject parse(InputStream inputStream) throws IOException {
        int firstByte = inputStream.read();
        if(firstByte == -1){
            throw new IOException("Unexpected end of stream");
        }

        switch (firstByte){
            case '+' -> {
                return parseSimpleString(inputStream);
            }
            case '-' -> {
                return parseError(inputStream);
            }
            case ':' -> {
                return parseInteger(inputStream);
            }
            case '$' -> {
                return parseBulkString(inputStream);
            }
            case '*' -> {
                return parseArray(inputStream);
            }
            default -> throw new IOException("Unknown type");
        }
    }

    private RedisObject parseArray(InputStream input) throws IOException {
        String length = readLine(input);
        int len;

        try{
            len = java.lang.Integer.parseInt(length);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid array length: " + length, e);
        }

        if(len == -1){
            return  new Array(null);
        }

        List<RedisObject> objects = new ArrayList<>(len);

        for(int i = 0; i< len;i++){
            objects.add(parse(input));
        }

        return new Array(objects);
    }

    private RedisObject parseBulkString(InputStream inputStream) throws IOException {
        String length = readLine(inputStream);
        int len = 0;
        try {
            len = Integer.parseInt(length);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid integer");
        }
        if (len == -1) {
            return new BulkString(null);
        }

        if (len < 0) throw new IOException("Invalid bulk string length: " + length);

        byte[] b = new byte[len];
        int bytesRead =0;

        while( bytesRead < len ){
            int count = inputStream.read(b,bytesRead, len - bytesRead);
            if (count == -1) throw new IOException("Unexpected end of stream while reading bulk strings");
            bytesRead+=count;
        }

        int cr = inputStream.read();
        int lf =inputStream.read();
//        if(cr !='\r' || lf !='\n')
//            throw new IOException("Expected CRLF after bulk string, got: " + (char) cr + (char) lf);
        return new BulkString(b);


    }

    private RedisObject parseInteger(InputStream inputStream) throws IOException {
        String value = readLine(inputStream);
        try {
            return new RedisInteger(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }

    private RedisObject parseError(InputStream inputStream) throws IOException {
        return new Error(readLine(inputStream));
    }

    private RedisObject parseSimpleString(InputStream inputStream) throws IOException {
        return new SimpleString(readLine(inputStream));
    }

    private String readLine(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder() ;

        int b;

        while ((b = inputStream.read()) != -1){

            if(b =='\r'){
                int next = inputStream.read();

                if(next != '\n'){
                    throw new IOException("Malformed input");
                }
                break;
            }
            sb.append((char)b);

        }

        return sb.toString();

    }

    public static void main(String[] args) throws IOException {
        String s1 = "+OK\r\n";
        String s2 = ":1000\r\n";
        String s3 = "$5\r\nhello\r\n";
        String s4 = "*2\r\n$5\r\nhello\r\n:42\r\n";

        RESPParser parser = new RESPParser();

        System.out.println(parser.parse(s1));
        System.out.println(parser.parse(s2));
        System.out.println(parser.parse(s3));
        System.out.println(parser.parse(s4));
    }


}
