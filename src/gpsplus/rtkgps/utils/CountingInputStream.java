package gpsplus.rtkgps.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

public final class CountingInputStream extends FilterInputStream {

    private long count;
    private long mark = -1;

    /**
     * Wraps another input stream, counting the number of bytes read.
     *
     * @param in the input stream to be wrapped
     */
    public CountingInputStream(@Nullable InputStream in) {
      super(in);
    }

    /** Returns the number of bytes read. */
    public long getCount() {
      return count;
    }

    @Override public int read() throws IOException {
      int result = in.read();
      if (result != -1) {
        count++;
      }
      return result;
    }

    @Override public int read(byte[] b, int off, int len) throws IOException {
      int result = in.read(b, off, len);
      if (result != -1) {
        count += result;
      }
      return result;
    }

    @Override public long skip(long n) throws IOException {
      long result = in.skip(n);
      count += result;
      return result;
    }

    @Override public synchronized void mark(int readlimit) {
      in.mark(readlimit);
      mark = count;
      // it's okay to mark even if mark isn't supported, as reset won't work
    }

    @Override public synchronized void reset() throws IOException {
      if (!in.markSupported()) {
        throw new IOException("Mark not supported");
      }
      if (mark == -1) {
        throw new IOException("Mark not set");
      }

      in.reset();
      count = mark;
    }
  }