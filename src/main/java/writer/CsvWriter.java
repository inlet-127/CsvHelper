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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import annotations.ColumnName;
import annotations.Ignore;
import annotations.Mask;
import annotations.Order;
import annotations.TargetSuperClass;

public class CsvWriter {

	/**
	 * ヘッダを書き込む
	 * 
	 * @param obj
	 * @return
	 */
	public static String writeHeader(Class<?> clazz) {
		Objects.requireNonNull(clazz);
		final List<Field> fList = sort(getField(clazz));
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
		sb.append(System.lineSeparator());
		return sb.toString();
	}

	public static <T> String write(List<T> obj, boolean isHeaderNeeded) {
		Objects.requireNonNull(obj);
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < obj.size(); i++) {
			T elem = obj.get(i);
			if (i == 0 && isHeaderNeeded) {
				sb.append(writeHeader(elem.getClass()));
			}
			final List<Field> fList = sort(getField(elem.getClass()));
			for (int j = 0; j < fList.size(); j++) {
				Field f = fList.get(j);
				if (isTargetField(f)) {
					f.setAccessible(true);
					Object val;
					try {
						val = mask(f, f.get(elem));
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
					if (i != 0) {
						sb.append(",");
					}
					sb.append("\"");
					sb.append(val);
					sb.append("\"");
					f.setAccessible(false);
				}
			}
			sb.append(System.lineSeparator());
		}
		return sb.toString();
	}

	public static <T> String write(List<T> obj, Charset cs, boolean isHeaderNeeded) {
		return new String(writeByte(obj, cs, isHeaderNeeded), cs);
	}

	public static <T> byte[] writeByte(List<T> obj, Charset cs, boolean isHeaderNeeded) {
		return encode(cs, write(obj, isHeaderNeeded));
	}

	public static byte[] encode(Charset cs, String text) {
		Objects.requireNonNull(cs);
		CharsetEncoder csEncoder = cs.newEncoder();
		ByteBuffer bytebuffer;
		try {
			bytebuffer = csEncoder.encode(CharBuffer.wrap(text));
			byte[] bytes = new byte[bytebuffer.limit()];
			bytebuffer.get(bytes);
			return bytes;
		} catch (CharacterCodingException e) {
			e.printStackTrace();
		}
		// 到達不可能
		return null;
	}

	/**
	 * 出力順を制御する
	 * 
	 * @param fList
	 * @return
	 */
	private static List<Field> sort(List<Field> fList) {
		List<Field> result = new ArrayList<>();
		List<Field> nonAnnotations = new ArrayList<>();
		Map<Integer, Field> fieldOrderMap = new HashMap<>();
		for (Field f : fList) {
			Order annotation = getAnnotation(f, Order.class);
			if (Objects.isNull(annotation)) {
				nonAnnotations.add(f);
			} else {
				int value = annotation.value();
				value = Objects.isNull(fieldOrderMap.get(value)) ? value : value + 1;
				fieldOrderMap.put(value, f);
			}
		}
		if (0 == fieldOrderMap.size()) {
			return fList;
		}
		result.addAll(nonAnnotations);
		fieldOrderMap = fieldOrderMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors
				.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, HashMap::new));

		for (Map.Entry<Integer, Field> entry : fieldOrderMap.entrySet()) {
			int order = entry.getKey();
			int index = order >= result.size() ? result.size() : order;
			result.add(index, entry.getValue());
		}
		return result;
	}

	/**
	 * フィールドを取得
	 * 
	 * @param <T>
	 * @param obj
	 * @return
	 */
	private static List<Field> getField(Class<?> clazz, Field... fields) {
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
	private static boolean isTargetField(Field f) {
		return isAnnotationNotExist(f, Ignore.class);
	}

	/**
	 * 設定された値を基にマスクする
	 * 
	 * @param f
	 * @param value
	 * @return
	 */
	private static Object mask(Field f, Object value) {
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
	private static String columnName(Field f, String value) {
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
	private static <T extends Annotation> boolean isAnnotationNotExist(Class<?> clazz, Class<T> annotationClazz) {
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
	private static <T extends Annotation> boolean isAnnotationNotExist(AccessibleObject ao, Class<T> annotationClazz) {
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
	private static <T extends Annotation> T getAnnotation(AccessibleObject ao, Class<T> annotationClazz) {
		return ao.getAnnotation(annotationClazz);
	}

}
