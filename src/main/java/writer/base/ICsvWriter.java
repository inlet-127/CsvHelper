package writer.base;

import java.nio.charset.Charset;
import java.util.List;

public interface ICsvWriter {

	String write(Object obj);
	
	<T> String write(List<T> obj);

	String write(Object obj, Charset cs);
	
	<T> String write(List<T> obj, Charset cs);
	
	byte[] writeByte(Object obj, Charset cs);
	
	byte[] encode(Charset cs, String text);
	
	String writeHeader(Class<?> obj);

}
