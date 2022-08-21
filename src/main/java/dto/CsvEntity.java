package dto;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;

import dto.base.ICsvEntity;

public class CsvEntity implements ICsvEntity {

	protected final String text;

	public CsvEntity(String text) {
		this.text = text;
	}

	@Override
	public String getText() {
		return this.text;
	}

	@Override
	public String getText(Charset cs) {
		return new String(this.getByte(cs), cs);
	}

	@Override
	public byte[] getByte(Charset cs) {
		Objects.requireNonNull(cs);
		CharsetEncoder csEncoder = cs.newEncoder();
		ByteBuffer bytebuffer;
		try {
			bytebuffer = csEncoder.onMalformedInput(CodingErrorAction.REPLACE)
					.onUnmappableCharacter(CodingErrorAction.REPLACE).encode(CharBuffer.wrap(text));
			byte[] bytes = new byte[bytebuffer.limit()];
			bytebuffer.get(bytes);
			return bytes;
		} catch (CharacterCodingException e) {
			e.printStackTrace();
		}
		// 到達不可能
		return null;
	}

	@Override
	public void outputFile(String fileName, Charset cs) throws IOException {
		File f = new File(fileName);
		OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(f), cs);
		try (BufferedWriter bw = new BufferedWriter(osw);) {
			bw.write(this.getText());
		}
	}

}
