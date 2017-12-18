package bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TimeClient {

	public static void main(String[] args) {

		int port = 8080;
		if (args != null && args.length > 0) {

			try {
				port = Integer.valueOf(args[0]);
			} catch (NumberFormatException e) {
			}

		}
		Socket socket = null;
		BufferedReader in = null;
		PrintWriter out = null;
		try {
			for(int i=0;i<1000;i++) {
				socket = new Socket("localhost", port);
			}
			System.out.println("客户端向服务器("+socket.getRemoteSocketAddress()+")发起连接请求成功");
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
			String message = "QUERY TIME ORDER";
			System.out.println("客户端向服务器发送消息:{"+message+"}");
			out.println(message);
			System.out.println("消息发送消息成功,等待服务器响应消息");
			String resp = in.readLine();
			System.out.println("服务器响应的消息:{" + resp+"}");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				out.close();
				out = null;
			}

			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				in = null;
			}

			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				socket = null;
			}
		}
	}
}
