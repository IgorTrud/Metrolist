package com.metrolist.innertube.utils

import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketException
import javax.net.SocketFactory
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class TunneledTlsSocketFactory(
    private val tunnelEndpoint: String,
    private val tunnelPort: Int,
    private val sslSocketFactory: SSLSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
) : SocketFactory() {

    override fun createSocket(): Socket = createAndConnectTunnel()
    override fun createSocket(host: String, port: Int): Socket = createAndConnectTunnel(host, port)
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket =
        createAndConnectTunnel(host, port)
    override fun createSocket(host: InetAddress, port: Int): Socket =
        createAndConnectTunnel(host.hostAddress, port)
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket =
        createAndConnectTunnel(address.hostAddress, port)

    private fun createAndConnectTunnel(
        targetHost: String? = null,
        targetPort: Int? = null
    ): Socket {
        val tunnelSocket = sslSocketFactory.createSocket(tunnelEndpoint, tunnelPort) as SSLSocket

        tunnelSocket.enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3")
        tunnelSocket.startHandshake()

        if (targetHost != null && targetPort != null) {
            establishTunnel(tunnelSocket, targetHost, targetPort)
        }

        return TunneledSocketWrapper(tunnelSocket, targetHost != null)
    }

    private fun establishTunnel(socket: SSLSocket, host: String, port: Int) {
       // DataOutputStream(socket.outputStream).apply {
           // writeUTF("CONNECT $host:$port")
         //   flush()
       // }

    /*    val response = DataInputStream(socket.inputStream).readUTF()
        if (!response.startsWith("200")) {
            throw IOException("Tunnel connection failed: $response")
        }*/
    }

    private inner class TunneledSocketWrapper(
        private val tunnelSocket: SSLSocket,
        private val isPreconnected: Boolean
    ) : Socket() {

        override fun connect(endpoint: SocketAddress, timeout: Int) {
            if (isPreconnected) {
                throw SocketException("Socket is already connected via tunnel")
            }

            val inetAddr = endpoint as InetSocketAddress
            establishTunnel(tunnelSocket, inetAddr.hostName, inetAddr.port)
        }

        override fun connect(endpoint: SocketAddress) = connect(endpoint, 10000)

        override fun getInputStream(): InputStream = tunnelSocket.inputStream
        override fun getOutputStream(): OutputStream = tunnelSocket.outputStream
        override fun close() = tunnelSocket.close()
        override fun isConnected() = tunnelSocket.isConnected
        override fun isClosed() = tunnelSocket.isClosed
        override fun shutdownInput() = tunnelSocket.shutdownInput()
        override fun shutdownOutput() = tunnelSocket.shutdownOutput()

    }
}


