// Copyright (C) 2001, Internet Designers Ltd. All rights reserved.
// This program is free software and may be distributed under the terms
// of the GNU Lesser General Public License, Version 2.1.
// This program comes with ABSOLUTELY NO WARRANTY.
//
// Neil Winton (neil.winton@idl-bt.com)
//
// $Id: ZBDTunnel.java,v 1.15 2001/10/10 08:59:44 wintonn Exp $

package zebedee;

import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.security.SecureRandom;
import java.math.*;

/**
 * The base class for client and server-side Zebedee tunnel objects. This also
 * contains definitions of most of the core constants used in the Zebedee
 * protocol.
 * <p>
 * All of the package-accessible instance variables which are intended to
 * be manipulated after instantiation have accessor methods defined. Note
 * that before protocol negotiation these instance variables generally hold
 * the preferred or requested values. After negotiation has completed they
 * hold the negotiated values.
 */

public class ZBDTunnel implements Cloneable
{
    // Public constants

    /** The default Diffie-Hellman generator value */
    public static final String DFLT_GENERATOR = "2";
    /** The default Diffie-Hellman modulus value */
    public static final String DFLT_MODULUS =		// Default modulus value
	"f488fd584e49dbcd20b49de49107366b336c380d451d0f7c88b31c7c5b2d8ef6" +
	"f3c923c043f0a55b188d8ebb558cb85d38d334fd7c175743a31d186cde33212c" +
	"b52aff3ce1b1294018118d7c84a70a72d686c40319c807297aca950cd9969fab" +
	"d00a509b0246d3083d66a45d419f9c7cbd894b221926baaba25ec355e92f78c7";

    /** Default compression type and level. Equivalent to "zlib:6". */
    public static final int DFLT_COMPRESSION = 0x06;
    /** Default key size, in bits. Set tp 128 bits. */
    public static final int DFLT_KEY_SIZE = 128;
    /** Default port on which a TCP-mode server listens. Set to 11965 (0x2ebd). */
    public static final int DFLT_TCP_PORT = 0x2EBD;
    /** Default port on which a UDP-mode server listens. Set to 11230 (0x2bde) */
    public static final int DFLT_UDP_PORT = 0x2BDE;
    /** Default maximum size for messages. Set to 8192. */
    public static final int DFLT_BUFFER_SIZE = 8192;
    /** Default shared key lifetime in seconds. Set to 3600 seconds (1 hour) */
    public static final int DFLT_KEY_LIFETIME = 3600;
    /** Default idle connection timeout in seconds. Set to 0 (infinite). */
    public static final int DFLT_IDLE_TIMEOUT = 0;

    /** Maximum key size in bits. Set to 576 bits. */
    public static final int MAX_KEY_SIZE = 576;
    /** Maximum compression level supported. Equivalent to "zlib:9". */
    public static final int MAX_COMPRESSION = 0x09;
    /** Maximum idle connection timeout in seconds. Set to 65535 seconds. */
    public static final int MAX_IDLE_TIMEOUT = 65535;
    /** Maximum size for messages. Set to 16383. */
    public static final int MAX_BUFFER_SIZE = 16383;
    /** Maximum shared key lifetime, in seconds. Set to 65535 seconds. */
    public static final int MAX_KEY_LIFETIME = 65535;
    /** Maximum length of generator or modulus strings. Set to 1024 characters. */
    public static final int MAX_NUM_LENGTH = 1024;

    /** Zlib compression type */
    public static final int COMPRESSION_ZLIB = 0x0;
    /** Bzip2 compression type */
    public static final int COMPRESSION_BZIP2 = 0x1;

    // Package-wide constants
    // Only classes implementing the protocol need to know these!

    static final int PROTOCOL_V201 = 0x0201;

    static final int MAX_HDR_SIZE = 26; // For protocol version 201
    static final int HDR_FLAG_UDPMODE = 0x1;
    static final int NONCE_SIZE = 8;
    static final int FLAG_COMPRESSED = 0x4000;
    static final int FLAG_ENCRYPTED = 0x8000;
    static final int SIZE_MASK = 0xffff ^ (FLAG_ENCRYPTED | FLAG_COMPRESSED);
    static final int CMP_MINIMUM = 32;
    static final int CMP_OVERHEAD = 250;
    static final int CHALLENGE_SIZE = 4;
    static final int THE_ANSWER = 42;	// Of course!

    // The following instance variables should be immutable after
    // the constructor has been called.

    String serverHost = null;
    int serverPort = -1;
    int protocol = PROTOCOL_V201;

    // The following can be changed after construction.

    boolean tcpMode = true;
    boolean udpMode = false;
    String generator = "";  // "" => default value
    String modulus = "";    // "" => default value
    int compressionInfo = DFLT_COMPRESSION;
    int keySize = DFLT_KEY_SIZE;
    int minKeySize = 0;
    int keyLifetime = DFLT_KEY_LIFETIME;
    int idleTimeout = DFLT_IDLE_TIMEOUT;
    int bufferSize = DFLT_BUFFER_SIZE;
    String privateKey = null;

    // The default (null) message logger
    ZBDLogger logger = new ZBDNullLogger();

    // The default (null) request validator
    ZBDValidator validator = new ZBDNullValidator();

    // The default key source
    ZBDKeySource keySource;

    // The token table is maintained automatically
    ZBDTokenTable tokens;

    // Other stuff ...

    // Set true once tunnel has been connected, false on read/write
    // failures
    boolean readOK = true;
    boolean writeOK = true;

    // The socket for the tunnel connection
    Socket tunnelSocket;

    // Tunnel data i/o streams
    DataInputStream dataIn;
    DataOutputStream dataOut;

    // Client and server nonce values
    byte[] clientNonce = new byte[NONCE_SIZE];
    byte[] serverNonce = new byte[NONCE_SIZE];

    // Blowfish encryption contexts for input and output
    ZBDBlowfish bfIn;
    ZBDBlowfish bfOut;

    // Blowfish initialisation vector
    byte[] initVec = new byte[] {
	0x54, 0x69, 0x6d, 0x65,	    // Time4Bed
	0x34, 0x42, 0x65, 0x64	    // said Zebedee ...
    };

