// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDTcpTunnelWriter.java,v 1.3 2001/06/21 14:53:53 wintonn Exp $

package zebedee;


import java.io.*;
import java.net.*;

/**
 * Read data from a local TCP/IP socket and write it to a tunnel.
 */

public class ZBDTcpTunnelWriter extends Thread
{
    ZBDTunnel tunnel;
    Socket localSock;


    public ZBDTcpTunnelWriter(ZBDTunnel tunnel, Socket sock)
    {
	this.tunnel = tunnel;
	localSock = sock;
    }

    public void run()
    {
	byte[] buffer = new byte[tunnel.getBufferSize()];
	int num = 0;

	InputStream in;
	OutputStream out;

	try
	{
	    out = tunnel.getOutputStream();
	    in = localSock.getInputStream(); 
	}
	catch (Exception e)
	{
	    tunnel.logger.error("can't get I/O streams: " + e);
	    return;
	}

	try
	{
	    while (tunnel.writeable() && (num = in.read(buffer)) > 0)
	    {
		out.write(buffer, 0, num);
	    }

	    tunnel.logger.log(1, "EOF encountered on source socket or tunnel connection closed");
	}
	catch (Exception e)
	{
	    tunnel.logger.error("failed reading source/writing to tunnel: " + e);
	}

	try
	{
	    localSock.shutdownInput();
	    out.close();
	}
	catch (Exception e)
	{
	}
    }
}
