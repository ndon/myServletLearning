# 解读Tomcat HttpConnector

同一个HttpProcessor对象的assign()方法和await()方法会相互配合处理socket

但是他们并不运行在同一个线程中。

assign()方法运行在HttpConnector的连接器线程中

await()方法运行在自身的处理器线程中

这两个方法通过available变量和wait(),notifyAll()方法沟通

处理器线程刚启动时，由于available变量为false，会一直阻塞

一直到HttpConnector对象调用HttpProcessor实例的assign方法



## 启动类Bootstrap
```java
HttpConnector connector = new HttpConnector();
SimpleContainer container = new SimpleContainer();
//绑定到一个container
connector.setContainer(container);
//创建serverSocket
connector.initialize();
//启动connector的连接器线程和所有processor的处理器线程
connector.start();[1]
```		

## HttpConnector
启动connector的连接器线程和所有processor的处理器线程

[1]connector.start():
```java
// 启动connector的连接器线程
threadStart();[2]

// 创建minProcessors个HttpProcessor，
// 并且启动HttpProcessor对象池中的每个处理器线程，
// 但是每个processor都处于阻塞状态，等待connector的连接器线程的唤醒
while (curProcessors < minProcessors) {
	if ((maxProcessors > 0) && (curProcessors >= maxProcessors))
		break;
	//newProcessor()方法实例化的同时，也启动处理器线程
	HttpProcessor processor = newProcessor();[3]
	recycle(processor);
}
```

HttpConnector 连接器线程

[2]threadStart()
```java
// 等待socket到来
socket = serverSocket.accept();
// 创建HttpProcessor，启动其处理器线程，
// 使每个HttpProcessor运行在自己的线程中，创建后线程处于阻塞状态
HttpProcessor processor = createProcessor();[4]
processor.assign(socket);[5]
```

[3]newProcessor()

//newProcessor()方法实例化的同时，也启动处理器线程（阻塞状态）
```java
private HttpProcessor newProcessor() {
	// if (debug >= 2)
	// log("newProcessor: Creating new processor");
	HttpProcessor processor = new HttpProcessor(this, curProcessors++);
	if (processor instanceof Lifecycle) {
		try {
			((Lifecycle) processor).start();[6]
		} catch (LifecycleException e) {
			log("newProcessor", e);
			return (null);
		}
	}
	created.addElement(processor);
	return (processor);
}
```

[4]createProcessor()
```java
private HttpProcessor createProcessor() {
	synchronized (processors) {
		if (processors.size() > 0) {
			// if (debug >= 2)
			// log("createProcessor: Reusing existing processor");
			return ((HttpProcessor) processors.pop());
		}
		if ((maxProcessors > 0) && (curProcessors < maxProcessors)) {
			// if (debug >= 2)
			// log("createProcessor: Creating new processor");
			return (newProcessor());
		} else {
			if (maxProcessors < 0) {
				// if (debug >= 2)
				// log("createProcessor: Creating new processor");
				return (newProcessor());
			} else {
				// if (debug >= 2)
				// log("createProcessor: Cannot create new processor");
				return (null);
			}
		}
	}
}
```
## HttpProcessor
[6]
```java	
// HttpProcessor创建后，处理器线程就启动，启动后处于阻塞状态，
// 等待持有该HttpProcessor实例的connector连接器线程的唤醒
public void run() {
    while (!stopped) {
        // await使处理器线程处于阻塞状态，
        // 直到被HttpConnector的连接器线程中该processor的assign方法唤醒
        Socket socket = await();[7]
        if (socket == null)
            continue;
        // 具体处理来自于socket的请求
        try {
            process(socket);[8]
        } catch (Throwable t) {
            log("process.invoke", t);
        }
        // 处理完成后，将该实例重置于HttpProcessor实例池中
        connector.recycle(this);
    }
    // Tell threadStop() we have shut ourselves down successfully
    synchronized (threadSync) {
        threadSync.notifyAll();
    }
} 
```
[7]await()
```java
private synchronized Socket await() {
    // Wait for the Connector to provide a new Socket
    while (!available) {
        try {
            wait();
        } catch (InterruptedException e) {
        }
    }
    // Notify the Connector that we have received this Socket
    Socket socket = this.socket;
    available = false;
    notifyAll();
    if ((debug >= 1) && (socket != null))
        log("  The incoming request has been awaited");
    return (socket);
}
```

HttpProcessor assign方法
[5]processor.assign(socket)
```java
synchronized void assign(Socket socket) {
    // 初始化available为false，故无需等待
    while (available) {
        try {
            wait();
        } catch (InterruptedException e) {
        }
    }
    this.socket = socket;
    // available置为true
    available = true;
    // 唤醒该processor置于阻塞状态的处理器线程
    notifyAll();
    if ((debug >= 1) && (socket != null))
        log(" An incoming request is being assigned");
}
    
 ```       
[8]HttpProcessor processor方法
```java
// 在处理器线程中被自身调用，
// 首先不断读取输入流
while (!stopped && ok && keepAlive) {
    request.setStream(input);
    request.setResponse(response);
    output = socket.getOutputStream();
    response.setStream(output);
    response.setRequest(request);
    ((HttpServletResponse) response.getResponse()).setHeader("Server", SERVER_INFO);
	
	// 解析连接
    parseConnection(socket);[9]
    
    // 解析请求
    parseRequest(input, output);[10]
    
    // 解析请求头
    parseHeaders(input);[11]
   
    // 将请求分发给具体的container处理
    if (ok) {
    	connector.getContainer().invoke(request, response);
    }   
            
    // 将响应返回给客户端
	response.finishResponse();
	request.finishRequest();
	output.flush();
	
    // 回收request和response对象
    request.recycle();
    response.recycle();
}

// 关闭socket
shutdownInput(input);
socket.close();
socket = null;
 ```   
    
[9]HttpProcessor parseConnection()
    
    // request获取地址
	((HttpRequestImpl) request).setInet(socket.getInetAddress());
    
    // 检查是否需要为request设置代理
    if (proxyPort != 0)
        request.setServerPort(proxyPort);
    else
        request.setServerPort(serverPort);
    
    // request获取socket
    request.setSocket(socket);
    
[10]HttpProcessor  parseRequest() 
	解析请求行（Http请求第一行：方法 URI 协议）

[11]HttpProcessor parseHeaders()
	解析请求头(Http请求第二行)
	
## 访问示例
静态资源：http://localhost:8080/index.html

servlet：http://localhost:8080/servlet/PrimitiveServlet

servlet：http://localhost:8080/servlet/ModernServlet