    // Zlib compressor/uncompressor
    Inflater inflater;
    Deflater deflater;

    // Statistics
    int readCount = 0;
    int writeCount = 0;
    int rawBytesIn = 0;
    int rawBytesOut = 0;
    int msgBytesIn = 0;
    int msgBytesOut = 0;

    // Default message read and temporary working buffers
    byte[] message = new byte[MAX_BUFFER_SIZE];
    byte[] rawIn = new byte[MAX_BUFFER_SIZE];
    byte[] rawOut = new byte[MAX_BUFFER_SIZE + 2];

    ZBDInputStream inputStream = null;
    ZBDOutputStream outputStream = null;

    /**
     * Default constructor. This allocate a new token table and
     * uses all other default values.
     */

    public ZBDTunnel()
    {
	tokens = new ZBDTokenTable();
	keySource = new ZBDKeySource();
    }

    /**
     * Create an instance by cloning the given <code>master</code> object.
     * This takes an existing <code>ZBDTunnel</code> and copies the most
     * important fields. The primary reasons for creating an object in
     * this way are to shared common configuration (such as key length)
     * for multiple connections and, more importantly, to allow the
     * reusable shared key information to be shared for multiple tunnels
     * to a single server.
     */

    public ZBDTunnel(ZBDTunnel master)
    {
	synchronized (master)
	{
	    // The following values are copied and should
	    // not be reset. If they were to be so they would
	    // invalidate the usefulness of the shared key table.

	    serverHost = master.serverHost;
	    serverPort = master.serverPort;
	    tcpMode = master.tcpMode;
	    udpMode = master.udpMode;
	    protocol = master.protocol;

	    // The following values may be reset, if desired

	    generator = master.generator;
	    modulus = master.modulus;
	    compressionInfo = master.compressionInfo;
	    keySize = master.keySize;
	    keyLifetime = master.keyLifetime;
	    idleTimeout = master.idleTimeout;
	    bufferSize = master.bufferSize;
	    privateKey = master.privateKey;
	    logger = master.logger;
	    validator = master.validator;
	    keySource = master.keySource;

	    // The token table (which is associated with a specific server)
	    // is shared, not duplicated.

	    tokens = master.tokens;
	}
    }

    /**
     * Clone a <code>ZBDTunnel</code>. See {@link #ZBDTunnel(ZBDTunnel)} for
     * further details.
     */

    public Object clone()
    {
	return new ZBDTunnel(this);
    }

    //
    // Variable accessor routines
    //

    /**
     * Sets TCP-mode handling.
     */

    synchronized public boolean setTcpMode(boolean onOff)
    {
	tcpMode = onOff;
	return tcpMode;
    }

    /**
     * Returns the current TCP-mode.
     */

    public boolean getTcpMode()
    {
	return tcpMode;
    }

    /**
     * Sets UDP-mode handling.
     */

    synchronized public boolean setUdpMode(boolean onOff)
    {
	udpMode = onOff;
	return udpMode;
    }

    /**
     * Returns the current UDP-mode.
     */

    public boolean getUdpMode()
    {
	return udpMode;
    }

    /**
     * Return the current server host.
     */

    public String getServerHost()
    {
	return serverHost;
    }

    /**
     * Return the current server port.
     */

    public int getServerPort()
    {
	return serverPort;
    }

    /**
     * Sets the Diffie-Hellman generator value to the given string. The string
     * must consist soley of hexadecimal characters (a-f, A-F, 0-9).
     *
     * @param gen The generator string.
     *
     * @throws ZBDValueException This will be thrown if the string is too
     * long (greater than {@link #MAX_NUM_LENGTH}) or contains invalid
     * hexadecimal data.  As a special case the empty string
     * (<code>""</code>) represents the default generator value.
     *
     * @return The generator string.
     */

    synchronized public String setGenerator(String gen) throws ZBDValueException
    {
	// Check length

	if (gen.length() > MAX_NUM_LENGTH)
	{
	    throw new ZBDValueException("generator too long, must be <= " + MAX_NUM_LENGTH);
	}

	// Validate hex data

	try
	{
	    if (gen.length() > 0)
	    {
		hexToBytes(gen);
	    }
	}
	catch (ZBDException ez)
	{
	    throw new ZBDValueException("invalid generator value: " + ez);
	}

	generator = gen;
	return generator;
    }

    /**
     * Returns the Diffie-Hellman generator value. As a special case
     * the empty string (<code>""</code>) represents the default
     * generator value.
     */

    synchronized public String getGenerator()
    {
	return generator;
    }

    /**
     * Sets the Diffie-Hellman modulus value to the given string. The string
     * must consist soley of hexadecimal characters (a-f, A-F, 0-9).
     *
     * @param mod The modulus string.
     *
     * @throws ZBDValueException This will be thrown if the string is too
     * long (greater than {@link #MAX_NUM_LENGTH}) or contains invalid
     * hexadecimal data.  As a special case the empty string
     * (<code>""</code>) represents the default modulus value.
     *
     * @return The modulus string.
     */

    synchronized public String setModulus(String mod) throws ZBDValueException
    {
	// Check length

	if (mod.length() > MAX_NUM_LENGTH)
	{
	    throw new ZBDValueException("modulus too long, must be <= " + MAX_NUM_LENGTH);
	}

	// Validate hex data

	try
	{
	    if (mod.length() > 0)
	    {
		hexToBytes(mod);
	    }
	}
	catch (ZBDException ez)
	{
	    throw new ZBDValueException("invalid modulus value: " + ez);
	}

	modulus = mod;
	return modulus;
    }

    /**
     * Returns the Diffie-Hellman modulus value. As a special case
     * the empty string (<code>""</code>) represents the default
     * modulus value.
     */

    synchronized public String getModulus()
    {
	return modulus;
    }

    /**
     * Set the compression type and level. See also
     * {@link #makeCompressionValue(int,int)}.
     *
     * @param type The compression type.
     * @param level The compression level.
     *
     * @throws ZBDValueException See {@link #makeCompressionValue(int,int)}
     * for details.
     *
     * @return An integer representing the combined type and level.
     */

    synchronized public int setCompression(int type, int level) throws ZBDValueException
    {
	compressionInfo = makeCompressionValue(type, level);
	return compressionInfo;
    }

