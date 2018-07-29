package com.openkappa.panama.vectorbenchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.YMM_DOUBLE;
import static com.openkappa.panama.vectorbenchmarks.Util.newDoubleVector;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector", "-XX:TypeProfileLevel=111"})
public class DAXPY {


  @Param({"1024"})
  int size;

  private double s;
  private double[] data;
  private double[] out;

  @Setup(Level.Iteration)
  public void init() {
    data = newDoubleVector(size);
    out = new double[size];
    for (int i = 0; i < s; ++i) {
      data[i] = ThreadLocalRandom.current().nextDouble();
    }
    s = ThreadLocalRandom.current().nextDouble();
  }


  @Benchmark
  public void daxpyPanama(Blackhole bh) {
    for (int i = 0; i < data.length; i += YMM_DOUBLE.length()) {
      YMM_DOUBLE.fromArray(out, i)
                .add(YMM_DOUBLE.fromArray(data, i).mul(s))
                .intoArray(out, i);
    }
    bh.consume(out);
  }

  @Benchmark
  public void daxpyPanamaFMA(Blackhole bh) {
    for (int i = 0; i < data.length; i += YMM_DOUBLE.length()) {
      YMM_DOUBLE.fromArray(data, i)
              .fma(YMM_DOUBLE.broadcast(s), YMM_DOUBLE.fromArray(out, i))
              .intoArray(out, i);
    }
    bh.consume(out);
  }

  @Benchmark
  public void daxpy(Blackhole bh) {
    for (int i = 0; i < data.length; ++i) {
      out[i] += s * data[i];
    }
    bh.consume(out);
  }
}
