package com.partsoft.umsp.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;

import com.partsoft.umsp.Connection;
import com.partsoft.umsp.Connector;
import com.partsoft.umsp.EndPoint;
import com.partsoft.umsp.Generator;
import com.partsoft.umsp.Handler;
import com.partsoft.umsp.OriginHandler;
import com.partsoft.umsp.io.BufferPools;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.io.EofException;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.packet.PacketParser.Input;

public class PacketConnection implements Connection {

	private static ThreadLocal<PacketConnection> __currentConnection = new ThreadLocal<PacketConnection>();

	private long _timeStamp = System.currentTimeMillis();

	private int _requests;

	private boolean _handling;

	private boolean _destroy;

	private String _protocol;

	protected final Connector _connector;

	protected final PacketGenerator _generator;

	protected final PacketRequest _request;

	protected final PacketResponse _response;

	protected final PacketParser _parser;

	protected final OriginHandler _handler;

	protected final EndPoint _endp;

	protected final BufferPools _bufferPools;

	protected Input _in;

	protected Output _out;

	private long _connectTimestamp;

	public static PacketConnection getCurrentConnection() {
		return __currentConnection.get();
	}

	protected static void setCurrentConnection(PacketConnection connection) {
		__currentConnection.set(connection);
	}

	public PacketConnection(OriginHandler origin, Connector connector, BufferPools bufferPools, EndPoint _endp,
			PacketGenerator _generator) {
		this._handler = origin;
		this._connector = connector;
		this._endp = _endp;
		this._bufferPools = bufferPools;
		this._parser = new PacketParser(connector, _endp, new RequestHandler(), connector.getRequestBufferSize());
		this._generator = _generator;
		this._request = new PacketRequest(this);
		this._response = new PacketResponse(this);
		this._connectTimestamp = System.currentTimeMillis();
	}

	public void reset(boolean returnBuffers) {
		_parser.reset(returnBuffers); // TODO maybe only release when low on
		_generator.reset(returnBuffers); // TODO maybe only release when low on
		_request.recycle();
		_response.recycle();
	}

	public long getConnectTimestamp() {
		return _connectTimestamp;
	}

	public Connector getConnector() {
		return _connector;
	}

	public boolean getResolveNames() {
		return _connector.getResolveNames();
	}

	public String getProtocol() {
		if (_protocol == null) {
			_protocol = _connector != null ? _connector.getProtocol() : "UNKNOWN";
		}
		return _protocol;
	}

