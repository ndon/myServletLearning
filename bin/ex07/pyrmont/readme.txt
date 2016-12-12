Tomcat 有3种日志记录器

FileLogger，SystemErrLogger，SystemOutLogger
这三个类均继承自LoggerBase

1. LoggerBase
	LoggerBase实现了Logger接口：
	
		public abstract class LoggerBase implements Logger
	
	Logger接口中定义了日志等级，越严重的问题，等级越低
	
		public static final int FATAL = Integer.MIN_VALUE;
		
		public static final int ERROR = 1;
		
		public static final int WARNING = 2;
		
		public static final int INFORMATION = 3;
		
		public static final int DEBUG = 4;
	
	LoggerBase实现了Logger接口中除public void log(String msg)外的所有方法，
	
	而各种日志记录器根据自身功能需求，实现抽象方法log(String msg)。
	
	在LoggerBase中，用verbosity属性设置日志等级，默认是ERROR
	只有当传入的日志等级小于verbosity时，才进行记录：
		public void log(String message, int verbosity) {
			if (this.verbosity >= verbosity)
				log(message);
		}


2. SystemOutLogger：打印日志信息
	public void log(String msg) {
		System.out.println(msg);
	}
	
3. SystemErrLogger：打印错误信息
	public void log(String msg) {

		System.err.println(msg);

	}
	
4. FileLogger：将日志写到文件中
	public void log(String msg) {
		// Construct the timestamp we will use, if requested
		Timestamp ts = new Timestamp(System.currentTimeMillis());
		String tsString = ts.toString().substring(0, 19);
		String tsDate = tsString.substring(0, 10);

		// If the date has changed, switch log files
		if (!date.equals(tsDate)) {
			synchronized (this) {
				if (!date.equals(tsDate)) {
					close();
					date = tsDate;
					open();
				}
			}
		}

		// Log this message, timestamped if necessary
		if (writer != null) {
			if (timestamp) {
				writer.println(tsString + " " + msg);
			} else {
				writer.println(msg);
			}
		}

	}
	
	FileLogger也实现了LifeCycle接口。
	
	