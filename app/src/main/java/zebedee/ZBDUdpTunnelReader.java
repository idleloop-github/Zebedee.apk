// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDUdpTunnelReader.java,v 1.2 2001/06/21 14:53:56 wintonn Exp $

package zebedee;


import java.io.*;
import java.net.*;

/**
 * Read data from a tunnel and write it to a local UDP socket.
 */

public class ZBDUdpTunnelReader extends Thread
{
    ZBDTunnel tunnel;
    DatagramSocket localSock;
    InetAddress sendAddr;
    int sendPort;
    boolean closeFlag = false;

    public ZBDUdpTunnelReader(ZBDTunnel tunnel, DatagramSocket sock,
			      InetAddress addr, int port, boolean closeFlag)
    {
	this.tunnel = tunnel;
	this.localSock = sock;
	this.sendAddr = addr;
	this.sendPort = port;
	this.closeFlag = closeFlag;
    }

    public ZBDUdpTunnelReader(ZBDTunnel tunnel, DatagramSocket sock,
			      InetAddress addr, int port)
    {
	this.tunnel = tunnel;
	this.localSock = sock;
	this.sendAddr = addr;
	this.sendPort = port;
	this.closeFlag = false;
    }

    public ZBDUdpTunnelReader(ZBDTunnel tunnel, DatagramSocket sock)
    {
	this(tunnel, sock, false);
    }

    public ZBDUdpTunnelReader(ZBDTunnel tunnel, DatagramSocket sock, boolean closeFlag)
    {
	this.tunnel = tunnel;
	this.localSock = sock;
	this.sendAddr = sock.getInetAddress();
	this.sendPort = sock.getPort();
	this.closeFlag = closeFlag;
    }

    public void run()
    {
	byte[] buffer = new byte[tunnel.getBufferSize()];
	int num = 0;

	InputStream in;
	DatagramPacket packet =
	    new DatagramPacket(buffer, buffer.length, sendAddr, sendPort);

	try
	{
	    in = tunnel.getInputStream();
	}
	catch (Exception e)
	{
	    tunnel.logger.error("Can't get I/O stream: " + e);
	    return;
	}

	try
	{
	    while (tunnel.readable() && (num = in.read(buffer)) > 0)
	    {
		packet.setLength(num);

		localSock.send(packet);

		tunnel.logger.log(5, "sent " + num + " bytes to local socket");
	    }
	    tunnel.logger.log(1, "EOF encountered on tunnel connection");
	}
	catch (InterruptedIOException et)
	{
	    // Timeouts are handled gracefully. We will close the
	    // tunnel down.

	    try { tunnel.close(); } catch (Exception e) {}
	    tunnel.logger.log(1, "timeout reached reading tunnel");
	}
	catch (Exception e)
	{
	    tunnel.logger.error("failed reading tunnel/writing source socket: " + e);
	}

	try
	{
	    in.close();
	    if (closeFlag)
	    {
		localSock.close();
	    }
	}
	catch (Exception e)
	{
	}
    }
}
