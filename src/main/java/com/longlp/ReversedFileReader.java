// Copyright 2022 Long Le Phi. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found input the LICENSE file.

package com.longlp;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;

/**
 * A file reader which will read a text file line-by-line (one line at a time), starting with the
 * last line input the file, and finishing with the first line.
 */
public class ReversedFileReader implements Closeable {
    private final BufferedReader readerImpl;

    private class ReversedLineInputStream extends InputStream {

        private static final int MAX_LINE_BYTES = 1024 * 1024;
        private static final int MAX_BUFFER_SIZE = 1024 * 1024;
        private static final byte LF_CHAR = 0xA;
        private static final byte CR_CHAR = 0xD;

        private RandomAccessFile file;
        // mark the next position which needs to be read from the file. < 0 ~ nothing to
        // read. Its supposed to be counted in reverse order, from end to begin of the
        // file.
        private long currentFilePosition;

        // The buffer will store a total of MAX_BUFFER_SIZE bytes from file, served as a
        // cache to avoid one-byte-at-a-time reading cost.
        private byte[] buffer;
        private int currentBufferPosition;

        // Store the data of a "line" in the file, from the buffer. Provide the stored
        // byte to read() method. The access to this member should be in reversed order to retrieve
        // the correct "line" in file.
        private byte[] currentLine;
        private int currentLinePosition;

        // check if whether the line is completely filled from the buffer or not.
        private boolean isLineFilled;

        ReversedLineInputStream(File file) throws IOException {
            currentFilePosition = file.length() - 1;

            this.file = new RandomAccessFile(file, "r");
            this.file.seek(currentFilePosition);

            buffer = new byte[MAX_BUFFER_SIZE];

            // ignore ending char
            if (this.file.readByte() == LF_CHAR) {
                --currentFilePosition;
            }

            currentLine = new byte[MAX_LINE_BYTES];
            currentLine[0] = LF_CHAR;

            currentLinePosition = 0;
            isLineFilled = false;

            fillBufferFromFile();
            fillLineFromBuffer();
        }

        @Override
        public int read() throws IOException {
            // The input stream is ended.
            if (currentFilePosition <= 0 && currentBufferPosition < 0 && currentLinePosition < 0) {
                return -1;
            }

            if (!isLineFilled) {
                fillLineFromBuffer();
            }

            if (isLineFilled) {
                // the "line" is completely read, need to be filled after.
                if (currentLinePosition == 0) {
                    isLineFilled = false;
                }

                byte result = currentLine[currentLinePosition];
                --currentLinePosition;
                return result;
            }

            // null byte
            return 0;
        }

        private void fillBufferFromFile() throws IOException {
            if (currentFilePosition < 0) {
                return;
            }

            // if all the leftovers in file is able to store in the buffer, reduce the
            // reserved size of the buffer space purpose. Then ended the input stream by
            // marking |currentFilePosition|.
            if (currentFilePosition < MAX_BUFFER_SIZE) {
                this.file.seek(0);
                buffer = new byte[(int) currentFilePosition + 1];
                // Ensure all bytes will be read
                // https://stackoverflow.com/questions/28831729/does-randomaccessfile-read-from-local-file-guarantee-that-exact-number-of-byte
                this.file.readFully(buffer);

                currentBufferPosition = (int) currentFilePosition;
                currentFilePosition = -1;
            } else {
                // This is the case where we should store next MAX_BUFFER_SIZE bytes from input
                // file,
                // to the buffer.

                this.file.seek(currentFilePosition - buffer.length);
                // Ensure all bytes will be read
                // https://stackoverflow.com/questions/28831729/does-randomaccessfile-read-from-local-file-guarantee-that-exact-number-of-byte
                this.file.readFully(buffer);

                currentBufferPosition = MAX_BUFFER_SIZE - 1;
                currentFilePosition = currentFilePosition - MAX_BUFFER_SIZE;
            }
        }

        private void fillLineFromBuffer() throws IOException {
            int writePosition = 1;
            while (true) {

                // we've read all the buffer - need to fill it again
                if (currentBufferPosition < 0) {
                    fillBufferFromFile();

                    // nothing was filled - we reached the beginning of a file
                    if (currentBufferPosition < 0) {
                        currentLinePosition = writePosition - 1;
                        isLineFilled = true;
                        return;
                    }
                }

                byte b = buffer[currentBufferPosition];
                --currentBufferPosition;

                // \n is found - line is fully filled
                if (b == LF_CHAR) {
                    currentLinePosition = writePosition - 1;
                    isLineFilled = true;
                    break;

                }

                // just ignore \r for now in case of CRLF
                if (b == CR_CHAR) {
                    continue;
                }

                if (writePosition == MAX_LINE_BYTES) {
                    final String msg =
                            String.format("file has a line exceeding %d bytes", MAX_LINE_BYTES);
                    throw new IOException(msg);

                }

                // write the current line bytes from in reverse order - reading from
                // the end will produce the correct line
                currentLine[writePosition] = b;
                ++writePosition;
            }
        }
    }

    public ReversedFileReader(File file) throws IOException {
        readerImpl = new BufferedReader(new InputStreamReader(new ReversedLineInputStream(file)));
    }

    public String readLine() throws IOException {
        return readerImpl.readLine();
    }

    public void close() throws IOException {
        readerImpl.close();
    }
}
