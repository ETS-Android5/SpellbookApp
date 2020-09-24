package dnd.jon.spellbook;

import java.util.SortedSet;
import java.util.TreeSet;

class SpellBuilder {

    SpellBuilder() { }

    // Member values
    private String name = "";
    private String description = "";
    private String higherLevel = "";
    private int page = 0;
    private Range range = new Range();
    private boolean[] components = {false, false, false};
    private String material = "";
    private boolean ritual = false;
    private Duration duration = new Duration();
    private boolean concentration = false;
    private CastingTime castingTime = new CastingTime();
    private int level = 0;
    private School school = School.ABJURATION;
    private SortedSet<CasterClass> classes = new TreeSet<>();
    private SortedSet<Subclass> subclasses = new TreeSet<>();
    private Sourcebook sourcebook = Sourcebook.PLAYERS_HANDBOOK;

    // Setters
    SpellBuilder setName(String nameIn) {name = nameIn; return this;}
    SpellBuilder setDescription(String descriptionIn) {description = descriptionIn; return this;}
    SpellBuilder setHigherLevelDesc(String higherLevelIn) {higherLevel = higherLevelIn; return this;}
    SpellBuilder setPage(int pageIn) {page = pageIn; return this;}
    SpellBuilder setRange(Range rangeIn) {range = rangeIn; return this;}
    SpellBuilder setComponents(boolean[] componentsIn) {components = componentsIn; return this;}
    SpellBuilder setMaterial(String materialIn) {material = materialIn; return this;}
    SpellBuilder setRitual(boolean ritualIn) {ritual = ritualIn; return this;}
    SpellBuilder setDuration(Duration durationIn) {duration = durationIn; return this;}
    SpellBuilder setConcentration(boolean concentrationIn) {concentration = concentrationIn; return this;}
    SpellBuilder setCastingTime(CastingTime castingTimeIn) {castingTime = castingTimeIn; return this;}
    SpellBuilder setLevel(int levelIn) {level = levelIn; return this;}
    SpellBuilder setSchool(School schoolIn) {school = schoolIn; return this;}
    SpellBuilder setClasses(SortedSet<CasterClass> classesIn) {classes = classesIn; return this;}
    SpellBuilder setSubclasses(SortedSet<Subclass> subclassesIn) {subclasses = subclassesIn; return this;}
    SpellBuilder setSourcebook(Sourcebook sourcebookIn) {sourcebook = sourcebookIn; return this;}

    SpellBuilder addClass(CasterClass cc) { classes.add(cc); return this; }
    SpellBuilder addSubclass(Subclass sc) { subclasses.add(sc); return this; }

    Spell build() {
        return new Spell(name, description, higherLevel, page, range, components, material, ritual, duration, concentration, castingTime, level, school, classes, subclasses, sourcebook);
    }

    void reset() {
        name = "";
        description = "";
        higherLevel = "";
        page = 0;
        range = new Range();
        components = new boolean[]{false, false, false};
        material = "";
        ritual = false;
        duration = new Duration();
        concentration = false;
        castingTime = new CastingTime();
        level = 0;
        school = School.ABJURATION;
        classes = new TreeSet<>();
        subclasses = new TreeSet<>();
        sourcebook = Sourcebook.PLAYERS_HANDBOOK;
    }

    Spell buildAndReset() {
        Spell spell = build();
        reset();
        return spell;
    }

}
