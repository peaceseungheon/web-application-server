package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
    private static final String DEFAULT_PATH = "/index.html";

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            byte[] body = null;
            String line;
            do {
                line = br.readLine();
                if(line == null){
                    throw new RuntimeException("올바른 HTTP 요청이 아닙니다.");
                }
                String[] tokens = line.split(" ");
                if(this.isHttpMethodLine(tokens)){
                    body = getResource(tokens[1]);
                }
            }
            while(!line.isEmpty());
            DataOutputStream dos = new DataOutputStream(out);
            if(body != null){
                response200Header(dos, body.length);
                responseBody(dos, body);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private byte[] getResource(String uri) throws IOException {
        File file;
        if(uri.equals("/")){
            file = new File("./webapp" + DEFAULT_PATH);
        }else {
            file = new File("./webapp" + uri);
        }
        try {
            return Files.readAllBytes(file.toPath());
        }catch (IOException e){
            throw new RuntimeException("파일을 읽지 못했습니다.");
        }
    }

    private boolean isHttpMethodLine(String[] tokens){
        if(tokens.length == 0){
            return false;
        }
        String method = tokens[0];

        String[] methods = new String[]{"GET", "POST", "PUT", "DELETE"};

        boolean isHttpMethodLine = false;
        for(String m : methods){
            if(method.equals(m)){
                isHttpMethodLine = true;
                break;
            }
        }
        return isHttpMethodLine;
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
