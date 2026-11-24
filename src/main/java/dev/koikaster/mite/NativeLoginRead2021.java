package dev.koikaster.mite;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
/** Keep the existing protocol-transition read pause on the channel's I/O owner. */
public final class NativeLoginRead2021 {
 public static ChannelConfig setAutoRead(ChannelConfig config,boolean enabled,Channel channel){
  if(channel.eventLoop().inEventLoop())return config.setAutoRead(enabled);
  channel.eventLoop().execute(()->config.setAutoRead(enabled));
  return config;
 }
 private NativeLoginRead2021(){}
}
