// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDNullValidator.java,v 1.4 2001/06/21 14:53:50 wintonn Exp $

package zebedee;

import java.net.Socket;

/**
 * Null implementation of {@link ZBDValidator}. All methods always return
 * <code>true</code>.
 */

public class ZBDNullValidator extends ZBDValidator
{
    public boolean validateIdentity(String gen, String mod, String key)
    {
	return true;
    }

    public boolean validatePeer(Socket sock)
    {
	return true;
    }

    public boolean validateTarget(String addr, int port, boolean udpMode)
    {
	return true;
    }
}
