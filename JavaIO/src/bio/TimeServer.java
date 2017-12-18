
package bio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class TimeServer {

	
	public static void main(String[] args) throws IOException {
		int port = 8080;
		if (args != null && args.length > 0) {

			try {
				port = Integer.valueOf(args[0]);
			} catch (NumberFormatException e) {
				// 采用默认�?
			}

		}
		ServerSocket server = null;
		try {
			server = new ServerSocket(port);
			System.out.println("服务器启成功,绑定的端口号:" + port);
			Socket socket = null;
			while (true) {
				System.out.println("服务器等待客户端连接");
				socket = server.accept();
				System.out.println("服务器收到来自客户端("+socket.getRemoteSocketAddress()+")的连接");
				TimeServerHandler runnable = new TimeServerHandler(socket);
				Thread thread = new Thread(runnable);
				thread.start();
				System.out.println("服务器已启动线程用于处理来自客户("+socket.getRemoteSocketAddress().toString()+")的请求,线程名称:"+thread.getName());
			}
		} 
		finally 
		{
			if (server != null) {
				System.out.println("服务器关闭");
				server.close();
				server = null;
			}
		}
	}
}
