package org.plumelib.util;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Iterator;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.common.value.qual.PolyValue;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * Utilities for interning objects. Interning is also known as canonicalization or hash-consing: it
 * returns a single representative object that {@link Object#equals} the object, and the client
 * discards the argument and uses the result instead. Since only one object exists for every set of
 * equal objects, space usage is reduced. Time may also be reduced, since it is possible to use
 * {@code ==} instead of {@code .equals()} for comparisons.
 *
 * <p>Java builds in interning for Strings, but not for other objects. The methods in this class
 * extend interning to all Java objects.
 */
public final class Intern {

  /** This class is a collection of methods; it does not represent anything. */
  private Intern() {
    throw new Error("do not instantiate");
  }

  /** Whether assertions are enabled. */
  private static boolean assertsEnabled = false;

  static {
    assert assertsEnabled = true; // Intentional side-effect!!!
    // Now assertsEnabled is set to the correct value
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Strings
  ///

  /**
   * Replace each element of the array by its interned version. Side-effects the array, but also
   * returns it.
   *
   * @param a the array whose elements to intern in place
   * @return an interned version of a
   * @see String#intern
   */
  @SuppressWarnings("interning") // side-effects the array in place (dangerous, but convenient)
  public static @Interned String[] internStrings(
      String[] a) {
    for (int i = 0; i < a.length; i++) {
      if (a[i] != null) {
        a[i] = a[i].intern();
      }
    }
    return a;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Testing interning
  ///

  /**
   * Returns true if the argument is interned (is canonical among all objects equal to itself).
   *
   * @param value the value to test for interning
   * @return true iff value is interned
   */
  @SuppressWarnings({"interning"}) // interning implementation
  public static boolean isInterned(Object value) {
    if (value == null) {
      // nothing to do
      return true;
    } else if (value instanceof String) {
      return value == ((String) value).intern();
    } else if (value instanceof String[]) {
      return value == intern((String[]) value);
    } else if (value instanceof Integer) {
      return value == intern((Integer) value);
    } else if (value instanceof Long) {
      return value == intern((Long) value);
    } else if (value instanceof int[]) {
      return value == intern((int[]) value);
    } else if (value instanceof long[]) {
      return value == intern((long[]) value);
    } else if (value instanceof Double) {
      return value == intern((Double) value);
    } else if (value instanceof double[]) {
      return value == intern((double[]) value);
    } else if (value instanceof Object[]) {
      return value == intern((Object[]) value);
    } else {
      // Nothing to do, because we don't intern other types.
      // System.out.println("What type? " + value.getClass().getName());
      return true;
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Interning objects
  ///

  /**
   * Hasher object which hashes and compares Integers. This is the obvious implementation that uses
   * intValue() for the hashCode.
   *
   * @see Hasher
   */
  private static final class IntegerHasher implements Hasher {
    /** Create a new IntegerHasher. */
    public IntegerHasher() {}

    @Override
    public boolean equals(Object a1, Object a2) {
      return a1.equals(a2);
    }

    @Override
    public int hashCode(Object o) {
      Integer i = (Integer) o;
      return i.intValue();
    }
  }

  /**
   * Hasher object which hashes and compares Longs. This is the obvious implementation that uses
   * intValue() for the hashCode.
   *
   * @see Hasher
   */
  private static final class LongHasher implements Hasher {
    /** Create a new LongHasher. */
    public LongHasher() {}

    @Override
    public boolean equals(Object a1, Object a2) {
      return a1.equals(a2);
    }

    @Override
    public int hashCode(Object o) {
      Long i = (Long) o;
      return i.intValue();
    }
  }

  /**
   * Hasher object which hashes and compares int[] objects according to their contents.
   *
   * @see Hasher
   * @see Arrays#equals(int[], int[])
   */
  private static final class IntArrayHasher implements Hasher {
    /** Create a new IntArrayHasher. */
    public IntArrayHasher() {}

    @Override
    public boolean equals(Object a1, Object a2) {
      return Arrays.equals((int[]) a1, (int[]) a2);
    }

    @Override
    public int hashCode(Object o) {
      return Arrays.hashCode((int[]) o);
    }
  }

  /**
   * Hasher object which hashes and compares long[] objects according to their contents.
   *
   * @see Hasher
   * @see Arrays#equals (long[], long[])
   */
  private static final class LongArrayHasher implements Hasher {
    /** Create a new LongArrayHasher. */
    public LongArrayHasher() {}

    @Override
    public boolean equals(Object a1, Object a2) {
      return Arrays.equals((long[]) a1, (long[]) a2);
    }

    @Override
    public int hashCode(Object o) {
      return Arrays.hashCode((long[]) o);
    }
  }

  /** Multiplicative constant for use in hashing function. */
  private static final int FACTOR = 23;
  /** Another multiplicative constant for use in hashing function. */
  private static final double DOUBLE_FACTOR = 263;

  /**
   * Hasher object which hashes and compares Doubles.
   *
   * @see Hasher
   */
  private static final class DoubleHasher implements Hasher {
    /** Create a new DoubleHasher. */
    public DoubleHasher() {}

    @Override
    public boolean equals(Object a1, Object a2) {
      return a1.equals(a2);
    }

    @Override
    public int hashCode(Object o) {
      Double d = (Double) o;
      return d.hashCode();
    }
  }

  /**
   * Hasher object which hashes and compares double[] objects according to their contents.
   *
   * @see Hasher
   * @see Arrays#equals(Object[],Object[])
   */
  private static final class DoubleArrayHasher implements Hasher {
    /** Create a new DoubleArrayHasher. */
    public DoubleArrayHasher() {}

    @Override
    public boolean equals(Object a1, Object a2) {
      // "Arrays.equals" considers +0.0 != -0.0.
      // Also, it gives inconsistent results (on different JVMs/classpaths?).
      // return Arrays.equals((double[])a1, (double[])a2);
      double[] da1 = (double[]) a1;
      double[] da2 = (double[]) a2;
      if (da1.length != da2.length) {
        return false;
      }
      for (int i = 0; i < da1.length; i++) {
        if (!((da1[i] == da2[i]) || (Double.isNaN(da1[i]) && Double.isNaN(da2[i])))) {
          return false;
        }
      }
      return true;
    }

    @Override
    public int hashCode(Object o) {
      double[] a = (double[]) o;
      // Not Arrays.hashCode(a), for consistency with equals method
      // immediately above.
      double running = 0;
      for (int i = 0; i < a.length; i++) {
        double elt = (Double.isNaN(a[i]) ? 0.0 : a[i]);
        running = running * FACTOR + elt * DOUBLE_FACTOR;
      }
      // Could add "... % Integer.MAX_VALUE" here; is that good to do?
      long result = Math.round(running);
      return (int) (result % Integer.MAX_VALUE);
    }
  }

  /**
   * Hasher object which hashes and compares String[] objects according to their contents.
   *
   * @see Hasher
   * @see Arrays#equals
   */
  private static final class StringArrayHasher implements Hasher {
    /** Create a new StringArrayHasher. */
    public StringArrayHasher() {}

    @Override
    public boolean equals(Object a1, Object a2) {
      return Arrays.equals((String[]) a1, (String[]) a2);
    }

    @Override
    public int hashCode(Object o) {
      return Arrays.hashCode((String[]) o);
    }
  }

  /**
   * Hasher object which hashes and compares Object[] objects according to their contents.
   *
   * @see Hasher
   * @see Arrays#equals(Object[], Object[])
   */
  private static final class ObjectArrayHasher implements Hasher {
    /** Create a new ObjectArrayHasher. */
    public ObjectArrayHasher() {}

    @Override
    public boolean equals(Object a1, Object a2) {
      return Arrays.equals((Object[]) a1, (Object[]) a2);
    }

    @Override
    public int hashCode(Object o) {
      return Arrays.hashCode((Object[]) o);
    }
  }

  // Each of these maps has:
  //   key = an interned object
  //   value = a WeakReference for the object itself.
  // They can be looked up using a non-interned value; equality tests know
  // nothing of the interning types.

  /** All the interned Integers. */
  private static WeakHasherMap<Integer, WeakReference<Integer>>
      internedIntegers;
  /** All the interned Longs. */
  private static WeakHasherMap<Long, WeakReference<Long>> internedLongs;
  /** All the interned Int arrays. */
  private static WeakHasherMap<int[], WeakReference<int[]>> internedIntArrays;
  /** All the interned Long arrays. */
  private static WeakHasherMap<long[], WeakReference<long[]>>
      internedLongArrays;
  /** All the interned Doubles. */
  private static WeakHasherMap<Double, WeakReference<Double>> internedDoubles;
  /** The interned NaN. */
  private static Double internedDoubleNaN;
  /** The interned Double zero. */
  private static Double internedDoubleZero;
  /** All the interned Double arrays. */
  private static WeakHasherMap<double[], WeakReference<double[]>>
      internedDoubleArrays;
  /** All the interned String arrays. */
  private static WeakHasherMap<
          String[],
          WeakReference<String[]>>
      internedStringArrays;
  /** All the interned Object arrays. */
  private static WeakHasherMap<
          Object[],
          WeakReference<Object[]>>
      internedObjectArrays;
  /** All the interned Int subsequences. */
  private static WeakHasherMap<Subsequence<int[]>, WeakReference<int[]>>
      internedIntSubsequence;
  /** All the interned Long subsequences. */
  private static WeakHasherMap<Subsequence<long[]>, WeakReference<long[]>>
      internedLongSubsequence;
  /** All the interned Double subsequences. */
  private static WeakHasherMap<Subsequence<double[]>, WeakReference<double[]>>
      internedDoubleSubsequence;
  /** All the interned Object subsequences. */
  private static WeakHasherMap<
          Subsequence<Object[]>,
          WeakReference<Object[]>>
      internedObjectSubsequence;
  /** All the interned String subsequences. */
  private static WeakHasherMap<
          Subsequence<String[]>,
          WeakReference<String[]>>
      internedStringSubsequence;

  static {
    internedIntegers = new WeakHasherMap<>(new IntegerHasher());
    internedLongs = new WeakHasherMap<>(new LongHasher());
    internedIntArrays = new WeakHasherMap<>(new IntArrayHasher());
    internedLongArrays = new WeakHasherMap<>(new LongArrayHasher());
    internedDoubles = new WeakHasherMap<>(new DoubleHasher());
    internedDoubleNaN = Double.NaN;
    internedDoubleZero = 0.0;
    internedDoubleArrays = new WeakHasherMap<>(new DoubleArrayHasher());
    internedStringArrays = new WeakHasherMap<>(new StringArrayHasher());
    internedObjectArrays =
        new WeakHasherMap<
            Object[],
            WeakReference<Object[]>>(new ObjectArrayHasher());
    internedIntSubsequence = new WeakHasherMap<>(new SubsequenceHasher<int[]>());
    internedLongSubsequence = new WeakHasherMap<>(new SubsequenceHasher<long[]>());
    internedDoubleSubsequence = new WeakHasherMap<>(new SubsequenceHasher<double[]>());
    internedObjectSubsequence =
        new WeakHasherMap<>(new SubsequenceHasher<Object[]>());
    internedStringSubsequence =
        new WeakHasherMap<>(new SubsequenceHasher<String[]>());
  }

  /// For testing only

  /**
   * Returns the number of interned integers. For testing only.
   *
   * @return the number of interned integers
   */
  static int numIntegers() {
    return internedIntegers.size();
  }

  /**
   * Returns the number of interned longs. For testing only.
   *
   * @return the number of interned longs
   */
  static int numLongs() {
    return internedLongs.size();
  }

  /**
   * Returns the number of interned int arrays. For testing only.
   *
   * @return the number of interned int arrays
   */
  static int numIntArrays() {
    return internedIntArrays.size();
  }

  /**
   * Returns the number of interned long arrays. For testing only.
   *
   * @return the number of interned long arrays
   */
  static int numLongArrays() {
    return internedLongArrays.size();
  }

  /**
   * Returns the number of interned doubles. For testing only.
   *
   * @return the number of interned doubles
   */
  static int numDoubles() {
    return internedDoubles.size();
  }

  /**
   * Returns the number of interned double arrays. For testing only.
   *
   * @return the number of interned double arrays
   */
  static int numDoubleArrays() {
    return internedDoubleArrays.size();
  }

  /**
   * Returns the number of interned string arrays. For testing only.
   *
   * @return the number of interned string arrays
   */
  static int numStringArrays() {
    return internedStringArrays.size();
  }

  /**
   * Returns the number of interned object arrays. For testing only.
   *
   * @return the number of interned object arrays
   */
  static int numObjectArrays() {
    return internedObjectArrays.size();
  }

  /**
   * Returns all the interned integers. For testing only.
   *
   * @return all the interned integers
   */
  static Iterator<Integer> integers() {
    return internedIntegers.keySet().iterator();
  }

  /**
   * Returns all the interned longs. For testing only.
   *
   * @return all the interned longs
   */
  static Iterator<Long> longs() {
    return internedLongs.keySet().iterator();
  }

  /**
   * Returns all the interned int arrays. For testing only.
   *
   * @return all the interned int arrays
   */
  static Iterator<int[]> intArrays() {
    return internedIntArrays.keySet().iterator();
  }

  /**
   * Returns all the interned long arrays. For testing only.
   *
   * @return all the interned long arrays
   */
  static Iterator<long[]> longArrays() {
    return internedLongArrays.keySet().iterator();
  }

  /**
   * Returns all the interned doubles. For testing only.
   *
   * @return all the interned doubles
   */
  static Iterator<Double> doubles() {
    return internedDoubles.keySet().iterator();
  }

  /**
   * Returns all the interned double arrays. For testing only.
   *
   * @return all the interned double arrays
   */
  static Iterator<double[]> doubleArrays() {
    return internedDoubleArrays.keySet().iterator();
  }

  /**
   * Returns all the interned string arrays. For testing only.
   *
   * @return all the interned string arrays
   */
  static Iterator<String[]> stringArrays() {
    return internedStringArrays.keySet().iterator();
  }

  /**
   * Returns all the interned object arrays. For testing only.
   *
   * @return all the interned object arrays
   */
  static Iterator<Object[]> objectArrays() {
    return internedObjectArrays.keySet().iterator();
  }

  /// End of testing methods

  /**
   * Interns a String. Delegates to the builtin String.intern() method, but handles {@code null}.
   *
   * @param a the string to intern; may be null
   * @return an interned version of the argument, or null if the argument was null
   */
  public static String intern(
      String a) {
    return (a == null) ? null : a.intern();
  }

  /**
   * Interns a long. A no-op. Provided for completeness.
   *
   * @param l the long to intern
   * @return an interned version of the argument
   */
  public static long intern(long l) {
    return l;
  }

  /**
   * Interns a double A no-op. Provided for completeness.
   *
   * @param d the double to intern
   * @return an interned version of the argument
   */
  public static double intern(double d) {
    return d;
  }

  /**
   * Intern (canonicalize) an Integer. Return a canonical representation for the Integer.
   *
   * @param a an Integer to canonicalize
   * @return a canonical representation for the Integer
   */
  // TODO: JLS 5.1.7 requires that the boxing conversion interns integer
  // values between -128 and 127 (and Intern.valueOf is intended to promise
  // the same).  This does not currently take advantage of that.
  @SuppressWarnings({"interning", "allcheckers:purity", "lock"}) // interning implementation
  public static @Interned Integer intern(Integer a) {
    WeakReference<@Interned Integer> lookup = internedIntegers.get(a);
    Integer result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      @Interned Integer result = (@Interned Integer) a;
      internedIntegers.put(result, new WeakReference<>(result));
      return result;
    }
  }

  // Not sure whether this convenience method is really worth it.
  /**
   * Returns an interned Integer with value i.
   *
   * @param i the value to intern
   * @return an interned Integer with value i
   */
  public static Integer internedInteger(int i) {
    return intern(Integer.valueOf(i));
  }

  // Not sure whether this convenience method is really worth it.
  /**
   * Returns an interned Integer with value parsed from the string.
   *
   * @param s the string to parse
   * @return an interned Integer parsed from s
   */
  public static Integer internedInteger(String s) {
    return intern(Integer.decode(s));
  }

  /**
   * Intern (canonicalize) a Long. Return a canonical representation for the Long.
   *
   * @param a the value to intern
   * @return a canonical representation for the Long
   */
  // TODO: JLS 5.1.7 requires that the boxing conversion interns integer
  // values between -128 and 127 (and Long.valueOf is intended to promise
  // the same).  This could take advantage of that.
  @SuppressWarnings({"interning", "allcheckers:purity", "lock"})
  public static @Interned Long intern(Long a) {
    WeakReference<@Interned Long> lookup = internedLongs.get(a);
    Long result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      @Interned Long result = (@Interned Long) a;
      internedLongs.put(result, new WeakReference<>(result));
      return result;
    }
  }

  // Not sure whether this convenience method is really worth it.
  /**
   * Returns an interned Long with value i.
   *
   * @param i the value to intern
   * @return an interned Integer with value i
   */
  public static Long internedLong(long i) {
    return intern(Long.valueOf(i));
  }

  // Not sure whether this convenience method is really worth it.
  /**
   * Returns an interned Long with value parsed from the string.
   *
   * @param s the string to parse
   * @return an interned Long parsed from s
   */
  public static Long internedLong(String s) {
    return intern(Long.decode(s));
  }

  // I might prefer to have the intern methods first check using a straight
  // eq hashing, which would be more efficient if the array is already
  // interned.  (How frequent do I expect that to be, and how much would
  // that really improve performance even in that case?)

  /**
   * Intern (canonicalize) an int[]. Return a canonical representation for the int[] array. Arrays
   * are compared according to their elements.
   *
   * @param a the array to canonicalize
   * @return a canonical representation for the int[] array
   */
  @SuppressWarnings({"interning", "allcheckers:purity", "lock"})
  public static int @Interned[] intern(int[] a) {
    // Throwable stack = new Throwable("debug traceback");
    // stack.fillInStackTrace();
    // stack.printStackTrace();

    WeakReference<int @Interned []> lookup = internedIntArrays.get(a);
    @SuppressWarnings({
      "samelen:assignment", // for this map, get() can be annotated as
      // @SameLen("#1")
      "value" // for this map, get() can be annotated as @PolyAll (except not interning); also see
      // https://github.com/kelloggm/checker-framework/issues/177
    })
    int @PolyValue[] result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      @Interned int[] result = (int @Interned[]) a;
      internedIntArrays.put(result, new WeakReference<>(result));
      return result;
    }
  }

  /**
   * Intern (canonicalize) a long[]. Return a canonical representation for the long[] array. Arrays
   * are compared according to their elements.
   *
   * @param a the array to canonicalize
   * @return a canonical representation for the long[] array
   */
  @SuppressWarnings({"interning", "allcheckers:purity", "lock"})
  public static long @Interned[] intern(long[] a) {
    // System.out.printf("intern %s %s long[] %s%n", a.getClass(),
    //                   a, Arrays.toString (a));
    WeakReference<long @Interned []> lookup = internedLongArrays.get(a);
    @SuppressWarnings({
      "samelen:assignment", // for this map, get() can be annotated as
      // @SameLen("#1")
      "value" // for this map, get() can be annotated as @PolyAll (except not interning); also see
      // https://github.com/kelloggm/checker-framework/issues/177
    })
    long @PolyValue[] result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      @Interned long[] result = (long @Interned[]) a;
      internedLongArrays.put(result, new WeakReference<>(result));
      return result;
    }
  }

  /**
   * Intern (canonicalize) a Double. Return a canonical representation for the Double.
   *
   * @param a the Double to canonicalize
   * @return a canonical representation for the Double
   */
  // TODO: JLS 5.1.7 requires that the boxing conversion interns integer
  // values between -128 and 127 (and Double.valueOf is intended to promise
  // the same).  This could take advantage of that.
  @SuppressWarnings({"interning", "allcheckers:purity", "lock"})
  public static @Interned Double intern(Double a) {
    // Double.NaN == Double.Nan  always evaluates to false.
    if (a.isNaN()) {
      return internedDoubleNaN;
    }
    // Double.+0 == Double.-0,  but they compare true via equals()
    if (a.doubleValue() == 0) { // catches both positive and negative zero
      return internedDoubleZero;
    }
    WeakReference<@Interned Double> lookup = internedDoubles.get(a);
    Double result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      @Interned Double result = (@Interned Double) a;
      internedDoubles.put(result, new WeakReference<>(result));
      return result;
    }
  }

  // Not sure whether this convenience method is really worth it.
  /**
   * Returns an interned Double with value i.
   *
   * @param d the value to intern
   * @return an interned Double with value d
   */
  public static Double internedDouble(double d) {
    return intern(Double.valueOf(d));
  }

  // Not sure whether this convenience method is really worth it.
  /**
   * Returns an interned Double with value parsed from the string.
   *
   * @param s the string to parse
   * @return an interned Double parsed from s
   */
  public static Double internedDouble(String s) {
    return internedDouble(Double.parseDouble(s));
  }

  // I might prefer to have the intern methods first check using a straight
  // eq hashing, which would be more efficient if the array is already
  // interned.  (How frequent do I expect that to be, and how much would
  // that really improve performance even in that case?)

  /**
   * Intern (canonicalize) a double[]. Return a canonical representation for the double[] array.
   * Arrays are compared according to their elements.
   *
   * @param a the array to canonicalize
   * @return a canonical representation for the double[] array
   */
  @SuppressWarnings({"interning", "allcheckers:purity", "lock"})
  public static double @Interned[] intern(double[] a) {
    WeakReference<double @Interned []> lookup = internedDoubleArrays.get(a);
    @SuppressWarnings({
      "samelen:assignment", // for this map, get() can be annotated as
      // @SameLen("#1")
      "value" // for this map, get() can be annotated as @PolyAll (except not interning); also see
      // https://github.com/kelloggm/checker-framework/issues/177
    })
    double @PolyValue[] result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      @Interned double[] result = (double @Interned[]) a;
      internedDoubleArrays.put(result, new WeakReference<>(result));
      return result;
    }
  }

  /**
   * Intern (canonicalize) a String[]. Return a canonical representation for the String[] array.
   * Arrays are compared according to their elements' equals() methods.
   *
   * @param a the array to canonicalize. Its elements should already be interned.
   * @return a canonical representation for the String[] array
   */
  @SuppressWarnings({
    // Java warnings
    "cast",
    // Checker Framework warnings
    "interning", // interns its argument
    "allcheckers:purity",
    "lock",
    // Error Prone Warnings
    "ReferenceEquality"
  }) // cast is redundant (except in JSR 308)
  public static @Interned String @Interned[] intern(
      @Interned String[] a) {

    // Make sure each element is already interned
    if (assertsEnabled) {
      for (int k = 0; k < a.length; k++) {
        if (a[k] != Intern.intern(a[k])) {
          throw new IllegalArgumentException();
        }
      }
    }

    WeakReference<@Interned String @Interned []> lookup = internedStringArrays.get(a);
    @Interned String @Interned [] result = (lookup != null) ? lookup.get() : null;
    if (result == null) {
      result = (@Interned String @Interned []) a;
      internedStringArrays.put(result, new WeakReference<>(result));
    }
    @SuppressWarnings({
      "nullness", // for this map, get() can be annotated as @PolyAll (except not interning); also
      // see https://github.com/kelloggm/checker-framework/issues/177
      "samelen:assignment", // for this map, get() can be annotated as
      // @SameLen("#1")
      "value" // for this map, get() can be annotated as @PolyAll (except not interning); also see
      // https://github.com/kelloggm/checker-framework/issues/177
    })
    @PolyNull @Interned String @Interned @PolyValue[] polyresult = result;
    return polyresult;
  }

  /**
   * Intern (canonicalize) an Object[]. Return a canonical representation for the Object[] array.
   * Arrays are compared according to their elements. The elements should themselves already be
   * interned; they are compared using their equals() methods.
   *
   * @param a the array to canonicalize
   * @return a canonical representation for the Object[] array
   */
  @SuppressWarnings({
    "interning", // interns its argument
    "allcheckers:purity",
    "lock",
    "cast"
  }) // cast is redundant (except in JSR 308)
  public static @Interned Object @Interned[] intern(
      @Interned Object[] a) {
    WeakReference<@Interned Object @Interned []> lookup = internedObjectArrays.get(a);
    @Interned Object @Interned [] result = (lookup != null) ? lookup.get() : null;
    if (result == null) {
      result = (@Interned Object @Interned []) a;
      internedObjectArrays.put(result, new WeakReference<>(result));
    }
    @SuppressWarnings({
      "nullness", // for this map, get() can be annotated as @PolyAll (except not interning); also
      // see https://github.com/kelloggm/checker-framework/issues/177
      "samelen:assignment", // for this map, get() can be annotated as
      // @SameLen("#1")
      "value" // for this map, get() can be annotated as @PolyAll (except not interning); also see
      // https://github.com/kelloggm/checker-framework/issues/177
    }) // PolyNull/PolyValue:  value = parameter a, so same type & nullness as for parameter a
    @PolyNull @Interned Object @Interned @PolyValue[] polyresult = result;
    return polyresult;
  }

  /**
   * Convenience method to intern an Object when we don't know its run-time type. Its run-time type
   * must be one of the types for which we have an intern() method, else an exception is thrown. If
   * the argument is an array, its elements should themselves be interned.
   *
   * @param a an Object to canonicalize
   * @return a canonical version of a
   */
  public static Object intern(Object a) {
    if (a == null) {
      return null;
    } else if (a instanceof String) {
      return intern((String) a);
    } else if (a instanceof String[]) {
      String[] asArray = (String[]) a;
      return intern(asArray);
    } else if (a instanceof Integer) {
      return intern((Integer) a);
    } else if (a instanceof Long) {
      return intern((Long) a);
    } else if (a instanceof int[]) {
      return intern((int[]) a);
    } else if (a instanceof long[]) {
      return intern((long[]) a);
    } else if (a instanceof Double) {
      return intern((Double) a);
    } else if (a instanceof double[]) {
      return intern((double[]) a);
    } else if (a instanceof Object[]) {
      Object[] asArray = (Object[]) a;
      return intern(asArray);
    } else {
      throw new IllegalArgumentException(
          "Arguments of type " + a.getClass() + " cannot be interned");
    }
  }

  /**
   * Returns an interned subsequence of seq from start (inclusive) to end (exclusive). The argument
   * seq should already be interned.
   *
   * <p>The result is the same as computing the subsequence and then interning it, but this method
   * is more efficient: if the subsequence is already interned, it avoids computing the subsequence.
   *
   * <p>For example, since derived variables in Daikon compute the subsequence many times, this
   * shortcut saves quite a bit of computation. It saves even more when there may be many derived
   * variables that are non-canonical, since they are guaranteed to be ==.
   *
   * @param seq the interned sequence whose subsequence should be computed and interned
   * @param start the index of the start of the subsequence to compute and intern
   * @param end the index of the end of the subsequence to compute and intern
   * @return a subsequence of seq from start to end that is interned
   */
  public static int[] internSubsequence(
      int[] seq,
      int start,
      int end) {
    if (assertsEnabled && !Intern.isInterned(seq)) {
      throw new IllegalArgumentException();
    }
    Subsequence<int[]> sai = new Subsequence<>(seq, start, end);
    WeakReference<int[]> lookup = internedIntSubsequence.get(sai);
    int[] result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      int[] subseqUninterned = ArraysPlume.subarray(seq, start, end - start);
      int[] subseq = Intern.intern(subseqUninterned);
      internedIntSubsequence.put(sai, new WeakReference<>(subseq));
      return subseq;
    }
  }

  /**
   * Returns a subsequence of seq from start to end that is interned.
   *
   * @param seq the interned sequence whose subsequence should be computed and interned
   * @param start the index of the start of the subsequence to compute and intern
   * @param end the index of the end of the subsequence to compute and intern
   * @return a subsequence of seq from start to end that is interned
   * @see #internSubsequence(int[], int, int)
   */
  @SuppressWarnings({"allcheckers:purity", "lock"}) // interning logic
  public static long[] internSubsequence(
      long[] seq,
      int start,
      int end) {
    if (assertsEnabled && !Intern.isInterned(seq)) {
      throw new IllegalArgumentException();
    }
    Subsequence<long[]> sai = new Subsequence<>(seq, start, end);
    WeakReference<long[]> lookup = internedLongSubsequence.get(sai);
    long[] result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      long[] subseq_uninterned = ArraysPlume.subarray(seq, start, end - start);
      long[] subseq = Intern.intern(subseq_uninterned);
      internedLongSubsequence.put(sai, new WeakReference<>(subseq));
      return subseq;
    }
  }

  /**
   * Returns a subsequence of seq from start to end that is interned.
   *
   * @param seq the interned sequence whose subsequence should be computed and interned
   * @param start the index of the start of the subsequence to compute and intern
   * @param end the index of the end of the subsequence to compute and intern
   * @return a subsequence of seq from start to end that is interned
   * @see #internSubsequence(int[], int, int)
   */
  @SuppressWarnings({"allcheckers:purity", "lock"}) // interning logic
  public static double[] internSubsequence(
      double[] seq,
      int start,
      int end) {
    if (assertsEnabled && !Intern.isInterned(seq)) {
      throw new IllegalArgumentException();
    }
    Subsequence<double[]> sai = new Subsequence<>(seq, start, end);
    WeakReference<double[]> lookup = internedDoubleSubsequence.get(sai);
    double[] result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      double[] subseq_uninterned = ArraysPlume.subarray(seq, start, end - start);
      double[] subseq = Intern.intern(subseq_uninterned);
      internedDoubleSubsequence.put(sai, new WeakReference<>(subseq));
      return subseq;
    }
  }

  /**
   * Returns a subsequence of seq from start to end that is interned.
   *
   * @param seq the interned sequence whose subsequence should be computed and interned
   * @param start the index of the start of the subsequence to compute and intern
   * @param end the index of the end of the subsequence to compute and intern
   * @return a subsequence of seq from start to end that is interned
   * @see #internSubsequence(int[], int, int)
   */
  @SuppressWarnings({"allcheckers:purity", "lock"}) // interning logic
  public static Object[] internSubsequence(
      Object[] seq,
      int start,
      int end) {
    if (assertsEnabled && !Intern.isInterned(seq)) {
      throw new IllegalArgumentException();
    }
    Subsequence<Object[]> sai =
        new Subsequence<Object[]>(seq, start, end);
    @SuppressWarnings("nullness") // same nullness as key
    WeakReference<@PolyNull Object[]> lookup =
        internedObjectSubsequence.get(sai);
    Object[] result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      Object[] subseq_uninterned = ArraysPlume.subarray(seq, start, end - start);
      Object[] subseq = Intern.intern(subseq_uninterned);
      @SuppressWarnings({"nullness", "UnusedVariable"}) // safe because map does no side effects
      Object
          ignore = // assignment just so there is a place to hang the @SuppressWarnings annotation
          internedObjectSubsequence.put(sai, new WeakReference<>(subseq));
      return subseq;
    }
  }

  /**
   * Returns a subsequence of seq from start to end that is interned.
   *
   * @param seq the interned sequence whose subsequence should be computed and interned
   * @param start the index of the start of the subsequence to compute and intern
   * @param end the index of the end of the subsequence to compute and intern
   * @return a subsequence of seq from start to end that is interned
   * @see #internSubsequence(int[], int, int)
   */
  @SuppressWarnings({"allcheckers:purity", "lock"}) // interning logic
  public static String[] internSubsequence(
      String[] seq,
      int start,
      int end) {
    if (assertsEnabled && !Intern.isInterned(seq)) {
      throw new IllegalArgumentException();
    }
    Subsequence<String[]> sai =
        new Subsequence<String[]>(seq, start, end);
    @SuppressWarnings("nullness") // same nullness as key
    WeakReference<@PolyNull String[]> lookup =
        internedStringSubsequence.get(sai);
    String[] result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      String[] subseq_uninterned = ArraysPlume.subarray(seq, start, end - start);
      String[] subseq = Intern.intern(subseq_uninterned);
      @SuppressWarnings({"nullness", "UnusedVariable"}) // safe because map does no side effects
      Object
          ignore = // assignment just so there is a place to hang the @SuppressWarnings annotation
          internedStringSubsequence.put(sai, new WeakReference<>(subseq));
      return subseq;
    }
  }

  /**
   * A subsequence view on a sequence. Actually, this imposes no semantics. It just has 3 fields: an
   * interned sequence, a start index, and an end index. Requires that the sequence be interned.
   * Used for interning the repeated finding of subsequences on the same sequence.
   */
  private static final class Subsequence<T extends Object> {
    /** The full sequence. The Subsequence object represents part of this sequence. */
    public T seq;
    /** The start index, inclusive. */
    public int start;
    // TODO: inclusive or exclusive?
    /** The end index. */
    public int end;

    /**
     * Creates a subsequence view.
     *
     * @param seq an interned array
     * @param start the start index
     * @param end the end index
     */
    public Subsequence(T seq, int start, int end) {
      if (assertsEnabled && !Intern.isInterned(seq)) {
        throw new IllegalArgumentException();
      }
      this.seq = seq;
      this.start = start;
      this.end = end;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(
        Subsequence<T> this, Object other) {
      if (other instanceof Subsequence<?>) {
        @SuppressWarnings("unchecked")
        Subsequence<T> otherSai = (Subsequence<T>) other;
        return equalsSubsequence(otherSai);
      } else {
        return false;
      }
    }

    /**
     * Returns true if this object equals the given one.
     *
     * @param other the sequence to compare to
     * @return true if this object equals {@code other}
     */
    public boolean equalsSubsequence(
        Subsequence<T> this, Subsequence<T> other) {
      return ((this.seq == other.seq) && this.start == other.start && this.end == other.end);
    }

    @Override
    public int hashCode(Subsequence<T> this) {
      return seq.hashCode() + start * 30 - end * 2;
    }

    // For debugging
    @Override
    public String toString(Subsequence<T> this) {
      return "SAI(" + start + "," + end + ") from: " + ArraysPlume.toString(seq);
    }
  }

  /**
   * Hasher object which hashes and compares String[] objects according to their contents.
   *
   * @see Hasher
   */
  private static final class SubsequenceHasher<T extends Object> implements Hasher {
    /** Create a new SubsequenceHasher. */
    public SubsequenceHasher() {}

    @Override
    public boolean equals(Object a1, Object a2) {
      @SuppressWarnings("unchecked")
      Subsequence<T> sai1 = (Subsequence<T>) a1;
      @SuppressWarnings("unchecked")
      Subsequence<T> sai2 = (Subsequence<T>) a2;
      // The SAI objects are *not* interned, but the arrays inside them are.
      return sai1.equals(sai2);
    }

    @Override
    public int hashCode(Object o) {
      return o.hashCode();
    }
  }
}
