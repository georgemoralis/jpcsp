/*
This file is part of jpcsp.

Jpcsp is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Jpcsp is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */
package jpcsp.HLE;

import jpcsp.Memory;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Utilities;

final public class TPointer implements ITPointerBase {
	private Memory memory;
	private int address;
	public static final TPointer NULL = new TPointer((Memory) null, 0);

	public TPointer(Memory memory, int address) {
		this.memory = memory;
		this.address = address & Memory.addressMask;
	}

	public TPointer(TPointer base) {
		memory = base.getMemory();
		address = base.getAddress();
	}

	public TPointer(TPointer base, int addressOffset) {
		memory = base.getMemory();
		if (base.isNull()) {
			address = 0;
		} else {
			address = base.getAddress() + addressOffset;
		}
	}

	public void add(int addressOffset) {
		if (isNotNull()) {
			address += addressOffset;
		}
	}

	@Override
	public boolean isAddressGood() {
		return Memory.isAddressGood(address);
	}

	@Override
	public boolean isAlignedTo(int offset) {
		return (address % offset) == 0;
	}

	@Override
	public int getAddress() {
		return address;
	}

	public void setAddress(int address) {
		this.address = address & Memory.addressMask;
	}

	@Override
	public Memory getMemory() {
		return memory;
	}

	@Override
	public boolean isNull() {
		return address == 0;
	}

	@Override
	public boolean isNotNull() {
		return address != 0;
	}

	public byte  getValue8() { return getValue8(0); }
	public short getValue16() { return getValue16(0); }
	public int   getValue32() { return getValue32(0); }
	public long  getValue64() { return getValue64(0); }

	public void setValue8(byte value) { setValue8(0, value); }
	public void setValue16(short value) { setValue16(0, value); }
	public void setValue32(int value) { setValue32(0, value); }
	public void setValue32(boolean value) { setValue32(0, value); }
	public void setValue64(long value) { setValue64(0, value); }

	public byte  getValue8(int offset) { return (byte) memory.read8(address + offset); }
	public short getValue16(int offset) { return (short) memory.read16(address + offset); }
	public int   getValue32(int offset) { return memory.read32(address + offset); }
	public long  getValue64(int offset) { return memory.read64(address + offset); }

	public void setValue8(int offset, byte value) { if (isNotNull()) memory.write8(address + offset, value); }
	public void setValue16(int offset, short value) { if (isNotNull()) memory.write16(address + offset, value); }
	public void setValue32(int offset, int value) { if (isNotNull()) memory.write32(address + offset, value); }
	public void setValue32(int offset, boolean value) { if (isNotNull()) memory.write32(address + offset, value ? 1 : 0); }
	public void setValue64(int offset, long value) { if (isNotNull()) memory.write64(address + offset, value); }

	public float getFloat() {
		return getFloat(0);
	}

	public float getFloat(int offset) {
		return Float.intBitsToFloat(getValue32(offset));
	}

	public void setFloat(float value) {
		setFloat(0, value);
	}

	public void setFloat(int offset, float value) {
		setValue32(offset, Float.floatToRawIntBits(value));
	}

	public String getStringZ() {
		return Utilities.readStringZ(memory, address);
	}

	public String getStringNZ(int n) {
		return getStringNZ(0, n);
	}

	public String getStringNZ(int offset, int n) {
		return Utilities.readStringNZ(memory, address + offset, n);
	}

	public void setStringNZ(int n, String s) {
		setStringNZ(0, n, s);
	}

	public void setStringNZ(int offset, int n, String s) {
		Utilities.writeStringNZ(memory, address + offset, n, s);
	}

	public void setStringZ(String s) {
		Utilities.writeStringZ(memory, address, s);
	}

	public byte[] getArray8(int n) {
		return getArray8(0, n);
	}

	public byte[] getArray8(int offset, int n) {
		return getArray8(offset, new byte[n], 0, n);
	}