    /**
     * Returns the single integer combined compression value for the given
     * type and level. The type must be one of {@link #COMPRESSION_ZLIB} or
     * {@link #COMPRESSION_BZIP2} and the level between 0 and
     * {@link #MAX_COMPRESSION}.
     *
     * @param type The compression type.
     * @param level The compression level.
     *
     * @throws ZBDValueException This will be thrown of the compression
     * type is unknown or unsupported or the compression level is less
     * than 0 or greater than {@link #MAX_COMPRESSION}.
     */

    public static int makeCompressionValue(int type, int level) throws ZBDValueException
    {
	if (type == COMPRESSION_BZIP2)
	{
	    throw new ZBDValueException("bzip2 compression is not supported in this version");
	}
	else if (type != COMPRESSION_ZLIB)
	{
	    throw new ZBDValueException("invalid compression type: " + type);
	}
	else if (level < 0)
	{
	    throw new ZBDValueException("compression level less than zero (" + level + ")!");
	}
	else if (level > MAX_COMPRESSION)
	{
	    level = MAX_COMPRESSION;
	}

	return (type << 8) | level;
    }
	
    /**
     * Set compression type and level as a single value, such as that
     * returned by {@link #makeCompressionValue(int,int)}.
     *
     * @param value Compression value.
     *
     * @throws ZBDValueException This will be thrown if the value appears
     * to be invalid.
     *
     * @return The new compression value.
     */

    synchronized public int setCompression(int value) throws ZBDValueException
    {
	if ((value >> 8) == COMPRESSION_BZIP2)
	{
	    throw new ZBDValueException("bzip2 compression is not supported in this version");
	}
	else if ((value >> 8) != COMPRESSION_ZLIB)
	{
	    throw new ZBDValueException("invalid compression type: " + (value >> 8));
	}

	compressionInfo = value;
	return compressionInfo;
    }

    /**
     * Returns the current compression type and level as a single integer.
     */

    synchronized public int getCompression()
    {
	return compressionInfo;
    }

    /**
     * Returns the compression type (either {@link #COMPRESSION_ZLIB} or
     * {@link #COMPRESSION_BZIP2}) for the given compression value.
     *
     * @param value The compression value.
     */

    public int getCompressionType(int value)
    {
	return (value >> 8) & 0xf;
    }

    /**
     * Returns the compression level the given compression value.
     *
     * @param value The compression value.
     */

    public int getCompressionLevel(int value)
    {
	return (value & 0xf);
    }

    /**
     * Set the requested or maximum permitted key size in bits. A client
     * requests the use of a Blowfish encryption key this size and a
     * server will not permit the use of a key greater than this size.
     * <p>
     * If the supplied value is greater than {@link #MAX_KEY_SIZE} it will be
     * set to {@link #MAX_KEY_SIZE}. If it is less than 0 an exception
     * will be thrown.
     *
     * @param bits The key size, in bits.
     *
     * @throws ZBDValueException This will be thrown if the requested
     * key size is less than 0.
     *
     * @return The key size that has been set.
     */

    synchronized public int setKeySize(int bits) throws ZBDValueException
    {
	if (bits > MAX_KEY_SIZE)
	{
	    bits = MAX_KEY_SIZE;
	}
	else if (bits < 0)
	{
	    throw new ZBDValueException("key size less than zero (" + bits + ")!");
	}

	keySize = bits;
	return keySize;
    }

    /**
     * Retrieves the requested or maximum key size in bits.
     */

    synchronized public int getKeySize()
    {
	return keySize;
    }

    /**
     * Set the minimum acceptable key size in bits.
     * If the supplied value is greater than {@link #MAX_KEY_SIZE} it will be
     * set to {@link #MAX_KEY_SIZE}. If it is less than 0 an exception
     * will be thrown.
     *
     * @param bits The key size, in bits.
     *
     * @throws ZBDValueException This will be thrown if the specified
     * key size is less than 0.
     *
     * @return The key size that has been set.
     */

    synchronized public int setMinKeySize(int bits) throws ZBDValueException
    {
	if (bits > MAX_KEY_SIZE)
	{
	    bits = MAX_KEY_SIZE;
	}
	else if (bits < 0)
	{
	    throw new ZBDValueException("minimum key size less than zero (" + bits + ")!");
	}

	minKeySize = bits;
	return minKeySize;
    }

    /**
     * Retrieves the minimum key size in bits.
     */

    synchronized public int getMinKeySize()
    {
	return minKeySize;
    }

    /**
     * Sets the key lifetime. This sets the time in seconds after which
     * shared keys will be considered to have expired and must be
     * renegotiated.
     * <p>
     * If the supplied value is greater than {@link #MAX_KEY_LIFETIME}
     * it will be set to {@link #MAX_KEY_LIFETIME}. If it is less than 0
     * an exception will be thrown.
     *
     * @param life The key lifetime, in seconds.
     *
     * @throws ZBDValueException Thrown if the lifetime is less than zero.
     */

    synchronized public int setKeyLifetime(int life) throws ZBDValueException
    {
	// This is, in fact, a convenience wrapper around
	// the token table routine of the same name.

	return tokens.setKeyLifetime(life);
    }

    /**
     * Retrieves the current key lifetime.
     */

    synchronized public int getKeyLifetime()
    {
	// This is, in fact, a convenience wrapper around
	// the token table routine of the same name.

	return tokens.getKeyLifetime();
    }

    /**
     * Sets the idle connection timeout value.
     * This sets the time in seconds for which a tunnel connection
     * must be idle before it can be closed. A value of 0 indicates
     * an infinite timeout, that is, the connection will never be close
     * just because it is idle.
     * <p>
     * If the supplied value is greater than {@link #MAX_IDLE_TIMEOUT}
     * it will be set to {@link #MAX_IDLE_TIMEOUT}. If it is less than 0
     * an exception will be thrown.
     *
     * @param time The idle timeout, in seconds.
     *
     * @throws ZBDValueException Thrown if the timeout is less than zero.
     *
     * @return The timeout value set.
     */

