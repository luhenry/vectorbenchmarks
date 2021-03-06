package com.openkappa.panama.vectorbenchmarks;

import jdk.incubator.vector.LongVector;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static com.openkappa.panama.vectorbenchmarks.Util.L256;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector", "-Djdk.incubator.vector.VECTOR_ACCESS_OOB_CHECK=0"})
public class IntersectionCardinality {


  @Param({"1024"})
  int size;

  public static void main(String[] args) {
    IntersectionCardinality ic = new IntersectionCardinality();
    ic.fill();
    System.out.println(ic.popcnt());
    System.out.println(ic.vpandExtractPopcntUnrolled());
  }

  private final long[] left = new long[1024];
  private final long[] right = new long[1024];
  private long[] buffer = new long[64];

  @Setup(Level.Iteration)
  public void fill() {
    Arrays.fill(left, 0, size, ThreadLocalRandom.current().nextLong());
    Arrays.fill(right, 0, size, ThreadLocalRandom.current().nextLong());
  }

  @Benchmark
  public int popcnt() {
    int cardinality = 0;
    for (int i = 0; i < size && i < left.length && i < right.length; ++i) {
      cardinality += Long.bitCount(left[i] & right[i]);
    }
    return cardinality;
  }

  @Benchmark
  public int unrolledPopcnt() {
    int cardinality1 = 0;
    int cardinality2 = 0;
    int cardinality3 = 0;
    int cardinality4 = 0;
    for (int i = 0; i < size && i < left.length && i < right.length; i += 4) {
      cardinality1 += Long.bitCount(left[i+0] & right[i+0]);
      cardinality2 += Long.bitCount(left[i+1] & right[i+1]);
      cardinality3 += Long.bitCount(left[i+2] & right[i+2]);
      cardinality4 += Long.bitCount(left[i+3] & right[i+3]);
    }
    return cardinality1 + cardinality2 + cardinality3 + cardinality4;
  }

  @Benchmark
  public int vpandStorePopcnt() {
    long[] intersections = buffer;
    int cardinality = 0;
    for (int i = 0; i < size && i < left.length && i < right.length; i += 4) {
      LongVector.fromArray(L256, left, i).and(LongVector.fromArray(L256, right, i)).intoArray(intersections, 0);
      cardinality += Long.bitCount(intersections[0]);
      cardinality += Long.bitCount(intersections[1]);
      cardinality += Long.bitCount(intersections[2]);
      cardinality += Long.bitCount(intersections[3]);
    }
    return cardinality;
  }

  @Benchmark
  public int vpandStorePopcntUnrolled() {
    long[] intersections = buffer;
    int cardinality1 = 0;
    int cardinality2 = 0;
    int cardinality3 = 0;
    for (int i = 0; i < size && i < left.length && i < right.length; i += 8) {
      LongVector.fromArray(L256, left, i).and(LongVector.fromArray(L256, right, i)).intoArray(intersections, 0);
      LongVector.fromArray(L256, left, i + 4).and(LongVector.fromArray(L256, right, i + 4)).intoArray(intersections, 4);
      cardinality1 += Long.bitCount(intersections[0]);
      cardinality2 += Long.bitCount(intersections[1]);
      cardinality3 += Long.bitCount(intersections[2]);
      cardinality1 += Long.bitCount(intersections[3]);
      cardinality2 += Long.bitCount(intersections[4]);
      cardinality3 += Long.bitCount(intersections[5]);
      cardinality1 += Long.bitCount(intersections[6]);
      cardinality2 += Long.bitCount(intersections[7]);
    }
    return cardinality1 + cardinality2 + cardinality3;
  }

  @Benchmark
  public int vpandExtractPopcnt() {
    int cardinality = 0;
    for (int i = 0; i < size && i < left.length && i < right.length; i += 4) {
      var intersection = LongVector.fromArray(L256, left, i).and(LongVector.fromArray(L256, right, i));
      cardinality += Long.bitCount(intersection.lane(0));
      cardinality += Long.bitCount(intersection.lane(1));
      cardinality += Long.bitCount(intersection.lane(2));
      cardinality += Long.bitCount(intersection.lane(3));
    }
    return cardinality;
  }

  @Benchmark
  public int vpandExtractPopcntUnrolled() {
    int cardinality1 = 0;
    int cardinality2 = 0;
    int cardinality3 = 0;
    for (int i = 0; i < size && i < left.length && i < right.length; i += 8) {
      var intersection1 = LongVector.fromArray(L256, left, i).and(LongVector.fromArray(L256, right, i));
      var intersection2 = LongVector.fromArray(L256, left, i + 4).and(LongVector.fromArray(L256, right, i + 4));
      cardinality1 += Long.bitCount(intersection1.lane(0));
      cardinality2 += Long.bitCount(intersection1.lane(1));
      cardinality3 += Long.bitCount(intersection1.lane(2));
      cardinality1 += Long.bitCount(intersection1.lane(3));
      cardinality2 += Long.bitCount(intersection2.lane(0));
      cardinality3 += Long.bitCount(intersection2.lane(1));
      cardinality1 += Long.bitCount(intersection2.lane(2));
      cardinality2 += Long.bitCount(intersection2.lane(3));
    }
    return cardinality1 + cardinality2 + cardinality3;
  }

}
