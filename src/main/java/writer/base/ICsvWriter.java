package writer.base;

import java.nio.charset.Charset;
import java.util.List;

public interface ICsvWriter {

	<T> String write(List<T> obj);

	<T> String write(List<T> obj, Charset cs);
	
	byte[] encode(Charset cs, String text);
	
	String writeHeader(Class<?> obj);

	<T> byte[] writeByte(List<T> obj, Charset cs);

}