    synchronized public int setIdleTimeout(int time) throws ZBDValueException
    {
	if (time < 0)
	{
	    throw new ZBDValueException("idle timeout less than zero (" + time + ")");
	}
	else if (time > MAX_IDLE_TIMEOUT)
	{
	    time = MAX_IDLE_TIMEOUT;
	}

	idleTimeout = time;
	return idleTimeout;
    }

    /**
     * Retrieves the idle timeout value.
     */

    synchronized public int getIdleTimeout()
    {
	return idleTimeout;
    }

    /**
     * Sets the requested or maximum permitted message buffer size.
     * If the supplied value is greater than {@link #MAX_BUFFER_SIZE}
     * it will be set to {@link #MAX_BUFFER_SIZE}. If it is less than 1
     * an exception will be thrown.
     *
     * @param size The buffer size, in bytes.
     *
     * @throws ZBDValueException Thrown if the size is less than 1.
     */

    synchronized public int setBufferSize(int size) throws ZBDValueException
    {
	if (size < 1)
	{
	    throw new ZBDValueException("buffer size less than one (" + size + ")");
	}
	else if (size > MAX_BUFFER_SIZE)
	{
	    size = MAX_BUFFER_SIZE;
	}

	bufferSize = size;
	return size;
    }

    /**
     * Retrieves the current buffer size.
     */

    synchronized public int getBufferSize()
    {
	return bufferSize;
    }

    /**
     * Sets the private key (the Diffie-Hellman exponent) value to the
     * given string. The string must consist soley of hexadecimal characters
     * (a-f, A-F, 0-9). If the key is null a new value will be generated
     * automatically, on demand.
     *
     * @param key The key string.
     *
     * @throws ZBDValueException This will be thrown if the string is too
     * long (greater than {@link #MAX_NUM_LENGTH}) or contains invalid
     * hexadecimal data.
     *
     * @return The key string.
     */
    
    synchronized public String setPrivateKey(String key) throws ZBDValueException
    {
	// Degenerate case -- key may be null.

	if (key == null)
	{
	    privateKey = null;
	    return null;
	}

	// Check length

	if (key.length() > MAX_NUM_LENGTH)
	{
	    throw new ZBDValueException("key too long, must be <= " + MAX_NUM_LENGTH);
	}

	// Validate hex data

	try
	{
	    if (key.length() > 0)
	    {
		hexToBytes(key);
	    }
	}
	catch (ZBDException ez)
	{
	    throw new ZBDValueException("invalid key value: " + ez);
	}

	privateKey = key;
	return privateKey;
    }

    /**
     * Retrieves the private key string.
     */

    synchronized public String getPrivateKey()
    {
	return privateKey;
    }

    /**
     * Sets the message logger instance. The <code>ZBDTunnel</code> class
     * and its sub-classes are instrumented to log varying amounts of
     * debugging and other information. They do this by calling methods
     * on an instance of a {@link ZBDLogger} class.
     * <p>
     * The default constructor uses a logger which does nothing (see
     * {@link ZBDNullLogger}). This routine can be used to provide an
     * implementation that does something more useful.
     *
     * @param log An instance of a <code>ZBDLogger</code> class.
     *
     * @return The logger being used.
     */

    synchronized public ZBDLogger setLogger(ZBDLogger log)
    {
	logger = log;
	return logger;
    }

    /**
     * Retrieves the current logger instance.
     */

    synchronized public ZBDLogger getLogger()
    {
	return logger;
    }

    /**
     * Sets the request validator instance. The <code>ZBDTunnel</code> class
     * and its sub-classes are can validate the target of tunnel requests,
     * the peer IP address and peer identity. They do this by calling methods
     * on an instance of a {@link ZBDValidator} class.
     * <p>
     * The default constructor uses a validator which permits all
     * requests (see {@link ZBDNullValidator}). This routine can be
     * used to provide an implementation that does something more useful.
     *
     * @param val An instance of a <code>ZBDValidator</code> class.
     *
     * @return The validator being used.
     */

    synchronized public ZBDValidator setValidator(ZBDValidator val)
    {
	validator = val;
	return validator;
    }

    /**
     * Retrieves the current validator instance.
     */

    synchronized public ZBDValidator getValidator()
    {
	return validator;
    }

    /**
     * Set the key source.
     */

    synchronized public ZBDKeySource setKeySource(ZBDKeySource source)
    {
	keySource = source;
	return source;
    }

    /**
     * Get the key source
     */

    synchronized public ZBDKeySource getKeySource()
    {
	return keySource;
    }

    /**
     * Creates a new hexadecimal session key string. This is done by
     * hashing together the client nonce, server nonce and a portion of
     * the ASCII string representation of the current shared secret key.
     *
     * @param sharedKey The shared Diffie-Hellman key as a hexadecimal string.
     *
     * @return The hexadecimal session key.
     */

    final String generateSessionKey(String sharedKey)
    {
	// Allocate space for key -- SHA hashes are 160 bits long
	// so we allocate multiples of 20 bytes.

	byte[] key = new byte[((keySize / 160) + 1) * 20];

	// Convert the key string to ASCII before hashing

	byte[] asciiKey = null;
	try
	{
	    asciiKey = sharedKey.getBytes("ASCII");
	}
	catch (UnsupportedEncodingException e)
	{
	    // Can not happen -- ASCII is guaranteed
	}
	int len = asciiKey.length;

	logger.log(999, "secret key = " + sharedKey);
	logger.log(999, "secret key length = " + len);
	logger.log(999, "client nonce = " + bytesToHex(clientNonce));
	logger.log(999, "server nonce = " + bytesToHex(serverNonce));

	// Now create the hash. Mix in the client and server nonces
	// with chunks of the shared secret key (as an ASCII string).

	ZBDHash sha = new ZBDHash();

	for (int bits = 0; bits < keySize; bits += 160)
	{
	    sha.init();
	    sha.update(clientNonce);
	    sha.update(serverNonce);
	    int nybbles = bits / 4;
	    if (nybbles > len)
	    {
		nybbles %= len;
	    }
	    sha.update(asciiKey, nybbles, (len - nybbles));

	    sha.finish();

	    byte[] digest = sha.digest();
	    System.arraycopy(digest, 0, key, bits / 8, digest.length);
	}

	// Return the final result as a String

	return bytesToHex(key);
    }

