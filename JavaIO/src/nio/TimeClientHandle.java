package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class TimeClientHandle implements Runnable {

	private String host;
	private int port;

	private Selector selector;
	private SocketChannel socketChannel;

	private volatile boolean stop;

	public TimeClientHandle(String host, int port) {
		this.host = host == null ? "127.0.0.1" : host;
		this.port = port;
		try {
			selector = Selector.open();
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void run() {
		try {
			doConnect();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		while (!stop) {
			try {
				System.out.println("\n客户端selector轮询指定事件的发生.");
				selector.select(1000);
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
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
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
			// 判断是否连接成功
			SocketChannel sc = (SocketChannel) key.channel();
			if (key.isConnectable()) {
				System.out.println("连接响应事件发生");
				if (sc.finishConnect()) {
					sc.register(selector, SelectionKey.OP_READ);
					System.out.println("客户端("+sc.getLocalAddress()+")连接服务器成功,并向selector委托监听消息接收事件(接收来来自服务器的消息)");
					doWrite(sc);
				} else
					System.exit(1);
			}
			if (key.isReadable()) {
				System.out.println("消息接收事件发生.");
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				int readBytes = sc.read(readBuffer);
				if (readBytes > 0) {
					readBuffer.flip();
					byte[] bytes = new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					String body = new String(bytes, "UTF-8");
					System.out.println("客户端("+sc.getLocalAddress()+")收到消息:{"+body+"},程序停止");
					this.stop = true;
				} else if (readBytes < 0) {
					// 对端链路关闭
					key.cancel();
					sc.close();
				} 
			}
		}

	}

	private void doConnect() throws IOException {
		System.out.println("客户端向服务器发起连接.");
		if (socketChannel.connect(new InetSocketAddress(host, port))) {
			socketChannel.register(selector, SelectionKey.OP_READ);
			System.out.println("连接成功,客户端("+socketChannel.getLocalAddress()+")向select委托监听接收事件消息(读取来来自服务器的消息)");
			doWrite(socketChannel);
		} else {
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
			System.out.println("正在连接,客户端向selector委托连接监听事件(监听是否连接成功的事件).");
		}
	}

	private void doWrite(SocketChannel sc) throws IOException {
		String message = "QUERY TIME ORDER";
		byte[] req =message.getBytes();
		System.out.println("客户端向服务器发送的消息:{"+message+"}");
		ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
		writeBuffer.put(req);
		writeBuffer.flip();
		sc.write(writeBuffer);
		if (!writeBuffer.hasRemaining())
			System.out.println("客户端消息发送完成");
	}

}
