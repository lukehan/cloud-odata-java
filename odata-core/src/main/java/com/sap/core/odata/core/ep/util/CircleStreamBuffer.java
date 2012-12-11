package com.sap.core.odata.core.ep.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Circular stream buffer to write/read into/from one single buffer.
 * With support of {@link InputStream} and {@link OutputStream} access to buffered data.
 * 
 * @author d046871
 */
public class CircleStreamBuffer {

  private final static int DEFAULT_CAPACITY = 8192;

  private boolean writeMode = true;
  private boolean writeClosed = false;
  private boolean readClosed = false;

  private Queue<ByteBuffer> bufferQueue = new LinkedBlockingQueue<ByteBuffer>();
  private ByteBuffer currentWriteBuffer;

  private InternalInputStream inStream;
  private InternalOutputStream outStream;

  public CircleStreamBuffer() {
    this(DEFAULT_CAPACITY);
  }

  public CircleStreamBuffer(int bufferSize) {
    ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
    bufferQueue.add(buffer);
    currentWriteBuffer = buffer;
    inStream = new InternalInputStream(this);
    outStream = new InternalOutputStream(this);
  }

  /**
   * Get {@link InputStream} for data read access.
   * 
   * @return
   */
  public InputStream getInputStream() {
    return inStream;
  }

  /**
   * Get {@link OutputStream} for write data.
   * 
   * @return
   */
  public OutputStream getOutputStream() {
    return outStream;
  }

  
  // #############################################
  // #
  // # Common parts
  // #
  // #############################################


  public void closeWrite() {
    this.writeClosed = true;
  }

  public void closeRead() {
    this.readClosed = true;
  }

  private int remaining() throws IOException {
    if (writeMode) {
      return currentWriteBuffer.remaining();
    } else {
      ByteBuffer toRead = getReadBuffer();
      if (toRead == null) {
        return 0;
      }
      return toRead.remaining();
    }
  }
  
  // #############################################
  // #
  // # Reading parts
  // #
  // #############################################

  private ByteBuffer getReadBuffer() throws IOException {
    if(readClosed) {
      throw new IOException("Tried to read from closed stream.");
    }

    boolean next = false;
    ByteBuffer tmp = null;
    if (writeMode) {
      writeMode = false;
      next = true;
    } else {
      tmp = bufferQueue.peek();
      if (tmp != null && !tmp.hasRemaining()) {
        bufferQueue.poll();
        next = true;
      }
    }

    if (next) {
      tmp = bufferQueue.peek();
      if (tmp != null) {
        tmp.flip();
      }
      tmp = getReadBuffer();
    }

    return tmp;
  }

  private int read(byte[] b, int off, int len) throws IOException {
    ByteBuffer toRead = getReadBuffer();
    if (toRead == null) {
      return -1;
    }

    final int remaining = toRead.remaining();
    if (remaining <= len) {
      len = remaining;
    }
    toRead.get(b, off, len);
    return len;
  }


  private int read() throws IOException {
    ByteBuffer readBuffer = getReadBuffer();
    if (readBuffer == null) {
      return -1;
    }

    return readBuffer.get();
  }
  
  // #############################################
  // #
  // # Writing parts
  // #
  // #############################################

  private void write(byte[] data, int off, int len) throws IOException {
    ByteBuffer writeBuffer = getWriteBuffer(len);
    writeBuffer.put(data, off, len);
  }

  private ByteBuffer getWriteBuffer(int size) throws IOException {
    if (writeClosed) {
      throw new IOException("Tried to write into closed stream.");
    }

    if (writeMode) {
      if (remaining() < size) {
        if (size < DEFAULT_CAPACITY) {
          size = DEFAULT_CAPACITY;
        }
        ByteBuffer b = ByteBuffer.allocate(size);
        bufferQueue.add(b);
        currentWriteBuffer = b;
      }

    } else {
      writeMode = true;
      ByteBuffer b = ByteBuffer.allocate(DEFAULT_CAPACITY);
      bufferQueue.add(b);
      currentWriteBuffer = b;
    }

    return currentWriteBuffer;
  }


  private void write(int b) throws IOException {
    ByteBuffer writeBuffer = getWriteBuffer(1);

    byte toWrite = (byte) b;
    writeBuffer.put(toWrite);
  }

  // #############################################
  // #
  // # Inner classes (streams)
  // #
  // #############################################

  /**
   * 
   */
  private static class InternalInputStream extends InputStream {

    private final CircleStreamBuffer inBuffer;

    public InternalInputStream(CircleStreamBuffer csBuffer) {
      this.inBuffer = csBuffer;
    }

    @Override
    public int available() throws IOException {
      return inBuffer.remaining();
    }

    @Override
    public int read() throws IOException {
      return inBuffer.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      return inBuffer.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
      inBuffer.closeRead();
    }
  }

  /**
   * 
   */
  private static class InternalOutputStream extends OutputStream {
    private final CircleStreamBuffer outBuffer;

    public InternalOutputStream(CircleStreamBuffer csBuffer) {
      this.outBuffer = csBuffer;
    }

    @Override
    public void write(int b) throws IOException {
      outBuffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      outBuffer.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
      outBuffer.closeWrite();
    }
  }
}