    /**
     * Generate a private key as a hex string. If a static string has
     * already been specified use that otherwise call on the keySource.
     *
     * @return A hexadecimal key string.
     */

    String generatePrivateKey() throws ZBDException
    {
	if (privateKey != null)
	{
	    return privateKey;
	}

	String key = keySource.generateKeyString();

	if (key == null)
	{
	    throw new ZBDException("failed to generate private key");
	}

	return key;
    }

    /**
     * Initialise the input and output Blowfish encryption streams using
     * the supplied key. The key is a hexadecimal string.
     *
     * @param stringKey A hexadecimal key string
     */

    final void setupBlowfish(String stringKey)
    {
	byte[] binData;
	try
	{
	    binData = hexToBytes(stringKey);
	}
	catch (ZBDException ez)
	{
	    return; // Things will fail gracefully higher up ... I hope ...
	}

	int bits = keySize;

	// Round key size up to multiple of 4

	bits = ((bits + 3) / 4) * 4;

	logger.log(999, "setting up Blowfish with a " + bits + " bit key");

	// Figure out how many bytes this is and allocate the key

	byte[] key = new byte[(bits + 7) / 8];

	// Copy in the data and then mask the least significant nybble
	// if necessary.

	System.arraycopy(binData, 0, key, 0, key.length);
	if ((bits % 8) != 0)
	{
	    key[key.length - 1] &= 0xf0;
	}

	logger.log(999, "key is " + bytesToHex(key));

	bfIn = new ZBDBlowfish(key, initVec);
	bfOut = new ZBDBlowfish(key, initVec);
    }

    /**
     * Retrieve the {@link ZBDInputStream} for this tunnel. If the tunnel has
     * been properly established this creates a new {@link ZBDInputStream}
     * instance wrapped around the tunnel socket, if it does not already.
     * exist. It then returns this instance.
     *
     * @throws ZBDNetworkException Thrown if an error occurs in creating
     * the stream.
     * @throws ZBDException Thrown if the tunnel has not yet been connected
     * or a prior read error has occurred.
     */

    public InputStream getInputStream() throws ZBDException
    {
	if (inputStream != null)
	{
	    return inputStream;
	}
	else if (!readOK)
	{
	    {
		throw new ZBDException("tunnel is not connected or a read error has occurred");
	    }
	}

	inputStream = new ZBDInputStream(this);
	return inputStream;
    }

    /**
     * Retrieve the {@link ZBDOutputStream} for this tunnel. If the tunnel has
     * been properly established this creates a new {@link ZBDOutputStream}
     * instance wrapped around the tunnel socket, if it does not already.
     * exist. It then returns this instance.
     *
     * @throws ZBDNetworkException Thrown if an error occurs in creating
     * the stream.
     * @throws ZBDException Thrown if the tunnel has not yet been connected or
     * a prior write error has occurred.
     */

    public OutputStream getOutputStream() throws ZBDException
    {
	if (outputStream != null)
	{
	    return outputStream;
	}
	else if (!writeOK)
	{
	    {
		throw new ZBDException("tunnel is not connected or a previous write error has occurred");
	    }
	}

	outputStream = new ZBDOutputStream(this);
	return outputStream;
    }

    /**
     * Utility function to convert a byte array to a hexadecimal string.
     *
     * @param bArray The byte array.
     *
     * @return A hexadecimal string.
     */

    static final public String bytesToHex(byte[] bArray)
    {
	return bytesToHex(bArray, 0, bArray.length);
    }

    /**
     * Utility function to convert a byte array to a hexadecimal string.
     *
     * @param bArray The byte array.
     * @param offset The offset at which to start conversion.
     * @param length The number of bytes to convert.
     *
     * @return A hexadecimal string.
     */

