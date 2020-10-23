package dnd.jon.spellbook;

import android.content.Context;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public class DisplayUtils {

    ///// General functions
    static <E extends Enum<E>,T> E getEnumFromResourceValue(Context context, Class<E> enumType, T resourceValue, ToIntFunction<E> enumIDGetter, BiFunction<Context,Integer,T> valueGetter) {
        final E[] es = enumType.getEnumConstants();
        if (es == null) { return null; }
        for (E e : es) {
            final int id = enumIDGetter.applyAsInt(e);
            if (resourceValue.equals(valueGetter.apply(context, id))) {
                return e;
            }
        }
        return null;
    }

    static <E extends Enum<E>> E getEnumFromResourceID(Integer resourceID, Class<E> enumType, ToIntFunction<E> enumIDGetter) {
        return getEnumFromResourceValue(null, enumType, resourceID, enumIDGetter, (ctx, id) -> id); // Use whatever ID as the 'resource value'
    }

    static <E extends Enum<E> & NameDisplayable> E getEnumFromDisplayName(Context context, Class<E> enumType, String name) {
        return getEnumFromResourceValue(context, enumType, name, E::getDisplayNameID, Context::getString);
    }

    public static <T,R> R getProperty(Context context, T item, Function<T,Integer> resourceIDGetter, BiFunction<Context,Integer,R> resourceGetter) {
        return resourceGetter.apply(context, resourceIDGetter.apply(item));
    }

    ///// Display names

    static <E extends Enum<E>> String[] getDisplayNames( Context context, Class<E> enumType, BiFunction<Context,E,String> idGetter) {
        final E[] es = enumType.getEnumConstants();
        if (es == null) { return null; }
        final String[] names = Arrays.stream(es).map((e) -> idGetter.apply(context, e)).toArray(String[]::new);
        Arrays.sort(names);
        return names;
    }

    static <E extends Enum<E> & NameDisplayable> String[] getDisplayNames(Context context, Class<E> enumType) {
        return getDisplayNames(context, enumType, (ctx, e) -> ctx.getString(e.getDisplayNameID()));
    }

    public static <T extends NameDisplayable> String getDisplayName(Context context, T item) {
        return getProperty(context, item, NameDisplayable::getDisplayNameID, Context::getString);
    }

    ///// For the spell window

    public static String classesString(Context context, Spell spell) {
        final Collection<CasterClass> classes = spell.getClasses();
        final String[] classStrings = new String[classes.size()];
        int i = 0;
        for (CasterClass cc : classes) {
            classStrings[i++] = getDisplayName(context, cc);
        }
        return TextUtils.join(", ", classStrings);
    }

    public static String sourcebookCode(Context context, Spell spell) {
        return context.getString(spell.getSourcebook().getCodeID());
    }

    public static String locationString(Context context, Spell spell) {
        final String code = sourcebookCode(context, spell);
        return code + " " + spell.getPage();
    }

    ///// Units

    static <U extends Enum<U> & Unit> U unitFromString(Context context, Class<U> unitType, String s) {
        System.out.println("Unit string is " + s);
        final U unit = SpellbookUtils.coalesce(getEnumFromResourceValue(context, unitType, s, Unit::getSingularNameID, Context::getString), getEnumFromResourceValue(context, unitType, s, Unit::getPluralNameID, Context::getString));
        if (unit == null) { System.out.println("NULL HERE"); }
        return SpellbookUtils.coalesce(getEnumFromResourceValue(context, unitType, s, Unit::getSingularNameID, Context::getString), getEnumFromResourceValue(context, unitType, s, Unit::getPluralNameID, Context::getString));
    }

    public static String string(Context context, Duration duration) {
        return duration.makeString((t) -> getDisplayName(context, t), (u) -> context.getString(u.getSingularNameID()), (u) -> context.getString(u.getPluralNameID()));
    }
    public static String string(Context context, CastingTime castingTime) {
        return castingTime.makeString((t) -> getDisplayName(context, t), (u) -> context.getString(u.getSingularNameID()), (u) -> context.getString(u.getPluralNameID()));
    }
    public static String string(Context context, Range range) {
        return range.makeString((t) -> getDisplayName(context, t), (u) -> context.getString(u.getSingularNameID()), (u) -> context.getString(u.getPluralNameID()), context.getString(R.string.foot_radius));
    }

    static String getSingularName(Context context, Unit unit) {
        return getProperty(context, unit, Unit::getSingularNameID, Context::getString);
    }

    static String getPluralName(Context context, Unit unit) {
        return getProperty(context, unit, Unit::getPluralNameID, Context::getString);
    }


    ///// Quantities

    static Duration durationFromString(Context context, String s) {
        return Duration.fromString(s, (t) -> getDisplayName(context, t), context.getString(R.string.concentration_prefix), (us) -> unitFromString(context, TimeUnit.class, us));
    }

    static CastingTime castingTimeFromString(Context context, String s) {
        return CastingTime.fromString(s, (ct) -> getProperty(context, ct, CastingTime.CastingTimeType::getParseNameID, Context::getString), (us) -> unitFromString(context, TimeUnit.class, us));
    }

    static Range rangeFromString(Context context, String s) {
        return Range.fromString(s, (t) -> getDisplayName(context, t), (us) -> unitFromString(context, LengthUnit.class, us));
    }

}
