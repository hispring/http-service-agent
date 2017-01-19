package cn.hq.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * PropertiesUtils 定义了从 properties 文件到 pojo 对象的转换方法；
 * 
 * 用于对充当配置文件的 properties 文件到定义了配置信息的 POJO 的转换；
 * 
 * 支持 properties 的 key 到 POJO 的字段转换，支持层级的 key 的转换，例如： "user.name" 到 user 字段的对象的
 * name 属性；
 * 
 * @author haiq
 *
 */
public abstract class PropertiesUtils {
	private PropertiesUtils() {
	}

	@SuppressWarnings("unchecked")
	public static <T> T createInstance(Class<T> configClass, Properties configValues) {
		BeanWrapper confBean = new BeanWrapperImpl(configClass);
		confBean.setAutoGrowNestedPaths(true);

		MutablePropertyValues values = new MutablePropertyValues(configValues);
		confBean.setPropertyValues(values, true);

		return (T) confBean.getWrappedInstance();
	}

	/**
	 * 设置配置值；
	 * 
	 * @param obj
	 *            配置对象；配置值将设置到此对象匹配的属性；
	 * @param configValues
	 *            配置值；
	 */
	public static void setValues(Object obj, Properties configValues) {
		BeanWrapper confBean = new BeanWrapperImpl(obj);
		confBean.setAutoGrowNestedPaths(true);

		MutablePropertyValues values = new MutablePropertyValues(configValues);
		confBean.setPropertyValues(values, true);
	}

	/**
	 * 从指定的路径加载配置；
	 * 
	 * @param configClass
	 *            配置对象的类型；
	 * @param configFilePathPattern
	 *            properties配置文件的路径；可以指定 spring 资源路径表达式；
	 * @param charset
	 *            字符集；
	 * @return
	 * @throws IOException
	 */
	public static <T> T load(Class<T> configClass, String configFilePathPattern, String charset) throws IOException {
		Properties props = loadProperties(configFilePathPattern, charset);
		return createInstance(configClass, props);
	}

	/**
	 * 从指定的路径加载配置；
	 * 
	 * @param obj
	 *            配置对象；配置文件的值将设置到此对象匹配的属性；
	 * @param configFilePathPattern
	 *            properties配置文件的路径；可以指定 spring 资源路径表达式；
	 * @param charset
	 *            字符集；
	 * @return
	 * @throws IOException
	 */
	public static void load(Object obj, String configFilePathPattern, String charset) throws IOException {
		Properties props = loadProperties(configFilePathPattern, charset);
		setValues(obj, props);
	}

	public static Properties loadProperties(String configFilePathPattern, String charset) throws IOException {
		ResourcePatternResolver resResolver = new PathMatchingResourcePatternResolver();
		Resource configResource = resResolver.getResource(configFilePathPattern);
		InputStream in = configResource.getInputStream();
		try {
			return load(in, charset);
		} finally {
			in.close();
		}
	}
	
	public static Properties loadProperties(File configFile, String charset) throws IOException {
		FileSystemResource resource =new FileSystemResource(configFile);
		InputStream in = resource.getInputStream();
		try {
			return load(in, charset);
		} finally {
			in.close();
		}
	}
	
	public static Properties load(InputStream in, String charset) throws IOException {
		Properties props = new Properties();
		InputStreamReader reader = new InputStreamReader(in, charset);
		try {
			props.load(reader);
		} finally {
			reader.close();
		}
		return props;
	}
	
	public static Properties load(byte[] bytes, String charset) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		return load(in, charset);
	}

	/**
	 * 合并两个 properties ；
	 * 
	 * @param props
	 *            要将其它值合并进来的属性集合；操作将对其产生修改；
	 * @param from
	 *            属性值将要合并进入其它属性集合；操作不对其产生修改；
	 */
	public static void mergeFrom(Properties props, Properties from) {
		mergeFrom(props, from, null);
	}

	/**
	 * 合并两个 properties ；
	 * 
	 * @param props
	 *            要将其它值合并进来的属性集合；操作将对其产生修改；
	 * @param from
	 *            属性值将要合并进入其它属性集合；操作不对其产生修改；
	 * @param propertyNamePrefix
	 *            属性名称前缀；
	 */
	public static void mergeFrom(Properties props, Properties from, String propertyNamePrefix) {
		if (propertyNamePrefix == null || propertyNamePrefix.length() == 0) {
			for (String name : from.stringPropertyNames()) {
				props.setProperty(name, from.getProperty(name));
			}
		} else {
			for (String name : from.stringPropertyNames()) {
				props.setProperty(propertyNamePrefix + name, from.getProperty(name));
			}
		}
	}
	
	/**
	 * 获取指定 properties 中以指定的前缀开头的子集；
	 * @param props 要抽取的属性集合；
	 * @param propertyNamePrefix 属性名称前缀；
	 * @param trimPrefix 是否在复制的新的属性集合去掉指定的前缀；
	 * @return
	 */
	public static Properties subset(Properties props, String propertyNamePrefix, boolean trimPrefix){
		Properties subProperties = new Properties();
		Set<String> names = props.stringPropertyNames();
		String newName;
		for (String name : names) {
			if (name.startsWith(propertyNamePrefix)) {
				newName = name;
				if (trimPrefix) {
					newName = name.substring(propertyNamePrefix.length());
				}
				subProperties.setProperty(newName, props.getProperty(name));
			}
		}
		return subProperties;
	}

	public static Properties cloneFrom(Properties props) {
		Properties newProps = new Properties();
		Set<String> names = props.stringPropertyNames();
		for (String name : names) {
			newProps.setProperty(name, props.getProperty(name));
		}
		return newProps;
	}

	public static byte[] toBytes(Properties props, String charsetName) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			OutputStreamWriter writer = new OutputStreamWriter(out, charsetName);
			try {
				props.store(writer, null);
				writer.flush();
			} finally{
				writer.close();
			}
			return out.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	public static int getInt(String propertyName, Properties props){
		String value = props.getProperty(propertyName);
		return Integer.parseInt(value);
	}

	public static boolean getBoolean(String propertyName, Properties props) {
		String value = props.getProperty(propertyName);
		return Boolean.parseBoolean(value);
	}
}
