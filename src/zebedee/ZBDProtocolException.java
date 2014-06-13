// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDProtocolException.java,v 1.3 2001/06/21 14:53:51 wintonn Exp $

package zebedee;

// package zebedee;

public class ZBDProtocolException extends ZBDException {

    public ZBDProtocolException() {
	super();
    }

    public ZBDProtocolException(String msg) {
	super(msg);
    }
}
