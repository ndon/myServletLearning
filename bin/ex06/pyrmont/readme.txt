1. Lifecycle接口

	通过实现Lifecycle接口，可以达到统一启动/关闭这些组件的效果，
	
	Catalina允许组件包含其他组件，因此父组件要负责启动/关闭子组件，
	
	从而实现启动一个组件，就将全部应用的组件全部启动的效果，
	
	这种单一启动/关闭的机制正是通过Lifecycle接口实现的


	Lifecycle接口如下：
	public interface Lifecycle {
		public static final String START_EVENT = "start";
		public static final String BEFORE_START_EVENT = "before_start";
		public static final String AFTER_START_EVENT = "after_start";
		public static final String STOP_EVENT = "stop";
		public static final String BEFORE_STOP_EVENT = "before_stop";
		public static final String AFTER_STOP_EVENT = "after_stop";
	
		// 
		public void addLifecycleListener(LifecycleListener listener);
	
		// 
		public LifecycleListener[] findLifecycleListeners();
	
		// 
		public void removeLifecycleListener(LifecycleListener listener);
	
		//
		public void start() throws LifecycleException;
	
		//
		public void stop() throws LifecycleException;
	
	}
	其中start()和stop()方法必须提供实现，以供父类调用，实现对它的启动/关闭
	addLifecycleListener findLifecycleListeners 和 removeLifecycleListener
	这三个方法与事件监听相关(观察者模式)


2.LifecycleEvent类：生命周期事件
	public final class LifecycleEvent extends EventObject {
	
		public LifecycleEvent(Lifecycle lifecycle, String type) {
			this(lifecycle, type, null);
		}
	
		public LifecycleEvent(Lifecycle lifecycle, String type, Object data) {
			super(lifecycle);
			this.lifecycle = lifecycle;
			this.type = type;
			this.data = data;
		}
	
		private Object data = null;
	
		private Lifecycle lifecycle = null;
	
		private String type = null;
	
		public Object getData() {
			return (this.data);
		}
		
		public Lifecycle getLifecycle() {
			return (this.lifecycle);
		}
	
		public String getType() {
			return (this.type);
		}
	}


3.LifecycleListener接口：生命周期监听器

	public interface LifecycleListener {
		// 当某个事件监听器监听到相关事件发生时，会调用该方法
		public void lifecycleEvent(LifecycleEvent event);
	}


4.工具类LifecycleSupport
	LifecycleSupport将所有的LifecycleListener存在数组listeners中
	实现了Lifecycle接口的组件可以使用LifecycleSupport类

	public final class LifecycleSupport {
		public LifecycleSupport(Lifecycle lifecycle) {
			super();
			this.lifecycle = lifecycle;
		}
	
		private Lifecycle lifecycle = null;
		private LifecycleListener listeners[] = new LifecycleListener[0];
	
		public void addLifecycleListener(LifecycleListener listener) {
	
			synchronized (listeners) {
				LifecycleListener results[] = new LifecycleListener[listeners.length + 1];
				for (int i = 0; i < listeners.length; i++)
					results[i] = listeners[i];
				results[listeners.length] = listener;
				listeners = results;
			}
	
		}
	
		
		public LifecycleListener[] findLifecycleListeners() {
			return listeners;
		}
	
		public void fireLifecycleEvent(String type, Object data) {
			LifecycleEvent event = new LifecycleEvent(lifecycle, type, data);
			LifecycleListener interested[] = null;
			synchronized (listeners) {
				interested = (LifecycleListener[]) listeners.clone();
			}
			for (int i = 0; i < interested.length; i++)
				interested[i].lifecycleEvent(event);
		}
	
		
		public void removeLifecycleListener(LifecycleListener listener) {
			synchronized (listeners) {
				int n = -1;
				for (int i = 0; i < listeners.length; i++) {
					if (listeners[i] == listener) {
						n = i;
						break;
					}
				}
				if (n < 0)
					return;
				LifecycleListener results[] = new LifecycleListener[listeners.length - 1];
				int j = 0;
				for (int i = 0; i < listeners.length; i++) {
					if (i != n)
						results[j++] = listeners[i];
				}
				listeners = results;
			}
		}
	}
	
	