    static final public String bytesToHex(byte[] bArray, int offset, int length)
    {
	StringBuffer s = new StringBuffer(2 * length);
	char[] digits = new char[] {
	    '0', '1', '2', '3', '4', '5', '6', '7',
	    '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};

	for (int i = offset; i < offset + length && i < bArray.length; i++)
	{
	    s.append(digits[(bArray[i] >>> 4) & 0xf]).append(digits[(bArray[i] & 0xf)]);
	}

	return s.toString();
    }

    /**
     * Convert a hexadecimal string to a byte array. This is the inverse
     * of {@link #bytesToHex(byte[])}. The string must consist only of the
     * characters A-F, a-f and 0-9 and whitespace.
     *
     * @param s A hexadecimal string.
     *
     * @throws ZBDValueException Thrown if any invalid hexadecimal characters
     * are encountered.
     */

    static final public byte[] hexToBytes(String s) throws ZBDValueException
    {
	
	// Purge whitespace

	char[] ch = new char[s.length()];
	s.getChars(0, s.length(), ch, 0);
	StringBuffer sb = new StringBuffer(s.length());
	for (int i = 0; i < ch.length; i++)
	{
	    if (!Character.isWhitespace(ch[i]))
	    {
		sb.append(ch[i]);
	    }
	}

	// Pad to even number of characters

	if (sb.length() % 2 != 0)
	{
	    // Pad with a leading zero if the number of characters is odd.
	    sb.insert(0, "0");
	}

	byte[] b = new byte[sb.length() / 2];

	for (int i = 0; i < sb.length(); i += 2)
	{
	    b[i / 2] = 0;
	    for (int j = 0; j < 2; j++)
	    {
		switch (sb.charAt(i + j))
		{
		    case '0': b[i / 2] |= (0x0 << ((1 - j) * 4)); break;
		    case '1': b[i / 2] |= (0x1 << ((1 - j) * 4)); break;
		    case '2': b[i / 2] |= (0x2 << ((1 - j) * 4)); break;
		    case '3': b[i / 2] |= (0x3 << ((1 - j) * 4)); break;
		    case '4': b[i / 2] |= (0x4 << ((1 - j) * 4)); break;
		    case '5': b[i / 2] |= (0x5 << ((1 - j) * 4)); break;
		    case '6': b[i / 2] |= (0x6 << ((1 - j) * 4)); break;
		    case '7': b[i / 2] |= (0x7 << ((1 - j) * 4)); break;
		    case '8': b[i / 2] |= (0x8 << ((1 - j) * 4)); break;
		    case '9': b[i / 2] |= (0x9 << ((1 - j) * 4)); break;
		    case 'a': case 'A': b[i / 2] |= (0xa << ((1 - j) * 4)); break;
		    case 'b': case 'B': b[i / 2] |= (0xb << ((1 - j) * 4)); break;
		    case 'c': case 'C': b[i / 2] |= (0xc << ((1 - j) * 4)); break;
		    case 'd': case 'D': b[i / 2] |= (0xd << ((1 - j) * 4)); break;
		    case 'e': case 'E': b[i / 2] |= (0xe << ((1 - j) * 4)); break;
		    case 'f': case 'F': b[i / 2] |= (0xf << ((1 - j) * 4)); break;
		    default: 
			throw new ZBDValueException("invalid hexadecimal digit: " + ch[i + j]);
		}
	    }
	}

	return b;
    }

    /**
     * Read a potentially compressed and encrypted message from the
     * tunnel into the supplied buffer. The buffer must be large enough
     * to accept any message that arrives. This means that it must be
     * at least {@link #MAX_BUFFER_SIZE} bytes in length.
     *
     * @param msgBuf The message buffer byte array.
     *
     * @throws ZBDNetworkException Thrown on encountering any kind of
     * problem in reading from the network. This includes premature EOF and
     * connection idle timeout.
     * @throws ZBDProtocolException Thrown if invalid data is detected.
     * @throws ZBDException Thrown on any other error.
     *
     * @return The number of bytes read or -1 on end of file.
     */

    final int readMessage(byte[] msgBuf) throws ZBDException
    {
	if (!readOK)
	{
	    return -1;
	}

	// Read the header -- a short integer

	int header = 0;

	try
	{
	    header = dataIn.readShort();
	}
	catch (EOFException eof)
	{
	    return -1;
	}
	catch (InterruptedIOException eint)
	{
	    readOK = false;
	    throw new ZBDTimeoutException("connection timed out while reading data");
	}
	catch (IOException eio)
	{
	    readOK = false;
	    throw new ZBDNetworkException("IO error reading message header: " + eio);
	}

	logger.log(5, "message header = " + header);

	rawBytesIn += 2;

	// Determine whether encrypted and compressed

	boolean compressed = ((header & FLAG_COMPRESSED) == FLAG_COMPRESSED);
	boolean encrypted = ((header & FLAG_ENCRYPTED) == FLAG_ENCRYPTED);

	// Extract the message payload size

	int size = header & SIZE_MASK;

	logger.log(5, "reading a " + size + " byte message, " +
	     (encrypted ? "" : "un") + "encrypted, " +
	     (compressed ? "" : "un") + "compressed");

	// Some sanity checks ...

// You can validly get "oversize" messages during the initial protocol
// exchange -- for example the DH key value.
//
//	if (size > bufferSize)
//	{
//	    throw new ZBDProtocolException("incoming message size (" + size +
//					   ") greater than maximum buffer size (" +
//					   bufferSize + ")");
//	}

	if (size > msgBuf.length)
	{
	    throw new ZBDProtocolException("incoming message size (" + size +
					   ") greater than buffer size (" +
					   msgBuf.length + ")");
	}

	// Now read the (potentially fragmented) message body. This is
	// assembled in the "rawIn" buffer.

	int needed = size;
	int offset = 0;
	try
	{
	    while (needed > 0)
	    {
		int num = dataIn.read(rawIn, offset, needed);
		if (num == -1)
		{
		    throw new ZBDNetworkException("EOF encountered reading message of size " + size);
		}
		needed -= num;
		offset += num;
	    }
	}
	catch (InterruptedIOException eint)
	{
	    readOK = false;
	    throw new ZBDTimeoutException("connection timed out while reading data");
	}
	catch (IOException eio)
	{
	    readOK = false;
	    throw new ZBDNetworkException("IO error while reading message");
	}

	readCount++;
	rawBytesIn += size;

	// Decrypt, if necessary

	if (encrypted)
	{
	    if (bfIn == null)
	    {
		throw new ZBDProtocolException("encrypted messaged received without an encryption context established");
	    }

	    bfIn.cfb64Encrypt(rawIn, 0, msgBuf, 0, size, false);
	    System.arraycopy(msgBuf, 0, rawIn, 0, size);
	}

	// Uncompress, if necessary

	if (compressed)
	{
	    // FIX-ME
	    // Should check the compression type but we only handle Zlib
	    // so I will not bother for the moment ...

	    int uncmp = 0;
	    try
	    {
		inflater.setInput(rawIn, 0, size);
		uncmp = inflater.inflate(msgBuf);
		inflater.reset();
	    }
	    catch (DataFormatException edf)
	    {
		readOK = false;
		throw new ZBDProtocolException("data format error uncompressing message buffer: " + edf);
	    }

	    if (uncmp == 0)
	    {
		readOK = false;
		throw new ZBDException("internal error: more input or preset dictionary required uncompressing data buffer");
	    }

	    size = uncmp;
	}
	else
	{
	    System.arraycopy(rawIn, 0, msgBuf, 0, size);
	}

	logger.log(5, "successfully read message, final size = " + size);

	msgBytesIn += size;
	return size;
    }

    /**
     * Reads a message from the tunnel into the default buffer
     * ({@link #message)}. See {@link #readMessage(byte[])} for further
     * details.
     */

    final int readMessage() throws ZBDException
    {
	return readMessage(message);
    }

    /**
     * Write the supplied data to the tunnel, compressed and encrypted as
     * required.
     *
     * @param msg The message buffer.
     * @param offset The offet from within the buffer from which to start
     * writing the data.
     * @param size The number of bytes to write.
     *
     * @throws ZBDNetworkException Thrown on encountering any kind of
     * problem in reading from the network. This includes premature EOF and
     * connection idle timeout.
     * @throws ZBDException Thrown on any other error.
     */

    final void writeMessage(byte[] msg, int offset, int size) throws ZBDException
    {
	if (!writeOK)
	{
	    throw new ZBDNetworkException("previous unrecoverable error prohibits writing");
	}

	// Get a temporary buffer -- big enough to allow for the data itself
	// plus any possible compression overhead

	byte[] tmp = new byte[size + CMP_OVERHEAD];

	int cmpSize = size;
	int header = size;

	logger.log(5, "Writing message of size " + size);

	// If we are compressing and the message size warrants it ...

	if (compressionInfo > 0 && size > CMP_MINIMUM)
	{
	    // FIX-ME
	    // Should select compression type here but we will only do
	    // Zlib so ...

	    deflater.reset();
	    deflater.setInput(msg, offset, size);
	    deflater.finish();
	    cmpSize = deflater.deflate(tmp);

	    // Only compress if it gains anything otherwise copy the
	    // original data to the temporary buffer.

	    if (cmpSize >= size)
	    {
		System.arraycopy(msg, offset, tmp, 0, size);
		cmpSize = size;
		logger.log(5, "message uncompressed");
	    }
	    else
	    {
		header = FLAG_COMPRESSED | cmpSize;
		logger.log(5, "message compressed to " + cmpSize + " bytes");
	    }
	}
	else
	{
	    System.arraycopy(msg, offset, tmp, 0, size);
	}

	// tmp will now contain message data either compressed or not.
	// cmpSize will contain the size.

	// If encryption has been set up then use it.

	if (bfOut != null)
	{
	    bfOut.cfb64Encrypt(tmp, 0, rawOut, 2, cmpSize, true);
	    header |= FLAG_ENCRYPTED;
	    logger.log(5, "message encrypted");
	}
	else
	{
	    System.arraycopy(tmp, 0, rawOut, 2, cmpSize);
	}

	// Add in header info (short integer, network byte order)

	rawOut[0] = (byte)((header >> 8) & 0xff);
	rawOut[1] = (byte)(header & 0xff);

	logger.log(999, "raw output data = " + bytesToHex(rawOut, 0, cmpSize + 2));

	// Write the data!

	try
	{
	    dataOut.write(rawOut, 0, cmpSize + 2);
	}
	catch (Exception e)
	{
	    writeOK = false;
	    throw new ZBDNetworkException("error writing message of " + (cmpSize + 2)
					  + "bytes");
	}

	writeCount++;
	rawBytesOut += cmpSize + 2;
	msgBytesOut += size;
    }

    /**
     * Writes all data in the supplied byte array buffer to the tunnel.
     * See {@link #writeMessage(byte[],int,int)} for more details.
     *
     * @param msg The data array to be written.
     */

    final void writeMessage(byte[] msg) throws ZBDException
    {
	writeMessage(msg, 0, msg.length);
    }


    /**
     * Convert the supplied string to a null-terminated ASCII string
     * and write it to the tunnel using {@link #writeMessage(byte[])}.
     *
     * @param str The string to be written
     */

    final void writeString(String str) throws ZBDException
    {
	byte[] byteStr = null;
	try
	{
	    byteStr = (str + "\000").getBytes("ASCII");
	}
	catch (UnsupportedEncodingException e)
	{
	    throw new ZBDException("ASCII encoding not supported!: " + e);
	}

	writeMessage(byteStr);
    }

    /**
     * Perform the the Diffie-Hellman exponentiation and return the result
     * as a string.
     *
     * @param generator The generator as a hexadecimal string or <code>""</code>
     * for the default value.
     * @param modulus The modulus as a hexadecimal string or <code>""</code>
     * for the default value.
     * @param exponent The exponent (private key) as a hexadecimal string.
     */

    public static String diffieHellman(String generator, String modulus, String exponent)
    {
	if (generator == null || generator.equals(""))
	{
	    generator = DFLT_GENERATOR;
	}

	if (modulus == null || modulus.equals(""))
	{
	    modulus = DFLT_MODULUS;
	}

	BigInteger gen = new BigInteger(generator, 16);
	BigInteger mod = new BigInteger(modulus, 16);
	BigInteger exp = new BigInteger(exponent, 16);
	BigInteger key = gen.modPow(exp, mod);

	return key.toString(16);
    }

    /**
     * Generate {@link #CHALLENGE_SIZE} random bytes.
     */

    final byte[] generateChallenge()
    {
	byte[] data = new byte[CHALLENGE_SIZE];
	new Random().nextBytes(data);
	return data;
    }

    /**
     * Answer a challenge. XOR each byte with {@link #THE_ANSWER}.
     */

    final void challengeAnswer(byte[] data)
    {
	for (int i = 0; i < CHALLENGE_SIZE; i++)
	{
	    data[i] ^= THE_ANSWER;
	}
    }

    /**
     * Close the tunnel connection.
     */

    synchronized public void close() throws ZBDException
    {
	try
	{
	    readOK = false;
	    writeOK = false;
	    tunnelSocket.close();
	}
	catch (Exception e)
	{
	    throw new ZBDNetworkException("error closing tunnel socket: " + e);
	}
    }

    /**
     * Shutdown the output side of the tunnel connection.
     */

    synchronized public void shutdownOutput() throws ZBDException
    {
	try
	{
	    writeOK = false;
	    tunnelSocket.shutdownOutput();
	}
	catch (Exception e)
	{
	    throw new ZBDNetworkException("error shutting down output for tunnel socket: " + e);
	}
    }

    /**
     * Shutdown the input side of the tunnel connection.
     */

    synchronized public void shutdownInput() throws ZBDException
    {
	try
	{
	    readOK = false;
	    tunnelSocket.shutdownInput();
	}
	catch (Exception e)
	{
	    throw new ZBDNetworkException("error shutting down input for tunnel socket: " + e);
	}
    }

    /**
     * Indicate if tunnel is readable.
     */

    public boolean readable()
    {
	return readOK;
    }

    /**
     * Indicate if tunnel is writeable.
     */

    public boolean writeable()
    {
	return writeOK;
    }

}

/**
 * A <code>ZBDTokenTable</code> holds shared session key reuse token
 * values. An instance of this class may be shared by multiple
 * {@link ZBDTunnel} instances to pool previously negotiated shared
 * keys. These keys are associated with tokens which are exchanged
 * during protocol negotiation. If the table holds a mapping for a
 * given token then an attempt will be made to reuse a previously
 * established key.
 */

class ZBDTokenTable // This could probably be an inner class of ZBDTunnel
{
    /** Special value used to request allocation of a new token */
    static final int TOKEN_NEW = 0xffffffff;
    /** The null token value */
    static final int TOKEN_NULL = 0;

