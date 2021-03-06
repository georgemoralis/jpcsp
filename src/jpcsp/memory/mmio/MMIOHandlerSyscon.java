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
package jpcsp.memory.mmio;

import static jpcsp.HLE.Modules.scePowerModule;
import static jpcsp.HLE.Modules.sceSysconModule;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_CYCLE;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_ELEC;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_FULL_CAP;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_LIMIT_TIME;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_STATUS_CAP;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_TEMP;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_VOLT;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_CTRL_ANALOG_XY_POLLING;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_CTRL_HR_POWER;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_CTRL_LED;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_CTRL_LEPTON_POWER;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_CTRL_WLAN_POWER;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_GET_BARYON;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_GET_KERNEL_DIGITAL_KEY;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_GET_POMMEL_VERSION;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_GET_POWER_STATUS;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_GET_POWER_SUPPLY_STATUS;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_GET_TIMESTAMP;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_READ_ALARM;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_READ_CLOCK;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_READ_SCRATCHPAD;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_RECEIVE_SETPARAM;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_RESET_DEVICE;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_SEND_SETPARAM;
import static jpcsp.HLE.modules.sceSyscon.getSysconCmdName;
import static jpcsp.memory.mmio.MMIOHandlerGpio.GPIO_BIT_SYSCON_END_CMD;

import java.util.Arrays;

import jpcsp.Controller;
import jpcsp.Emulator;
import jpcsp.State;
import jpcsp.HLE.Modules;
import jpcsp.hardware.Battery;
import jpcsp.hardware.LED;
import jpcsp.hardware.Model;
import jpcsp.hardware.UMDDrive;
import jpcsp.util.Utilities;

public class MMIOHandlerSyscon extends MMIOHandlerBase {
	private static MMIOHandlerSyscon instance;
	public static final int BASE_ADDRESS = 0xBE580000;
	public static final int PSP_SYSCON_RX_STATUS = 0;
	public static final int PSP_SYSCON_RX_LEN = 1;
	public static final int PSP_SYSCON_RX_RESPONSE = 2;
	public static final int PSP_SYSCON_TX_CMD = 0;
	public static final int PSP_SYSCON_TX_LEN = 1;
	public static final int PSP_SYSCON_TX_DATA = 2;
	private static final int MAX_DATA_LENGTH = 16;
	private int data[] = new int[MAX_DATA_LENGTH];
	private int dataIndex;
	private boolean endDataIndex;
	private int error;

