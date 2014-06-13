// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDBasicValidator.java,v 1.7 2001/06/26 16:40:15 wintonn Exp $

package zebedee;

import java.net.*;
import java.util.*;
import java.io.*;

/**
 * Basic {@link ZBDValidator} implementation. This is compatible with the
 * "C" version of Zebedee. It validates identities on the basis of the
 * contents of identity files and targets from supplied lists of allowable
 * target host and port combinations.
 */

public class ZBDBasicValidator extends ZBDValidator
{
    private LinkedList idFileList = new LinkedList();
    private ZBDTargetInfoList targetList = new ZBDTargetInfoList();
    private ZBDTargetInfoList peerList = new ZBDTargetInfoList();

    /**
     * Generate an identity hash value, as used in Zebedee identity
     * files. The lowercase ASCII string values of generator, modulus
     * and key are hashed together and the resultant string value is
     * returned. If either the generator or the modulus value is an empty
     * strings (<code>""</code>) then the default values is used
     * in its place (see {@link ZBDTunnel#DFLT_GENERATOR} and
     * {@link ZBDTunnel#DFLT_MODULUS}).
     *
     * @param gen The Diffie-Hellman generator value as a hexadecimal string.
     * @param mod The Diffie-Hellman modulus value as a hexadecimal string.
     * @param key The Diffie-Hellman public key as a hexadecimal string.
     */

    public static String identityHash(String gen, String mod, String key)
    {
	if (gen.equals(""))
	{
	    gen = ZBDTunnel.DFLT_GENERATOR;
	}
	if (mod.equals(""))
	{
	    mod = ZBDTunnel.DFLT_MODULUS;
	}

	gen = canonicalize(gen);
	mod = canonicalize(mod);
	key = canonicalize(key);

	ZBDHash sha = new ZBDHash();

	byte[] ascGen = null;
	byte[] ascMod = null;
	byte[] ascKey = null;

	sha.init();

	try
	{
	    ascGen = gen.getBytes("ASCII");
	    ascMod = mod.getBytes("ASCII");
	    ascKey = key.getBytes("ASCII");
	}
	catch (Exception e)
	{
	    // Can not happen -- ASCII is guaranteed
	}

	sha.init();
	sha.update(ascGen);
	sha.update(ascMod);
	sha.update(ascKey);
	sha.finish();

	byte[] digest = sha.digest();

	return ZBDTunnel.bytesToHex(digest);
    }

    /**
     * Canonicalize a hexadecimal string. Converts to lowercase and purges
     * all whitespace.
     *
     * @param hex The hexadecimal string.
     */

    static String canonicalize(String hex)
    {
	StringBuffer sb = new StringBuffer(hex.length());
	for (int i = 0; i < hex.length(); i++)
	{
	    if (!Character.isWhitespace(hex.charAt(i)))
	    {
		sb.append(Character.toLowerCase(hex.charAt(i)));
	    }
	}

	return sb.toString();
    }

    /**
     * Validates the identity against those held in the files previously
     * registered using {@link #addIdFile(String)}. If no files have been
     * registered then the routine will return <code>true</code> otherwise
     * it will only return <code>true</code> if the identity matches one
     * of those found in the registered files. Any errors in opening or
     * reading files are silently ignored.
     *
     * @param gen The Diffie-Hellman generator value as a hexadecimal string.
     * @param mod The Diffie-Hellman modulus value as a hexadecimal string.
     * @param key The Diffie-Hellman public key as a hexadecimal string.
     */

    public boolean validateIdentity(String gen, String mod, String key)
    {
	// If there are no ID files then always return true.

	if (idFileList.size() == 0)
	{
	    return true;
	}

	// Work out the identity hash.

	String id = identityHash(gen, mod, key);
	boolean found = false;

	// Now work through the list of files. Read each line and
	// see if it starts with the generated identity hash value.

	ListIterator iter = idFileList.listIterator();
	while (iter.hasNext() && !found)
	{
	    String file = (String)iter.next();
	    BufferedReader in = null;
	    try
	    {
		in = new BufferedReader(new FileReader(file));

		String line;
		while ((line = in.readLine()) != null)
		{
		    if (line.startsWith(id))
		    {
			found = true;
			break;
		    }
		}
	    }
	    catch (Exception e)
	    {
		// Ignore
	    }
	    finally
	    {
		try
		{
		    in.close();
		}
		catch (Exception ec)
		{
		    // Ignore
		}
	    }
	}

	return found;
    }

