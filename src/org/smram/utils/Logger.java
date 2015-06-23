package org.smram.utils;

/**
 * Basic self-contained logger, logs at different levels
 * @author smram
 *
 */
public class Logger {

	final LogLevel logLevel;
	
	public Logger(LogLevel level) {
		logLevel = level;
	}
	
	public void info(String s) {
		System.out.println(LogLevel.INFO.printName + s);
	}
	
	public void debug(String s) {
		System.out.println(LogLevel.DEBUG.printName + s);
	}

	public void trace(String s) {
		System.out.println(LogLevel.TRACE.printName + s);
	}

	public void trickle(String s) {
		System.out.println(LogLevel.TRICKLE.printName + s);
	}
	
	public boolean isDebugEnabled() {
		return logLevel.level >= LogLevel.DEBUG.level;
	}
	
	public boolean isTraceEnabled() {
		return logLevel.level >= LogLevel.TRACE.level;
	}
	
	public boolean isTrickleEnabled() {
		return logLevel.level >= LogLevel.TRICKLE.level;
	}

	public enum LogLevel { 
		INFO(1, "LOG.INFO: "), 
		DEBUG(2, "LOG.DEBUG: "), 
		TRACE(3, "LOG.TRACE: "), 
		TRICKLE(4, "LOG.TRICKLE: ");
		
		final int level;
		final String printName;
		
		LogLevel(int level, String printName) {
			this.level = level;
			this.printName = printName;
		}
	}	
}
