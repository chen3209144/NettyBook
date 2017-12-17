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
package com.phei.netty.protocol.http.xml.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;

import java.net.InetSocketAddress;

import com.phei.netty.protocol.http.xml.codec.HttpXmlRequestEncoder;
import com.phei.netty.protocol.http.xml.codec.HttpXmlResponseDecoder;
import com.phei.netty.protocol.http.xml.pojo.Order;

/**
 * @author lilinfeng
 * @date 2014年2月14日
 * @version 1.0
 */
public class HttpXmlClient {

	public void connect(int port) throws Exception {
		// 配置客户端NIO线程组
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group)
			.channel(NioSocketChannel.class)
			.option(ChannelOption.TCP_NODELAY, true)
			.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							/**
							 * 客户端消息解码过程(针对响应对象)
							 * 1,客户端收到服务器响应报文(Tcp层递交的二进制码流),先使用HttpResponseDecoder对响应报文解码(HttpResponseDecoder会以Http协议的响应报文格式对响应报文进行解码).
							 * 2,将解释好的Http报文,使用HttpObjectAggregator合并或者解析出Request或者Response对象,可能会是从多个Http报文合并之后才解释出一个Request或者Response对象.
							 * 3,调用自定义的HttpXmlResponseDecoder解码器,将响应对象中消息体(xml)转换为pojo.并向业务层递交pojo对象.
							 */
							/**
							 * 客户端编辑过程(针对请求对象)
							 * 1,业务层递交pojo对象,先调用HttpXmlRequestEncoder编码器,将pojo对象编辑为xml格式的字符,并设置为Request对象的消息内容.
							 * 2,调用HttpRequestEncoder将Response对象解析为Http请求报文.
							 */
							//响应消息解码器(xml->pojo)
							ch.pipeline().addLast("http-decoder", new HttpResponseDecoder());
							// 目的是将多个消息转换为单一的request或者response对象
							ch.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
							// 响应消息解码器,xml->pojo
							ch.pipeline().addLast("xml-decoder", new HttpXmlResponseDecoder(Order.class, true));
							ch.pipeline().addLast("http-encoder", new HttpRequestEncoder());
							ch.pipeline().addLast("xml-encoder", new HttpXmlRequestEncoder());
							ch.pipeline().addLast("xmlClientHandler", new HttpXmlClientHandle());
						}
					});

			// 发起异步连接操作
			ChannelFuture f = b.connect(new InetSocketAddress(port)).sync();

			// 当代客户端链路关闭
			f.channel().closeFuture().sync();
		} finally {
			// 优雅退出，释放NIO线程组
			group.shutdownGracefully();
		}
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		int port = 8080;
		if (args != null && args.length > 0) {
			try {
				port = Integer.valueOf(args[0]);
			} catch (NumberFormatException e) {
				// 采用默认值
			}
		}
		new HttpXmlClient().connect(port);
	}
}
