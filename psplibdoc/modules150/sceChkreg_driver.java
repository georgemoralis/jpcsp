/* This autogenerated file is part of jpcsp. */
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

package jpcsp.HLE.modules150;

import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

import jpcsp.Memory;
import jpcsp.Processor;

import jpcsp.Allegrex.CpuState; // New-Style Processor

public class sceChkreg_driver implements HLEModule {
	@Override
	public String getName() { return "sceChkreg_driver"; }
	
	@Override
	public void installModule(HLEModuleManager mm, int version) {
		if (version >= 150) {
		
			mm.addFunction(sceChkregCheckRegionFunction, 0x54495B19);
			mm.addFunction(sceChkregGetPsCodeFunction, 0x59F8491D);
			
		}
	}
	
	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		if (version >= 150) {
		
			mm.removeFunction(sceChkregCheckRegionFunction);
			mm.removeFunction(sceChkregGetPsCodeFunction);
			
		}
	}
	
	
	public void sceChkregCheckRegion(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceChkregCheckRegion [0x54495B19]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceChkregGetPsCode(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceChkregGetPsCode [0x59F8491D]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public final HLEModuleFunction sceChkregCheckRegionFunction = new HLEModuleFunction("sceChkreg_driver", "sceChkregCheckRegion") {
		@Override
		public final void execute(Processor processor) {
			sceChkregCheckRegion(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceChkreg_driverModule.sceChkregCheckRegion(processor);";
		}
	};
    
	public final HLEModuleFunction sceChkregGetPsCodeFunction = new HLEModuleFunction("sceChkreg_driver", "sceChkregGetPsCode") {
		@Override
		public final void execute(Processor processor) {
			sceChkregGetPsCode(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceChkreg_driverModule.sceChkregGetPsCode(processor);";
		}
	};
    
};
