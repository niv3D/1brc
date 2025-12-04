import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class CalculateAverageBaseline {

  private static final String FILE = "./src/measurements.txt";

  static void main() throws IOException {
    long start = System.currentTimeMillis();
    Collector<Measurement, MeasurementAggregator, ResultRow> collector = Collector.of(
        MeasurementAggregator::new,
        (a, m) -> {
          a.min = Math.min(a.min, m.value());
          a.max = Math.max(a.max, m.value());
          a.sum += m.value();
          a.count++;
        },
        (agg1, agg2) -> {
          var res = new MeasurementAggregator();
          res.min = Math.min(agg1.min, agg2.min);
          res.max = Math.max(agg1.max, agg2.max);
          res.sum = agg1.sum + agg2.sum;
          res.count = agg1.count + agg2.count;
          return res;
        },
        agg -> {
          return new ResultRow(agg.min, (Math.round(agg.sum * 10.0) / 10.0) / agg.count, agg.max);
        }
    );
    Map<String, ResultRow> measurements = new TreeMap<>(
        Files.lines(Paths.get(FILE))
            .map(l -> new Measurement(l.split(";")))
            .collect(Collectors.groupingBy(Measurement::station, collector))
    );
    IO.println(System.currentTimeMillis() - start + "ms");
    IO.println(measurements);
  }

}

record Measurement(String station, double value) {

  Measurement(String[] parts) {
    this(parts[0], Double.parseDouble(parts[1]));
  }
}

record ResultRow(double min, double mean, double max) {

  public String toString() {
    return round(min) + "/" + round(mean) + "/" + round(max);
  }

  private double round(double value) {
    return Math.round(value * 10.0) / 10.0;
  }

}

class MeasurementAggregator {
  double min = Double.POSITIVE_INFINITY;
  double max = Double.NEGATIVE_INFINITY;
  double sum;
  double count;
}