	public void handle() throws IOException {

		if (Log.isDebugEnabled()) {
			Log.debug(String.format("start packet connection handle"));
		}

		try {
			boolean more_in_buffer = true;
			boolean dis_connect = false;
			int no_progress = 0;
			if (Log.isDebugEnabled()) {
				Log.debug(String.format("do receive request packet data"));
			}
			while (more_in_buffer) {
				try {
					if (Log.isDebugEnabled() && no_progress > 0) {
						Log.debug("continue receive last request data packet");
					}
					synchronized (this) {
						if (_handling)
							throw new IllegalStateException();
						_handling = true;
					}
					setCurrentConnection(this);
					// 连接第一次请求
					long io = 0;
					if (!_parser.isComplete()) {
						if (Log.isDebugEnabled() && no_progress > 0) {
							Log.debug(String.format("last request data packet not complete to be continue"));
						}
						if (_requests <= 0) {
							handleConnect();
							reset(false);
							_requests++;
							continue;
						} else {
							io = _parser.parseAvailable();
						}
					} else if (Log.isDebugEnabled()) {
						Log.debug(String.format("request data packet was complete"));
					}

					boolean g_commited = _generator.isCommitted();
					boolean g_complete = _generator.isComplete();

					while (g_commited && !g_complete) {
						if (Log.isDebugEnabled()) {
							Log.debug(String.format(
									"last response data packet is not flush, state (commited=%s;complete=%s) ",
									Boolean.toString(g_commited), Boolean.toString(g_complete)));
						}
						long written = _generator.flush();
						io += written;
						if (written <= 0)
							break;
						if (_endp.isBufferingOutput())
							_endp.flush();
					}

					if (_endp.isBufferingOutput()) {
						_endp.flush();
						if (!_endp.isBufferingOutput())
							no_progress = 0;
					}
					if (io > 0)
						no_progress = 0;
					else if (no_progress++ >= 2)
						return;
				} catch (IOException e) {
					if (e instanceof SocketTimeoutException || e.getCause() instanceof SocketTimeoutException
							&& _parser.isIdle()) {
						reset(false);
						handleTimeout();
						reset(false);
					} else {
						reset(true);
						_endp.close();
						dis_connect = true;
						handleDisConnect();
						throw e;
					}
				} finally {
					if (!_endp.isOpen()) {
						more_in_buffer = false;
						if (dis_connect == false) {
							dis_connect = true;
							handleDisConnect();
						}
					}

					setCurrentConnection(null);
					if (Log.isDebugEnabled()) {
						Log.debug("check to see if there more bytes of request data packet?");
					}

					more_in_buffer = more_in_buffer ? _parser.isMoreInBuffer() || _endp.isBufferingInput()
							: more_in_buffer;

					synchronized (this) {
						_handling = false;
						if (_destroy) {
							if (Log.isDebugEnabled()) {
								Log.debug("return by connection has destoried");
							}
							destroy();
							return;
						}
					}

					boolean parser_complete = _parser.isComplete();
					boolean generator_complete = _generator.isComplete();
					if (Log.isDebugEnabled()) {
						Log.debug("check to see if complete packet parse and complete response?");
					}
					if (parser_complete && generator_complete && !_endp.isBufferingOutput()) {
						if (!_generator.isPersistent()) {
							if (Log.isDebugEnabled()) {
								Log.debug("not have next response data, reset packet parser");
							}
							_parser.reset(true);
							more_in_buffer = false;
						}
						if (more_in_buffer) {
							reset(false);
							more_in_buffer = _parser.isMoreInBuffer() || _endp.isBufferingInput();
						} else {
//							_request.updateRequestTime(-1);
							reset(true);
						}
						no_progress = 0;
					}
				}
			}
		} finally {
			if (Log.isDebugEnabled()) {
				Log.debug("end packet connect handle");
			}
		}
	}

	protected void handleTimeout() throws IOException {
		if (_handler.isRunning()) {
			boolean error = false;
			try {
				if (_out != null)
					_out.reopen();
				_connector.customize(_endp, _request);
				_handler.handle(this, Handler.TIMEOUT);
				_request.setHandled(true);
			}  catch (IOException e) {
				_request.setHandled(true);
				error = true;
				throw e;
			} catch (Throwable e) {
				if (e instanceof ThreadDeath)
					throw (ThreadDeath) e;
				Log.error(e.getMessage(), e);
				error = true;
				throw new IOException(e);
			} finally {
				if (_endp.isOpen()) {
					if (_generator.isPersistent()) {
						_connector.persist(_endp);
					}
					if (error) {
						_endp.close();
					}
					_response.complete();
				} else {
					_response.complete();
				}
			}
		}
	}

	protected void handleRequest() throws IOException {
		if (_handler.isRunning()) {
			_request.updateRequestTime(System.currentTimeMillis());
			boolean error = false;
			try {
				if (_out != null)
					_out.reopen();
				_connector.customize(_endp, _request);
				_handler.handle(this, Handler.REQUEST);
			} catch (IOException e) {
				_request.setHandled(true);
				error = true;
				throw e;
			} catch (Throwable e) {
				if (e instanceof ThreadDeath)
					throw (ThreadDeath) e;
				Log.error(e.getMessage(), e);
				error = true;
				throw new IOException(e);
			} finally {
				if (_endp.isOpen()) {
					if (_generator.isPersistent())
						_connector.persist(_endp);
					if (error)
						_endp.close();
					else if (!_response.isCommitted() && !_request.isHandled()) {
						if (Log.isDebugEnabled()) {
							Log.debug(String.format("not found \"%s\" protocol handler for %s",
									_connector.getProtocol(), _connector.getName()));
						}
						_response.errorTerminated();
					}
					_response.complete();
				} else {
					_response.complete();
				}
			}
		}
	}

