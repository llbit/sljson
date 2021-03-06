/* Copyright (c) 2013-2017, Jesper Öqvist
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package se.llbit.json;

/** Represents a member of a JSON object. */
public class JsonMember implements PrettyPrintable {
  public final String name;
  public final JsonValue value;

  public JsonMember(String name, JsonValue value) {
    this.name = name;
    this.value = value;
  }

  public void prettyPrint(PrettyPrinter out) {
    out.print("\"");
    out.print(getName());
    out.print("\": ");
    out.print(getValue());
  }

  public String toCompactString() {
    return "\"" + JsonString.escape(getName()) + "\":" + getValue().toCompactString();
  }

  @Override public String toString() {
    return "\"" + getName() + "\": " + getValue().toString();
  }

  public String getName() {
    return name;
  }

  public JsonValue getValue() {
    return value;
  }

  /** Create an independent copy of this JSON member. */
  public JsonMember copy() {
    return new JsonMember(name, value.copy());
  }

  @Override public int hashCode() {
    return name.hashCode() ^ value.hashCode();
  }

  /**
   * @return {@code true} if the argument object is a JsonMember with
   * equal name and value to this member.
   */
  @Override public boolean equals(Object obj) {
    if (!(obj instanceof JsonMember)) {
      return false;
    }
    JsonMember other = (JsonMember) obj;
    return (name == other.name || name.equals(other.name))
        && (value == other.value || value.equals(other.value));
  }
}
