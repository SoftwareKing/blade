package com.blade.server.netty;

import com.blade.Blade;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * Http服务端ChannelInitializer
 */
public class HttpServerInitializer extends ChannelInitializer<SocketChannel> {

    private final Blade blade;
    private final SslContext sslCtx;

    public HttpServerInitializer(Blade blade, SslContext sslCtx) {
        this.blade = blade;
        this.sslCtx = null;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        if (sslCtx != null) {
            p.addLast(sslCtx.newHandler(ch.alloc()));
        }
        p.addLast(new HttpServerCodec())
                .addLast(new HttpServerExpectContinueHandler())
                .addLast(new HttpObjectAggregator(Integer.MAX_VALUE))
                .addLast(new ChunkedWriteHandler());

        if (blade.gzip()) {
            p.addLast(new HttpContentCompressor());
        }
        p.addLast(new HttpServerHandler(blade));
    }
}
