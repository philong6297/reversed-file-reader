# reversed-file-reader
Java implementation of a reader which read text file line-by-line (one line at a time),
starting with the last line in the file, and finishing with the first line

## Development and Runtime Environment Specifications

**Developed and tested in:**
- OS: Windows 10 & Ubuntu 22.04
- IDE: VS Code
- Java: 8
- Package manager: [maven](https://maven.apache.org/)

**Maven Plugins:**
- [maven-checkstyle-plugin](https://maven.apache.org/plugins/maven-checkstyle-plugin/) for enforcing [Google Java Style](https://google.github.io/styleguide/javaguide.html) standard.

## Project Structure
```
└── src
  └── main
    ├── java
    │  └── com
    │    └── longlp
    │      ├── Benchmark.java // benchmark execution time
    |      |                   // of ReversedFileReader
    │      └── ReversedFileReader.java
    └── resources
      ├── large.log // large input (~100MB, ~800K lines)
      └── small.txt // small input
```
## Quick Performance Result
With input file [src/main/resources/large.txt](src/main/resources/large.txt):
  - size: ~100MB
  - lines: ~800K

**Hardware Specifications**:
`Windows 10, Intel Xeon E3-1505M @ 2.80Hz (8 CPUs), 16Gb RAM`

**Result**:
- **Execution Time**: ~500ms
- **Memory Usage**: ~5MB

## Building Project
Get the project source codes
```bash
git clone git@github.com:philong6297/reversed-file-reader.git
```

Make sure you have installed:
- [maven](https://maven.apache.org/)
- appropriated JDK & JRE for Java 8

Build the project:
```bash
mvn verify

# Run the benchmark
java -cp ./target/reversed-file-reader-1.0.jar \
          com.longlp.Benchmark \
          [path-to-input-file] \
          print_reversed=[0,1]
```
If you want to use my attached benchmark files, make sure to unarchive the ``input.zip`` file in [src/main/resources](src/main/resources)

For large file, it is recommended to not print the reversed contents to the output:
```
java -cp ./target/reversed-file-reader-1.0.jar \
          com.longlp.Benchmark \
          ./src/main/resources/large.txt
```
For small file, we can easily print them with ``print_reversed=1``
```
java -cp ./target/reversed-file-reader-1.0.jar \
          com.longlp.Benchmark \
          ./src/main/resources/small.txt \
          print_reversed=1
```

## About the solution
We can split the problem into the following main tasks:
- Read the file in reversed order
- Achieve a good performance instead of abusing one-byte-per-access

### Read the file in reversed order
Nothing much to describe in this task, we can just ``seek`` to the end of the file, ignore the EOL then start to read.
### Replace one-byte-per-access
Inspired from [BufferedReader](https://docs.oracle.com/javase/8/docs/api/java/io/BufferedReader.html),
I have used a naive implementation of **buffered accessing**. In summary, my approach is described as follows:
- Store a part of the file into a buffer to reduce the number of direct ``read`` operations from the file
- With the buffer, we can calculate and provide the ``line`` to the output.
- After a `line`` is read, we can re-fill the buffer with the next bytes from the file again.

For convenience, instead of implementing from scratch the entire ``Reader`` class, I have decided to implement a ``ReversedLineInputStream`` so for an adoption-friendly purpose. Thus, we can wrap it in any ``Reader``:
```java
InputStream istream = new ReversedLineInputStream(file);
ReversedFileReader reader = new BufferedReader(new InputStreamReader(istream));
String line = "";
while (true) {
    line = reader.readLine();
    if (line == null) {
        break;
    }
    System.out.println(line);
}
```

### Implementation Caveats
- **Encoding supports**:\
The problem did not mention encoding awareness. Thus, my implementation has not supported these things.
- **Fixed size buffer at 1024x1024**:\
It is not hard to the user-defined buffer size. However, as per ``REQ02: The project will be implemented as a single Java class``, I have decided to simplify my codes as much as possible for reviewing purposes.
- **Synchronization & Atomic operations in file accessing**:
It is not hard to solve this task with C++. However,
due to the lack of in-depth knowledge of Java advanced concepts, I cannot yet try these concepts in time for the test.
