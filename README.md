# resvered-file-reader
A Java implementation of a reader which read text file line-by-line (one line at a time),
starting with the last line in the file, and finishing with the first line

## Development and runtime environment specifications

Developed and tested in:
- OS: Windows 10 & Ubuntu 22.04
- IDE: VS Code
- Java: 8
- Package manager: [maven](https://maven.apache.org/)

Third parties:
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

## Building project
Get the project source codes
```bash
git clone git@github.com:philong6297/reversed-file-reader.git
```

Make sure you have installed:
- [maven](https://maven.apache.org/)

Build the project with CMake and Ninja:
```bash
# create a build environment
mkdir build
cd build

# Configure project and install third parties at `build` folder
cmake -DCMAKE_BUILD_TYPE=Release \
      -DCMAKE_TOOLCHAIN_FILE=external/vcpkg/scripts/buildsystems/vcpkg.cmake \
      -DCMAKE_CXX_STANDARD=17 \
      -G Ninja \
      ..


# Build the project
cmake --build . --config Release

# Run the executable
./order-book-watcher

## About the solution
After some manual tests, I came up with these assumptions:
- input files are formatted in JSON Lines, each line is either an `Order Book status` or a successful `Trade record` with corresponding information
- the data is well-structured, there is no need to validate them
- based on the problem description, I can know that:
  - the `CANCEL` cases only happen when there is no `Trade record` in between.
  - by comparing the previous and next `Order Book status`, with additional information from `Trade record`, I can classify the `AGGRESSIVE` order in which side.

For program parallelizing task, I have observed that to classify the order, I only need the information of the previous and next `Order Book status` with `Trade record`. Therefore, it is reasonable to pipeline the parsing phase with classification phase:
- Parse each line from input file
- Get the ``symbol`` field, assign it to a worker (``InstrumentFeedsWorker``).
- Create a task that will let this worker record the previous `Order Book status` (also the `Trade record` in-between if any), compare with the current `Order Book status` which is parsed from current line, then log the classified result to ``<symbol>.txt`` file.
- Post this task to a thread pool, minimize task working time and thread usage.

However we must ensure the sequential between tasks with the same `symbol`, the task which is recorded first must be processed first.

With these requirements, I don't think it is neccessary to re-implement from scratch the thread pool with prioriy handling (or dependency graph). Therefore, I have used [taskflow](https://github.com/taskflow/taskflow) for high performance parallel computing.

In conclusion, my approach for this problem is using multi-threading with priority, on ``symbol`` level.

This is a snapshot for runtime execution from my local machine - `Windows 10, LLVM Clang 14, Release build, Intel Xeon E3-1505M @ 2.80Hz (8 CPUs), 16Gb RAM` :
![snapshop](/data/snapshop.PNG)

I have also setup a basic CI/CD flow with Github Actions for Linux, MacOS and Windows environme