    /** Tokens are considered valid up to this many millisecs before expiry */
    static private final int TOKEN_EXPIRE_GRACE = 10000;
    /** Expired tokens are purged from the table every <code>PURGE_INTERVAL</code> millisecs */
    static private final long PURGE_INTERVAL = 600000;

    /** The current token value in use */
    private int currentToken = TOKEN_NEW;

    /** The lifetime of token/key combinations, in seconds */
    private int keyLifetime = ZBDTunnel.DFLT_KEY_LIFETIME;

    /** The next token to be allocated */
    private int nextToken = 0;

    private class KeyInfo
    {
	String key;
	long expiry;

	KeyInfo(String key, int lifetime)
	{
	    this.key = key;
	    expiry = System.currentTimeMillis() + lifetime * 1000;
	}
    }

    // Hashtable is used in preference to HashMap so that this can be
    // used with pre Java 2 versions.

    private Hashtable keyInfoTable = new Hashtable();

    /**
     * Create a new <code>ZBDTokenTable</code> instance. This also
     * kicks off a background thread to purge expired entries from
     * the table on a regular basis.
     */

    ZBDTokenTable()
    {
	// Initialise nextToken to a random value (this does not need to be
	// a cryptographically strong random number).

	nextToken = (new Random()).nextInt();

	// Start clean-up thread

	Thread bg = new Thread()
	{
	    public void run()
	    {
		setPriority(NORM_PRIORITY - 1);
		purgeExpired();
	    }
	};
	bg.setDaemon(true);
	bg.start();
    }

