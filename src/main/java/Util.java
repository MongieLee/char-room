import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class Util {
    public static void writeMessage(Socket socket, String msg) {
        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(msg.getBytes());
            outputStream.write('\n');
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
