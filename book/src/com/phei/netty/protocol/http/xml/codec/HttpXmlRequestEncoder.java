package com.phei.netty.protocol.http.xml.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.net.InetAddress;
import java.util.List;

/**
 * 请求消息编码器,比如客户端向服务器发送请求消息时,客户端在发送请求消息时需要编码.将pojo编辑为xml格式字符串.
 * @author CHEN
 */
public class HttpXmlRequestEncoder extends AbstractHttpXmlEncoder<HttpXmlRequest> {

	@Override
	protected void encode(ChannelHandlerContext ctx, HttpXmlRequest msg, List<Object> out) throws Exception {
		System.out.println("HttpXmlRequestEncoder(请求消息编辑器)开始工作.");
		System.out.println("业务层提交的pojo:{"+msg.getBody()+"}.");
		ByteBuf body = encode0(ctx, msg.getBody());
		System.out.println("pojo编码后:{"+body+"}.并将此消息设置为request的消息.");
		FullHttpRequest request = msg.getRequest();
		//业务层可能会提交空的request对象.如果业务层提交了空的reauest就要构建一个request对象.并将编码好的xml字符设置为request的消息体.
		if (request == null) {
			request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/do", body);
			HttpHeaders headers = request.headers();
			headers.set(HttpHeaders.Names.HOST, InetAddress.getLocalHost().getHostAddress());
			headers.set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
			headers.set(HttpHeaders.Names.ACCEPT_ENCODING,
					HttpHeaders.Values.GZIP.toString() + ',' + HttpHeaders.Values.DEFLATE.toString());
			headers.set(HttpHeaders.Names.ACCEPT_CHARSET, "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
			headers.set(HttpHeaders.Names.ACCEPT_LANGUAGE, "zh");
			headers.set(HttpHeaders.Names.USER_AGENT, "Netty xml Http Client side");
			headers.set(HttpHeaders.Names.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		}
		//将编码好的xml字符设置为request的消息体
		HttpHeaders.setContentLength(request, body.readableBytes());
		System.out.println("向下一个编辑器递交request对象.");
		//向低层的编码器递交request,如HttpRequestEncoder编码器可以将请求对象编码为Http请求报文.
		out.add(request);
	}

}
