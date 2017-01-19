package cn.hq.utils.http.agent;

import java.io.InputStream;

import cn.hq.utils.http.HttpServiceException;
import cn.hq.utils.io.EmptyInputStream;

class RequestBodyResolvers {

	public static final RequestBodyResolver NULL_BODY_RESOLVER = new NullBodyResolver();

	/**
	 * 创建基于参数列表的解析器；
	 * 
	 * @param argIndex
	 *            要作为 body 输出的参数的位置；
	 * @param converter
	 *            参数值转换器；
	 * @return
	 */
	public static RequestBodyResolver createArgumentResolver(ArgDefEntry<RequestBodyDefinition> defEntry) {
		return new ArgurmentResolver(defEntry);
	}

	private static final class ArgurmentResolver implements RequestBodyResolver {

		private ArgDefEntry<RequestBodyDefinition> defEntry;

		public ArgurmentResolver(ArgDefEntry<RequestBodyDefinition> defEntry) {
			this.defEntry = defEntry;
		}

		@Override
		public InputStream resolve(Object[] args) {
			Object arg = args[defEntry.getIndex()];
			if (arg == null && defEntry.getDefinition().isRequired()) {
				throw new HttpServiceException("The required body argument is null!");
			}
			return defEntry.getDefinition().getConverter().toInputStream(arg);
		}

	}

	/**
	 * 空的 body 解析器；
	 * 
	 * @author haiq
	 *
	 */
	private static final class NullBodyResolver implements RequestBodyResolver {
		
		@Override
		public InputStream resolve(Object[] args) {
			return EmptyInputStream.INSTANCE;
		}

	}
}
