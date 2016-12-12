1. Tomcat的四种容器（图 Tomcat Container.jpg）
	Engine：整个Catalina servlet引擎
	Host:表示包含一个或多个context容器的虚拟主机
	Context：表示一个Web应用，可以有多个Wrapper
	Wrapper：表示一个独立的servlet

2.Container接口提供增删查子容器的方法
	public void addChild(Container child);
	public void removeChild(Container child);
	public Container findChild(String name);
	public Container[] findChildren();

3.容器包含组件：载入器，记录器，管理器，领域，资源
	Container接口提供getter，setter方法将这些组件与容器关联
	public Loader getLoader();
	public void setLoader(Loader loader);
	public Logger getLogger();
	public void setLogger(Logger logger);
	public Manager getManager();
	public void setManager(Manager manager);
	public Cluster getCluster();
	public void setCluster(Cluster cluster);	
	public Realm getRealm();
	public void setRealm(Realm realm);
	public DirContext getResources();
	public void setResources(DirContext resources);

4.pipeline：责任链设计模式

	// 容器的invoke方法将工作交由pipeline的invoke方法完成
	Container：public void invoke(Request request, Response response) throws IOException, ServletException
		调用
		SimplePipeline：public void invoke(Request request, Response response) throws IOException, ServletException {
			
			// Pipeline的invoke方法将工作交给自身内部类ValveContext的invokeNext完成
			// 由于ValveContext是Pipeline的内部类，所以它可以访问Pipeline的所有成员
			SimplePipelineValveContext：public void invokeNext(Request request, Response response) throws IOException, ServletException
				
				// ValveContext将自身传给每一个value
				// Value调用自身的invoke方法:invoke(request, response, this)
				// Value自身的invoke方法会再次调用invokeNext方法，从而调用下一个value的invoke方法
				// 
				if (subscript < valves.length) {
					valves[subscript].invoke(request, response, this);
					
				// 基础value最后执行
				} else if ((subscript == valves.length) && (basic != null)) {
					basic.invoke(request, response, this);
				} else {
					throw new ServletException("No valve");
				}

5. Wrapper 容器的模拟实现

	4.1 SimpleLoader：实现org.apache.catalina.Loader接口，可返回一个ClassLoader实例
	// WEB_ROOT：servlet目录
	public static final String WEB_ROOT = System.getProperty("user.dir") + File.separator + "webroot";
	
	// 构造方法为属性classLoader赋值，指定classLoader的加载路径
	public SimpleLoader() {
		URL[] urls = new URL[1];
		URLStreamHandler streamHandler = null;
		File classPath = new File(WEB_ROOT);
		String repository = (new URL("file", null, classPath.getCanonicalPath() + File.separator)).toString();
		urls[0] = new URL(null, repository, streamHandler);
		classLoader = new URLClassLoader(urls);
	}
	
	4.2 SimpleWrapper：
	// 首先构造方法指定basic Value
	public SimpleWrapper() {
		pipeline.setBasic(new SimpleWrapperValve());
	}
	
	// 该方法获取Servlet实例
	private Servlet loadServlet() throws ServletException {
		
		// 首先获取载入器--Loader loader = new SimpleLoader();wrapper.setLoader(loader);
		Loader loader = getLoader();
		
		//由类加载器获得ClassLoader
		ClassLoader classLoader = loader.getClassLoader();

		// Load the specified servlet class from the appropriate class loader
		Class classClass = classLoader.loadClass(actualClass);
		
		// Instantiate and initialize an instance of the servlet class itself
		servlet = (Servlet) classClass.newInstance();

		// Call the initialization method of this servlet
		servlet.init(null);
		
		return servlet;
	}
	
	// allocate方法为属性instance 赋值
	public Servlet allocate() throws ServletException ：
		instance = loadServlet();
	
	4.3 SimpleWrapperValve：basic Value
	在这个类的invoke方法中，实际上调用了servlet.service方法，处理业务逻辑
	
	
	4.4 Bootstrap
	// 创建HttpConnector
	HttpConnector connector = new HttpConnector();
	
	// 创建Wrapper
	Wrapper wrapper = new SimpleWrapper();
	
	// 指定管理的servlet，从而构成一条完整的路径，使得类加载器找到Servlet，并完成实例化
	wrapper.setServletClass("ModernServlet");
	
	// 创建加载servlet的Loader
	Loader loader = new SimpleLoader();
	
	// 创建两个Value
	Valve valve1 = new HeaderLoggerValve();
	Valve valve2 = new ClientIPLoggerValve();

	// 为wrapper的属性Loader赋值，也就是说在这里，启动时就已经指定了wrapper的loader为SimpleLoader实例
	wrapper.setLoader(loader);
	
	// 设置Value
	((Pipeline) wrapper).addValve(valve1);
	((Pipeline) wrapper).addValve(valve2);

	// HttpConnector设置wrapper
	connector.setContainer(wrapper);

	//。。
	connector.initialize();
	connector.start();
	
}

 由前面关于同一个HttpProcessor对象的assign()方法和await()方法会相互配合处理socket可知，
 请求会被交由HttpProcessor的process方法处理
 process方法会将请求交由Container的invoke方法处理：
 	connector.getContainer().invoke(request, response);
 
 在本例中即交由SimpleWrapper的invoke方法处理：
 	public void invoke(Request request, Response response) throws IOException, ServletException {
		pipeline.invoke(request, response);
	}


 由前面内容可知容器的invoke方法将工作交由pipeline的invoke方法完成
 在本例中即SimplePipeline的invoke方法
 	public void invoke(Request request, Response response) throws IOException, ServletException {
		// Invoke the first Valve in this pipeline for this request
		(new SimplePipelineValveContext()).invokeNext(request, response);
	}
	
	再继续由内部类SimplePipelineValveContext处理
		public void invokeNext(Request request, Response response) throws IOException, ServletException {
			int subscript = stage;
			stage = stage + 1;
			// Invoke the requested Valve for the current request thread
			if (subscript < valves.length) {
				valves[subscript].invoke(request, response, this);
			} else if ((subscript == valves.length) && (basic != null)) {
				basic.invoke(request, response, this);
			} else {
				throw new ServletException("No valve");
			}
		}

	其中basic也就是SimpleWrapperValue将会加载servlet处理请求
		public void invoke(Request request, Response response, ValveContext valveContext)
				throws IOException, ServletException {
	
			SimpleWrapper wrapper = (SimpleWrapper) getContainer();
			ServletRequest sreq = request.getRequest();
			ServletResponse sres = response.getResponse();
			Servlet servlet = null;
			HttpServletRequest hreq = null;
			if (sreq instanceof HttpServletRequest)
				hreq = (HttpServletRequest) sreq;
			HttpServletResponse hres = null;
			if (sres instanceof HttpServletResponse)
				hres = (HttpServletResponse) sres;
	
			// Allocate a servlet instance to process this request
			try {
				servlet = wrapper.allocate();
				if (hres != null && hreq != null) {
					servlet.service(hreq, hres);
				} else {
					servlet.service(sreq, sres);
				}
			} catch (ServletException e) {
			}
		}
		
