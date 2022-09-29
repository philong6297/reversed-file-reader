// Copyright 2022 Long Le Phi. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.longlp;

import java.io.File;
import java.io.IOException;

public final class Benchmark {
    private Benchmark() {}

    /**
     * @param args The arguments of the program.
     */
    public static void main(String[] args) {
        final String file_path = args[0];
        final boolean should_print_line = args.length > 1 && args[1].equals("print_reversed=1");

        File file = new File(file_path);

        System.out.println(String.format("Reading %s", file_path));
        System.out
                .println(String.format("Print each line in reversed order: %b", should_print_line));

        long start = System.currentTimeMillis();
        try {
            ReversedFileReader reader = new ReversedFileReader(file);
            String line = "";
            while (true) {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (should_print_line) {
                    System.out.println(line);
                }
            }
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        System.out.println("execution time: " + (end - start) + "ms");

    }
}
