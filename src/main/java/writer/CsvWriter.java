package writer;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import annotations.ColumnName;
import annotations.Ignore;
import annotations.Mask;
import annotations.TargetSuperClass;
import writer.base.ICsvWriter;

public class CsvWriter implements ICsvWriter {

	/**
	 * ヘッダを書き込む
	 * 
	 * @param obj
	 * @return
	 */
	@Override
	public String writeHeader(Object obj) {
		Objects.requireNonNull(obj);
		final List<Field> fList = getField(obj.getClass());
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < fList.size(); i++) {
			Field f = fList.get(i);
			if (isTargetField(f)) {
				f.setAccessible(true);
				String name = f.getName();
				if (i != 0) {
					sb.append(",");
				}
				sb.append("\"");
				name = columnName(f, name);
				sb.append(name);
				sb.append("\"");
				f.setAccessible(false);
			}
		}
		sb.append("\n");
		return sb.toString();
	}

	@Override
	public String write(Object obj) {
		Objects.requireNonNull(obj);
		final List<Field> fList = getField(obj.getClass());
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < fList.size(); i++) {
			Field f = fList.get(i);
			if (isTargetField(f)) {
				f.setAccessible(true);
				String name = f.getName();
				if (i != 0) {
					sb.append(",");
				}
				sb.append("\"");
				name = columnName(f, name);
				sb.append(name);
				sb.append("\"");
				f.setAccessible(false);
			}
		}
		sb.append("\n");
		return sb.toString();
	}

	@Override
	public String write(Object obj, Charset cs) {
		return new String(writeByte(obj, cs), cs);
	}

	@Override
	public byte[] writeByte(Object obj, Charset cs) {
		Objects.requireNonNull(obj);
		final List<Field> fList = getField(obj.getClass());
		StringBuilder sb = new StringBuilder(writeHeader(obj));
		for (int i = 0; i < fList.size(); i++) {
			Field f = fList.get(i);
			if (isTargetField(f)) {
				f.setAccessible(true);
				try {
					Object value = f.get(obj);
					if (i != 0) {
						sb.append(",");
					}
					sb.append("\"");
					value = Objects.isNull(value) ? "" : mask(f, value);
					sb.append(value);
					sb.append("\"");
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
				f.setAccessible(false);
			}
		}
		return encode(cs, sb.toString());
	}
	
	@Override
	public byte[] encode(Charset cs, String text) {
		Objects.requireNonNull(cs);
		CharsetEncoder csEncoder = cs.newEncoder();
		ByteBuffer bytebuffer;
		try {
			bytebuffer = csEncoder.encode(CharBuffer.wrap(text));
			byte[] bytes = new byte[bytebuffer.limit()];
			bytebuffer.get(bytes);
			return bytes;
		} catch (CharacterCodingException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		// 到達不可能
		return null;
	}
	
	/**
	 * フィールドを取得
	 * 
	 * @param <T>
	 * @param obj
	 * @return
	 */
	protected List<Field> getField(Class<?> clazz, Field... fields) {
		List<Field> fieldList = fields.length == 0 ? new ArrayList<Field>()
				: new ArrayList<Field>(Arrays.asList(fields));
		fieldList.addAll(new ArrayList<Field>(Arrays.asList(clazz.getDeclaredFields())));
		if (!isAnnotationNotExist(clazz, TargetSuperClass.class)) {
			if (!Object.class.equals(clazz.getSuperclass())) {
				return getField(clazz.getSuperclass(), (Field[]) fieldList.toArray(new Field[] {}));
			}
		}
		return fieldList;
	}

	/**
	 * 出力対象の属性か判定
	 * 
	 * @param f
	 * @return
	 */
	protected boolean isTargetField(Field f) {
		return isAnnotationNotExist(f, Ignore.class);
	}

	/**
	 * 設定された値を基にマスクする
	 * 
	 * @param f
	 * @param value
	 * @return
	 */
	protected Object mask(Field f, Object value) {
		Mask mask = getAnnotation(f, Mask.class);
		return Objects.isNull(mask) ? value : mask.value();
	}

	/**
	 * カラム名を取得
	 * 
	 * @param f
	 * @param value
	 * @return
	 */
	protected String columnName(Field f, String value) {
		ColumnName columnName = getAnnotation(f, ColumnName.class);
		return Objects.isNull(columnName) ? value : columnName.value();
	}

	/**
	 * クラスにアノテーションが設定されているか判定
	 * 
	 * @param <T>
	 * @param f
	 * @param annotationClazz
	 * @return
	 */
	protected <T extends Annotation> boolean isAnnotationNotExist(Class<?> clazz, Class<T> annotationClazz) {
		return Objects.isNull(clazz.getAnnotation(annotationClazz));
	}

	/**
	 * アノテーションが設定されているか判定
	 * 
	 * @param <T>
	 * @param f
	 * @param annotationClazz
	 * @return
	 */
	protected <T extends Annotation> boolean isAnnotationNotExist(AccessibleObject ao,
			Class<T> annotationClazz) {
		return Objects.isNull(ao.getAnnotation(annotationClazz));
	}

	/**
	 * アノテーションを取得
	 * 
	 * @param <T>
	 * @param f
	 * @param annotationClazz
	 * @return
	 */
	protected <T extends Annotation> T getAnnotation(AccessibleObject ao, Class<T> annotationClazz) {
		return ao.getAnnotation(annotationClazz);
	}

}
