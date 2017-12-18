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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;

import java.util.List;

/**
 * @author Lilinfeng
 * @date 2014年3月1日
 * @version 1.0
 */
public class HttpXmlResponseDecoder extends AbstractHttpXmlDecoder<DefaultFullHttpResponse> {

	public HttpXmlResponseDecoder(Class<?> clazz) {
		this(clazz, false);
	}

	public HttpXmlResponseDecoder(Class<?> clazz, boolean isPrintlog) {
		super(clazz, isPrintlog);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, DefaultFullHttpResponse msg, List<Object> out) throws Exception {
		System.out.println("HttpXmlResponseDecoder(响应消息解码器(xml->pojo))开始解码,待解码消息");
		System.out.println("待解码消息:{"+msg.content()+"}");
		HttpXmlResponse resHttpXmlResponse = new HttpXmlResponse(msg, decode0(ctx, msg.content()));
		System.out.println("解码后:{"+resHttpXmlResponse.getResult()+"}");
		//在业务层向递交HttpXmlResponse对象
		System.out.println("业务层向递交HttpXmlResponse对象(HttpXmlResponseDecoder是最后一个解码器,是消息到达业务层之前最后经过的包解码器.)");
		out.add(resHttpXmlResponse);
	}

}