5.一个例子

	例如context SimpleContext[Lifecycle]，拥有LifecycleSupport属性(实现了Lifecycle接口的组件可以使用LifecycleSupport类)
		protected LifecycleSupport lifecycle = new LifecycleSupport(this);
		
	添加事件监听器[LifecycleListener]：
		public void addLifecycleListener(LifecycleListener listener) {
	    	lifecycle.addLifecycleListener(listener);
	  	}
	
	移除事件监听器[LifecycleListener]：
		public void removeLifecycleListener(LifecycleListener listener) {
	    	lifecycle.removeLifecycleListener(listener);
	  	}
	
	触发事件：
		通过lifecycle.fireLifecycleEvent()方法，类触发某个事件。
		而该方法会调用LifecycleListener实例的lifecycleEvent(event)方法实际进行动作：
			for (int i = 0; i < interested.length; i++)
				interested[i].lifecycleEvent(event[LifecycleEvent]);
		

6.应用程序实例

	6.1 SimpleContext
	与上一节相比SimpleContext在实现了Context, Pipeline的基础上，又实现了Lifecycle	接口，几个方法实现如下：

	// 如前所述，通过LifecycleSupport实例，完成addLifecycleListener，removeLifecycleListener
	public void addLifecycleListener(LifecycleListener listener) {
		lifecycle.addLifecycleListener(listener);
	}

	public void removeLifecycleListener(LifecycleListener listener) {
		lifecycle.removeLifecycleListener(listener);
	}
	
	public LifecycleListener[] findLifecycleListeners() {
		return null;
	}

	// 需要将所有子容器、以及相关组件包括载入器Loader、管道Pipeline、映射器Mapper等都启动
	public synchronized void start() throws LifecycleException {
		
		// 触发BEFORE_START_EVENT事件
		lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);
		started = true;
		
		// loader启动
		if ((loader != null) && (loader instanceof Lifecycle))
			((Lifecycle) loader).start();

		// 子容器启动
		Container children[] = findChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof Lifecycle)
				((Lifecycle) children[i]).start();
		}

		//Pipeline中的Value启动 
		if (pipeline instanceof Lifecycle)
			((Lifecycle) pipeline).start();
			
		// 触发START_EVENT事件
		lifecycle.fireLifecycleEvent(START_EVENT, null);

		// 触发AFTER_START_EVENT事件
		lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);
	}

	public void stop() throws LifecycleException {
		
		// 触发事件
		lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);
		lifecycle.fireLifecycleEvent(STOP_EVENT, null);
		started = false;
		// Stop the Valves in our pipeline (including the basic), if any
		if (pipeline instanceof Lifecycle) {
			((Lifecycle) pipeline).stop();
		}

		// Stop our child containers, if any
		Container children[] = findChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof Lifecycle)
				((Lifecycle) children[i]).stop();
		}
		
		if ((loader != null) && (loader instanceof Lifecycle)) {
			((Lifecycle) loader).stop();
		}
		
		// 触发事件
		lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);
	}
		
		
	
	6.2 SimpleContextLifecycleListener：应用实现了一个生命周期监听器
		public void lifecycleEvent(LifecycleEvent event) {
			Lifecycle lifecycle = event.getLifecycle();
			System.out.println("SimpleContextLifecycleListener's event " + event.getType().toString());
			if (Lifecycle.START_EVENT.equals(event.getType())) {
				System.out.println("Starting context.");
			} else if (Lifecycle.STOP_EVENT.equals(event.getType())) {
				System.out.println("Stopping context.");
			}
		}

7.Bootstrap

	与前面不一样的部分：
		为context添加生命周期监听器
		LifecycleListener listener = new SimpleContextLifecycleListener();
		((Lifecycle) context).addLifecycleListener(listener);
		
		启动context的生命周期
		((Lifecycle) context).start();
		在start方法中
		1. 会处理自身的一些事件：
			通过LifecycleSupport实例的fireLifecycleEvent方法，
			来调用每个注册的生命周期监听器的lifecycleEvent(LifecycleEvent event)方法
			LifecycleEvent的构造方法如下：
				public LifecycleEvent(Lifecycle lifecycle, String type, Object data) {
					super(lifecycle);
					this.lifecycle = lifecycle;
					this.type = type;
					this.data = data;
		
				}
			本例中实现的生命周期监听器如6.2 所示，通过event参数，可以获取事件发生的生命周期，事件的类型以及传入的data
			该生命周期监听器没有什么实际的意义
		2. 并且触发相关生命周期的start方法
			而这些被触发的start方法，也将处理自身的一些事件，同时触发一些相关生命周期的start方法。。。
			依次类推，完成Tomcat每个组件的生命周期的启动过程！
		
			

