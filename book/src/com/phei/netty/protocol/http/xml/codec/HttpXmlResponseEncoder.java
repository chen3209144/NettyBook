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
package com.phei.netty.protocol.http.xml.codec;

import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;

import java.util.List;

/**
 * @author Lilinfeng
 * @date 2014年3月1日
 * @version 1.0
 */
public class HttpXmlResponseEncoder extends AbstractHttpXmlEncoder<HttpXmlResponse> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.handler.codec.MessageToMessageEncoder#encode(io.netty.channel
	 * .ChannelHandlerContext, java.lang.Object, java.util.List)
	 */
	protected void encode(ChannelHandlerContext ctx, HttpXmlResponse msg, List<Object> out) throws Exception {
		//将业务层递交的pojo(HttpXmlResponseEncoder是响应消息经过的第一个编码器,会直接收到业务层的消息)转换为xml字符串.
		ByteBuf body = encode0(ctx, msg.getResult());
		FullHttpResponse response = msg.getHttpResponse();
		//业务层可能会递交空的reponse对象.
		if (response == null) {
			response = new DefaultFullHttpResponse(HTTP_1_1, OK, body);
		} else {
			response = new DefaultFullHttpResponse(msg.getHttpResponse().getProtocolVersion(),
					msg.getHttpResponse().getStatus(), body);
		}
		response.headers().set(CONTENT_TYPE, "text/xml");
		//将解析好的xml设置为响应对象的消息体.
		setContentLength(response, body.readableBytes());
		//将response递交给下一个编码器,如HttpResponseEncoder可以将response编码为Http响应报文.
		out.add(response);
	}
}
