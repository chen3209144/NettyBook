package aio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class AcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, AsyncTimeServerHandler> {

	@Override
	public void completed(AsynchronousSocketChannel result, AsyncTimeServerHandler attachment) {
		SocketAddress addr = null;
		try {
			addr = result.getRemoteAddress();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("\n"+this.hashCode()+"-->客户端("+addr+")连接成功,服务器为下一次连接,注册连接事件回调处理器("+this.hashCode()+")");
		attachment.asynchronousServerSocketChannel.accept(attachment, this);
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		ReadCompletionHandler handler = new ReadCompletionHandler(result);
		System.out.println(this.hashCode()+"-->创建buffer实例("+buffer.hashCode()+")接收来自客户端("+addr+")的消息,注册消息接收事件回调处理器("+handler.hashCode()+")");
		result.read(buffer, buffer, handler);
	}

	@Override
	public void failed(Throwable exc, AsyncTimeServerHandler attachment) {
		exc.printStackTrace();
		System.out.println(this.hashCode()+"-->连接失败,结束程序.");
		attachment.latch.countDown();
	}

}
