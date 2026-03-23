// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDOutputStream.java,v 1.7 2001/06/21 14:53:50 wintonn Exp $

package zebedee;

import java.io.*;


public class ZBDOutputStream extends OutputStream
{
    ZBDTunnel tnl;


    public ZBDOutputStream(ZBDTunnel tunnel)
    {
	super();
	tnl = tunnel;
    }

    synchronized public void write(byte[] b, int offset, int len) throws IOException
    {
	int left = len;
	int myOffset = offset;
	int max = tnl.getBufferSize();


	while (left > 0)
	{
	    try
	    {
		if (left < max)
		{
		    tnl.writeMessage(b, myOffset, left);
		    left = 0;
		}
		else
		{
		    tnl.writeMessage(b, myOffset, max);
		    left -= max;
		    myOffset += max;
		}
	    }
	    catch (ZBDException ez)
	    {
		throw new IOException("ZBD IO error: " + ez);
	    }
	}
    }

    synchronized public void write(int b) throws IOException
    {
	byte[] single = new byte[1];
	single[0] = (byte)b;
	write(single, 0, 1);
    }

    synchronized public void close() throws IOException
    {
	try
	{
	    tnl.shutdownOutput();
	}
	catch (NoSuchMethodError en)
	{
	    try
	    {
		tnl.close();
	    }
	    catch (Exception e)
	    {
		throw new IOException("ZBD error: " + e.getMessage());
	    }
	}
	catch (Exception e)
	{
	    throw new IOException("ZBD error: " + e.getMessage());
	}
    }

}
