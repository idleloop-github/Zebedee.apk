// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDTunnelInfoList.java,v 1.5 2001/06/21 14:53:55 wintonn Exp $

package zebedee;

import java.util.*;

public class ZBDTunnelInfoList extends ZBDInfoList
{
    public class TunnelInfo extends ZBDInfoList.Info
    {
	int[] from = new int[2];

	TunnelInfo(int[] from, String target, int[] to)
	{
	    super(target, to);

	    if (from[0] > from[1])
	    {
		int tmp = from[0];
		from[0] = from[1];
		from[1] = tmp;
	    }

	    this.from = from;
	}

	public int[] getFrom()
	{
	    return from;
	}

	public boolean containsFrom(int port)
	{
	    return (port >= from[0] && port <= from[1]);
	}

	public int mapPort(int port)
	{
	    if (port >= from[0] && port <= from[0])
	    {
		return (to[0] + (from[1] - port));
	    }

	    return -1;
	}
    }

    /**
     * Default constructor.
     */

    public ZBDTunnelInfoList()
    {
	super();
    }

    public void add(String tunnelSpec) throws ZBDParseException
    {
	StringTokenizer st = new StringTokenizer(tunnelSpec, ":");
	String fromRange = null;
	String toRange = null;
	String target = null;


	switch (st.countTokens())
	{
	case 1:
	    fromRange = "0";
	    toRange = "telnet";	// Default destination
	    target = tunnelSpec;
	    break;

	case 2:
	    fromRange = "0";
	    target = st.nextToken();
	    toRange = st.nextToken();
	    break;

	case 3:
	    fromRange = st.nextToken();
	    target = st.nextToken();
	    toRange = st.nextToken();
	    break;

	default:
	    throw new ZBDParseException("invalid tunnel specification: " + tunnelSpec);
	}

	int[][] from = parseRange(fromRange);
	int[][] to = parseRange(toRange);

	if (from.length != to.length)
	{
	    throw new ZBDParseException("port ranges must be the same length");
	}

	add(from, target, to);
    }

    public void add(int[][] from, String target, int[][] to)
    {
	for (int i = 0; i < from.length; i++)
	{
	    list.add((Object)(new TunnelInfo(from[i], target, to[i])));
	}
    }

    int findTargetPort(int port)
    {
	if (port == -1)
	{
	    return -1;
	}

	ListIterator iter = list.listIterator();

	while (iter.hasNext())
	{
	    TunnelInfo elem = (TunnelInfo)(iter.next());
	    int targetPort = elem.mapPort(port);
	    if (targetPort != -1)
	    {
		return targetPort;
	    }
	}

	return -1;
    }

    String findTargetHost(int port)
    {
	if (port == -1)
	{
	    return null;
	}

	ListIterator iter = list.listIterator();

	while (iter.hasNext())
	{
	    TunnelInfo elem = (TunnelInfo)(iter.next());
	    if (elem.containsFrom(port))
	    {
		return elem.getTarget();
	    }
	}

	return null;
    }
}
