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
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

import annotations.ColumnName;
import annotations.Ignore;
import annotations.Mask;
import annotations.Order;
import annotations.TargetSuperClass;
import annotations.TargetSuperClasses;

/**
 * CSV書き込みクラス
 */
public class CsvWriter {

	public static void main(String... a) {
		Sample s = new Sample();
		s.id = 1;
		s.name = "sample";
		s.text = "sample text";
		s.oyaId = 2;
		s.oyaName = "sample1";
		s.oyaText = "sample1 text";
		List<Sample> list = new ArrayList<>();
		list.add(s);
		System.out.println(write(list, true));
	}

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
				if (0 < sb.length()) {
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

	/**
	 * CSV形式の文字列を返却
	 * 
	 * @param <T>
	 * @param obj
	 * @param isHeaderNeeded
	 * @return
	 */
	public static <T> String write(List<T> obj, boolean isHeaderNeeded) {
		Objects.requireNonNull(obj);
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < obj.size(); i++) {
			T elem = obj.get(i);
			if (i == 0 && isHeaderNeeded) {
				sb.append(writeHeader(elem.getClass()));
			}
			final List<Field> fList = sort(getField(elem.getClass()));
			int len = sb.length();
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
					if (len < sb.length()) {
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

	/**
	 * 指定した文字コードで変換済みの文字列を返却
	 * 
	 * @param <T>
	 * @param obj
	 * @param cs
	 * @param isHeaderNeeded
	 * @return
	 */
	public static <T> String write(List<T> obj, Charset cs, boolean isHeaderNeeded) {
		return new String(writeByte(obj, cs, isHeaderNeeded), cs);
	}

	/**
	 * 指定した文字コードでエンコード済みのバイナリを返却
	 * 
	 * @param <T>
	 * @param obj
	 * @param cs
	 * @param isHeaderNeeded
	 * @return
	 */
	public static <T> byte[] writeByte(List<T> obj, Charset cs, boolean isHeaderNeeded) {
		return encode(cs, write(obj, isHeaderNeeded));
	}

	/**
	 * 指定した文字コードでエンコードする
	 * 
	 * @param cs
	 * @param text
	 * @return
	 */
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
		NavigableMap<Integer, Field> fieldOrderMap = new TreeMap<>();
		for (Field f : fList) {
			Order annotation = getAnnotation(f, Order.class);
			if (Objects.isNull(annotation)) {
				nonAnnotations.add(f);
			} else {
				int value = annotation.value() - 1;
				value = Objects.isNull(fieldOrderMap.get(value)) ? value : fieldOrderMap.lastKey() + 1;
				fieldOrderMap.put(value, f);
			}
		}
		if (0 == fieldOrderMap.size()) {
			return fList;
		}
		result.addAll(nonAnnotations);

		int previousIndex = -1;
		for (Map.Entry<Integer, Field> entry : fieldOrderMap.entrySet()) {
			int order = entry.getKey();
			int index = order >= result.size() ? result.size() : previousIndex <= order ? order : previousIndex + 1;
			result.add(index, entry.getValue());
			previousIndex = index;
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
		if (isAnnotationExist(clazz, TargetSuperClasses.class)) {
			return getParentField(clazz, (Field[]) fieldList.toArray(new Field[] {}));
		}
		if (isAnnotationExist(clazz, TargetSuperClass.class)) {
			if (!Objects.isNull(clazz.getSuperclass()) && !Object.class.equals(clazz.getSuperclass())) {
				return getField(clazz.getSuperclass(), (Field[]) fieldList.toArray(new Field[] {}));
			}
		}
		return fieldList;
	}

	/**
	 * フィールドを取得
	 * 
	 * @param <T>
	 * @param obj
	 * @return
	 */
	private static List<Field> getParentField(Class<?> clazz, Field... fields) {
		List<Field> fieldList = fields.length == 0 ? new ArrayList<Field>()
				: new ArrayList<Field>(Arrays.asList(fields));
		Class<?> superClass = clazz.getSuperclass();
		if (!Objects.isNull(superClass) && !Object.class.equals(superClass)) {
			fieldList.addAll(new ArrayList<Field>(Arrays.asList(clazz.getSuperclass().getDeclaredFields())));
			return getField(superClass.getSuperclass(), (Field[]) fieldList.toArray(new Field[] {}));
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
		return !isAnnotationExist(f, Ignore.class);
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
	private static <T extends Annotation> boolean isAnnotationExist(Class<?> clazz, Class<T> annotationClazz) {
		return !Objects.isNull(clazz.getDeclaredAnnotation(annotationClazz));
	}

	/**
	 * アノテーションが設定されているか判定
	 * 
	 * @param <T>
	 * @param f
	 * @param annotationClazz
	 * @return
	 */
	private static <T extends Annotation> boolean isAnnotationExist(AccessibleObject ao, Class<T> annotationClazz) {
		return !Objects.isNull(ao.getDeclaredAnnotation(annotationClazz));
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
		return ao.getDeclaredAnnotation(annotationClazz);
	}

}