5. Wrapper 容器的模拟实现
	
	5.1 Context：SimpleContext
		// 构造方法，指定basic value
		public SimpleContext() {
			pipeline.setBasic(new SimpleContextValve());
		}
		
		
		// addMapper方法，指定mapper
		public void addMapper(Mapper mapper) {
		// this method is adopted from addMapper in ContainerBase
		// the first mapper added becomes the default mapper
		mapper.setContainer((Container) this); // May throw IAE
		this.mapper = mapper;
		// mappers 是一个HashMap
		synchronized (mappers) {
			if (mappers.get(mapper.getProtocol()) != null)
				throw new IllegalArgumentException("addMapper:  Protocol '" + mapper.getProtocol() + "' is not unique");
			mapper.setContainer((Container) this); // May throw IAE
			mappers.put(mapper.getProtocol(), mapper);
			if (mappers.size() == 1)
				this.mapper = mapper;
			else
				this.mapper = null;
		}
		
		// map方法，映射到wrapper，实际有basic value的invoke方法调用
		// SimpleContextValve.invoke——SimpleContext.map——SimpleContextMapper.map
		public Container map(Request request, boolean update) {
			// this method is taken from the map method in
			// org.apache.cataline.core.ContainerBase
			// the findMapper method always returns the default mapper, if any,
			// regardless the
			// request's protocol
			Mapper mapper = findMapper(request.getRequest().getProtocol());
			if (mapper == null)
				return (null);
	
			// Use this Mapper to perform this mapping
			return (mapper.map(request, update));
		}

	5.2 SimpleContextValve：basic Value
		// 在这个类的invoke方法中，通过映射器找到处理servlet请求的wrapper
		// 并通过调用wrapper.invoke方法，实际处理请求
		wrapper = (Wrapper) context.map(request, true);
		wrapper.invoke(request, response);
		
		
		
	5.3 SimpleContextMapper
	
	// map方法，由Context的map方法调用，返回一个wrapper
	public Container map(Request request, boolean update) {
		wrapper = (Wrapper) context.findChild(name);
	}
	
	5.4 BootStrap2
		HttpConnector connector = new HttpConnector();
		
		// Wraper
		Wrapper wrapper1 = new SimpleWrapper();
		wrapper1.setName("Primitive");
		wrapper1.setServletClass("PrimitiveServlet");
		
		Wrapper wrapper2 = new SimpleWrapper();
		wrapper2.setName("Modern");
		wrapper2.setServletClass("ModernServlet");
		
		// Context
		Context context = new SimpleContext();
		context.addChild(wrapper1);
		context.addChild(wrapper2);
		
		// Value
		Valve valve1 = new HeaderLoggerValve();
		Valve valve2 = new ClientIPLoggerValve();
		
		((Pipeline) context).addValve(valve1);
		((Pipeline) context).addValve(valve2);
		
		// Mapper
		Mapper mapper = new SimpleContextMapper();
		mapper.setProtocol("http");
		context.addMapper(mapper);
		
		// Loader
		Loader loader = new SimpleLoader();
		context.setLoader(loader);
		
		// context.addServletMapping(pattern, name);
		context.addServletMapping("/Primitive", "Primitive");
		context.addServletMapping("/Modern", "Modern");
		
		connector.setContainer(context);
		
		connector.initialize();
		connector.start();
		

		//
		由前面关于同一个HttpProcessor对象的assign()方法和await()方法会相互配合处理socket可知，
		请求会被交由HttpProcessor的process方法处理：
			process方法会将请求交由Container的invoke方法处理：
				connector.getContainer().invoke(request, response);
 
		在本例中即交由SimpleContext的invoke方法处理：
			public void invoke(Request request, Response response) throws IOException, ServletException {
				pipeline.invoke(request, response);
			}


		由前面内容可知容器的invoke方法将工作交由pipeline的invoke方法完成
		在本例中即SimplePipeline的invoke方法
		 	public void invoke(Request request, Response response) throws IOException, ServletException {
				// Invoke the first Valve in this pipeline for this request
				(new SimplePipelineValveContext()).invokeNext(request, response);
			}
	
		再继续由内部类SimplePipelineValveContext处理
			public void invokeNext(Request request, Response response) throws IOException, ServletException {
				int subscript = stage;
				stage = stage + 1;
				// Invoke the requested Valve for the current request thread
				if (subscript < valves.length) {
					valves[subscript].invoke(request, response, this);
				} else if ((subscript == valves.length) && (basic != null)) {
					basic.invoke(request, response, this);
				} else {
					throw new ServletException("No valve");
				}
			}

		其中basic也就是SimpleContextValve将会通过映射器，找到处理请求的wrapper
		wrapper = (Wrapper) context.map(request, true);
		// wrapper再具体处理请求
		// 注意，wrapper实例中并没有与载入器相关联，但是context实例关联了载入器，因此
		// SimpleWrapper类的getLoader()方法会返回父容器的载入器
		wrapper.invoke(request, response);










