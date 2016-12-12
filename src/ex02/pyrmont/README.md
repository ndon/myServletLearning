## 不同的资源(静态资源，动态资源)，用不同的处理器处理
```java
// check if this is a request for a servlet or a static resource
// a request for a servlet begins with "/servlet/"
if (request.getUri().startsWith("/servlet/")) {
	ServletProcessor1 processor = new ServletProcessor1();
	processor.process(request, response);
} else {
	StaticResourceProcessor processor = new StaticResourceProcessor();
	processor.process(request, response);
}
```

## Servlet Processor获取Servlet实例的流程

根据request获取servlet的uri信息

从而创建classLoader实例

通过classLoader实例创建servlet实例

## 入口类分别为HttpServer1和HttpServer2

HttpServer2使用的servlet Processor，使用了Facade模式对request和response进行了包装

ServletProcess1.java
```java
try {
	servlet = (Servlet) myClass.newInstance();
	servlet.service((ServletRequest) request, (ServletResponse) response);
} catch (Exception e) {
	System.out.println(e.toString());
} catch (Throwable e) {
	System.out.println(e.toString());
}
```

ServletProcess2.java
```java
RequestFacade requestFacade = new RequestFacade(request);
ResponseFacade responseFacade = new ResponseFacade(response);
try {
	servlet = (Servlet) myClass.newInstance();
	servlet.service((ServletRequest) requestFacade, (ServletResponse) responseFacade);
} catch (Exception e) {
	System.out.println(e.toString());
} catch (Throwable e) {
	System.out.println(e.toString());
}
```

## 访问示例
静态资源：http://localhost:8080/index.html

servlet：http://localhost:8080/servlet/PrimitiveServlet