	public static MMIOHandlerSyscon getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerSyscon(BASE_ADDRESS);
		}
		return instance;
	}

	private MMIOHandlerSyscon(int baseAddress) {
		super(baseAddress);
	}

	private void clearData() {
		Arrays.fill(data, 0);
	}

	private void setDataValue(int offset, int value) {
		data[offset] = value & 0xFF;
	}

	private void addHashValue() {
		int hash = 0;
		int length = data[PSP_SYSCON_RX_LEN];
		for (int i = 0; i < length; i++) {
			hash = (hash + data[i]) & 0xFF;
		}
		data[length] = (~hash) & 0xFF;
	}

	private int[] addResponseData16(int[] responseData, int value) {
		responseData = Utilities.add(responseData, value & 0xFF);
		responseData = Utilities.add(responseData, (value >> 8) & 0xFF);

		return responseData;
	}

	private int[] addResponseData32(int[] responseData, int value) {
		responseData = Utilities.add(responseData, value & 0xFF);
		responseData = Utilities.add(responseData, (value >> 8) & 0xFF);
		responseData = Utilities.add(responseData, (value >> 16) & 0xFF);
		responseData = Utilities.add(responseData, (value >> 24) & 0xFF);

		return responseData;
	}

	private void startSysconCmd() {
		int cmd = data[PSP_SYSCON_TX_CMD];
		if (log.isDebugEnabled()) {
			log.debug(String.format("startSysconCmd cmd=0x%02X(%s), %s", cmd, getSysconCmdName(cmd), this));
		}

		// The default response
		int[] responseData = new int[] { 0x82 };

		switch (cmd) {
			case PSP_SYSCON_CMD_CTRL_LEPTON_POWER:
				UMDDrive.setUmdPower(data[PSP_SYSCON_TX_DATA] != 0);
				break;
			case PSP_SYSCON_CMD_RESET_DEVICE:
				break;
			case PSP_SYSCON_CMD_GET_KERNEL_DIGITAL_KEY:
				Controller controller = State.controller;
				controller.hleControllerPoll();
				int buttons = controller.getButtons();
				responseData = Utilities.add(responseData, ((buttons & 0xF000) >> 8) | ((buttons & 0xF0) >> 4));
				responseData = Utilities.add(responseData, ((buttons & 0xF0000) >> 12) | ((buttons & 0x300) >> 7) | (buttons & 0x9));
				responseData = Utilities.add(responseData, (buttons & 0xBF00000) >> 20);
				responseData = Utilities.add(responseData, (buttons & 0x30000000) >> 28);
				break;
			case PSP_SYSCON_CMD_CTRL_ANALOG_XY_POLLING:
				Modules.sceCtrlModule.setSamplingMode(data[PSP_SYSCON_TX_DATA]);
				break;
			case PSP_SYSCON_CMD_CTRL_LED:
				int flag = data[PSP_SYSCON_TX_DATA];
				boolean setOn;
				int led;
				if (Model.getModel() == Model.MODEL_PSP_GO) {
					setOn = (flag & 0x01) != 0;
					led = flag & 0xF0;
				} else {
					setOn = (flag & 0x10) != 0;
					led = flag & 0xE0;
				}

				switch (led) {
					case 0x40: LED.setLedMemoryStickOn(setOn); break;
					case 0x80: LED.setLedWlanOn(setOn); break;
					case 0x20: LED.setLedPowerOn(setOn); break;
					case 0x10: LED.setLedBluetoothOn(setOn); break;
					default:
						log.warn(String.format("startSysconCmd PSP_SYSCON_CMD_CTRL_LED unknown flag value 0x%02X", flag));
						break;
				}
				break;
			case PSP_SYSCON_CMD_RECEIVE_SETPARAM: {
				int parameterId = 0;
				// Depending on the Baryon version, there is a parameter or not
				if (data[PSP_SYSCON_TX_LEN] >= 3) {
					parameterId = data[PSP_SYSCON_TX_DATA];
				}

				if (log.isDebugEnabled()) {
					log.debug(String.format("startSysconCmd PSP_SYSCON_CMD_RECEIVE_SETPARAM parameterId=0x%X", parameterId));
				}

				// 8 bytes response data:
				// - 2 bytes scePowerGetForceSuspendCapacity() (usually 72)
				// - 6 bytes unknown
				responseData = addResponseData16(responseData, scePowerModule.scePowerGetForceSuspendCapacity());
				for (int i = 2; i < 8; i++) {
					responseData = Utilities.add(responseData, 0);
				}
				break;
			}
			case PSP_SYSCON_CMD_SEND_SETPARAM: {
				int parameterId = 0;
				if (data[PSP_SYSCON_TX_LEN] >= 11) {
					parameterId = data[PSP_SYSCON_TX_DATA + 10];
				}

				int forceSuspendCapacity = data[PSP_SYSCON_TX_DATA + 0];
				forceSuspendCapacity |= data[PSP_SYSCON_TX_DATA + 1] << 8;

				if (log.isDebugEnabled()) {
					log.debug(String.format("startSysconCmd PSP_SYSCON_CMD_SEND_SETPARAM parameterId=0x%X, forceSuspendCapacity=0x%X", parameterId, forceSuspendCapacity));
				}
				break;
			}
			case PSP_SYSCON_CMD_CTRL_HR_POWER: {
				boolean power = data[PSP_SYSCON_TX_DATA] != 0;
				Modules.sceSysconModule.sceSysconCtrlHRPower(power);
				break;
			}
			case PSP_SYSCON_CMD_CTRL_WLAN_POWER: {
				boolean power = data[PSP_SYSCON_TX_DATA] != 0;
				Modules.sceSysconModule.sceSysconCtrlWlanPower(power);
				break;
			}
			case PSP_SYSCON_CMD_GET_POWER_SUPPLY_STATUS:
				responseData = addResponseData32(responseData, sceSysconModule.getPowerSupplyStatus());
				break;
			case PSP_SYSCON_CMD_BATTERY_GET_STATUS_CAP:
				responseData = addResponseData16(responseData, sceSysconModule.getBatteryStatusCap1());
				responseData = addResponseData16(responseData, sceSysconModule.getBatteryStatusCap2());
				break;
			case PSP_SYSCON_CMD_BATTERY_GET_FULL_CAP:
				responseData = addResponseData32(responseData, Battery.getFullCapacity());
				break;
			case PSP_SYSCON_CMD_BATTERY_GET_CYCLE:
				responseData = addResponseData32(responseData, sceSysconModule.getBatteryCycle());
				break;
			case PSP_SYSCON_CMD_BATTERY_GET_LIMIT_TIME:
				responseData = addResponseData32(responseData, sceSysconModule.getBatteryLimitTime());
				break;
			case PSP_SYSCON_CMD_BATTERY_GET_TEMP:
				responseData = addResponseData32(responseData, Battery.getTemperature());
				break;
			case PSP_SYSCON_CMD_BATTERY_GET_ELEC:
				responseData = addResponseData32(responseData, sceSysconModule.getBatteryElec());
				break;
			case PSP_SYSCON_CMD_BATTERY_GET_VOLT:
				responseData = addResponseData32(responseData, Battery.getVoltage());
				break;
			case PSP_SYSCON_CMD_GET_BARYON:
				responseData = addResponseData32(responseData, sceSysconModule._sceSysconGetBaryonVersion());
				break;
			case PSP_SYSCON_CMD_GET_POMMEL_VERSION:
				responseData = addResponseData32(responseData, sceSysconModule.getPommelVersion());
				break;
			case PSP_SYSCON_CMD_GET_POWER_STATUS:
				responseData = addResponseData32(responseData, sceSysconModule.getPowerStatus());
				break;
			case PSP_SYSCON_CMD_GET_TIMESTAMP:
				int[] timeStamp = sceSysconModule.getTimeStamp();
				for (int i = 0; i < timeStamp.length; i++) {
					responseData = Utilities.add(responseData, timeStamp[i] & 0xFF);
				}
				break;
			case PSP_SYSCON_CMD_READ_SCRATCHPAD:
				int src = (data[PSP_SYSCON_TX_DATA] & 0xFC) >> 2;
				int size = 1 << (data[PSP_SYSCON_TX_DATA] & 0x03);
				int values[] = new int[size];
				sceSysconModule.readScratchpad(src, values, size);
				for (int i = 0; i < size; i++) {
					responseData = Utilities.add(responseData, values[i] & 0xFF);
				}
				break;
			case PSP_SYSCON_CMD_READ_CLOCK:
				responseData = addResponseData32(responseData, sceSysconModule.readClock());
				break;
			case PSP_SYSCON_CMD_READ_ALARM:
				responseData = addResponseData32(responseData, sceSysconModule.readAlarm());
				break;
			default:
				log.warn(String.format("startSysconCmd: unknown cmd=0x%02X(%s), %s", cmd, getSysconCmdName(cmd), this));
				break;
		}

		setResponseData(0, responseData, 0, responseData.length);

		endSysconCmd();
	}

	private void endSysconCmd() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("endSysconCmd %s", this));
		}
		MMIOHandlerGpio.getInstance().setPortBit(GPIO_BIT_SYSCON_END_CMD);
	}

	public void setResponseData(int status, int[] responseData, int offset, int length) {
		clearData();
		if (length >= 0 && length <= MAX_DATA_LENGTH - 3) {
			setDataValue(PSP_SYSCON_RX_STATUS, status);
			setDataValue(PSP_SYSCON_RX_LEN, length + 2);
			for (int i = 0; i < length; i++) {
				setDataValue(PSP_SYSCON_RX_RESPONSE + i, responseData[offset + i]);
			}
			addHashValue();
		}
	}

	private int readData16() {
		int value = (data[dataIndex++] << 8) | data[dataIndex++];
		if (dataIndex >= data[PSP_SYSCON_RX_LEN]) {
			endDataIndex = true;
		}
		return value;
	}

	private void writeData16(int value) {
		data[dataIndex++] = (value >> 8) & 0xFF;
		data[dataIndex++] = value & 0xFF;
		if (dataIndex >= MAX_DATA_LENGTH) {
			endDataIndex = true;
		}
	}

	private int getFlags0C() {
		int flags = 0;

		if (endDataIndex) {
			dataIndex = 0;
			endDataIndex = false;
		} else {
			flags |= 4;
		}

		if (error == 0) {
			flags |= 1;
		}

		return flags;
	}

	private void setFlags04(int flags) {
		if ((flags & 4) != 0) {
			dataIndex = 0;
			endDataIndex = false;
		}

		if ((flags & 2) != 0) {
			startSysconCmd();
		} else {
			MMIOHandlerGpio.getInstance().clearPortBit(GPIO_BIT_SYSCON_END_CMD);
		}
	}

	private void setFlags20(int flags) {
		// TODO Unknown flags: clear error status?
		if ((flags & 3) != 0) {
			error = 0;
		}
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x08: value = readData16(); break;
			case 0x0C: value = getFlags0C(); break;
			case 0x18: value = 0; break;
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", Emulator.getProcessor().cpu.pc, address, value));
		}

		return value;
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x00:
				if (value != 0xCF) {
					super.write32(address, value);
				}
				break;
			case 0x04: setFlags04(value); break;
			case 0x08: writeData16(value); break;
			case 0x14:
				if (value != 0) {
					super.write32(address, value);
				}
				break;
			case 0x20: setFlags20(value); break;
			case 0x24:
				if (value != 0) {
					super.write32(address, value);
				}
				break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", Emulator.getProcessor().cpu.pc, address, value, this));
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("MMIOHandlerSyscon dataIndex=0x%X, data: [", dataIndex));
		for (int i = 0; i < MAX_DATA_LENGTH; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(String.format("0x%02X", data[i]));
		}
		sb.append("]");

		return sb.toString();
	}
}
