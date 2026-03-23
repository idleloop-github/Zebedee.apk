// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDException.java,v 1.4 2001/06/21 14:53:48 wintonn Exp $

package zebedee;

// package zebedee;

public class ZBDException extends Exception
{
    public ZBDException()
    {
	super();
    }

    public ZBDException(String msg)
    {
	super(msg);
    }
}
