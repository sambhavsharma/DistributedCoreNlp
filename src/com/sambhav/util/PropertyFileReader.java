package com.sambhav.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class PropertyFileReader {
	
	final static String propertyFilePath = "./properties/config";
	
	public PropertyFileReader() {
		super();
	}

	public static Properties GetPropertyFromConfigFile(FileInputStream fileInputStreamForReadingConfigFile) {
		try {
			Properties properties = new Properties();
			fileInputStreamForReadingConfigFile  = new FileInputStream(propertyFilePath);
			/*
			 * Load property file in Properties object
			 */
			properties.load(fileInputStreamForReadingConfigFile);

			return properties;
		}catch(FileNotFoundException ex){
			ex.printStackTrace();
		}catch (IOException ex) {
			ex.printStackTrace();
		}

		return null;
	}
}