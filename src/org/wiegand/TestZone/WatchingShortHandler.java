package org.wiegand.TestZone;

import java.util.Queue;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.wiegand.at8000.WgUdpCommShort;


    
    /**
     * Class the extends IoHandlerAdapter in order to properly handle
     * connections and the data the connections send
     *
     * @author <a href="http://mina.apache.org" mce_href="http://mina.apache.org">Apache MINA Project</a>
     */
    public class WatchingShortHandler extends IoHandlerAdapter {

    	private Queue<byte[]> queue;
        public WatchingShortHandler(Queue<byte[]> queue) {
    		super();
    		this.queue = queue;
    	}
        /**
         * 异常来关闭session
         */
        @Override
        public void exceptionCaught(IoSession session, Throwable cause)
                throws Exception {
            cause.printStackTrace();
            session.close(true);
        }

        /**
         * 服务器端收到一个消息
         */
        @Override
        public void messageReceived(IoSession session, Object message)
                throws Exception {
        	String mes = message.toString();
        	System.out.println(mes);
        	//=================================
        	IoBuffer io = (IoBuffer) message;
    		if (io.hasRemaining())
    		{
    			byte[] validBytes = new byte[io.remaining()];
    			io.get(validBytes,0,io.remaining());
    			if (validBytes.length == WgUdpCommShort.WGPacketSize)
    			{
    				 synchronized (queue)
    		         {
      				   queue.offer(validBytes);
    		         }
    			}
    			else
    			{
    				//System.out.print("收到无效数据包: ????\r\n");
    			}
    			//System.out.println("");
    		}
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
//            System.out.println("服务器端关闭session...");
        }

        @Override
        public void sessionCreated(IoSession session) throws Exception {
//            System.out.println("服务器端成功创建一个session...");
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status)
                throws Exception {
           //  System.out.println("Session idle...");
        }

        @Override
        public void sessionOpened(IoSession session) throws Exception {
//            System.out.println("服务器端成功开启一个session...");
        }
    }
