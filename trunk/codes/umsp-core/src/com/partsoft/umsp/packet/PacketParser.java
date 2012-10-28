package com.partsoft.umsp.packet;

import java.io.IOException;

import com.partsoft.umsp.EndPoint;
import com.partsoft.umsp.Parser;
import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.io.BufferPools;
import com.partsoft.umsp.io.ByteBufferInputStream;
import com.partsoft.umsp.io.EofException;
import com.partsoft.umsp.io.View;
import com.partsoft.umsp.log.Log;

public class PacketParser implements Parser {

	public static final int STATE_START = -13;
	public static final int STATE_HEADER = -5;
	public static final int STATE_END = 0;
	public static final int STATE_CONTENT = 2;

	protected int _state = STATE_START;

	private EndPoint _endPoint;

	private BufferPools _buffers;

	protected int _contentBufferSize;

	private View _contentView = new View();

	private EventHandler _handler;

	private Buffer _packet;

	private Buffer _buffer;

	protected int _header_wellbe_filled = Buffer.INT_SIZE;

	protected int _content_wellbe_filled = 0;

	@SuppressWarnings("unused")
	private Input _input;

	public PacketParser(Buffer buffer, EventHandler handler) {
		_packet = buffer;
		_buffer = buffer;
		_handler = handler;
	}

	public PacketParser(BufferPools buffers, EndPoint _endPoint, EventHandler _handler, int contentBufferSize) {
		_header_wellbe_filled = Buffer.INT_SIZE;
		this._buffers = buffers;
		this._endPoint = _endPoint;
		this._handler = _handler;
		this._contentBufferSize = contentBufferSize;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}

	public boolean isComplete() {
		return isState(STATE_END);
	}

	public boolean isMoreInBuffer() {
		if (_packet != null && _packet.hasContent())
			return true;
		return false;
	}

	public boolean isState(int state) {
		return _state == state;
	}

	public boolean isIdle() {
		return isState(STATE_START);
	}

	public Buffer getPacketBuffer() {
		if (_packet == null) {
			_packet = _buffers.getBuffer(_contentBufferSize);
		}
		return _packet;
	}

	public long parseAvailable() throws IOException {
		long len = parseNext();
		long total = len > 0 ? len : 0;

		// continue parsing
		while (!isComplete() && _buffer != null && _buffer.length() > 0) {
			len = parseNext();
			if (len > 0)
				total += len;
			else if (len < 0) {
				throw new EofException();
			}
		}
		return total;
	}

	public long parseNext() throws IOException {
		long total_filled = -1;

		if (_state == STATE_END)
			return -1;

		if (_buffer == null) {
			if (_packet == null) {
				_packet = _buffers.getBuffer(_contentBufferSize);
			}
			_buffer = _packet;
		}

		int length = _buffer.length();

		// Fill buffer if we can
		if (length == 0) {
			int filled = -1;

			if (_buffer.markIndex() == 0 && _buffer.putIndex() == _buffer.capacity())
				throw new IOException("request packet too large");

			IOException ioex = null;

			if (_endPoint != null) {
				// Compress buffer if handling _content buffer
				// TODO check this is not moving data too much
				_buffer.compact();

				if (_buffer.space() == 0)
					throw new IOException("request packet too large");
				try {
					if (total_filled < 0)
						total_filled = 0;
					filled = _endPoint.fill(_buffer, _header_wellbe_filled);
					if (filled > 0) {
						total_filled += filled;
						_header_wellbe_filled -= filled;
						_state = STATE_HEADER;
						if (_header_wellbe_filled <= 0) {
							_header_wellbe_filled = 0;
							_content_wellbe_filled = _buffer.getInt() - Buffer.INT_SIZE;
							if (_content_wellbe_filled < 0)
								throw new PacketException("packet size must greater than zero");
							_state = STATE_CONTENT;
						}
					}
				} catch (IOException e) {
					Log.debug(e);
					ioex = e;
					filled = -1;
				}
			}

			if (filled < 0) {
				if (_state == STATE_CONTENT) {
					if (_buffer.length() > 0) {
						// TODO should we do this here or fall down to main
						// loop?
						Buffer chunk = _buffer.get(_buffer.length());
						_contentView.update(chunk);
					}
					_state = STATE_END;
					return total_filled;
				}
				reset(true);
				throw new EofException(ioex);
			}
			length = _buffer.length();
		}

		while (_state < STATE_END && _header_wellbe_filled > 0) {
			int filled = -1;
			switch (_state) {
			case STATE_START:
				if (length > 0) {
					_state = STATE_HEADER;
				}
				break;
			case STATE_HEADER:
				filled = _endPoint.fill(_buffer, _header_wellbe_filled);
				if (filled > 0) {
					total_filled += filled;
					_header_wellbe_filled -= filled;
				}
				if (_header_wellbe_filled <= 0) {
					_header_wellbe_filled = 0;
					_content_wellbe_filled = _buffer.getInt() - Buffer.INT_SIZE;
					if (_content_wellbe_filled < 0)
						throw new PacketException("packet size must greater than zero");
					_state = STATE_CONTENT;
				}
				break;
			default:
				_state = STATE_CONTENT;
				break;
			}
		} // end of HEADER states loop

		while (_state > STATE_END) {
			int filled = _endPoint.fill(_buffer, _content_wellbe_filled);
			if (filled > 0) {
				total_filled += filled;
				_content_wellbe_filled -= filled;
			} else
				break;
			if (_content_wellbe_filled <= 0) {
				_state = STATE_END;
				Buffer chunk = _buffer.get(_buffer.length());
				_contentView.update(chunk);
				_handler.packetComplete(_contentView.length());
			}
		}
		return total_filled;
	}

	public int getContentReaded() {
		return _contentView.length();
	}

	public int getContentLength() {
		return _contentView.length() - _content_wellbe_filled;
	}

	public void reset(boolean returnBuffers) {
		synchronized (this) {
			_contentView.setGetIndex(_contentView.putIndex());
			_state = STATE_START;
			_header_wellbe_filled = Buffer.INT_SIZE;
			_content_wellbe_filled = 0;
			if (_packet != null) {
				if (_packet.length() == 0) {
					if (_packet != null && returnBuffers)
						_buffers.returnBuffer(_packet);
					_packet = null;
				} else {
					_packet.setMarkIndex(-1);
					_packet.compact();
				}
			}
			_buffer = _packet;
		}
	}

	public abstract static class EventHandler {

		public abstract void packetComplete(long contentLength) throws IOException;

	}

	public static class Input extends ByteBufferInputStream {

		protected PacketParser _parser;

		protected EndPoint _endp;

		protected long _maxIdleTime;

		protected Buffer _content;

		public Input(PacketParser parser, long maxIdleTime) {
			super(parser._contentView);
			_parser = parser;
			_endp = parser._endPoint;
			_maxIdleTime = maxIdleTime;
			_content = _parser._contentView;
			_parser._input = this;
		}

	}
}
