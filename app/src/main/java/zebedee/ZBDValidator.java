// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDValidator.java,v 1.4 2001/06/21 14:53:56 wintonn Exp $

package zebedee;

import java.net.Socket;

/**
 * This abstract base class defines the Zebedee tunnel connection
 * validation operations. A {@link ZBDTunnel} instance calls on the
 * validation methods of a <code>ZBDValidator</code> instance in order
 * to decide whether to proceed at different points in the protocol
 * exchange. All of the methods return a boolean value to indicate
 * whether validation has been successful and the connection should
 * proceed (<code>true</code>) or if it has failed and should be
 * terminated (<code>false</code>).
 */

abstract public class ZBDValidator
{
    /**
     * Validates the peer identity. This method examines the current
     * Diffie-Hellman generator, modulus and public key received
     * from the peer and determines whether this combination of values
     * corresponds to a known source. The premise is that for a given
     * generator and modulus the (public) key value can be generated
     * only by a party knowing the corresponding private key (exponent).
     *
     * @param generator The Diffie-Hellman generator as a hexadecimal string.
     * @param modulus The Diffie-Hellman modulus as as hexadecimal string.
     * @param key The public key value received from the peer.
     */
    abstract public boolean validateIdentity(String generator, String modulus, String key);

    /**
     * Validates the connection's peer address. This provides for a loose
     * form of validation based on the network address of the peer. This
     * may be obtained from the supplied socket.
     *
     * @param sock The socket for the established connection.
     */

    abstract public boolean validatePeer(Socket sock);

    /**
     * Validates the requested target address and port. This is a server-side
     * only function.
     *
     * @param addr The target host address in string form.
     * @param port The target port.
     * @param udpMode True if a UDP-mode connection, false if TCP.
     */

    abstract public boolean validateTarget(String addr, int port, boolean udpMode);
}
