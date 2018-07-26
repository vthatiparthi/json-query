package io.json;

import java.io.IOException;
import java.io.RandomAccessFile;

public class BufferedRandomAccessFile extends RandomAccessFile {
	byte buffer[];
	int bufEnd = 0;
	int bufPos = 0;
	long realPos = 0;
	int bufSize;
	public BufferedRandomAccessFile(String filename, String mode, int bufsize) throws IOException {
		super(filename, mode);
		invalidate();
		bufSize = bufsize;
		buffer = new byte[bufSize];
	}

	public final int read() throws IOException {
		if (bufPos >= bufEnd) {
			if (fillBuffer() < 0)
				return -1;
		}
		if (bufEnd == 0) {
			return -1;
		} else {
			return buffer[bufPos++];
		}
	}

	private int fillBuffer() throws IOException {
		int n = super.read(buffer, 0, bufSize);
		if (n >= 0) {
			realPos += n;
			bufEnd = n;
			bufPos = 0;
		}
		return n;
	}

	private void invalidate() throws IOException {
		bufEnd = 0;
		bufPos = 0;
		realPos = super.getFilePointer();
	}

	public int read(byte b[], int off, int len) throws IOException {
		int leftover = bufEnd - bufPos;
		if (len <= leftover) {
			System.arraycopy(buffer, bufPos, b, off, len);
			bufPos += len;
			return len;
		}
		for (int i = 0; i < len; i++) {
			int c = this.read();
			if (c != -1)
				b[off + i] = (byte) c;
			else {
				return i;
			}
		}
		return len;
	}

	public long getFilePointer() throws IOException {
		long l = realPos;
		return (l - bufEnd + bufPos);
	}

	public void seek(long pos) throws IOException {
		int n = (int) (realPos - pos);
		if (n >= 0 && n <= bufEnd) {
			bufPos = bufEnd - n;
		} else {
			super.seek(pos);
			invalidate();
		}
	}

	public final String getNextLine() throws IOException {
		String str = null;
		if (bufEnd - bufPos <= 0) {
			if (fillBuffer() < 0) {
				throw new IOException("error in filling buffer!");
			}
		}
		int lineend = -1;
		for (int i = bufPos; i < bufEnd; i++) {
			if (buffer[i] == '\n') {
				lineend = i;
				break;
			}
		}
		if (lineend < 0) {
			StringBuffer input = new StringBuffer(256);
			int c;
			while (((c = read()) != -1) && (c != '\n')) {
				input.append((char) c);
			}
			if ((c == -1) && (input.length() == 0)) {
				return null;
			}
			return input.toString();
		}
		if (lineend > 0 && buffer[lineend - 1] == '\r')
			str = new String(buffer, 0, bufPos, lineend - bufPos - 1);
		else
			str = new String(buffer, 0, bufPos, lineend - bufPos);
		bufPos = lineend + 1;
		return str;
	}
}
