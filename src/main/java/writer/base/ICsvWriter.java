package writer.base;

import java.nio.charset.Charset;

public interface ICsvWriter {

	String write(Object obj);

	String write(Object obj, Charset cs);
	
	byte[] writeByte(Object obj, Charset cs);
	
	byte[] encode(Charset cs, String text);
	
	String writeHeader(Object obj);

}
