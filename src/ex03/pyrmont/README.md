# 模拟HttpConnector

## 启动类Bootstrap
```java
HttpConnector connector = new HttpConnector();
connector.start();
```	
## HttpConnector将接收到的socket实际交给HttpProcessor处理
```java
HttpProcessor processor = new HttpProcessor(this);
processor.process(socket);
```	
## HttpProcessor创建HttpRequest和HttpResponse对象

解析Header，
根据请求文件类型（动态/静态）将请求分发给具体的Processor处理

## Connector
Connector和Adapter的功能简单的说就是将请求分发到某个Container，

具体来说Connector处理底层的socket，

并将http请求、响应等字节流层面的东西，封装成Request和Response两个类

（这两个类是tomcat定义的，而非servlet中的ServletRequest和ServletResponse），供容器使用；

同时，为了能让我们编写的servlet能够得到ServletRequest，

Tomcat使用了Facade模式，将比较底层、低级的Request包装成为ServletRequest（这一过程通常发生在Wrapper容器一级）。

因此，Coyote本质上是为tomcat的容器提供了对底层socket连接数据的封装，

以Request类的形式，让容器能够访问到底层的数据。

而关于连接池、线程池等直接和socket打交道的事情，

tomcat交给了org.apache.tomcat.util.net包的类去完成。

## 访问示例
静态资源：http://localhost:8080/index.html

servlet：http://localhost:8080/servlet/PrimitiveServlet

servlet：http://localhost:8080/servlet/ModernServlet
