// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDTcpTunnelReader.java,v 1.3 2001/06/21 14:53:53 wintonn Exp $

package zebedee;


import java.io.*;
import java.net.*;

/**
 * Read data from a tunnel and write it to a local TCP/IP socket.
 */

public class ZBDTcpTunnelReader extends Thread
{
    ZBDTunnel tunnel;
    Socket localSock;

    public ZBDTcpTunnelReader(ZBDTunnel tunnel, Socket sock)
    {
	this.tunnel = tunnel;
	this.localSock = sock;
    }

    public void run()
    {
	byte[] buffer = new byte[tunnel.getBufferSize()];
	int num = 0;

	InputStream in;
	OutputStream out;

	try
	{
	    in = tunnel.getInputStream();
	    out = localSock.getOutputStream(); 
	}
	catch (Exception e)
	{
	    tunnel.logger.error("Can't get I/O streams: " + e);
	    return;
	}

	try
	{
	    while (tunnel.readable() && (num = in.read(buffer)) > 0)
	    {
		out.write(buffer, 0, num);
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
	    localSock.shutdownOutput();
	}
	catch (Exception e)
	{
	}
    }
}
