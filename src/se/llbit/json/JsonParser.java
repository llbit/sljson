package se.llbit.json;

import java.io.IOException;
import java.io.InputStream;

import se.llbit.io.LookaheadReader;

/**
 * Parses JSON
 * @author Jesper Öqvist <jesper.oqvist@cs.lth.se>
 */
public class JsonParser {

	private static final char BEGIN_OBJECT = '{';
	private static final char END_OBJECT = '}';
	private static final char BEGIN_ARRAY = '[';
	private static final char END_ARRAY = ']';
	private static final char NAME_SEPARATOR = ':';
	private static final char VALUE_SEPARATOR = ',';
	private static final char[] TRUE = "true".toCharArray();
	private static final char[] FALSE = "false".toCharArray();
	private static final char[] NULL = "null".toCharArray();
	private static final char QUOTE_MARK = '"';
	private static final char ESCAPE = '\\';

	/**
	 * @author Jesper Öqvist <jesper.oqvist@cs.lth.se>
	 */
	@SuppressWarnings("serial")
	public static class SyntaxError extends Exception {
		/**
		 * @param message
		 */
		public SyntaxError(String message) {
			super("Syntax Error: " + message);
		}
	}

	private final LookaheadReader in;

	/**
	 * @param input
	 */
	public JsonParser(InputStream input) {
		in = new LookaheadReader(input, 8);
	}

	/**
	 * Parses a JSON object or array.
	 * @return JsonObject or JsonArray, not null
	 * @throws IOException
	 * @throws SyntaxError
	 */
	public JsonValue parse() throws IOException, SyntaxError {
		skipWhitespace();
		if (in.peek() == BEGIN_OBJECT) {
			return parseObject();
		} else if (in.peek() == BEGIN_ARRAY) {
			return parseArray();
		} else {
			throw new SyntaxError("expected object or array!");
		}
	}

	private JsonArray parseArray() throws IOException, SyntaxError {
		require(BEGIN_ARRAY);
		JsonArray array = new JsonArray();
		while (true) {
			skipWhitespace();
			JsonValue value = parseValue();
			if (value == null) {
				if (array.hasElement() || in.peek() == VALUE_SEPARATOR) {
					throw new SyntaxError("missing element in array");
				}
				break;
			}
			array.addElement(value);
			skipWhitespace();
			if (in.peek() == VALUE_SEPARATOR) {
				require(VALUE_SEPARATOR);
			} else {
				break;
			}
		}
		require(END_ARRAY);
		return array;
	}

	private JsonValue parseValue() throws IOException, SyntaxError {
		if (in.peek() == BEGIN_OBJECT) {
			return parseObject();
		} else if (in.peek() == BEGIN_ARRAY) {
			return parseArray();
		} else if (isNumber()) {
			return parseNumber();
		} else if (isString()) {
			return parseString();
		} else if (acceptLiteral(TRUE)) {
			return new JsonTrue();
		} else if (acceptLiteral(FALSE)) {
			return new JsonFalse();
		} else if (acceptLiteral(NULL)) {
			return new JsonNull();
		} else {
			// TODO use Null Object pattern
			return null; // not a JSON value
		}
	}

	private JsonString parseString() throws IOException, SyntaxError {
		require(QUOTE_MARK);
		StringBuilder sb = new StringBuilder();
		while (true) {
			int next = in.pop();
			if (next == -1) {
				throw new SyntaxError("EOF while parsing JSON string");
			} else if (next == ESCAPE) {
				sb.append(unescapeStringChar());
			} else if (next == QUOTE_MARK) {
				break;
			} else {
				sb.append((char) next);
			}
		}
		return new JsonString(sb.toString());
	}

	private char unescapeStringChar() throws IOException, SyntaxError {
		int next = in.pop();
		switch (next) {
		case QUOTE_MARK:
		case ESCAPE:
		case '/':
			return (char) next;
		case 'b':
			return '\b';
		case 'f':
			return '\f';
		case 'n':
			return '\n';
		case 'r':
			return '\r';
		case 't':
			return '\t';
		case 'u':
			int u1 = hexdigit();
			int u2 = hexdigit();
			int u3 = hexdigit();
			int u4 = hexdigit();
			return (char) ((u1 << 12) | (u2 << 8) | (u3 << 4) | u4);
		case -1:
			throw new SyntaxError("EOF while parsing JSON string");
		default:
			throw new SyntaxError("illegal escape sequence in JSON string");
		}
	}

	private int hexdigit() throws IOException, SyntaxError {
		int next = in.pop();
		if (next >= '0' && next <= '9') {
			return next - '0';
		} else if (next >= 'A' && next <= 'F') {
			return next - 'A' + 0xA;
		} else if (next >= 'a' && next <= 'f') {
			return next - 'a' + 0xA;
		} else {
			throw new SyntaxError("non-hexadecimal digit in unicode escape sequence");
		}
	}

	private JsonValue parseNumber() throws IOException, SyntaxError {
		StringBuilder sb = new StringBuilder();
		while (true) {
			int peek = in.peek();
			if (peek == -1) {
				throw new SyntaxError("EOF while parsing JSON number");
			} else if (isDigit(peek)) {
				sb.append((char) in.pop());
			} else {
				return new JsonNumber(sb.toString());
			}
		}
	}

	private boolean isDigit(int chr) {
		return (chr >= '0' && chr <= '9') || chr == '.' || chr == 'e' ||
			chr == 'E' || chr == '-' || chr == '+';
	}

	private void skipWhitespace() throws IOException {
		while (isWhitespace(in.peek())) {
			in.pop();
		}
	}

	private boolean isWhitespace(int chr) {
		return chr == 0x20 || chr == 0x09 || chr == 0x0A || chr == 0x0D;
	}

	private boolean isNumber() throws IOException {
		int peek = in.peek();
		return (peek >= '0' && peek <= '9') || peek == '-' || peek == '+';
	}

	private boolean isString() throws IOException {
		return in.peek() == '"';
	}

	private boolean acceptLiteral(char[] literal) throws IOException {
		for (int i = 0; i < literal.length; ++i) {
			if (in.peek(i) != literal[i]) {
				return false;
			}
		}
		in.skip(literal.length);
		return true;
	}

	private JsonObject parseObject() throws IOException, SyntaxError {
		require(BEGIN_OBJECT);
		JsonObject object = new JsonObject();
		while (true) {
			skipWhitespace();
			JsonMember member = parseMember();
			if (member == null) {
				if (object.hasMember() || in.peek() == VALUE_SEPARATOR) {
					throw new SyntaxError("missing member in object");
				}
				break;
			}
			object.addMember(member);
			skipWhitespace();
			if (in.peek() == VALUE_SEPARATOR) {
				require(VALUE_SEPARATOR);
			} else {
				break;
			}
		}
		require(END_OBJECT);
		return object;
	}

	private void require(char c) throws IOException, SyntaxError {
		int next = in.pop();
		if (next == -1) {
			throw new SyntaxError("encountered EOF while expecting '" + c + "'");
		}
		if (next != c) {
			throw new SyntaxError("unexpected character (was '" +
				((char) next) + "', expected '" + c + "')");
		}
	}

	private JsonMember parseMember() throws IOException, SyntaxError {
		if (isString()) {
			JsonString name = parseString();
			skipWhitespace();
			require(NAME_SEPARATOR);
			skipWhitespace();
			JsonValue value = parseValue();
			if (value == null) {
				throw new SyntaxError("missing value for object member");
			}
			return new JsonMember(name.getValue(), value);
		} else {
			return null;
		}
	}
}
