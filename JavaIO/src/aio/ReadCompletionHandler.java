package aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ReadCompletionHandler implements CompletionHandler<Integer, ByteBuffer> {

	private AsynchronousSocketChannel channel;

	public ReadCompletionHandler(AsynchronousSocketChannel channel) {
		if (this.channel == null)
			this.channel = channel;
	}

	@Override
	public void completed(Integer result, ByteBuffer attachment) {
		System.out.println(this.hashCode()+"-->服务器接收消息完成事件,消息内容已放置在缓冲区("+attachment.hashCode()+").");
		attachment.flip();
		byte[] body = new byte[attachment.remaining()];
		attachment.get(body);
		try {
			String req = new String(body, "UTF-8");
			System.out.println(this.hashCode()+"-->服务器收到的消息:{"+req+"}.");
			String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(req)
					? new java.util.Date(System.currentTimeMillis()).toString()
					: "BAD ORDER";
			doWrite(currentTime);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private void doWrite(String currentTime) {
		if (currentTime != null && currentTime.trim().length() > 0) {
			byte[] bytes = (currentTime).getBytes();
			ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
			writeBuffer.put(bytes);
			writeBuffer.flip();
			CompletionHandler<Integer, ByteBuffer> handler = new CompletionHandler<Integer, ByteBuffer>() {
				@Override
				public void completed(Integer result, ByteBuffer buffer) {
					System.out.println(this.hashCode() + "-->部分消息的发送事件完成(长消息会被分成多次发送),消息内容来自缓冲区(" + buffer.hashCode() + ").");
					if (buffer.hasRemaining()) {
						System.out.println(this.hashCode() + "-->消息未全部发送完成,继续调用发送接口,并注册发送事件处理器实例("+this.hashCode()+").");
						channel.write(buffer, buffer, this);
					} else {
						System.out.println(this.hashCode() + "-->全部消息的发送事件完成.");
					}
				}

				@Override
				public void failed(Throwable exc, ByteBuffer attachment) {
					try {
						channel.close();
						System.out.println(this.hashCode() + "-->部分消息的发送事件失败(长消息会被分成多次发送),消息内容来自缓冲区(" + attachment.hashCode() + "),关闭channel.");
					} catch (IOException e) {
						// ingnore on close
					}
				}
			};
			System.out.println(this.hashCode()+"-->服务器将响应消息:{"+currentTime+"}放置在缓冲区("+writeBuffer.hashCode()+"),并向客户端发送响应消息,同时注册消息发送事件处理器("+handler.hashCode()+").");
			channel.write(writeBuffer, writeBuffer,handler);
		}
	}

	@Override
	public void failed(Throwable exc, ByteBuffer attachment) {
		try {
			this.channel.close();
			System.out.println(this.hashCode()+"-->接收消息失败,消息内容来自缓冲区("+attachment+"),channel已关闭.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
