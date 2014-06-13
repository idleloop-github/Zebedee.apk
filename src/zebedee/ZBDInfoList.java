// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDInfoList.java,v 1.5 2001/06/26 16:40:15 wintonn Exp $

package zebedee;

import java.util.*;

class ZBDInfoList
{
    LinkedList list;

    public class Info
    {
	int[] to = new int[2];
	String target;


	Info(String target, int[] to)
	{
	    if (to[0] > to[1])
	    {
		int tmp = to[0];
		to[0] = to[1];
		to[1] = tmp;
	    }

	    this.to = to;
	    this.target = target;
	}

	public int count()
	{
	    return (to[1] - to[0] + 1);
	}

	public boolean containsTo(int port)
	{
	    return (port >= to[0] && port <= to[1]);
	}

	public String getTarget()
	{
	    return target;
	}

	public int[] getTo()
	{
	    return to;
	}
    }


    ZBDInfoList()
    {
	list = new LinkedList();
    }

    int[][] parseRange(String range) throws ZBDParseException
    {
	StringTokenizer st = new StringTokenizer(range, ", \t");
	int num = st.countTokens();

	// Now we know how many array elements there will be

	int[][] ret = new int[num][2];

	for (int i = 0; i < num; i++)
	{
	    int lo = -1;
	    int hi = -1;
	    String s = st.nextToken();

	    // If it contains a "-" then this string could be a numeric
	    // range -- but it could also be a port name that contains
	    // a hyphen ...

	    int index = s.indexOf('-');
	    if (index == 0 || index == s.length() - 1)
	    {
		throw new ZBDParseException("invalid port range: " + range);
	    }

	    if (index != -1 && Character.isDigit(s.charAt(0)))
	    {
		// Two integer port values

		try
		{
		    lo = Integer.parseInt(s.substring(0, index));
		    hi = Integer.parseInt(s.substring(index + 1));
		}
		catch (NumberFormatException e)
		{
		    throw new ZBDParseException("invalid port range: " + range);
		}
	    }
	    else
	    {
		// Should be a single port name or number

		int port = ZBDServiceLookup.lookup(s, "tcp");
		if (port == -1)
		{
		    // Try UDP name if TCP failed
		    port = ZBDServiceLookup.lookup(s, "udp");
		}

		if (port == -1)
		{
		    try
		    {
			port = Integer.parseInt(s);
		    }
		    catch (NumberFormatException e)
		    {
			throw new ZBDParseException("invalid port specification: " + range);
		    }
		}

		lo = hi = port;
	    }
	    ret[i][0] = lo;
	    ret[i][1] = hi;
	}
	return ret;
    }

    public int size()
    {
	return list.size();
    }

    public Info get(int index)
    {
	return (Info)(list.get(index));
    }

    public ListIterator listIterator()
    {
	return list.listIterator();
    }

    public boolean isEmpty()
    {
	return list.isEmpty();
    }
}