	protected void handleConnect() throws IOException {
		if (_handler.isRunning()) {
			boolean error = false;
			try {
				if (_out != null)
					_out.reopen();
				_connector.customize(_endp, _request);
				_handler.handle(this, Handler.CONNECT);
			} catch (IOException e) {
				_request.setHandled(true);
				error = true;
				throw new IOException(e);
			} catch (Throwable e) {
				if (e instanceof ThreadDeath)
					throw (ThreadDeath) e;
				Log.error(e.getMessage(), e);
				error = true;
				throw new IOException(e);
			} finally {
				if (_endp.isOpen()) {
					if (_generator.isPersistent()) {
						_connector.persist(_endp);
					}
					if (error) {
						_endp.close();
					}
					_response.complete();
				} else {
					_response.complete(); // 关闭了。。。。做爱做的事
				}
			}
		}
	}

	protected void handleDisConnect() {
		try {
			_handler.handle(this, Handler.DISCONNECT);
			_request.setHandled(true);
		} catch (Throwable e) {
			Log.ignore(e);
		}
	}

	public boolean isIdle() {
		return _generator.isIdle() && _parser.isIdle();
	}

	public boolean isConfidential(PacketRequest request) {
		return _connector.isConfidential(request);
	}

	public long getTimeStamp() {
		return _timeStamp;
	}

	public int getRequests() {
		return _requests;
	}

	public PacketParser getParser() {
		return _parser;
	}

	public PacketGenerator getGenerator() {
		return _generator;
	}

	public boolean isResponseCommitted() {
		return _generator.isCommitted();
	}

	public InputStream getInputStream() {
		if (_in == null)
			_in = new PacketParser.Input(((PacketParser) _parser), _connector.getMaxIdleTime());
		return _in;
	}

	public OutputStream getOutputStream() {
		if (_out == null)
			_out = new Output();
		return _out;
	}

	public void destroy() {
		synchronized (this) {
			_destroy = true;
			if (!_handling) {
				if (_parser != null)
					_parser.reset(true);

				if (_generator != null)
					_generator.reset(true);
			}
		}
	}

	public void completeResponse() throws IOException {
		_generator.complete();
	}

	public void finalCompleteResponse() throws IOException {
		completeResponse();
		_endp.close();
	}

	public void terminateResponse() throws IOException {
		_endp.close();
	}

	public void commitResponse(boolean last) throws IOException {
		if (!_generator.isCommitted()) {
			_generator.complete();
		}
		if (last)
			_generator.complete();
	}

	public void flushResponse() throws IOException {
		try {
			commitResponse(Generator.MORE);
			_generator.flush();
		} catch (IOException e) {
			throw (e instanceof EofException) ? e : new EofException(e);
		}
	}

	@Override
	public String toString() {
		return "PacketConnection [_timeStamp=" + _timeStamp + ", _endp=" + _endp + "]";
	}

	public class RequestHandler extends PacketParser.EventHandler {

		@Override
		public void packetComplete(long contentLength) throws IOException {
			_requests++;
			if (Log.isDebugEnabled()) {
				Log.debug(String.format("complete receivied request data packet(size=%d)", contentLength));
				Log.debug(String.format("packet data: %s",
						new ByteArrayBuffer(_parser.getPacketBuffer().array()).toDetailString()));
			}
			handleRequest();
		}

	}

	public class Output extends AbstractGenerator.Output {
		Output() {
			super((AbstractGenerator) PacketConnection.this._generator, _connector.getMaxIdleTime());
		}

		public void close() throws IOException {
			if (_closed)
				return;

			if (!_generator.isCommitted())
				commitResponse(Generator.LAST);
			else
				flushResponse();

			super.close();
		}

		public void flush() throws IOException {
			if (!_generator.isCommitted())
				commitResponse(Generator.MORE);
			super.flush();
		}

	}

	public PacketRequest getRequest() {
		return _request;
	}

	public PacketResponse getResponse() {
		return _response;
	}

}