    /**
     * Sets the key lifetime. This sets the time in seconds after which
     * shared keys will be considered to have expired and must be
     * renegotiated.
     *
     * @param life The key lifetime, in seconds.
     *
     * @throws ZBDValueException Thrown if the lifetime is less than zero.
     */

    int setKeyLifetime(int life) throws ZBDValueException
    {
	if (life < 0)
	{
	    throw new ZBDValueException("key lifetime less than zero (" + life + ")");
	}
	else if (life > ZBDTunnel.MAX_KEY_LIFETIME)
	{
	    life = ZBDTunnel.MAX_KEY_LIFETIME;
	}

	keyLifetime = life;
	return keyLifetime;
    }

    /**
     * Retrieves the current key lifetime.
     */

    int getKeyLifetime()
    {
	return keyLifetime;
    }

    /**
     * Retrieves the current key reuse token. This is only used on the
     * client side of the protocol. If there is no current token
     * or the token is within {@link #TOKEN_EXPIRE_GRACE} millisecs
     * of expiry then returns {@link #TOKEN_NEW}.
     */

    synchronized int getCurrentToken() // throws ZBDException
    {

	if (currentToken == TOKEN_NEW || currentToken == TOKEN_NULL) {
	    return currentToken;
	}

	KeyInfo info = (KeyInfo)keyInfoTable.get(new Integer(currentToken));
	if (info != null) {
	    if (System.currentTimeMillis() < (info.expiry - TOKEN_EXPIRE_GRACE)) {
		return currentToken;
	    }
	}

	return TOKEN_NEW;
    }

    /**
     * Sets the current reuse token and associates the given key string
     * with it.
     *
     * @param token The token value.
     * @param key The key string.
     */

    synchronized int setCurrentToken(int token, String key)
    {
	setKeyForToken(token, key);
	currentToken = token;
	return currentToken;
    }

    /**
     * Retrieves the key string, if any, associated with the given token.
     * If no matching token is found or the key has expired then returns
     * <code>null</code>.
     */

    synchronized String getKeyForToken(int token)
    {
	// Save a table search

	if (token == -1)
	{
	    return null;
	}

	KeyInfo info = (KeyInfo)keyInfoTable.get(new Integer(token));
	if (info != null)
	{
	    if (System.currentTimeMillis() < info.expiry)
	    {
		return info.key;
	    }
	}
	return null;
    }

    /**
     * Associates the specified key string with the given token. The expiry
     * time is set to the current time plus the current key lifetime
     * (see {@link #getKeyLifetime()}).
     */

    synchronized void setKeyForToken(int token, String key)
    {
	KeyInfo ki = new KeyInfo(key, keyLifetime);

	keyInfoTable.put(new Integer(token), ki);
    }

    /**
     * Generates a new token, explicitly avoiding the value in
     * <code>oldToken</code> and the {@link #TOKEN_NEW} and {@link #TOKEN_NULL}
     * values.
     *
     * @param oldToken Previous token value to avoid.
     */

    synchronized int generateToken(int oldToken)
    {
	int token = 0;

	while (token == 0)
	{
	    nextToken = (nextToken + 1) & 0xffffffff;

	    // Screen out special values

	    if (nextToken == 0 || nextToken == TOKEN_NEW || nextToken == oldToken)
	    {
		continue;
	    }

	    // Check we do not already have an entry for this token

	    if (keyInfoTable.get(new Integer(nextToken)) != null)
	    {
		continue;
	    }

	    // Gotcha!

	    token = nextToken;
	}

	return token;
    }

    /**
     * Function to perform a house-keeping task, run as a background
     * thread by the default constructor. It cleans up expired
     * {@link #keyInfoTable} entries.
     */

    private void purgeExpired()
    {
	try
	{
	    while (true)
	    {
		Thread.sleep(PURGE_INTERVAL);
		
		long now = System.currentTimeMillis();

		synchronized (this)
		{
		    Vector expired = new Vector(10, 10);
		    Enumeration i = keyInfoTable.keys();

		    // Figure out which keys have expired and add them
		    // to a Vector -- we want to avoid modifying the
		    // Hashtable at this point.

		    while (i.hasMoreElements())
		    {
			Object key = i.nextElement();
			KeyInfo ki = (KeyInfo)keyInfoTable.get(key);
			if (ki.expiry < now)
			{
			    expired.addElement(key);
			}
		    }

		    // Now zap the expired elements

		    i = expired.elements();
		    while (i.hasMoreElements())
		    {
			keyInfoTable.remove(i.nextElement());
		    }
		}
	    }
	}
	catch (InterruptedException e)
	{
	    // Go quietly ...
	    return;
	}
    }
}

