package dnd.jon.spellbook;

// Base class for quantity types
// The ValueType class is meant to account for cases that necessarily have a standard value
// i.e. 'Touch' for a spell's range, 'Until dispelled' for a duration, etc.
// value represents the value (i.e. 30 in 30 seconds, or 1 in 1 mile)
// unit represents the type of unit, so that the actual quantity can be calculated
// str is for a string representation, if it's not obviously reconstructible from the data
public abstract class Quantity<ValueType extends Enum<ValueType> & QuantityType, UnitType extends Unit> implements Comparable<Quantity<ValueType, UnitType>> {

    final ValueType type;
    final int value;
    final UnitType unit;
    final String str;

    // Constructor
    Quantity(ValueType type, int value, UnitType unit, String str) {
        this.type = type;
        this.value = value;
        this.unit = unit;
        this.str = str;
    }

    // Convenience constructor with no string
    Quantity(ValueType type, int value, UnitType unit) {
        this(type, value, unit, "");
    }

    // What's the value of this quantity in the base units?
    // i.e. 1 mile = 5280 feet; feet are the smallest length unit (in this context)
    int baseValue() { return value * unit.value(); }

    // Getters
    public ValueType getType() { return type; }
    public UnitType getUnit() { return unit; }
    public int getValue() { return value; }

    // Is this quantity of the spanning type?
    // i.e. Ranged for range
    // basically, not one of the unusual types
    boolean isTypeSpanning() { return type.isSpanningType(); }

    // For sorting
    public int compareTo(Quantity<ValueType, UnitType> other) {
        if (type == other.type) {
            return baseValue() - other.baseValue();
        }
        return type.ordinal() - other.type.ordinal();
    }

    // Subclasses can implement constructing the string from data
    abstract public String string();

}
