package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class MultiplexerTimeServer implements Runnable {

	private Selector selector;

	private ServerSocketChannel servChannel;

	private volatile boolean stop;

	public MultiplexerTimeServer(int port) {
		try {
			selector = Selector.open();
			servChannel = ServerSocketChannel.open();
			servChannel.configureBlocking(false);
			servChannel.socket().bind(new InetSocketAddress(port), 1024);
			System.out.println("服务器启成功,绑定的端口号:" + port);
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
				System.out.println("连接请求事件发生.");
				ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
				SocketChannel sc = ssc.accept();
				System.out.println("服务端获取到来自客户端(" + sc.getRemoteAddress().toString() + ")的连接");
				sc.configureBlocking(false);
				// Add the new connection to the selector
				sc.register(selector, SelectionKey.OP_READ);
				System.out.println("客户端通道(" + sc.getRemoteAddress() + ")向select注册消息接收事件(读取客户端向服务器发送的消息)");
			}
			if (key.isReadable()) {
				// Read the data
				System.out.println("消息接收事件发生");
				SocketChannel sc = (SocketChannel) key.channel();
				System.out.println("收到来自客户端(" + sc.getRemoteAddress() + ")的消息");
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				int readBytes = sc.read(readBuffer);
				if (readBytes > 0) {
					readBuffer.flip();
					byte[] bytes = new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					String body = new String(bytes, "UTF-8");
					System.out.println("客户端(" + sc.getRemoteAddress() + ")发送过来的消息:{" + body + "}");
					String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body)
							? new java.util.Date(System.currentTimeMillis()).toString()
							: "BAD ORDER";
					System.out.println("服务器响应的消息:{" + currentTime + "}");
					doWrite(sc, currentTime);
				} else if (readBytes < 0) {
					key.cancel();
					sc.close();
				}
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
