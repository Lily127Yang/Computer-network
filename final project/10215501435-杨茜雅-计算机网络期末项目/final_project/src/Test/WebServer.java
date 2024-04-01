package Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;

public class WebServer {
    //定义服务器监听的端口号
    private static final int PORT = 8081;
    private static ServerSocket serverSocket;

    public static void main(String[] args) throws IOException {
        //创建一个服务器套接字 serverSocket 并将其绑定到指定的端口号 PORT 上，以便监听客户端的连接请求。
        serverSocket = new ServerSocket(PORT);
        System.out.println("Web server listening on port " + PORT);
        //可以接受客户端的连接请求了！！


        while (true) {
            // 接受客户端的连接请求
            Socket clientSocket = serverSocket.accept();
            //accept() 方法是一个阻塞方法，直到有客户端连接请求到达并被接收时才会返回，表示建立了客户端与服务器的TCP连接
            // 为每个连接创建一个新的线程来处理请求
            Thread thread = new Thread(new WebServerRunnable(clientSocket));
            thread.start();
        }
    }

    // 内部类WebServerRunnable，实现了 Runnable 接口，用于处理客户端连接的请求
    private static class WebServerRunnable implements Runnable {
        //        创建一个客户端套接字
        private Socket clientSocket;//端口
        private BufferedReader fromClient;//客户端来的
        private OutputStream toClient;//去客户端的

        public WebServerRunnable(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                // 获取客户端输入流和输出流
                fromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                toClient = clientSocket.getOutputStream();
                String requestLine;

                // 读取客户端发送的HTTP请求报文的请求行
                while((requestLine = fromClient.readLine())!=null && !requestLine.isEmpty()){
                    System.out.println(requestLine);
                    //关闭： 1）从客户端读取的数据为 null，即客户端已经关闭连接；2）从客户端读取的数据为空行，即已经读取完了所有的请求头信息。
                    //第一行是请求行
                    String[] tokens = requestLine.split("\\s+");
                    //用split将请求行分割，得到方法，URI还有版本

                    String method = tokens[0];// HTTP请求方法
                    String uri = tokens[1]; // 请求的URI（资源标识符）
                    String version = tokens[2];// HTTP协议版本

                    // 检查请求行是否符合HTTP协议的格式，开始解析请求行了！！
                    if (tokens.length != 3) {
                        // 如果请求行不符合 HTTP 协议的格式，返回 400 错误
                        sendError(toClient, "400 Bad Request", "Invalid request line.");
                        return;
                    }

                    if ("/".equals(uri)) {
                        // 如果请求的是根目录，指定返回的文件路径
                        uri = "C:\\Users\\86138\\Desktop\\final_project\\src\\Test\\index.html";
                    } else if(uri.contains("index.html")){
                        // 如果请求的是 index.html，指定返回的文件路径
                        uri = "C:\\Users\\86138\\Desktop\\final_project\\src\\Test\\index.html";
                    }


                    //shutdown功能实现
//支持关闭服务器的功能，可以在请求 URI 中添加 /shutdown 来关闭服务器
                    //具体实现为调用 ServerSocket 对象的 close() 方法来关闭服务器端口。
                    if ("/shutdown".equals(uri)) {
                        // 如果请求的是关闭服务器命令，返回 503 错误，并关闭服务器
                        System.out.println("Server shutting down...");
                        sendError(toClient, "503 Service Unavailable", "shutdown");
                        serverSocket.close();
                        System.exit(0);//退出程序
                        return;
                    }

                    String filename = uri;
                    File file = new File(filename);

                    //不是shutdown也不是Index
                    if (!file.exists()) {
                        //如果请求的文件不存在，将返回404错误。
                        sendError(toClient, "404 Not Found", "404 Not Found.");
                        return;
                    }
                    //FileInputStream 类和 read() 方法来读取指定文件的二进制数据，然后将其转换成字符串类型的数据进行返回。
                    FileInputStream fis = new FileInputStream(file);
                    //读取文件内容
                    byte[] fileContent = new byte[(int) file.length()];
                    //存在fileContent里面
                    //读完了！
                    fis.read(fileContent);
                    //在读取完成之后，需要显式地调用 fis.close() 方法关闭文件输入流，以释放系统资源和避免资源泄漏等问题
                    fis.close();
                    String content = new String(fileContent).trim();
                    //将文件内容转化为字符串
                    String contentType = Files.probeContentType(file.toPath());
                    //获取文件内容类型（根据文件的拓展名来判断内容类型

                    // 发送HTTP响应报文的头部
                    //头部包含了状态行 xxok!!
                    toClient.write(("HTTP/1.1 200 OK\r\n").getBytes());
                    //指定了返回的类型和字符
                    toClient.write(("Content-Type: " + contentType + "; charset=utf-8\r\n").getBytes());
                    //主体内容的长度
                    toClient.write(("Content-Length: " + content.getBytes().length + "\r\n").getBytes());
                    //表示结束，分隔响应头和响应主体
                    toClient.write(("\r\n").getBytes());

                    // 发送HTTP响应报文的主体内容，将文件内容作为响应主体发送给客户端
                    toClient.write(content.getBytes());
                    toClient.flush();
                    //通过输出流的flush()方法将响应发回客户端
                    System.out.println("send response");
                }
            } catch (IOException e) {
                //打印错误信息
                System.err.println("Error handling request: " + e.getMessage());
            } finally {
                try {
                    // 关闭流和套接字
                    if(fromClient!=null) fromClient.close();
                    if(toClient!=null) toClient.close();
                    if(clientSocket!=null) clientSocket.close();
                } catch (IOException e) {
                    // 处理关闭流和套接字时可能发生的异常
                }
            }
        }

        private void sendError(OutputStream out, String status, String message) throws IOException {
            // 发送HTTP错误响应，根据输出流、状态码、错误消息这些参数来构建错误相应报文，发回客户端
            out.write(("HTTP/1.1 " + status + "\r\n").getBytes());
            out.write(("Content-Type: text/plain; charset=utf-8\r\n").getBytes());
            out.write(("Content-Length: " + message.getBytes().length + "\r\n").getBytes());
            out.write("\r\n".getBytes());
            out.write(message.getBytes());
        }
    }
}

