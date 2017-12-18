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
package com.phei.netty.protocol.http.xml.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import java.net.InetSocketAddress;
import com.phei.netty.protocol.http.xml.codec.HttpXmlRequestDecoder;
import com.phei.netty.protocol.http.xml.codec.HttpXmlResponseEncoder;
import com.phei.netty.protocol.http.xml.pojo.Order;

/**
 * @author lilinfeng
 * @date 2014年2月14日
 * @version 1.0
 */
public class HttpXmlServer {
	public void run(final int port) throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						//客户端请求服务器时,Netty框架为这个客户完成初始化channel操作之后,调用这个方法.
						protected void initChannel(SocketChannel ch) throws Exception {
							/**
							 * 服务器解码过程(针对请求对象)
							 * 1,接收到来自客户端的发送的请求报文,先HttpRequestDecoder将请求报文解析为Http的请求报文.
							 * 2,将解释好的Http请求报文,使用HttpObjectAggregator合并或者解析出request对象,可能会是从多个Http报文合并之后才解释出一个Request对象.
							 * 3,HttpXmlRequestDecoder将request的消息体(xml格式的字符串)转换为pojo对象,并递交给业务层.
							 */
							/**
							 * 服务器编码过程(针对响应对象)
							 * 1,业务层递交pojo对象,先调用HttpXmlResponseEncoder将pojo对象编码为xml格式的字符串.将将转换好的xml设置为reponse的消息体.
							 * 2,HttpResponseEncoder将response解码为http响应报文.
							 */
							System.out.println("服务器收到一个客户端请求,并为客户端构建了channel对象,现在给channel设置解码器和编码器.");
							ch.pipeline().addLast("http-decoder", new HttpRequestDecoder());
							ch.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
							ch.pipeline().addLast("xml-decoder", new HttpXmlRequestDecoder(Order.class, true));
							ch.pipeline().addLast("http-encoder", new HttpResponseEncoder());
							ch.pipeline().addLast("xml-encoder", new HttpXmlResponseEncoder());
							ch.pipeline().addLast("xmlServerHandler", new HttpXmlServerHandler());
						}
					});
			ChannelFuture future = b.bind(new InetSocketAddress(port)).sync();
			//这个例子原来也想的文件服务器一样通过 future.channel().localAddress()接口获得,服务器启动后的网址,但此处调用这个接口,并没有获取正确的ip地址.
			System.out.println("HTTP订购服务器启动，网址是 :" + "http://127.0.0.1:8080");
			future.channel().closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) throws Exception {
		int port = 8080;
		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		new HttpXmlServer().run(port);
	}
}
