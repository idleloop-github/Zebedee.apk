// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDTargetInfoList.java,v 1.5 2001/06/26 16:40:16 wintonn Exp $

package zebedee;

import java.util.*;

public class ZBDTargetInfoList extends ZBDInfoList
{
    class TargetInfo extends ZBDInfoList.Info
    {
	int maskBits = 32;


	public TargetInfo(String target, int[] to, int bits)
	{
	    super(target, to);
	    maskBits = bits;
	}

	public long mask()
	{
	    return ((0xffffffffL << (32 - maskBits)) & 0xffffffffL);
	}
    }


    public ZBDTargetInfoList()
    {
	super();
    }

    public void add(String targetSpec) throws ZBDParseException
    {
	StringTokenizer st = new StringTokenizer(targetSpec, ":/", true);
	String toRange = null;
	String target = null;
	String mask = null;

	switch (st.countTokens())
	{
	case 1:
	    target = targetSpec;
	    mask = null;
	    toRange = "1-65535"; // => any port
	    break;

	case 3:
	    target = st.nextToken();
	    if (st.nextToken().equals(":"))
	    {
		// target:ports
		mask = null;
		toRange = st.nextToken();
	    }
	    else
	    {
		// target/mask
		mask = st.nextToken();
		toRange = "1-65535";
	    }
	    break;

	case 5:
	    target = st.nextToken();
	    String d1 = st.nextToken();
	    mask = st.nextToken();
	    String d2 = st.nextToken();
	    toRange = st.nextToken();
	    if (d1.equals("/") && !d2.equals(":"))
	    {
		// Only break if the delimiter characters were correct
		break;
	    }
	    // Otherwise fall through ...

	default:
	    throw new ZBDParseException("invalid target specification: " + targetSpec);
	}

	int[][] to = parseRange(toRange);
	
	int maskBits = 32;
	if (mask != null)
	{
	    try
	    {
		maskBits = Integer.parseInt(mask);
		if (maskBits < 0 || maskBits > 32) throw new Exception();
	    }
	    catch (Exception e)
	    {
		throw new ZBDParseException("invalid target address mask: " + target + "/" + mask);
	    }
	}

	for (int i = 0; i < to.length; i++)
	{
	    list.add((Object)(new TargetInfo(target, to[i], maskBits)));
	}
    }

    public void add(String target, int[] to, int maskBits)
    {
	list.add((Object)(new TargetInfo(target, to, maskBits)));
    }
}
