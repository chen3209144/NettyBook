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
package com.phei.netty.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Administrator
 * @date 2014年2月16日
 * @version 1.0
 */
public class MultiplexerTimeServer implements Runnable {

	private Selector selector;

	private ServerSocketChannel servChannel;

	private volatile boolean stop;

	/**
	 * 初始化多路复用器、绑定监听端口
	 * 
	 * @param port
	 */
	public MultiplexerTimeServer(int port) {
		try {
			// 创建ServerSocketChanne并设置ip和端口.
			/*
			 * ServerSocket与ServerSocketChannel的异同
			 * 1,ServerSocket仅支持阻塞式IO模型,但ServerSocketChannel可支持NIO 2,创建方式不同 2.1
			 * 创建Selector实例.
			 * Selector实例用于多路复用I\O,负责给对应的channel监听连接和读写事件,当有指定应事件发生时,可以被Selector实例轮询出来.
			 * 这样服务器可以委托Selector进行事件监听避免了服务端因为没有连接而被阻塞,服务端也无须为未能响应好的连接建立线程,
			 * 也避免了创建好的线程因为连接不响应而阻塞.节省了机器的线程资源. 2.2
			 * 创建ServerSocketChannel实例和设置IP和端口,注册Selector实例
			 * ServerSocketChannel通过ServerSockerChannel.open()静态方式获取一个实例,
			 * 然后实例serverSocketChannel.socket().bind(IP,port),设置IP和端口. 2.2
			 * ServerSocketChannel实例将特定的事件注册到一个Selector实例.
			 * 启动一个线程,对Selector进行轮询,或有事件要处理则启动对应的处理程序或者线程,
			 * Selector是ServerSocketChannel支付NIO模式的重要组件.
			 */

			/*
			 * Selector实例的创建是通过Selector.open()静态方法获取的.
			 */
			selector = Selector.open();
			servChannel = ServerSocketChannel.open();
			servChannel.configureBlocking(false);
			servChannel.socket().bind(new InetSocketAddress(port), 1024);
			System.out.println("服务器启动成功,绑定的端口号为:" + port);
			servChannel.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("服务器向selector委托监听连接事件");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void stop() {
		this.stop = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while (!stop) {
			try {
				System.out.println("\n服务端selector轮询指定事件的发生");
				selector.select(0);
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = selectedKeys.iterator();
				System.out.println("selector轮询结果返回,是否有事件发生:" + it.hasNext());
				SelectionKey key = null;
				while (it.hasNext()) {
					key = it.next();
					it.remove();
					try {
						handleInput(key);
					} catch (Exception e) {
						if (key != null) {
							key.cancel();
							if (key.channel() != null)
								key.channel().close();
						}
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		// 多路复用器关闭后，所有注册在上面的Channel和Pipe等资源都会被自动去注册并关闭，所以不需要重复释放资源
		if (selector != null)
			try {
				selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	private void handleInput(SelectionKey key) throws IOException {

		if (key.isValid()) {
			// 处理新接入的请求消息
			if (key.isAcceptable()) {
				// Accept the new connection
				System.out.println("连接请求事件发生");
				ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
				SocketChannel sc = ssc.accept();
				System.out.println("服务端获取到来自"+sc.getRemoteAddress().toString()+"的连接");
				sc.configureBlocking(false);
				// Add the new connection to the selector
				sc.register(selector, SelectionKey.OP_READ);
				System.out.println("客户端通道("+sc.getRemoteAddress()+")向select注册读事件(读取客户端向服务器发消息的事件)");
			}
			if (key.isReadable()) {
				// Read the data
				System.out.println("读事件发生");
				SocketChannel sc = (SocketChannel) key.channel();
				System.out.println("收到来自客户端("+sc.getRemoteAddress()+")的消息");
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				int readBytes = sc.read(readBuffer);
				if (readBytes > 0) {
					readBuffer.flip();
					byte[] bytes = new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					String body = new String(bytes, "UTF-8");
					System.out.println("客户端("+sc.getRemoteAddress()+")发送过来的消息:{" + body+"}");
					String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body)
							? new java.util.Date(System.currentTimeMillis()).toString()
							: "BAD ORDER";
					System.out.println("服务器响应的消息为:{"+currentTime+"}");
					doWrite(sc, currentTime);
				} else if (readBytes < 0) {
					// 对端链路关闭
					key.cancel();
					sc.close();
				} else
					; // 读到0字节，忽略
			}
		}
	}

	private void doWrite(SocketChannel channel, String response) throws IOException {
		if (response != null && response.trim().length() > 0) {
			byte[] bytes = response.getBytes();
			ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
			writeBuffer.put(bytes);
			writeBuffer.flip();
			channel.write(writeBuffer);
		}
	}
}
