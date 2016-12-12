1. 不使用jdk的类加载器原因
	servlet只被允许访问位于WEB-INF/classes目录下和WEB-INF/lib下的文件
	提供自动重载的功能
	在载入类中指定某些规则
	缓存已经载入的类
	实现类的预载入，方便使用
	
	
2. Loader接口
	Tomcat载入器——应用程序载入器
		——使用一个自定义的类载入器
	
	WebappLoader：
		1. 创建类加载器
		// 默认的类加载器，可以继承WebappClassLoader以自己实现类加载器，并通过get set方法，改变类加载器
		private String loaderClass = "org.apache.catalina.loader.WebappClassLoader";
		
		2. 设置仓库
		默认的仓库为WEB-INF/classes目录和WEB-INF/lib目录
		
		3. 设置类路径
		Set the appropriate context attribute for our class path. 
		This is required only because Jasper depends on it.
		servletContext.setAttribute(Globals.CLASS_PATH_ATTR, classpath.toString());
		
		4. 设置访问权限
		为类载入器设置访问相关目录的权限，若没有使用安全管理器，则什么也不做
		if ((securityManager != null) && (permission != null)) {
			permissionList.add(permission);
		}
		
		5. 开启新线程执行类的重新载入
		// Loop until the termination semaphore is set
		while (!threadDone) {

			// Wait for our check interval
			threadSleep();

			if (!started)
				break;

			try {
				// Perform our modification check
				if (!classLoader.modified())
					continue;
			} catch (Exception e) {
				log(sm.getString("webappLoader.failModifiedCheck"), e);
				continue;
			}

			// Handle a need for reloading
			notifyContext();
			break;

		}
		
		


3.类载入器：WebappClassLoader类











