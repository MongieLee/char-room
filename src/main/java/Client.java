import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws IOException {
        System.out.println("请输入你的网名（如孤独的狼🐺）");
        Scanner scanner = new Scanner(System.in);
        String name = scanner.nextLine();
        Socket socket = new Socket("127.0.0.1", 8080);


        Util.writeMessage(socket, name);

        new Thread(() -> readFromServer(socket)).start();
        while (true) {
            System.out.println("请输入你要发送的聊天信息");
            System.out.println("hello:1 代表给1发送消息hello");
            System.out.println("id为0代表给所有在线用户发送消息");
            String line = scanner.nextLine();
            // 分割用户信息
            if (!line.contains(":")) {
                System.out.println("消息格式不正确");
            } else {
                int colonIndex = line.indexOf(":");
                int id = Integer.parseInt(line.substring(colonIndex + 1));
                String msg = line.substring(0, colonIndex);
                String json = JSON.toJSONString(new Message(id, msg));
                Util.writeMessage(socket, json);
            }
        }
    }

    private static void readFromServer(Socket socket) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = null;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
