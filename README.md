# One Billion Row Challenge (1BRC)

This repository contains my implementation of **The One Billion Row Challenge** originally created by [Gunnar Morling](https://github.com/gunnarmorling/1brc).  
The challenge explores how far Java performance can be pushed when aggregating one **billion** rows of text data.

---

## Overview

The text file contains temperature values for a range of weather stations.
Each row is one measurement in the format `<string: station name>;<double: measurement>`, with the measurement value having exactly one fractional digit.
The following shows ten rows as an example:

```
Hamburg;12.0
Bulawayo;8.9
Palembang;38.8
St. John's;15.2
Cracow;12.6
Bridgetown;26.9
Istanbul;6.2
Roseau;34.4
Conakry;31.2
Istanbul;23.0
```

The task is to write a Java program which reads the file, calculates the min, mean, and max temperature value per weather station, and emits the results on stdout like this
(i.e. sorted alphabetically by station name, and the result values per station in the format `<min>/<mean>/<max>`, rounded to one fractional digit):

```
{Abha=-23.0/18.0/59.2, Abidjan=-16.2/26.0/67.3, Abéché=-10.0/29.4/69.0, Accra=-10.1/26.4/66.4, Addis Ababa=-23.7/16.0/67.0, Adelaide=-27.8/17.3/58.5, ...}
```

## My Progress

I was able to reduce the time 82s avg on my machine for the baseline implementation to 4s avg [code](https://github.com/niv3D/1brc/blob/master/src/CalculateAverage.java)

  ### Overview
  - Split the file into memory-mapped segments
  - Process each segment in parallel by loading a segment into `MappedByteBuffer` using `FileChannel.map` (keep segment size below 2GB(Integer.MAX_LIMIT) because indexing is done with Integer)
  - Parse each line manually to get the bytes for city and temperature
  - Use the city bytes for creating the hash for the hash map
  - Parse the temperature as int and later divide the aggregate accordingly since default implementation of parsing double is heavy and operations on Integer in CPU is faster
  - Aggregate the results per segment
  - Merge per segment results into final `TreeMap` sorted by city name
  ### Further Possible Improvements
  - Implement custom hash map with smaller size instead of default `HashMap`

## Prerequisites
- Java 21 or later
- [JBang](https://www.jbang.dev/) [optional]

## Running the Challenge

This repository contains two programs:
* `src/GenerateMeasurements.java` : Creates the file _measurements.txt_ with a configurable number of random measurement values
* `src/CalculateAverageBaseline.java` : Calculates the average values for the file _measurements.txt_

1. Create the measurements file with 1B rows (just once):
    run the program `GenerateMeasurements.java` natively or using JBang.
    ```
    jbang --runtime-option="-Xmx4g" GenerateMeasurements.java 1000000000
    ```
    give enough memory as -Xmx option if out of memory exception occurs.
    This will take a few minutes.
    **Attention:** the generated file has a size of approx. **12 GB**, so make sure to have enough disk space.

2. Calculate the average measurement values:
    Calculate the average measurement using `CalculateAverageBaseline.java` with the generated `measurements.txt` file.
    The provided naive example implementation uses the Java streams API for processing the file.
3. Optimize
    Create your own copy `CalculateAverageBaseline.java` and optimize it
