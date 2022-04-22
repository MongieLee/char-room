import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Server {
    private AtomicInteger COUNTER = new AtomicInteger(0);
    private final ServerSocket serverSocket;
    // 存放客户端信息
    private final Map<Integer, ClientConnection> clientMap = new ConcurrentHashMap<>();

    // TCP链接的端口号，0~65535
    public Server(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    public void start() throws IOException {
        // 需要一致监听，所以死循环
        while (true) {
            // 有客户端链接后就会返回，否则会一直阻塞
            // 返回一个Socket，可以收发数据
            Socket socket = serverSocket.accept();
            // accept.getInputStream().read();
            // 读数据的时候可能花很长时间，此时其他的客户端无法链接上，只有在运行accept方法时，才能被链接
            // 所以需要和socket相关的放到另一个线程运行
            new ClientConnection(COUNTER.incrementAndGet(), this, socket).start();
        }
    }

    public static void main(String[] args) throws IOException {
        new Server(8080).start();
    }

    // 注册客户端信息
    public void registerClient(ClientConnection clientConnection) {
        clientMap.put(clientConnection.getClientId(), clientConnection);
        this.clientOnline(clientConnection);
    }

    // 客户端会调用，由服务端发送信息
    public void sendMessage(ClientConnection source, Message msg) {
        String message = msg.getMessage();
        if (msg.getId() == 0) {
            clientMap.values().forEach(client -> dispatchMessage(client, source.getClientName(), "所有人", message));
        } else {
            int targetUser = msg.getId();
            ClientConnection target = clientMap.get(targetUser);
            if (target == null) {
                System.err.println("用户" + targetUser + "不存在");
            } else {
                dispatchMessage(target, source.getClientName(), "你", message);
            }
        }
    }

    // 客户端上线
    public void clientOnline(ClientConnection clientConnection) {
        clientMap.values().forEach(client -> dispatchMessage(client, "系统", "所有人", clientConnection.getClientName() + "上线了" + getAllClientInfo()));
    }

    // 客户端下线
    public void clientOffLine(ClientConnection clientConnection) {
        clientMap.remove(clientConnection.getClientId());
        clientMap.values().forEach(client -> dispatchMessage(client, "系统", "所有人", clientConnection.getClientName() + "下线了" + getAllClientInfo()));

    }

    public void dispatchMessage(ClientConnection client, String source, String target, String msg) {
        client.sendMessage(source + "对" + target + "说" + msg);
    }

    // 获取所有用户信息
    private String getAllClientInfo() {
        return clientMap.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue().getClientName())
                .collect(Collectors.joining(","));
    }
}

class ClientConnection extends Thread {
    private final Socket socket;
    private int clientId;
    private String clientName;
    private Server server;

    ClientConnection(int clientId, Server server, Socket socket) {
        this.clientId = clientId;
        this.server = server;
        this.socket = socket;
    }

    public int getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    @Override
    public void run() {
        // 读消息也是个死循环
        // 按约定每次只能发一行，回车就发送
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = null;
            while (Objects.nonNull(line = reader.readLine())) {
                if (Objects.isNull(clientName)) {
                    // 第一次链接，需要注册
                    clientName = line;
                    server.registerClient(this);
                } else {
                    Message msg = JSON.parseObject(line, Message.class);
                    server.sendMessage(this, msg);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 当客户端断开时，reader会抛出错误就会走到finally块
            // 通知服务端这人下线了
            server.clientOffLine(this);
        }
    }

    public void sendMessage(String message) {
        OutputStream outputStream = null;
        try {
            outputStream = socket.getOutputStream();
            outputStream.write(message.getBytes());
            outputStream.write('\n');
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

class Message {
    public Message() {
    }

    public Message(int id, String message) {
        this.id = id;
        this.message = message;
    }

    private int id;
    private String message;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
