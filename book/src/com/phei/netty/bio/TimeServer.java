/*
 * Copyright 2013-2018 Lilinfeng.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.phei.netty.bio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author lilinfeng
 * @date 2014年2月14日
 * @version 1.0
 */
public class TimeServer {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		int port = 8080;
		if (args != null && args.length > 0) {

			try {
				port = Integer.valueOf(args[0]);
			} catch (NumberFormatException e) {
				// 采用默认值
			}

		}
		ServerSocket server = null;
		try {
			server = new ServerSocket(port);
			System.out.println("服务器启动,绑定的端口号是:" + port);
			Socket socket = null;
			while (true) {
				System.out.println("服务器等待客户端连接");
				socket = server.accept();
				System.out.println("服务器收到来自"+socket.getRemoteSocketAddress().toString()+"连接");
				TimeServerHandler runnable = new TimeServerHandler(socket);
				Thread thread = new Thread(runnable);
				thread.start();
				System.out.println("服务器已启动线程用于处理"+socket.getRemoteSocketAddress().toString()+"的请求,线程名:"+thread.getName());
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
