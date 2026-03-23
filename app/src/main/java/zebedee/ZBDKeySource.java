// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDKeySource.java,v 1.1 2001/06/29 10:42:29 wintonn Exp $

package zebedee;

import java.security.SecureRandom;

public class ZBDKeySource
{
    SecureRandom rand = new SecureRandom();

    public ZBDKeySource()
    {
	// Kick off a thread to initialise the SecureRandom generator

	new Thread() {
	    public void run() { rand.nextBytes(new byte[1]); }
	}.start();
    }

    public synchronized String generateKeyString()
    {
	byte[] key = new byte[20];
	rand.nextBytes(key);
	return ZBDTunnel.bytesToHex(key);
    }
}
