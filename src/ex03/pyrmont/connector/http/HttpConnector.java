package ex03.pyrmont.connector.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * Connector和Adapter的功能简单的说就是将请求分发到某个Container，
 * 具体来说Connector处理底层的socket，
 * 并将http请求、响应等字节流层面的东西，封装成Request和Response两个类
 * （这两个类是tomcat定义的，而非servlet中的ServletRequest和ServletResponse），供容器使用；
 * 同时，为了能让我们编写的servlet能够得到ServletRequest，
 * Tomcat使用了Facade模式，将比较底层、低级的Request包装成为ServletRequest（这一过程通常发生在Wrapper容器一级）。
 * 因此，Coyote本质上是为tomcat的容器提供了对底层socket连接数据的封装，
 * 以Request类的形式，让容器能够访问到底层的数据。
 * 而关于连接池、线程池等直接和socket打交道的事情，
 * tomcat交给了org.apache.tomcat.util.net包的类去完成。
 * 
 */
public class HttpConnector implements Runnable {

	boolean stopped;
	private String scheme = "http";

	public String getScheme() {
		return scheme;
	}

	public void run() {
		ServerSocket serverSocket = null;
		int port = 8080;
		try {
			serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		while (!stopped) {
			// Accept the next incoming connection from the server socket
			Socket socket = null;
			try {
				socket = serverSocket.accept();
			} catch (Exception e) {
				continue;
			}
			// Hand this socket off to an HttpProcessor
			HttpProcessor processor = new HttpProcessor(this);
			processor.process(socket);
		}
	}

	public void start() {
		Thread thread = new Thread(this);
		thread.start();
	}
}