	public byte[] getArray8(byte[] bytes) {
		if (bytes == null) {
			return bytes;
		}
		return getArray8(0, bytes, 0, bytes.length);
	}

	public byte[] getArray8(int offset, byte[] bytes, int bytesOffset, int n) {
		if (isNotNull()) {
			IMemoryReader memoryReader = MemoryReader.getMemoryReader(getAddress() + offset, n, 1);
			for (int i = 0; i < n; i++) {
				bytes[bytesOffset + i] = (byte) memoryReader.readNext();
			}
		}

		return bytes;
	}

	public void setArray(byte[] bytes) {
		if (bytes != null) {
			setArray(bytes, bytes.length);
		}
	}

	public void setArray(byte[] bytes, int n) {
		setArray(0, bytes, n);
	}

	public void setArray(int offset, byte[] bytes, int n) {
		setArray(offset, bytes, 0, n);
	}

	public void setArray(int offset, byte[] bytes, int bytesOffset, int n) {
		if (isNotNull()) {
			IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(getAddress() + offset, n, 1);
			for (int i = 0; i < n; i++) {
				memoryWriter.writeNext(bytes[bytesOffset + i] & 0xFF);
			}
			memoryWriter.flush();
		}
	}

	public TPointer getPointer() {
		return getPointer(0);
	}

	public TPointer getPointer(int offset) {
		if (isNull()) {
			return TPointer.NULL;
		}

		return new TPointer(getMemory(), getValue32(offset));
	}

	public void setPointer(TPointer value) {
		setPointer(0, value);
	}

	public void setPointer(int offset, TPointer value) {
		if (value == null) {
			setValue32(offset, 0);
		} else {
			setValue32(offset, value.getAddress());
		}
	}

	public void memcpy(int src, int length) {
		memcpy(0, src, length);
	}

	public void memcpy(int offset, int src, int length) {
		if (isNotNull()) {
			memory.memcpy(getAddress() + offset, src, length);
		}
	}

	public void memmove(int src, int length) {
		memmove(0, src, length);
	}

	public void memmove(int offset, int src, int length) {
		if (isNotNull()) {
			memory.memmove(getAddress() + offset, src, length);
		}
	}

	/**
	 * Set "length" bytes to the value "data" starting at the pointer address.
	 * Equivalent to
	 *     Memory.memset(getAddress(), data, length);
	 *
	 * @param data    the byte to be set in memory
	 * @param length  the number of bytes to be set
	 */
	public void memset(byte data, int length) {
		memset(0, data, length);
	}

	/**
	 * Set "length" bytes to the value "data" starting at the pointer address
	 * with the given "offset".
	 * Equivalent to
	 *     Memory.memset(getAddress() + offset, data, length);
	 *
	 * @param offset  the address offset from the pointer address
	 * @param data    the byte to be set in memory
	 * @param length  the number of bytes to be set
	 */
	public void memset(int offset, byte data, int length) {
		if (isNotNull()) {
			memory.memset(getAddress() + offset, data, length);
		}
	}

	/**
	 * Set "length" bytes to the value 0 starting at the pointer address.
	 * Equivalent to
	 *     Memory.memset(getAddress(), 0, length);
	 *
	 * @param length  the number of bytes to be set
	 */
	public void clear(int length) {
		clear(0, length);
	}

	/**
	 * Set "length" bytes to the value 0 starting at the pointer address
	 * with the given "offset".
	 * Equivalent to
	 *     Memory.memset(getAddress() + offset, 0, length);
	 *
	 * @param offset  the address offset from the pointer address
	 * @param length  the number of bytes to be set
	 */
	public void clear(int offset, int length) {
		memset(offset, (byte) 0, length);
	}

	public void setUnalignedValue32(int offset, int value) {
		if (isNotNull()) {
			Utilities.writeUnaligned32(getMemory(), getAddress() + offset, value);
		}
	}

	@Override
	public String toString() {
		return String.format("0x%08X", getAddress());
	}
}
