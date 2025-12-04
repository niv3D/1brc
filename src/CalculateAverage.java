private static final String FILE = "./src/measurements.txt";
private static final int MAX_LINE_SIZE = 300;
private static final long CHUNK_SIZE = 100 * 1024 * 1024;
private static final byte NEW_LINE = (byte) '\n';
private static final byte SEMI_COL = (byte) ';';

void main() {
  final long startTime = System.currentTimeMillis();
  try (
      var channel = FileChannel.open(Paths.get(FILE), StandardOpenOption.READ)
  ) {
    List<Segment> segments = getSegments(channel);
    List<HashMap<Integer, Result>> results = segments.stream()
        .map(segment -> processChunk(segment, channel))
        .parallel()
        .toList();
    var accumulated = accumulateResults(results);
    IO.println(accumulated);
  } catch (IOException e) {
    throw new RuntimeException(e);
  }
  final long endTime = System.currentTimeMillis() - startTime;
  IO.println(endTime + "ms");
}

private static Map<String, Result> accumulateResults(List<HashMap<Integer, Result>> results) {
  TreeMap<String, Result> aggregated = new TreeMap<>();
  for (var map : results) {
    for (var current : map.values()) {
      Result prev = aggregated.putIfAbsent(current.city(), current);
      if (prev != null) {
        prev.accept(current);
      }
    }
  }
  return aggregated;
}

private static HashMap<Integer, Result> processChunk(Segment segment, FileChannel channel) {
  HashMap<Integer, Result> map = new HashMap<>();
  try {
    var byteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, segment.start, segment.size);
    byteBuffer.load();
    final int limit = byteBuffer.limit();
    int start = 0;
    int semi = -1;
    while (start < limit) {
      int cursor = start;
      while (cursor < limit) {
        byte b = byteBuffer.get(cursor);
        if (b == SEMI_COL) {
          semi = cursor;
        }
        if (b == NEW_LINE) {
          break;
        }
        cursor++;
      }
      int eol = Math.min(cursor, limit);
      byte[] city = new byte[semi - start];
      byte[] value = new byte[eol - (semi + 1)];
      byteBuffer.get(start, city);
      byteBuffer.get(semi + 1, value);
      int hash = getCityHash(city);
      int temp = parseTemp(value);
      Result current = new Result(city, temp);
      Result prev = map.putIfAbsent(hash, current);
      if (prev != null) {
        prev.accept(current);
      }
      start = cursor + 1;
    }
  } catch (IOException e) {
    throw new RuntimeException(e);
  }
  return map;
}

private static int getCityHash(byte[] city) {
  return Arrays.hashCode(city);
}

private static int parseTemp(byte[] temp) {
  boolean neg = false;
  int val = 0;
  for (byte b : temp) {
    if (b == (byte) '-') {
      neg = true;
      continue;
    }
    if (b == (byte) '.') {
      continue;
    }
    val = val * 10 + (b - '0');
  }
  return neg ? -val : val;
}

private static List<Segment> getSegments(FileChannel channel) throws IOException {
  List<Segment> segments = new ArrayList<>();
  final long fileSize = channel.size();
  long position = 0;
  ByteBuffer lineBuffer = ByteBuffer.allocate(MAX_LINE_SIZE);
  while (position < fileSize) {
    long chunkSize = Math.min(CHUNK_SIZE, fileSize - position);
    channel.position(position + chunkSize);
    lineBuffer.clear();
    int read = channel.read(lineBuffer);
    if (read > 0) {
      lineBuffer.flip();
      while (lineBuffer.hasRemaining()) {
        if (lineBuffer.get() == NEW_LINE) {
          break;
        }
        chunkSize++;
      }
      if (!lineBuffer.hasRemaining()) {
        throw new RuntimeException("new line character not found");
      }
    }
    segments.add(new Segment(position, chunkSize));
    position += chunkSize + 1;
  }
  return segments;
}

record Segment(long start, long size) {

}

static class Result {

  byte[] city;
  int min;
  int max;
  int sum;
  int count;

  Result(byte[] city, int value) {
    this.city = city;
    this.min = value;
    this.max = value;
    this.sum = value;
    this.count = 1;
  }

  void accept(Result other) {
    min = Math.min(min, other.min);
    max = Math.max(max, other.max);
    sum += other.sum;
    count += other.count;
  }

  String city() {
    return new String(city, StandardCharsets.UTF_8);
  }

  public String toString() {
    return round(((double) min) / 10.0) + "/" + round((((double) sum) / 10.0) / count) + "/" + round(((double) max) / 10.0);
  }

  private double round(double value) {
    return Math.round(value * 10.0) / 10.0;
  }
}