// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDInputStream.java,v 1.8 2001/06/21 14:53:49 wintonn Exp $

package zebedee;

import java.io.*;


public class ZBDInputStream extends InputStream
{
    ZBDTunnel tnl;
    byte[] buffer = new byte[ZBDTunnel.MAX_BUFFER_SIZE];
    int buffered = 0;


    public ZBDInputStream(ZBDTunnel tunnel)
    {
	super();
	tnl = tunnel;
    }

    synchronized public int read(byte[] b, int offset, int len) throws IOException
    {
	// Can we satisfy this read from the buffer?

	if (buffered > 0)
	{
	    int returned = (buffered > len ? len : buffered);

	    System.arraycopy(buffer, 0, b, offset, returned);
	    buffered -= returned;
	    if (buffered > 0)
	    {
		System.arraycopy(buffer, returned, buffer, 0, buffered);
	    }
	    return returned;
	}

	// We need to get some more data ...

	int num = 0;
	try
	{
	    num = tnl.readMessage(buffer);
	}
	catch (ZBDTimeoutException et)
	{
	    throw new InterruptedIOException(et.toString());
	}
	catch (ZBDException ez)
	{
	    throw new IOException(ez.toString());
	}

	if (num < 0)
	{
	    return num;
	}

	// If we read less than requested just return it immediately
	// otherwise we need to buffer up the extra data for next time.

	if (num <= len)
	{
	    System.arraycopy(buffer, 0, b, offset, num);
	    return num;
	}
	else
	{
	    System.arraycopy(buffer, 0, b, offset, len);
	    buffered = (num - len);
	    System.arraycopy(buffer, len, buffer, 0, buffered);
	    return len;
	}
    }

    synchronized public int read() throws IOException
    {
	byte[] single = new byte[1];
	if (read(single) < 0) return -1;
	return ((int)single[0] & 0xff);
    }

    synchronized public int available()
    {
	return buffered;
    }

    synchronized public void close() throws IOException
    {
	try
	{
	    tnl.shutdownInput();
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
