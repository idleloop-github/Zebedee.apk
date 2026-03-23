// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDHash.java,v 1.3 2001/06/21 14:53:49 wintonn Exp $

package zebedee;

// Almost but not quite SHA1 ...
//
// $Id: ZBDHash.java,v 1.3 2001/06/21 14:53:49 wintonn Exp $

public class ZBDHash extends SHA1
{
    final int blk(int i) {

// NDW
// The following definition is correct for compliance with the SHA1 FIPS
// standard ... however, it is not quite the same as that used by the C
// version of Zebedee.
//
//        block[i&15] = rol(block[(i+13)&15]^block[(i+8)&15]^
//                          block[(i+2)&15]^block[i&15], 1);
//
// This is the Zebedee-compatible version, note the absence of the rol()
// call. This was apparently added by NIST before the FIPS was published
// but after SHA had appeared in Applied Cryptography ...

        block[i&15] = (block[(i+13)&15]^block[(i+8)&15]^
                          block[(i+2)&15]^block[i&15]);
        return (block[i&15]);
    }
}
