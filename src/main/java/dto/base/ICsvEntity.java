package dto.base;

import java.nio.charset.Charset;

public interface ICsvEntity {

	String getText();

	String getText(Charset cs);

	byte[] getByte(Charset cs);

	void outputFile(String fileName, Charset cs) throws Exception;

}
