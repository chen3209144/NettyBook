package aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.CountDownLatch;

public class AsyncTimeServerHandler implements Runnable {


	CountDownLatch latch;
	AsynchronousServerSocketChannel asynchronousServerSocketChannel;

	public AsyncTimeServerHandler(int port) {
		try {
			asynchronousServerSocketChannel = AsynchronousServerSocketChannel.open();
			asynchronousServerSocketChannel.bind(new InetSocketAddress(port));
			System.out.println("服务器启动成功,绑定了端口:" + port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		latch = new CountDownLatch(1);
		doAccept();
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void doAccept() {
		AcceptCompletionHandler handler = new AcceptCompletionHandler();
		System.out.println("服务器等待客户端连接,并注册连接事件回调处理器实例("+handler.hashCode()+")");
		asynchronousServerSocketChannel.accept(this, handler);
	}

}
