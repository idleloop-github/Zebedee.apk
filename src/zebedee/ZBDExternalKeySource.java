// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDExternalKeySource.java,v 1.1 2001/06/29 10:43:09 wintonn Exp $

package zebedee;

import java.io.*;

public class ZBDExternalKeySource extends ZBDKeySource
{
    String command;

    public ZBDExternalKeySource(String cmd)
    {
	super();
	command = cmd;
    }

    synchronized public String generateKeyString()
    {
	Runtime rt = Runtime.getRuntime();
	Process proc = null;
	try
	{
	    proc = rt.exec(command);
	}
	catch (Exception e)
	{
	    return null;
	}

	String key = null;
	if (proc != null)
	{
	    BufferedReader in =
		new BufferedReader(new InputStreamReader(proc.getInputStream()));
	    try
	    {
		key = in.readLine();
		in.close();
	    }
	    catch (Exception e)
	    {
		// Ignore
	    }
	    proc.destroy();
	}

	return key;
    }
}
