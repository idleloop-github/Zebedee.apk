// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDValidationException.java,v 1.3 2001/06/21 14:53:56 wintonn Exp $

package zebedee;

// package zebedee;

public class ZBDValidationException extends ZBDException {

    public ZBDValidationException() {
	super();
    }

    public ZBDValidationException(String msg) {
	super(msg);
    }
}
