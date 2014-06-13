// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDServiceLookup.java,v 1.5 2001/06/29 10:42:30 wintonn Exp $

package zebedee;

import java.io.*;
import java.util.*;

public class ZBDServiceLookup
{
    public static int lookup(String service, String protocol) throws ZBDParseException
    {
	String[] paths;
	String os = System.getProperty("os.name");
	if (os.toLowerCase().startsWith("windows"))
	{
	    Runtime rt = Runtime.getRuntime();
	    Process proc = null;
	    String systemRoot = null;

	    String[] cmds = new String[] {
		"cmd /c echo %SYSTEMROOT%",
		"command /c echo %SYSTEMROOT%"
	    };

	    for (int i = 0; i < cmds.length && proc != null; i++)
	    {
		try
		{
		    proc = rt.exec(cmds[i]);
		}
		catch (Exception e)
		{
		    // Ignore, try again
		}
	    }

	    if (proc != null)
	    {
		BufferedReader in =
		    new BufferedReader(new InputStreamReader(proc.getInputStream()));
		try
		{
		    systemRoot = in.readLine();
		    in.close();
		}
		catch (Exception e)
		{
		    // Ignore
		}
		proc.destroy();
	    }

	    if (systemRoot == null)
	    {
		systemRoot = "c:\\windows";
	    }

	    paths = new String[] {
		systemRoot + "\\system32\\drivers\\etc\\services",
		systemRoot + "\\services",
		"c:\\winnt\\system32\\drivers\\etc\\services",
		"c:\\windows\\system32\\drivers\\etc\\services",
		"c:\\winnt\\services",
		"c:\\windows\\services",
	    };
	}
	else
	{
	    paths = new String[] {
		"/etc/services",
	    };
	}

	for (int i = 0; i < paths.length; i++)
	{
	    FileReader fr;

	    try
	    {
		fr = new FileReader(paths[i]);
	    }
	    catch (Exception e)
	    {
		// Try the next source
		continue;
	    }

	    StreamTokenizer st = new StreamTokenizer(fr);
	    st.resetSyntax();
	    st.whitespaceChars('\u0000', '\u0020');
	    st.wordChars('\u0021', '\u00ff');
	    st.ordinaryChar('/');
	    st.commentChar('#');
	    st.parseNumbers();
	    st.eolIsSignificant(true);

	    try
	    {
		int state = StreamTokenizer.TT_EOL;
		Stack names = new Stack();
		int port = -1;
		String ptype = null;

		while (st.nextToken() != StreamTokenizer.TT_EOF)
		{
		    switch (state)
		    {
		    case StreamTokenizer.TT_EOL:
			switch (st.ttype)
			{
			case StreamTokenizer.TT_EOL:
			    continue;
		
			case StreamTokenizer.TT_WORD:
			    names.push(st.sval);
			    state = StreamTokenizer.TT_NUMBER;
			    break;

			default:
			    throw new ZBDParseException("invalid data in services file '" + paths[i] + "'");
			}
			break;

		    case StreamTokenizer.TT_NUMBER:
			if (st.ttype != StreamTokenizer.TT_NUMBER)
			{
			    throw new ZBDParseException("invalid data in services file '" + paths[i] + "'");
			}
			port = (int)st.nval;
			state = '/';
			break;

		    case '/':
			if (st.ttype != '/')
			{
			    throw new ZBDParseException("invalid data in services file '" + paths[i] + "'");
			}
			state = 'T';
			break;

		    case 'T':
			if (st.ttype != StreamTokenizer.TT_WORD)
			{
			    throw new ZBDParseException("invalid data in services file '" + paths[i] + "'");
			}
			ptype = st.sval;
			state = StreamTokenizer.TT_WORD;

			break;

		    case StreamTokenizer.TT_WORD:
			if (st.ttype == StreamTokenizer.TT_EOL)
			{
			    state = StreamTokenizer.TT_EOL;

			    // Check for a match

			    if (!protocol.equalsIgnoreCase(ptype))
			    {
				continue;
			    }

			    while (!names.empty())
			    {
				String s = (String)names.pop();
				if (s.equalsIgnoreCase(service))
				{
				    return port;
				}
			    }
			}
			else
			{
			    names.push(st.sval);
			}
			break;
		    }
		}

		fr.close();
	    }
	    catch (Exception e)
	    {
		throw new ZBDParseException(e.getMessage());
	    }
	}

	return -1;
    }

    public static void main(String args[]) throws ZBDParseException
    {
	if (args.length != 2)
	{
	    System.out.println("Usage: ZBDServiceLookup service-name tcp-or-udp");
	}
	else
	{
	    System.out.println(args[0] + "/" + args[1] + " -> " + lookup(args[0], args[1]));
	}
    }
}
