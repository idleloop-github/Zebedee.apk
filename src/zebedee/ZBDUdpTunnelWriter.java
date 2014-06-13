// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDUdpTunnelWriter.java,v 1.2 2001/06/21 14:53:56 wintonn Exp $

package zebedee;


import java.io.*;
import java.net.*;

/**
 * Read data from a local UDP socket and write it to the tunnel.
 */

public class ZBDUdpTunnelWriter extends Thread
{
    ZBDTunnel tunnel;
    DatagramSocket localSock;
    boolean closeFlag = false;

    public ZBDUdpTunnelWriter(ZBDTunnel tunnel, DatagramSocket sock, boolean closeFlag)
    {
	this.tunnel = tunnel;
	this.localSock = sock;
	this.closeFlag = closeFlag;
    }

    public ZBDUdpTunnelWriter(ZBDTunnel tunnel, DatagramSocket sock)
    {
	this.tunnel = tunnel;
	this.localSock = sock;
	this.closeFlag = false;
    }

    public void run()
    {
	byte[] buffer = new byte[tunnel.getBufferSize()];
	int num = 0;

	OutputStream out;
	DatagramPacket packet =
	    new DatagramPacket(buffer, buffer.length);

	try
	{
	    out = tunnel.getOutputStream();
	}
	catch (Exception e)
	{
	    tunnel.logger.error("Can't get I/O stream: " + e);
	    return;
	}

	try
	{
	    while (tunnel.writeable())
	    {
		packet.setLength(tunnel.getBufferSize());
		localSock.receive(packet);
		out.write(buffer, 0, packet.getLength());

		tunnel.logger.log(5, "read " + packet.getLength() + " bytes from local socket");
	    }
	    tunnel.logger.log(1, "EOF encountered on tunnel connection");
	}
	catch (InterruptedIOException ie)
	{
	    // UDP timeouts are handled gracefully. We will close the
	    // tunnel down.

	    try { tunnel.close(); } catch (Exception e) {}
	    tunnel.logger.log(1, "timeout reached reading local socket");
	}
	catch (Exception e)
	{
	    if (tunnel.readable() && tunnel.writeable())
	    {
		tunnel.logger.error("failed reading source socket/writing to tunnel: " + e);
	    }
	}

	try
	{
	    out.close();
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
