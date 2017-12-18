package aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

public class AsyncTimeClientHandler implements CompletionHandler<Void, AsyncTimeClientHandler>, Runnable {

	private AsynchronousSocketChannel client;
	private String host;
	private int port;
	private CountDownLatch latch;

	public AsyncTimeClientHandler(String host, int port) {
		this.host = host;
		this.port = port;
		try {
			client = AsynchronousSocketChannel.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		latch = new CountDownLatch(1);
		InetSocketAddress server = new InetSocketAddress(host, port);
		System.out.println("客户端创建成功,向服务器("+server+")发起连接,并注册连接事件处理器实例("+this+")");
		client.connect(server, this, this);
		try {
			latch.await();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void completed(Void result, AsyncTimeClientHandler attachment) {
		String message = "QUERY TIME ORDER";
		byte[] req = message.getBytes();
		ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
		writeBuffer.put(req);
		writeBuffer.flip();
		CompletionHandler<Integer, ByteBuffer> handler = new CompletionHandler<Integer, ByteBuffer>() {
			@Override
			public void completed(Integer result, ByteBuffer buffer) {
				System.out.println(this + "-->客户端部分消息的发送事件成功(长消息会被分成多次发送),消息内容来自缓冲区(" + buffer + ").");
				if (buffer.hasRemaining()) {
					System.out.println(this + "-->消息未全部发送完成,继续调用发送消息接口,并注册消息发送事件处理器("+this+").");
					client.write(buffer, buffer, this);
				} else {
					ByteBuffer readBuffer = ByteBuffer.allocate(1024);
					CompletionHandler<Integer, ByteBuffer> handler2 = new CompletionHandler<Integer, ByteBuffer>() {
						@Override
						public void completed(Integer result, ByteBuffer buffer) {
							buffer.flip();
							byte[] bytes = new byte[buffer.remaining()];
							buffer.get(bytes);
							String body;
							System.out.println(this+"-->客户端接收消息完成,消息内容已放置在缓冲区("+buffer+").");
							try {
								body = new String(bytes, "UTF-8");
								System.out.println(this+"-->客户端收到消息:{" + body+"},结束程序");
								latch.countDown();
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
						}

						@Override
						public void failed(Throwable exc, ByteBuffer attachment) {
							try {
								client.close();
								System.out.println(this+"-->接收消息失败,消息内容来自缓冲区("+attachment+"),结束程序");
								latch.countDown();
							} catch (IOException e) {
								// ingnore on close
							}
						}
					};
					System.out.println(this + "-->消息的发送事件完成.等待服务器响应,创建一个缓冲区("+readBuffer+")用于接收服务器的响应消息,并注册一个消息接收事件处理器实例("+handler2+")");
					client.read(readBuffer, readBuffer,handler2);
				}
			}

			@Override
			public void failed(Throwable exc, ByteBuffer attachment) {
				try {
					client.close();
					latch.countDown();
				} catch (IOException e) {
					// ingnore on close
				}
			}
		};
		System.out.println(this+"-->客户端连接服务器成功,向服务器发送消息{"+message+"},消息放置在缓冲区("+writeBuffer+"),然后发送,并注册发送事件处理器实例("+handler+")");
		client.write(writeBuffer, writeBuffer,handler );
	}

	@Override
	public void failed(Throwable exc, AsyncTimeClientHandler attachment) {
		exc.printStackTrace();
		try {
			client.close();
			latch.countDown();
			System.out.println(this + "-->部分消息的发送事件失败(长消息会被分成多次发送),消息内容来自缓冲区(" + attachment + "),程序结束.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