    /**
     * Validates the peer network address. This is checks that the address
     * and port associated with the supplied socket match an entry from
     * the peerList, if this is non-empty.
     *
     * @param sock The tunnel connection socket.
     */

    public boolean validatePeer(Socket sock)
    {
	if (peerList.isEmpty())
	{
	    return true;
	}

	ListIterator iter = peerList.listIterator();
	InetAddress peerAddr = sock.getInetAddress();
	int port = sock.getPort();
System.out.println("validating " + peerAddr + ":" + port);

	while (iter.hasNext())
	{
	    ZBDTargetInfoList.TargetInfo elem = (ZBDTargetInfoList.TargetInfo)(iter.next());

	    // Check port range first -- it is a quicker operation

	    if (elem.containsTo(port))
	    {
		// The port is within range, now match addresses

		InetAddress[] targetAddrs;
		try
		{
		    targetAddrs = InetAddress.getAllByName(elem.getTarget());
		}
		catch (Exception e)
		{
		    // Skip to next entry
		    continue;
		}

		for (int i = 0; i < targetAddrs.length; i++)
		{
		    byte[] pb = peerAddr.getAddress();
		    byte[] tb = targetAddrs[i].getAddress();

		    long pl = ((pb[0] << 24) |
			       (pb[1] << 16) |
			       (pb[2] <<  8) |
			       (pb[3] <<  0));
		    long tl = ((tb[0] << 24) |
			       (tb[1] << 16) |
			       (tb[2] <<  8) |
			       (tb[3] <<  0));
System.out.println("comparing " + Long.toString(pl, 16) +
" and " + Long.toString(tl, 16) + " with mask " + Long.toString(elem.mask(), 16));

		    if ((pl & elem.mask()) == (tl & elem.mask()))
		    {
			// We have a match!

			return true;
		    }
		}
	    }
	}

	// No matches

	return false;
    }

    public boolean validateTarget(String host, int port, boolean udpMode)
    {
	ListIterator iter = targetList.listIterator();
	InetAddress[] hostAddrs;
	try
	{
	    hostAddrs = InetAddress.getAllByName(host);
	}
	catch (Exception e)
	{
	    return false;
	}

	while (iter.hasNext())
	{
	    ZBDTargetInfoList.TargetInfo elem = (ZBDTargetInfoList.TargetInfo)(iter.next());

	    // Check port range first -- it is a quicker operation

	    if (elem.containsTo(port))
	    {
		// The port is within range, now match addresses

		InetAddress[] targetAddrs;
		try
		{
		    targetAddrs = InetAddress.getAllByName(elem.getTarget());
		}
		catch (Exception e)
		{
		    // Skip to next entry
		    continue;
		}

		for (int i = 0; i < hostAddrs.length; i++)
		{
		    for (int j = 0; j < targetAddrs.length; j++)
		    {
			byte[] hb = hostAddrs[i].getAddress();
			byte[] tb = targetAddrs[j].getAddress();

			long hl = ((hb[0] << 24) |
				   (hb[1] << 16) |
				   (hb[2] <<  8) |
				   (hb[3] <<  0));
			long tl = ((tb[0] << 24) |
				   (tb[1] << 16) |
				   (tb[2] <<  8) |
				   (tb[3] <<  0));

			if ((hl & elem.mask()) == (tl & elem.mask()))
			{
			    // We have a match!

			    return true;
			}
		    }
		}
	    }
	}

	// No matches

	return false;
    }

    public void addIdFile(String name)
    {
	idFileList.add((Object)name);
    }

    public void addTarget(String target) throws ZBDParseException
    {
	if (target.equals("*"))
	{
	    targetList.add("0.0.0.0/0");
	}
	else
	{
	    targetList.add(target);
	}
    }

    /**
     * Retrieve the name of the default target host. This is the last
     * named target system (or null if no targets have been
     * added).
     */

    public String defaultTarget()
    {
	int size = targetList.size();
	if (size == 0)
	{
	    return null;
	}
	else
	{
	    return targetList.get(size - 1).getTarget();
	}
    }

    /**
     * Add a peer address specification.
     */

    public void addPeer(String peer) throws ZBDParseException
    {
	if (peer.equals("*"))
	{
	    peerList.add("0.0.0.0/0");
	}
	else
	{
	    peerList.add(peer);
	}
    }
}

