/*
 * ftp4j - A pure Java FTP client library
 * 
 * Copyright (C) 2008-2010 Carlo Pelliccia (www.sauronsoftware.it)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version
 * 2.1, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License 2.1 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License version 2.1 along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package it.sauronsoftware.ftp4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * This abstract class is the base for creating a connector. Connectors are used
 * by the client to establish connections with remote servers.
 * 
 * @author Carlo Pelliccia
 */
public abstract class FTPConnector {

	/**
	 * Timeout in seconds for connection enstablishing.
	 * 
	 * @since 1.7
	 */
	protected int connectionTimeout = 10;

	/**
	 * Timeout in seconds for read operations.
	 * 
	 * @since 1.7
	 */
	protected int readTimeout = 10;

	/**
	 * Timeout in seconds for connection regular closing.
	 * 
	 * @since 1.7
	 */
	protected int closeTimeout = 10;

	/**
	 * The socket of an ongoing connection attempt for a communication channel.
	 * 
	 * @since 1.7
	 */
	private Socket connectingCommunicationChannelSocket;

	/**
	 * Sets the timeout for connection operations.
	 * 
	 * @param connectionTimeout
	 *            The timeout value in seconds.
	 * @since 1.7
	 */
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * Sets the timeout for read operations.
	 * 
	 * @param readTimeout
	 *            The timeout value in seconds.
	 * @since 1.7
	 */
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	/**
	 * Sets the timeout for close operations.
	 * 
	 * @param closeTimeout
	 *            The timeout value in seconds.
	 * @since 1.7
	 */
	public void setCloseTimeout(int closeTimeout) {
		this.closeTimeout = closeTimeout;
	}

	/**
	 * Creates a socket and connects it to the given host for a communication
	 * channel. Socket timeouts are automatically set according to the values of
	 * {@link FTPConnector#connectionTimeout}, {@link FTPConnector#readTimeout}
	 * and {@link FTPConnector#closeTimeout}.
	 * 
	 * If you are extending FTPConnector, consider using this method to
	 * establish your socket connection for the communication channel, instead
	 * of creating Socket objects, since it is already aware of the timeout
	 * values possibly given by the caller. Moreover the caller can abort
	 * connection calling {@link FTPClient#abortCurrentConnectionAttempt()}.
	 * 
	 * @param host
	 *            The host for the connection.
	 * @param port
	 *            The port for the connection.
	 * @return The connected socket.
	 * @throws IOException
	 *             If connection fails.
	 * @since 1.7
	 */
	protected Socket tcpConnectForCommunicationChannel(String host, int port) throws IOException {
		try {
			connectingCommunicationChannelSocket = new Socket();
			connectingCommunicationChannelSocket.setSoTimeout(readTimeout * 1000);
			connectingCommunicationChannelSocket.setSoLinger(true, closeTimeout);
			connectingCommunicationChannelSocket.connect(new InetSocketAddress(host, port), connectionTimeout * 1000);
			return connectingCommunicationChannelSocket;
		} finally {
			connectingCommunicationChannelSocket = null;
		}
	}

	/**
	 * Creates a socket and connects it to the given host for a data transfer
	 * channel. Socket timeouts are automatically set according to the values of
	 * {@link FTPConnector#connectionTimeout}, {@link FTPConnector#readTimeout}
	 * and {@link FTPConnector#closeTimeout}.
	 * 
	 * If you are extending FTPConnector, consider using this method to
	 * establish your socket connection for the communication channel, instead
	 * of creating Socket objects, since it is already aware of the timeout
	 * values possibly given by the caller.
	 * 
	 * @param host
	 *            The host for the connection.
	 * @param port
	 *            The port for the connection.
	 * @return The connected socket.
	 * @throws IOException
	 *             If connection fails.
	 * @since 1.7
	 */
	protected Socket tcpConnectForDataTransferChannel(String host, int port) throws IOException {
		Socket socket = new Socket();
		socket.setSoTimeout(readTimeout * 1000);
		socket.setSoLinger(true, closeTimeout);
		socket.setReceiveBufferSize(512 * 1024);
		socket.setSendBufferSize(512 * 1024);
		socket.connect(new InetSocketAddress(host, port), connectionTimeout * 1000);
		return socket;
	}

	/**
	 * Aborts an ongoing connection attempt for a communication channel.
	 * 
	 * @since 1.7
	 */
	public void abortConnectForCommunicationChannel() {
		if (connectingCommunicationChannelSocket != null) {
			try {
				connectingCommunicationChannelSocket.close();
			} catch (Throwable t) {
			}
		}
	}

	/**
	 * This methods returns an established connection to a remote host, suitable
	 * for a FTP communication channel.
	 * 
	 * @param host
	 *            The remote host name or address.
	 * @param port
	 *            The remote port.
	 * @return The connection with the remote host.
	 * @throws IOException
	 *             If the connection cannot be established.
	 */
	public abstract Socket connectForCommunicationChannel(String host, int port)
			throws IOException;

	/**
	 * This methods returns an established connection to a remote host, suitable
	 * for a FTP data transfer channel.
	 * 
	 * @param host
	 *            The remote host name or address.
	 * @param port
	 *            The remote port.
	 * @return The connection with the remote host.
	 * @throws IOException
	 *             If the connection cannot be established.
	 */
	public abstract Socket connectForDataTransferChannel(String host, int port)
			throws IOException;

}
