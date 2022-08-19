package writer;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Objects;

import annotations.Ignore;
import annotations.Mask;

/**
 * CSV書き込みクラス
 * 
 * @author k.urakawa
 *
 */
public class CsvWriter {

	/**
	 * 指定された文字コードでcsv形式の文字列を作成
	 * 
	 * @param <T>
	 * @param obj
	 * @param cs
	 * @return
	 * @throws IOException
	 */
	public static <T> String write(T obj, Charset cs) throws IOException {
		Objects.requireNonNull(obj);
		final Field[] fArray = getField(obj);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < fArray.length; i++) {
			Field f = fArray[i];
			if (isTargetField(f)) {
				f.setAccessible(true);
				try {
					Object value = f.get(obj);
					if (i != 0) {
						sb.append(",");
					}
					sb.append("\"");
					value = Objects.isNull(value) ? "" : CsvWriter.mask(f, value);
					sb.append(value);
					sb.append("\"");
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
				f.setAccessible(false);
			}
		}
		return new String(CsvWriter.encode(cs, sb.toString()), cs);
	}

	/**
	 * 実行環境のデフォルト文字コードでcsv形式の文字列を作成
	 * 
	 * @param <T>
	 * @param obj
	 * @return
	 * @throws IOException
	 */
	public static <T> String write(T obj) throws IOException {
		System.out.println(System.getProperty("file.encoding"));
		return CsvWriter.write(obj, Charset.forName(System.getProperty("file.encoding")));
	}

	/**
	 * 指定された文字コードでエンコード
	 * 
	 * @param cs
	 * @param text
	 * @return
	 * @throws IOException
	 */
	private static byte[] encode(Charset cs, String text) throws IOException {
		CharsetEncoder csEncoder = cs.newEncoder();
		ByteBuffer bytebuffer;
		bytebuffer = csEncoder.encode(CharBuffer.wrap(text));
		byte[] bytes = new byte[bytebuffer.limit()];
		bytebuffer.get(bytes);
		return bytes;
	}

	/**
	 * フィールドを取得
	 * 
	 * @param <T>
	 * @param obj
	 * @return
	 */
	private static <T> Field[] getField(T obj) {
		return obj.getClass().getDeclaredFields();
	}

	/**
	 * 出力対象の属性か判定
	 * 
	 * @param f
	 * @return
	 */
	private static boolean isTargetField(Field f) {
		return CsvWriter.isAnnotationNotExist(f, Ignore.class);
	}

	/**
	 * 設定された値を基にマスクする
	 * 
	 * @param f
	 * @param value
	 * @return
	 */
	private static Object mask(Field f, Object value) {
		Mask mask = CsvWriter.getAnnotation(f, Mask.class);
		return Objects.isNull(mask) ? value : mask.value();
	}

	/**
	 * アノテーションが設定されているか判定
	 * 
	 * @param <T>
	 * @param f
	 * @param annotationClazz
	 * @return
	 */
	private static <T extends Annotation> boolean isAnnotationNotExist(Field f, Class<T> annotationClazz) {
		return Objects.isNull(f.getAnnotation(annotationClazz));
	}

	/**
	 * アノテーションを取得
	 * 
	 * @param <T>
	 * @param f
	 * @param annotationClazz
	 * @return
	 */
	private static <T extends Annotation> T getAnnotation(Field f, Class<T> annotationClazz) {
		return f.getAnnotation(annotationClazz);
	}

}
