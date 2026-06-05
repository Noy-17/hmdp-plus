package org.javaup.exception;

import lombok.Data;

@Data
public class ArgumentError {
	
	private String argumentName;
	
	private String message;
}
